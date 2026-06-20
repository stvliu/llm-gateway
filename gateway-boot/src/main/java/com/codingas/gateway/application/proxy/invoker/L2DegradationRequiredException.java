package com.codingas.gateway.application.proxy.invoker;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;

/**
 * L2 模型降级重路由信号异常
 *
 * <p>{@link ChannelFailoverInvoker} 在 L1 候选全部耗尽且 L2 降级门禁开启时，调用
 * {@code DegradationService.degrade} 获取 fallback 模型。拿到 fallback 后抛出本异常
 * （携带 fallbackModel + 原始失败上下文 cause），由上层 {@code ChatDispatchServiceImpl}
 * 捕获并用 fallback 模型重新 {@code resolveCandidates} + 调用 {@link ChannelFailoverInvoker}。</p>
 *
 * <p><b>显式化决策（兑现 Task 3.3 技术债）</b>：本异常替代 3.3 的隐式契约
 * （ProviderException.model 字段 + {@code L2_DEGRADATION_PREFIX} 字符串前缀），
 * 为显式类型化信号，消除字符串前缀识别的脆弱性。继承 {@link RuntimeException}
 * （非 ProviderException），语义为"重路由信号"而非"上游失败"，避免 ProviderException.model
 * 字段语义重载（既表示失败模型又表示 fallback 模型）。原始上游失败作为 {@link #getCause()}
 * 保留，防递归或深度超限时由调用方解包抛出以保留原始失败上下文。</p>
 */
public class L2DegradationRequiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 降级目标模型名（fallback），上层用此值重新路由 */
    private final String fallbackModel;
    /** 原始模型名（触发降级的模型），用于日志与防递归追踪 */
    private final String originalModel;
    /** 最后一次上游失败的错误类型（degrade 的 reason 入参），用于日志追踪 */
    private final ProviderErrorType lastErrorType;

    /**
     * 构造 L2 降级重路由信号
     *
     * @param fallbackModel  降级目标模型名（非 null）
     * @param originalModel  原始模型名（非 null）
     * @param lastErrorType  最后一次上游失败错误类型
     * @param cause          原始上游失败异常（ProviderException），保留失败上下文供防递归/深度超限时解包抛出
     */
    public L2DegradationRequiredException(String fallbackModel, String originalModel,
                                          ProviderErrorType lastErrorType, Throwable cause) {
        super("L2 降级：模型 " + originalModel + " → " + fallbackModel, cause);
        this.fallbackModel = fallbackModel;
        this.originalModel = originalModel;
        this.lastErrorType = lastErrorType;
    }

    /**
     * @return 降级目标模型名（fallback），上层用此值重新 resolveCandidates
     */
    public String getFallbackModel() {
        return fallbackModel;
    }

    /**
     * @return 原始模型名（触发降级的模型）
     */
    public String getOriginalModel() {
        return originalModel;
    }

    /**
     * @return 最后一次上游失败错误类型
     */
    public ProviderErrorType getLastErrorType() {
        return lastErrorType;
    }
}
