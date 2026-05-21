package com.codingas.gateway.application.proxy;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.entity.ProductApiKey;
import com.codingas.gateway.domain.product.gateway.ProductApiKeyGateway;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.domain.product.service.ProductDomainService;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.team.entity.UserApiKey;
import com.codingas.gateway.domain.team.gateway.UserApiKeyGateway;
import com.codingas.gateway.domain.team.service.UserApiKeyDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 产品路由服务（新架构）
 * <p>
 * 一个 UserApiKey 可关联多个产品。路由时按 model name 在关联产品中匹配。
 * 路由结果包含协议名称，由 ProxyServiceImpl 通过 ProtocolGatewayRegistry 查找协议网关。
 */
@Service
public class ProductRoutingService {

    private static final Logger log = LoggerFactory.getLogger(ProductRoutingService.class);

    private final UserApiKeyGateway userApiKeyGateway;
    private final ProductGateway productGateway;
    private final ProductApiKeyGateway productApiKeyGateway;
    private final ProviderGateway providerGateway;
    private final UserApiKeyDomainService userApiKeyDomainService;
    private final ProductDomainService productDomainService;

    public ProductRoutingService(UserApiKeyGateway userApiKeyGateway,
                                 ProductGateway productGateway,
                                 ProductApiKeyGateway productApiKeyGateway,
                                 ProviderGateway providerGateway,
                                 UserApiKeyDomainService userApiKeyDomainService,
                                 ProductDomainService productDomainService) {
        this.userApiKeyGateway = userApiKeyGateway;
        this.productGateway = productGateway;
        this.productApiKeyGateway = productApiKeyGateway;
        this.providerGateway = providerGateway;
        this.userApiKeyDomainService = userApiKeyDomainService;
        this.productDomainService = productDomainService;
    }

    /**
     * 基于新架构解析路由
     *
     * @param userApiKeyId 用户密钥 ID
     * @param model        请求的模型名
     * @param protocol     请求协议（可为空，自动推断）
     * @return 路由上下文
     */
    public RoutingContext resolve(Long userApiKeyId, String model, String protocol) {
        // 1. 查找 UserApiKey
        UserApiKey userApiKey = userApiKeyGateway.findById(userApiKeyId)
                .orElseThrow(() -> new ResourceNotFoundException("UserApiKey", userApiKeyId));

        // 2. 校验 Key 级别模型权限
        if (!userApiKeyDomainService.canAccessModel(userApiKey, model)) {
            throw new ResourceNotFoundException("Model", model);
        }

        // 3. 在关联产品中匹配 model
        Product product = matchProduct(userApiKey.getProductIds(), model);

        // 4. 选择 ProductApiKey
        ProductApiKey apiKey = selectProductApiKey(product.getId());
        if (apiKey == null) {
            throw new ResourceNotFoundException("ProductApiKey", product.getId());
        }

        String plainApiKey = apiKey.getApiKeyPlain();
        if (plainApiKey == null || plainApiKey.isBlank()) {
            throw new ResourceNotFoundException("ProductApiKey", apiKey.getId());
        }

        // 5. 获取 Provider 信息
        Provider provider = providerGateway.findById(product.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider", product.getProviderId()));

        // 6. 解析协议名称和端点
        ResolvedEndpoint resolved = resolveEndpoint(product, protocol);

        // 7. 构建路由上下文
        return RoutingContext.builder()
                .providerId(product.getProviderId())
                .providerName(product.getProviderName())
                .productId(product.getId())
                .productType(product.getProductType())
                .userApiKeyId(userApiKey.getId())
                .teamId(userApiKey.getTeamId())
                .model(model)
                .protocol(resolved.protocolName)
                .providerApiKey(plainApiKey)
                .providerApiKeyId(apiKey.getId())
                .endpoint(resolved.endpointUrl)
                .build();
    }

    /**
     * 在关联产品中匹配包含指定 model 的产品
     */
    private Product matchProduct(List<Long> productIds, String modelName) {
        if (productIds == null || productIds.isEmpty()) {
            throw new ResourceNotFoundException("Product", "no products associated");
        }

        List<Product> products = productGateway.findByIds(productIds);
        for (Product product : products) {
            if (!product.isAvailable()) {
                continue;
            }
            if (productDomainService.containsModel(product, modelName)) {
                return product;
            }
        }

        throw new ResourceNotFoundException("Model", modelName);
    }

    /**
     * 选择 ProductApiKey（优先级 + 权重策略）
     */
    private ProductApiKey selectProductApiKey(Long productId) {
        var defaultKeyOpt = productApiKeyGateway.findDefaultByProductId(productId);
        if (defaultKeyOpt.isPresent()) {
            ProductApiKey defaultKey = defaultKeyOpt.get();
            if (defaultKey.isAvailable()) {
                return defaultKey;
            }
            log.warn("Default ProductApiKey not available for product {}, falling back", productId);
        }

        List<ProductApiKey> activeKeys = productApiKeyGateway.findActiveByProductId(productId);
        if (activeKeys.isEmpty()) {
            return null;
        }

        if (activeKeys.size() == 1) {
            return activeKeys.get(0);
        }

        return selectByWeight(activeKeys);
    }

    private ProductApiKey selectByWeight(List<ProductApiKey> keys) {
        int totalWeight = keys.stream()
                .mapToInt(k -> k.getWeight() != null ? k.getWeight() : 1)
                .sum();

        if (totalWeight <= 0) {
            return keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        }

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;

        for (ProductApiKey key : keys) {
            int weight = key.getWeight() != null ? key.getWeight() : 1;
            cumulative += weight;
            if (random < cumulative) {
                return key;
            }
        }

        return keys.get(keys.size() - 1);
    }

    /**
     * 解析协议名称和端点 URL
     *
     * <p>如果请求指定了协议，使用请求协议从产品端点获取 URL；</p>
     * <p>如果未指定协议，从产品端点推断默认协议（优先 openai）。</p>
     */
    private ResolvedEndpoint resolveEndpoint(Product product, String requestedProtocol) {
        Map<String, String> endpoints = product.getEndpoints();
        if (endpoints == null || endpoints.isEmpty()) {
            throw new ResourceNotFoundException("Endpoint", product.getName());
        }

        if (requestedProtocol != null && !requestedProtocol.isBlank()) {
            String endpointUrl = endpoints.get(requestedProtocol);
            if (endpointUrl != null) {
                return new ResolvedEndpoint(requestedProtocol, endpointUrl);
            }
            log.warn("Protocol {} not supported by product {}, using default", requestedProtocol, product.getName());
        }

        // 推断默认协议：优先 openai，其次第一个
        String defaultProtocol = endpoints.containsKey("openai") ? "openai" : endpoints.keySet().iterator().next();
        return new ResolvedEndpoint(defaultProtocol, endpoints.get(defaultProtocol));
    }

    /**
     * 解析后的端点信息
     */
    private record ResolvedEndpoint(String protocolName, String endpointUrl) {}
}