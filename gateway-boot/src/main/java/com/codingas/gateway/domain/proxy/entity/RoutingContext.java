package com.codingas.gateway.domain.proxy.entity;

import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;

/**
 * 路由决策结果
 *
 * <p>封装路由解析完成后的上下文信息，包含选中的渠道、供应商和 API Key。</p>
 *
 * @param model 选中的渠道（Model 记录）
 * @param provider 关联的供应商
 * @param apiKey 选中的 API Key
 * @param providerModelId 实际发给供应商的模型标识
 */
public record RoutingContext(
    Model model,
    Provider provider,
    ProviderApiKey apiKey,
    String providerModelId
) {
    /**
     * 获取超时时间（秒）
     */
    public int getTimeoutSeconds() {
        if (provider != null && provider.getTimeout() != null) {
            return provider.getTimeout() / 1000;
        }
        return 30; // 默认 30 秒
    }
}
