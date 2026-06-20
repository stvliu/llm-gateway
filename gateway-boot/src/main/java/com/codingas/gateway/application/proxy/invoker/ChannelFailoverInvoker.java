package com.codingas.gateway.application.proxy.invoker;

import com.codingas.gateway.application.degradation.DegradationService;
import com.codingas.gateway.application.proxy.failover.ErrorClassifier;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.enums.FailoverDecision;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 渠道级故障转移 Invoker（L1 候选内逐个试 + L2 模型降级）
 *
 * <p>遍历候选渠道列表（同模型不同渠道，已按 priority 升序），对每个候选调用
 * {@link KeyFailoverInvoker}（内部跑 L0 Key 级转移）。捕获 ProviderException 后
 * 经 {@link ErrorClassifier} 分类决定转移层级（D3/D5/深化点5）：</p>
 *
 * <ul>
 *   <li>NONE（请求级错误如 INVALID_REQUEST）：直接抛出，不试下一候选也不降级</li>
 *   <li>L1（共因故障如 AUTH/RATE_LIMIT/NETWORK）：换下一候选，全部耗尽后进 L2</li>
 *   <li>L2（模型能力问题如 UNKNOWN_ERROR）：换下一候选，全部耗尽后进 L2</li>
 * </ul>
 *
 * <p><b>L1 全耗尽才进 L2</b>：候选列表均为同模型不同渠道（L1 层级），任一候选失败
 * 无论 classify 返回 L1 还是 L2，均继续试下一候选；仅当全部候选耗尽且最后的决策非 NONE，
 * 才进入 L2 模型降级。</p>
 *
 * <p><b>L2 降级实现决策</b>：L1 耗尽后，若画像门禁 {@code enableL2ModelDegradation} 开启，
 * 调用 {@link DegradationService#degrade} 获取备选模型名。拿到 fallback 后，抛出携带
 * fallback 模型名的 {@link ProviderException}（model 字段设为 fallback），由上层调用方
 * 重新路由。本 Invoker 签名不含路由参数（无 userId/role/strategy），且按 plan 约定
 * "不自己调 resolveCandidates"，故无法在内部完成跨模型重路由，交由上层
 * （ChatDispatchService）识别异常 model 字段后重新解析候选并再次调用本 Invoker。</p>
 *
 * <p><b>L2 隐式契约（临时技术债，3.6 替换）</b>：当前 L2 降级信号通过 ProviderException 的
 * model 字段（=fallback）+ message 的 {@link #L2_DEGRADATION_PREFIX} 前缀向上层传递，
 * 上层靠字符串前缀识别，较脆弱。此为临时隐式实现，Task 3.6 将替换为显式
 * {@code L2DegradationRequiredException} 异常类，届时移除前缀常量与字符串拼接。
 * 另：tryL2Degradation 对 {@code DegradationService.degrade} 做了异常防御（该实现违背
 * 接口契约"无可用备选返回 null"实际抛异常），详见方法 javadoc。</p>
 *
 * <p><b>签名说明</b>：plan 原签名含 {@code ResilienceProfile profile}，因 ResilienceProfile
 * 类 P2 才建（当前不存在），3.3 暂用 {@code boolean enableL2ModelDegradation} 占位，
 * P2 建画像后改为 ResilienceProfile。</p>
 */
@Component
public class ChannelFailoverInvoker {

    private static final Logger log = LoggerFactory.getLogger(ChannelFailoverInvoker.class);

    /**
     * L2 降级信号前缀：携带 fallback 模型名的 ProviderException 的 message 以此前缀开头。
     *
     * <p><b>临时隐式契约（技术债）</b>：当前通过 ProviderException 的 model 字段（=fallback）+
     * message 的 {@value #L2_DEGRADATION_PREFIX} 前缀向上层（ChatDispatchService）传递 L2 降级信号。
     * 此隐式契约较脆弱（上层靠字符串前缀识别），Task 3.6 将替换为显式
     * {@code L2DegradationRequiredException} 异常类，届时移除本前缀常量及 tryL2Degradation
     * 中的字符串拼接。</p>
     */
    public static final String L2_DEGRADATION_PREFIX = "L2_DEGRADATION_REQUIRED:";

    private final KeyFailoverInvoker keyFailoverInvoker;
    private final ErrorClassifier errorClassifier;
    private final DegradationService degradationService;

    /**
     * 构造渠道级故障转移 Invoker
     *
     * @param keyFailoverInvoker  Key 级故障转移 Invoker（L0，对每个候选内部跑）
     * @param errorClassifier     错误分流器（L1/L2/NONE 决策）
     * @param degradationService  智能降级服务（L2 换模型）
     */
    public ChannelFailoverInvoker(KeyFailoverInvoker keyFailoverInvoker,
                                   ErrorClassifier errorClassifier,
                                   DegradationService degradationService) {
        this.keyFailoverInvoker = keyFailoverInvoker;
        this.errorClassifier = errorClassifier;
        this.degradationService = degradationService;
    }

    /**
     * 非流式调用 — L1 候选内逐个试 + L2 模型降级
     *
     * <p>遍历 candidates（已按 priority 升序），对每个候选调 {@link KeyFailoverInvoker#invoke}：
     * 成功返回；失败按 {@link ErrorClassifier#classify} 分类——NONE 直接抛，
     * L1/L2 换下一候选。全部候选耗尽后，若 enableL2ModelDegradation 开启，调
     * {@link DegradationService#degrade} 获取 fallback 模型；拿到则抛携带 fallback
     * 模型名的 ProviderException 让上层重路由，否则抛最后捕获的异常。</p>
     *
     * @param primaryCtx               主路由上下文（候选列表首项，用于日志/审计锚点）
     * @param candidates               按 priority 升序的候选路由上下文列表（由调用方传入）
     * @param request                  协议请求
     * @param inboundProtocol          入站协议
     * @param applicationId            应用 ID（权限锚点）
     * @param enableL2ModelDegradation L2 模型降级门禁（ResilienceProfile 占位，P2 替换）
     * @return 上游响应
     * @throws ProviderException INVALID_REQUEST 等请求级错误直接抛出；
     *                           L2 降级成功时抛出携带 fallback 模型名（getModel()）的异常由上层重路由；
     *                           所有候选失败且无法降级时抛出最后捕获的异常
     */
    public ProtocolResponse invoke(RoutingContext primaryCtx, List<RoutingContext> candidates,
                                    ProtocolRequest request, Protocol inboundProtocol, Long applicationId,
                                    boolean enableL2ModelDegradation) {
        ProviderException lastException = null;
        ProviderErrorType lastErrorType = null;

        for (RoutingContext candidate : candidates) {
            try {
                return keyFailoverInvoker.invoke(candidate, request);
            } catch (ProviderException e) {
                FailoverDecision decision = errorClassifier.classify(e.getErrorType());
                log.warn("候选渠道 channelId={} endpointId={} 失败: {} (决策:{}), 尝试下一候选",
                        candidate.channelId(), candidate.channelEndpointId(),
                        e.getErrorType(), decision);

                // NONE：请求级错误（如 INVALID_REQUEST），换哪都无效，直接抛出不转移
                if (decision == FailoverDecision.NONE) {
                    throw e;
                }

                // L1/L2：记录失败，继续试下一候选（L1 全耗尽才进 L2）
                lastException = e;
                lastErrorType = e.getErrorType();
            }
        }

        // L1 候选全部耗尽，进入 L2 模型降级
        ProviderException l2Exception = tryL2Degradation(request, lastErrorType, enableL2ModelDegradation);
        if (l2Exception != null) {
            throw l2Exception;
        }

        // L2 未触发或 degrade 返回 null：抛最后捕获的异常
        if (lastException != null) {
            throw lastException;
        }
        // 防御性兜底：候选列表非空但无成功无异常（理论不可达）
        throw new ProviderException(ProviderErrorType.UNKNOWN_ERROR,
                "候选列表处理完成但无结果：candidates=" + (candidates == null ? 0 : candidates.size()));
    }

    /**
     * 流式调用 — L1 候选内逐个试（首字节前可转移，首字节后不换渠道）
     *
     * <p>遍历 candidates，对每个候选调 {@link KeyFailoverInvoker#invokeStream}。
     * 包装传入的 callback 追踪首字节是否已发送（首次 {@link StreamCallback#onChunk} 标记）。
     * 首字节前同步启动失败（invokeStream 抛 ProviderException 且 onChunk 未触发）按分类转移：
     * NONE 直接抛，L1/L2 换下一候选。若 onChunk 已触发（首字节已发给客户端），catch 块检查
     * firstByteSent 后直接抛出不换候选（避免重复首字节）。一旦 invokeStream 正常 return 表示
     * 流已建立，ChannelFailoverInvoker 即返回；后续首字节前异步失败或首字节后失败均通过
     * wrappedCallback 转发原 callback 的 onError，不再换渠道（继承 KeyFailoverInvoker
     * "传输开始后不切换"约束）。</p>
     *
     * <p>全部候选启动失败后，若 enableL2ModelDegradation 开启，调 degrade 获取 fallback，
     * 拿到则抛携带 fallback 的异常让上层重路由，否则抛最后异常。</p>
     *
     * @param primaryCtx               主路由上下文
     * @param candidates               按 priority 升序的候选列表
     * @param request                  协议请求
     * @param inboundProtocol          入站协议
     * @param applicationId            应用 ID
     * @param enableL2ModelDegradation L2 门禁（ResilienceProfile 占位）
     * @param callback                 流式回调
     * @throws ProviderException INVALID_REQUEST 等请求级错误直接抛出；
     *                           L2 降级成功时抛出携带 fallback 模型名的异常由上层重路由；
     *                           所有候选启动失败且无法降级时抛出最后捕获的异常
     */
    public void invokeStream(RoutingContext primaryCtx, List<RoutingContext> candidates,
                              ProtocolRequest request, Protocol inboundProtocol, Long applicationId,
                              boolean enableL2ModelDegradation, StreamCallback callback) {
        ProviderException lastException = null;
        ProviderErrorType lastErrorType = null;

        for (RoutingContext candidate : candidates) {
            // 首字节追踪标志：包装 callback 标记首字节是否已发送
            // 首字节前同步启动失败可换候选；首字节后失败不换候选（继承 KeyFailoverInvoker 约束）
            AtomicBoolean firstByteSent = new AtomicBoolean(false);
            StreamCallback wrappedCallback = new StreamCallback() {
                @Override
                public void onChunk(String data) {
                    // 首次 onChunk 标记首字节已发送
                    firstByteSent.set(true);
                    callback.onChunk(data);
                }

                @Override
                public void onComplete() {
                    callback.onComplete();
                }

                @Override
                public void onError(Throwable t) {
                    callback.onError(t);
                }
            };

            try {
                // KeyFailoverInvoker.invokeStream 正常 return 表示流已建立（enqueue 成功）
                // 首字节前的异步失败（onError 在 onChunk 前）和首字节后失败均通过 wrappedCallback
                // 转发原 callback，ChannelFailoverInvoker 已返回不再换候选
                keyFailoverInvoker.invokeStream(candidate, request, wrappedCallback);
                return;
            } catch (ProviderException e) {
                // 首字节已发送：不换候选，直接抛传播给调用方（首字节后转移边界）
                // 客户端已收到首字节，换候选重发会导致重复首字节，故直接终止
                if (firstByteSent.get()) {
                    throw e;
                }

                // 首字节前同步启动失败：按 L1/L2/NONE 分流换候选
                FailoverDecision decision = errorClassifier.classify(e.getErrorType());
                log.warn("流式候选渠道 channelId={} endpointId={} 启动失败: {} (决策:{}), 尝试下一候选",
                        candidate.channelId(), candidate.channelEndpointId(),
                        e.getErrorType(), decision);

                // NONE：请求级错误，直接抛出不转移
                if (decision == FailoverDecision.NONE) {
                    throw e;
                }

                // L1/L2：记录失败，继续试下一候选（首字节前失败可转移）
                lastException = e;
                lastErrorType = e.getErrorType();
            }
        }

        // L1 候选全部启动失败，进入 L2 模型降级
        ProviderException l2Exception = tryL2Degradation(request, lastErrorType, enableL2ModelDegradation);
        if (l2Exception != null) {
            throw l2Exception;
        }

        // L2 未触发或 degrade 返回 null：抛最后捕获的异常
        if (lastException != null) {
            throw lastException;
        }
        throw new ProviderException(ProviderErrorType.UNKNOWN_ERROR,
                "流式候选列表处理完成但无结果：candidates=" + (candidates == null ? 0 : candidates.size()));
    }

    /**
     * 尝试 L2 模型降级
     *
     * <p>L1 候选全部耗尽后调用。若门禁开启且存在失败原因，调用 degrade 获取 fallback 模型。
     * 拿到 fallback 则构造携带 fallback 模型名的 ProviderException（由上层重路由）；
     * 否则返回 null（由调用方抛最后异常）。</p>
     *
     * <p><b>degrade 契约防御</b>：{@link DegradationService#degrade} 接口 javadoc 声明
     * "无可用备选返回 null"，但 {@code DegradationServiceImpl} 在"有链但所有备选不可用"时
     * 违背契约抛出 {@code ProviderException("ALL_MODELS_DEGRADED:...")}（既有问题，非本任务范围）。
     * 本方法 try-catch 包裹 degrade 调用，捕获后返回 null，让调用方抛 lastException 保留
     * 原始失败上下文，避免 degrade 异常传播丢失上下文。</p>
     *
     * @param request                  协议请求（用于读取原模型名）
     * @param lastErrorType            最后一次失败的错误类型（degrade 的 reason 参数）
     * @param enableL2ModelDegradation L2 门禁
     * @return 携带 fallback 模型名的降级异常（上层重路由）；未降级或 degrade 抛异常时返回 null
     */
    private ProviderException tryL2Degradation(ProtocolRequest request, ProviderErrorType lastErrorType,
                                                boolean enableL2ModelDegradation) {
        if (!enableL2ModelDegradation || lastErrorType == null) {
            return null;
        }
        String originalModel = request.getModel();
        String fallbackModel;
        try {
            // 防御 DegradationServiceImpl 违背 DegradationService.degrade 接口契约
            // （"无可用备选返回 null" vs 实际抛 ProviderException），捕获后返回 null
            // 让调用方抛 lastException 保留原始失败上下文
            fallbackModel = degradationService.degrade(originalModel, lastErrorType);
        } catch (ProviderException e) {
            log.warn("DegradationService.degrade 违背接口契约抛异常（忽略），回退到 lastException: model={} reason={} msg={}",
                    originalModel, lastErrorType, e.getMessage());
            return null;
        }
        if (fallbackModel == null) {
            log.warn("L2 降级失败：模型 {} 无可用备选，将抛出最后异常", originalModel);
            return null;
        }
        log.info("L1 候选全部耗尽，模型 {} 降级为 {}，抛出降级异常由上层重路由",
                originalModel, fallbackModel);
        // 抛出携带 fallback 模型名的异常（model 字段），由上层识别并用 fallback 重新路由。
        // message 前缀 L2_DEGRADATION_PREFIX 为临时隐式契约，3.6 替换为显式异常类。
        return new ProviderException(lastErrorType,
                L2_DEGRADATION_PREFIX + " 模型 " + originalModel + " 降级为 " + fallbackModel
                        + "，请上层用 fallback 模型重新路由",
                null, fallbackModel, null, null, null);
    }
}
