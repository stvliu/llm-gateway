package com.codingas.gateway.application.degradation;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;

/**
 * 智能降级服务接口
 *
 * <p>当上游模型连续失败时，根据降级链自动切换到备选模型。</p>
 */
public interface DegradationService {

    /**
     * 获取备选模型
     *
     * @param originalModel 原模型名称
     * @param reason        失败原因
     * @return 降级链中第一个可用的备选模型，无可用备选时返回 null
     */
    String degrade(String originalModel, ProviderErrorType reason);

    /**
     * 检查原模型是否已恢复
     */
    boolean canRecover(String model);

    /**
     * 定期健康检查任务，恢复后标记模型可用
     */
    void recoveryCheck();
}
