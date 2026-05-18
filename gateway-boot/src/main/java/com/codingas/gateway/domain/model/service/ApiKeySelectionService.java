package com.codingas.gateway.domain.model.service;

import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * API Key 动态选择服务
 *
 * <p>为指定供应商选择一个可用的 API Key。</p>
 *
 * <h3>选择策略</h3>
 * <ol>
 *   <li>优先选择 isDefault=true 的 Key</li>
 *   <li>否则按 weight 加权随机选择</li>
 *   <li>无可用 Key 返回 null</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
/**
 * @deprecated 旧架构 API Key 选择服务，由 ProductRoutingService 替代
 */
@Deprecated(since = "2.0", forRemoval = true)
public class ApiKeySelectionService {

    private final ProviderApiKeyGateway providerApiKeyGateway;

    /**
     * 为指定供应商选择一个可用的 API Key
     *
     * @param providerId 供应商ID
     * @return 选中的 API Key，无可用 Key 返回 null
     */
    public ProviderApiKey selectApiKey(Long providerId) {
        // 1. 优先选择默认 Key
        var defaultKeyOpt = providerApiKeyGateway.findDefaultKeyByProviderId(providerId);
        if (defaultKeyOpt.isPresent()) {
            ProviderApiKey defaultKey = defaultKeyOpt.get();
            if (defaultKey.isAvailable()) {
                log.debug("Selected default API key for provider {}: {}", providerId, defaultKey.getKeyName());
                return defaultKey;
            } else {
                log.warn("Default API key for provider {} is not available (state={}), falling back to weighted selection",
                    providerId, defaultKey.getState());
            }
        }

        // 2. 获取所有活跃 Key，按权重选择
        List<ProviderApiKey> activeKeys = providerApiKeyGateway.findActiveKeysByProviderId(providerId);
        if (activeKeys.isEmpty()) {
            log.warn("No available API keys for provider: {}", providerId);
            return null;
        }

        // 3. 单个 Key 直接返回
        if (activeKeys.size() == 1) {
            return activeKeys.get(0);
        }

        // 4. 按 weight 加权随机选择
        return selectByWeight(activeKeys);
    }

    /**
     * 按权重随机选择
     */
    private ProviderApiKey selectByWeight(List<ProviderApiKey> keys) {
        int totalWeight = keys.stream()
            .mapToInt(k -> k.getWeight() != null ? k.getWeight() : 100)
            .sum();

        if (totalWeight <= 0) {
            // 所有权重都为 0，随机选一个
            return keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        }

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;

        for (ProviderApiKey key : keys) {
            int weight = key.getWeight() != null ? key.getWeight() : 100;
            cumulative += weight;
            if (random < cumulative) {
                log.debug("Selected API key by weight: {} (weight={})", key.getKeyName(), weight);
                return key;
            }
        }

        // 理论上不应该到达这里，返回最后一个
        return keys.get(keys.size() - 1);
    }
}
