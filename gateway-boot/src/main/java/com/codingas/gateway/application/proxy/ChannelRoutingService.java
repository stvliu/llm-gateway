package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import com.codingas.gateway.domain.model.service.ApiKeySelectionService;
import com.codingas.gateway.domain.model.service.ModelDomainService;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import com.codingas.gateway.domain.team.entity.UserApiKey;
import com.codingas.gateway.domain.team.gateway.UserApiKeyGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 渠道路由服务（双路分发）
 *
 * <p>根据认证结果分发到新架构或旧架构路由：</p>
 * <ul>
 *   <li>新架构：UserApiKey → Product → ProductApiKey（ProductRoutingService）</li>
 *   <li>旧架构：GatewayApiKey → Provider → ProviderApiKey（原有逻辑）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelRoutingService {

    private final ModelDomainService modelDomainService;
    private final ProviderGateway providerGateway;
    private final ApiKeySelectionService apiKeySelectionService;
    private final ProductRoutingService productRoutingService;
    private final UserApiKeyGateway userApiKeyGateway;

    /**
     * 基于认证结果解析路由
     *
     * @param authResult 认证结果
     * @param modelName 模型名
     * @param strategy 路由策略
     * @return 路由上下文
     */
    public RoutingContext resolve(UserAuthResult authResult, String modelName,
                                   RouteGroup.RoutingStrategy strategy) {
        if (authResult.newArchitecture()) {
            return resolveNewArchitecture(authResult, modelName);
        } else {
            return resolveLegacy(modelName, strategy);
        }
    }

    /**
     * 旧架构路由（兼容现有数据）
     *
     * <p>保留原有路由逻辑不变。</p>
     */
    public RoutingContext resolve(String modelName, RouteGroup.RoutingStrategy strategy) {
        return resolveLegacy(modelName, strategy);
    }

    /**
     * 新架构路由
     */
    private RoutingContext resolveNewArchitecture(UserAuthResult authResult, String modelName) {
        UserApiKey userApiKey = userApiKeyGateway.findById(authResult.userApiKeyId())
            .orElseThrow(() -> new NoSuchElementException(
                "UserApiKey not found: id=" + authResult.userApiKeyId()));

        // 从请求路径推断协议
        String protocol = "openai"; // 默认 OpenAI 协议

        return productRoutingService.resolve(userApiKey, modelName, protocol);
    }

    /**
     * 旧架构路由（保留原有逻辑）
     */
    private RoutingContext resolveLegacy(String modelName, RouteGroup.RoutingStrategy strategy) {
        log.debug("Legacy routing for model: {}, strategy: {}", modelName, strategy);

        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("Model name is required");
        }

        List<Model> channels = modelDomainService.findActiveChannels(modelName);

        if (channels.isEmpty()) {
            return resolveLegacyFallback(modelName);
        }

        if (channels.size() == 1) {
            return buildLegacyContext(channels.get(0));
        }

        Model selected = selectChannel(channels, strategy);
        return buildLegacyContext(selected);
    }

    private RoutingContext resolveLegacyFallback(String modelName) {
        ModelDomainService.ModelProviderInfo info =
            modelDomainService.getModelWithProviderByProviderModelId(modelName);

        ProviderApiKey apiKey = apiKeySelectionService.selectApiKey(info.provider().getId());
        if (apiKey == null) {
            throw new IllegalStateException(
                "No available API key for provider: " + info.provider().getName());
        }

        return RoutingContext.builder()
            .providerId(info.provider().getId())
            .providerName(info.provider().getName())
            .providerType(info.provider().getType())
            .model(info.model().getProviderModelId())
            .providerApiKey(apiKey.getApiKey())
            .providerApiKeyId(apiKey.getId())
            .endpoint(info.provider().getBaseUrl())
            .build();
    }

    private Model selectChannel(List<Model> channels, RouteGroup.RoutingStrategy strategy) {
        if (strategy == null) {
            strategy = RouteGroup.RoutingStrategy.FAILOVER;
        }

        return switch (strategy) {
            case FAILOVER -> channels.get(0);
            case WEIGHTED -> selectByWeight(channels);
            case RANDOM -> channels.get(ThreadLocalRandom.current().nextInt(channels.size()));
            case COST_OPTIMIZED -> selectByCost(channels);
            case LATENCY_OPTIMIZED -> channels.get(0);
        };
    }

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

    private Model selectByCost(List<Model> channels) {
        Model selected = channels.stream()
            .filter(m -> m.getInputPrice() != null)
            .min(java.util.Comparator.comparing(Model::getInputPrice))
            .orElse(null);

        if (selected != null) {
            return selected;
        }

        log.warn("No channel with configured price, using first available");
        return channels.get(0);
    }

    private RoutingContext buildLegacyContext(Model model) {
        Provider provider = providerGateway.findById(model.getProviderId())
            .orElseThrow(() -> new NoSuchElementException(
                "Provider not found: " + model.getProviderId()));

        ProviderApiKey apiKey = apiKeySelectionService.selectApiKey(provider.getId());
        if (apiKey == null) {
            throw new IllegalStateException(
                "No available API key for provider: " + provider.getName());
        }

        return RoutingContext.builder()
            .providerId(provider.getId())
            .providerName(provider.getName())
            .providerType(provider.getType())
            .model(model.getProviderModelId())
            .providerApiKey(apiKey.getApiKey())
            .providerApiKeyId(apiKey.getId())
            .endpoint(provider.getBaseUrl())
            .build();
    }
}