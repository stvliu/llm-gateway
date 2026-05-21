package com.codingas.gateway.application.productapikey;

import com.codingas.gateway.application.product.dto.ApiKeyTestResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyCreateRequest;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyCreateResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyDetailResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyUpdateRequest;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.product.entity.ProductApiKey;
import com.codingas.gateway.domain.product.enums.ProductApiKeyState;
import com.codingas.gateway.domain.product.gateway.ProductApiKeyGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 产品 API Key 应用服务实现
 *
 * <p>加解密由基础设施层（GatewayImpl）处理，Application 层只传递明文 Key。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductApiKeyServiceImpl implements ProductApiKeyService {

    private final ProductApiKeyGateway productApiKeyGateway;

    @Override
    @Transactional
    public ProductApiKeyCreateResponse create(Long productId, ProductApiKeyCreateRequest request) {
        String plainKey = request.apiKey();
        String keyPrefix = plainKey.substring(0, Math.min(8, plainKey.length()));

        ProductApiKey apiKey = new ProductApiKey();
        apiKey.setProductId(productId);
        apiKey.setApiKeyPlain(plainKey);
        apiKey.setApiKeyPrefix(keyPrefix);
        apiKey.setName(request.description());
        apiKey.setDescription(request.description());
        apiKey.setWeight(request.weight());
        apiKey.setPriority(request.priority());
        apiKey.setState(ProductApiKeyState.ACTIVE);

        // GatewayImpl 内部处理加密和哈希
        ProductApiKey saved = productApiKeyGateway.save(apiKey);
        log.info("Created ProductApiKey: id={}, productId={}", saved.getId(), saved.getProductId());

        return new ProductApiKeyCreateResponse(saved.getId(), keyPrefix, plainKey);
    }

    @Override
    public List<ProductApiKeyResponse> listByProductId(Long productId) {
        return productApiKeyGateway.findByProductId(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ProductApiKeyResponse getById(Long productId, Long id) {
        ProductApiKey apiKey = findAndValidateOwnership(productId, id);
        return toResponse(apiKey);
    }

    @Override
    public ProductApiKeyDetailResponse getDetailById(Long productId, Long id) {
        ProductApiKey apiKey = findAndValidateOwnership(productId, id);
        return toDetailResponse(apiKey);
    }

    @Override
    @Transactional
    public ProductApiKeyResponse update(Long productId, Long id, ProductApiKeyUpdateRequest request) {
        ProductApiKey apiKey = findAndValidateOwnership(productId, id);

        if (request.description() != null) {
            apiKey.setDescription(request.description());
        }
        if (request.weight() != null) {
            apiKey.setWeight(request.weight());
        }
        if (request.priority() != null) {
            apiKey.setPriority(request.priority());
        }
        if (request.state() != null) {
            apiKey.setState(request.state());
        }

        ProductApiKey saved = productApiKeyGateway.save(apiKey);
        log.info("Updated ProductApiKey: id={}", saved.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long productId, Long id) {
        findAndValidateOwnership(productId, id);
        productApiKeyGateway.deleteById(id);
        log.info("Deleted ProductApiKey: id={}", id);
    }

    @Override
    public ApiKeyTestResponse testApiKey(Long productId, Long id) {
        // 验证归属关系
        ProductApiKey apiKey = findAndValidateOwnership(productId, id);

        // TODO: 实现真实的 API Key 测试逻辑
        // 1. 获取 API Key 明文
        // 2. 获取产品端点配置
        // 3. 发送测试请求
        // 4. 返回测试结果

        log.info("Testing ProductApiKey: id={}, productId={}", id, productId);

        return ApiKeyTestResponse.builder()
                .success(true)
                .latency(100L)
                .modelName("gpt-4o")
                .responsePreview("Hello! How can I assist you today?")
                .testedAt(Instant.now())
                .build();
    }

    /**
     * 验证归属关系并返回实体
     */
    private ProductApiKey findAndValidateOwnership(Long productId, Long id) {
        ProductApiKey apiKey = productApiKeyGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductApiKey", id));
        if (!apiKey.getProductId().equals(productId)) {
            throw new ResourceNotFoundException("ProductApiKey", id);
        }
        return apiKey;
    }

    private ProductApiKeyResponse toResponse(ProductApiKey apiKey) {
        return new ProductApiKeyResponse(
                apiKey.getId(),
                apiKey.getProductId(),
                apiKey.getApiKeyPrefix(),
                apiKey.getName(),
                apiKey.getDescription(),
                apiKey.getWeight(),
                apiKey.getPriority(),
                apiKey.getState(),
                apiKey.getCreatedAt(),
                apiKey.getUpdatedAt()
        );
    }

    private ProductApiKeyDetailResponse toDetailResponse(ProductApiKey apiKey) {
        return new ProductApiKeyDetailResponse(
                apiKey.getId(),
                apiKey.getProductId(),
                apiKey.getApiKeyPrefix(),
                apiKey.getApiKeyPlain(),
                apiKey.getName(),
                apiKey.getDescription(),
                apiKey.getWeight(),
                apiKey.getPriority(),
                apiKey.getState(),
                apiKey.getCreatedAt(),
                apiKey.getUpdatedAt()
        );
    }
}
