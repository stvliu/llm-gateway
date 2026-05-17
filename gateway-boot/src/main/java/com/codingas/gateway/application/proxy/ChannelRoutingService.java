package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import com.codingas.gateway.domain.model.service.ApiKeySelectionService;
import com.codingas.gateway.domain.model.service.ModelDomainService;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 渠道路由服务
 *
 * <p>核心路由解析服务，负责：</p>
 * <ol>
 *   <li>模型名 → 渠道列表</li>
 *   <li>按策略选择渠道 → Provider</li>
 *   <li>选择 API Key</li>
 *   <li>构建路由上下文</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelRoutingService {

    private final ModelDomainService modelDomainService;
    private final ProviderGateway providerGateway;
    private final ApiKeySelectionService apiKeySelectionService;

    /**
     * 解析路由
     *
     * @param modelName 用户传入的模型名（如 "gpt-4o"）
     * @param strategy 路由策略
     * @return 路由上下文
     * @throws NoSuchElementException 无可用渠道或供应商
     * @throws IllegalStateException 无可用 API Key
     */
    public RoutingContext resolve(String modelName, RouteGroup.RoutingStrategy strategy) {
        log.debug("Resolving route for model: {}, strategy: {}", modelName, strategy);

        // 参数验证
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("Model name is required");
        }

        // 1. 查找活跃渠道
        List<Model> channels = modelDomainService.findActiveChannels(modelName);

        // 2. 无渠道时 fallback 到旧逻辑（按原逻辑查单个模型）
        if (channels.isEmpty()) {
            log.debug("No active channels found, falling back to legacy lookup");
            return resolveLegacy(modelName);
        }

        // 3. 单渠道直接返回
        if (channels.size() == 1) {
            log.debug("Single channel found, using directly");
            return buildContext(channels.get(0));
        }

        // 4. 多渠道按策略选择
        Model selected = selectChannel(channels, strategy);
        log.info("Selected channel: model={}, providerId={}, priority={}",
            selected.getProviderModelId(), selected.getProviderId(), selected.getPriority());

        return buildContext(selected);
    }

    /**
     * 使用旧逻辑解析（兼容现有数据）
     */
    private RoutingContext resolveLegacy(String modelName) {
        ModelDomainService.ModelProviderInfo info = modelDomainService.getModelWithProviderByProviderModelId(modelName);

        ProviderApiKey apiKey = apiKeySelectionService.selectApiKey(info.provider().getId());
        if (apiKey == null) {
            throw new IllegalStateException("No available API key for provider: " + info.provider().getName());
        }

        return new RoutingContext(info.model(), info.provider(), apiKey, modelName);
    }

    /**
     * 按策略选择渠道
     */
    private Model selectChannel(List<Model> channels, RouteGroup.RoutingStrategy strategy) {
        if (strategy == null) {
            strategy = RouteGroup.RoutingStrategy.FAILOVER;
        }

        return switch (strategy) {
            case FAILOVER -> channels.get(0); // 已按 priority ASC 排序
            case WEIGHTED -> selectByWeight(channels);
            case RANDOM -> channels.get(ThreadLocalRandom.current().nextInt(channels.size()));
            case COST_OPTIMIZED -> selectByCost(channels);
            case LATENCY_OPTIMIZED -> channels.get(0); // Phase 2: 按延迟统计选择
        };
    }

    /**
     * 按权重随机选择
     */
    private Model selectByWeight(List<Model> channels) {
        int totalWeight = channels.stream()
            .mapToInt(m -> m.getWeight() != null ? m.getWeight() : 100)
            .sum();

        if (totalWeight <= 0) {
            return channels.get(ThreadLocalRandom.current().nextInt(channels.size()));
        }

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;

        for (Model channel : channels) {
            int weight = channel.getWeight() != null ? channel.getWeight() : 100;
            cumulative += weight;
            if (random < cumulative) {
                return channel;
            }
        }

        return channels.get(channels.size() - 1);
    }

    /**
     * 按成本选择（选择 input_price 最低的）
     *
     * <p>注意：未配置价格的渠道会被排序到最后，避免被错误地优先选择。</p>
     */
    private Model selectByCost(List<Model> channels) {
        // 优先选择已配置价格的渠道
        Model selected = channels.stream()
            .filter(m -> m.getInputPrice() != null)
            .min(Comparator.comparing(Model::getInputPrice))
            .orElse(null);

        if (selected != null) {
            log.debug("Selected channel by cost: providerId={}, price={}",
                selected.getProviderId(), selected.getInputPrice());
            return selected;
        }

        // 所有渠道都没有配置价格，记录警告并返回第一个
        log.warn("No channel with configured price found for model, using first available");
        return channels.get(0);
    }

    /**
     * 构建路由上下文
     */
    private RoutingContext buildContext(Model model) {
        // 获取 Provider
        Provider provider = providerGateway.findById(model.getProviderId())
            .orElseThrow(() -> new NoSuchElementException("Provider not found: " + model.getProviderId()));

        // 选择 API Key
        ProviderApiKey apiKey = apiKeySelectionService.selectApiKey(provider.getId());
        if (apiKey == null) {
            throw new IllegalStateException("No available API key for provider: " + provider.getName());
        }

        return new RoutingContext(model, provider, apiKey, model.getProviderModelId());
    }
}
