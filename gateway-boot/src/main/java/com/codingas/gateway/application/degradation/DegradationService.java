package com.codingas.gateway.application.degradation;

import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;

/**
 * 智能降级服务接口
 *
 * <p>当上游模型连续失败时，根据降级链自动切换到备选模型。</p>
 */
public interface DegradationService {

    /**
     * 获取备选模型（无画像门禁，向后兼容）
     *
     * <p>不传 {@link ResilienceProfile} 时走无门禁旧逻辑：不按 errorType 分流，
     * 深度上限取 {@code gateway.degradation.max-chain-depth} 配置。委托
     * {@link #degrade(String, ProviderErrorType, ResilienceProfile)} 传 null profile。</p>
     *
     * @param originalModel 原模型名称
     * @param reason        失败原因
     * @return 降级链中第一个可用的备选模型，无可用备选时返回 null
     */
    String degrade(String originalModel, ProviderErrorType reason);

    /**
     * 获取备选模型（受画像门禁，Task 4.8）
     *
     * <p>L2 模型降级受 {@link ResilienceProfile} 画像门禁控制：</p>
     * <ul>
     *   <li>{@code profile == null}：回退无门禁旧逻辑（向后兼容）</li>
     *   <li>{@code !profile.enableL2ModelDegradation}：返回 null（画像关闭 L2 降级）</li>
     *   <li>{@code profile.degradationMaxDepth == 0}：返回 null（深度 0 禁用降级）</li>
     *   <li>按 errorType 分流：经 {@code ErrorClassifier} 判定为 L2 的 errorType 才触发模型降级，
     *       L1/NONE 类错误（共因故障、请求级错误）不换模型返回 null</li>
     *   <li>{@code profile.degradationMaxDepth} 覆盖备选遍历深度上限</li>
     * </ul>
     *
     * @param originalModel 原模型名称
     * @param reason        失败原因（按 errorType 分流判定是否触发模型降级）
     * @param profile       容灾画像（L2 门禁；为 null 时回退无门禁逻辑）
     * @return 降级链中第一个可用的备选模型；门禁关闭、分流不匹配或无可用备选时返回 null
     */
    String degrade(String originalModel, ProviderErrorType reason, ResilienceProfile profile);

    /**
     * 检查原模型是否已恢复
     */
    boolean canRecover(String model);

    /**
     * 定期健康检查任务，恢复后标记模型可用
     */
    void recoveryCheck();
}
