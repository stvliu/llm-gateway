package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.entity.ProductApiKey;
import com.codingas.gateway.domain.product.enums.ProductType;
import com.codingas.gateway.domain.product.gateway.ProductApiKeyGateway;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.security.service.ApiKeyEncryptionDomainService;
import com.codingas.gateway.domain.team.entity.UserApiKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 产品路由服务（新架构）
 *
 * <p>基于 UserApiKey → Product → ProductApiKey 的路由链路。</p>
 * <p>职责：</p>
 * <ol>
 *   <li>查 Product（通过 UserApiKey.productId）</li>
 *   <li>校验模型权限（Product.containsModel + UserApiKey.canAccessModel）</li>
 *   <li>选 ProductApiKey（weight/priority 策略）</li>
 *   <li>构建 RoutingContext</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRoutingService {

    private final ProductGateway productGateway;
    private final ProductApiKeyGateway productApiKeyGateway;
    private final ApiKeyEncryptionDomainService encryptionService;

    /**
     * 基于新架构解析路由
     *
     * @param userApiKey 用户密钥
     * @param model 请求的模型名
     * @param protocol 请求协议（openai/anthropic/native）
     * @return 路由上下文
     */
    public RoutingContext resolve(UserApiKey userApiKey, String model, String protocol) {
        log.debug("Product routing: productId={}, model={}, protocol={}",
            userApiKey.getProductId(), model, protocol);

        // 1. 查找产品
        Product product = productGateway.findById(userApiKey.getProductId())
            .orElseThrow(() -> new IllegalStateException(
                "Product not found: id=" + userApiKey.getProductId()));

        if (!product.isAvailable()) {
            throw new IllegalStateException("Product is not available: " + product.getName());
        }

        // 2. 校验模型权限
        if (!product.containsModel(model)) {
            throw new IllegalStateException(
                "Model " + model + " not available in product " + product.getName());
        }

        if (!userApiKey.canAccessModel(model)) {
            throw new IllegalStateException(
                "UserApiKey does not have permission to access model: " + model);
        }

        // 3. 选择 ProductApiKey
        ProductApiKey apiKey = selectProductApiKey(product.getId());
        if (apiKey == null) {
            throw new IllegalStateException(
                "No available ProductApiKey for product: " + product.getName());
        }

        // 4. 解密 API Key
        String decryptedApiKey = encryptionService.decrypt(apiKey.getApiKeyEncrypted());

        // 5. 选择端点
        String endpoint = resolveEndpoint(product, protocol);

        // 6. 构建路由上下文
        return RoutingContext.builder()
            .providerId(product.getProviderId())
            .providerName(product.getProviderName())
            .productId(product.getId())
            .productType(product.getProductType())
            .userApiKeyId(userApiKey.getId())
            .teamId(userApiKey.getTeamId())
            .model(model)
            .protocol(protocol)
            .providerApiKey(decryptedApiKey)
            .providerApiKeyId(apiKey.getId())
            .endpoint(endpoint)
            .build();
    }

    /**
     * 选择 ProductApiKey（优先级 + 权重策略）
     */
    private ProductApiKey selectProductApiKey(Long productId) {
        // 1. 优先选择默认（最高优先级）Key
        var defaultKeyOpt = productApiKeyGateway.findDefaultByProductId(productId);
        if (defaultKeyOpt.isPresent()) {
            ProductApiKey defaultKey = defaultKeyOpt.get();
            if (defaultKey.isAvailable()) {
                log.debug("Selected default ProductApiKey for product {}", productId);
                return defaultKey;
            }
            log.warn("Default ProductApiKey for product {} is not available, falling back", productId);
        }

        // 2. 获取所有活跃 Key
        List<ProductApiKey> activeKeys = productApiKeyGateway.findActiveByProductId(productId);
        if (activeKeys.isEmpty()) {
            return null;
        }

        // 3. 单个 Key 直接返回
        if (activeKeys.size() == 1) {
            return activeKeys.get(0);
        }

        // 4. 按权重随机选择
        return selectByWeight(activeKeys);
    }

    /**
     * 按权重随机选择
     */
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
     * 解析端点 URL
     */
    private String resolveEndpoint(Product product, String protocol) {
        if (protocol != null && !protocol.isBlank()) {
            String endpoint = product.getEndpoint(protocol);
            if (endpoint != null) {
                return endpoint;
            }
            log.warn("Protocol {} not supported by product {}, using default", protocol, product.getName());
        }
        return product.getDefaultEndpoint();
    }
}
