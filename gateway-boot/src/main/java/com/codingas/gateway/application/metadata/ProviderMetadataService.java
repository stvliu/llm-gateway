package com.codingas.gateway.application.metadata;

import com.codingas.gateway.application.metadata.dto.ApplyMetadataRequest;
import com.codingas.gateway.application.metadata.dto.ApplyMetadataResult;
import com.codingas.gateway.application.metadata.dto.MetadataCreateRequest;
import com.codingas.gateway.application.metadata.dto.MetadataUpdateRequest;
import com.codingas.gateway.application.metadata.dto.ProviderMetadataResponse;
import com.codingas.gateway.domain.metadata.entity.ModelMetadata;
import com.codingas.gateway.domain.metadata.entity.ProductMetadata;
import com.codingas.gateway.domain.metadata.entity.ProviderMetadata;
import com.codingas.gateway.domain.metadata.gateway.ModelMetadataGateway;
import com.codingas.gateway.domain.metadata.gateway.ProductMetadataGateway;
import com.codingas.gateway.domain.metadata.gateway.ProviderMetadataGateway;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.enums.ModelState;
import com.codingas.gateway.domain.model.enums.ProviderState;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.entity.ProductApiKey;
import com.codingas.gateway.domain.product.enums.ProductApiKeyState;
import com.codingas.gateway.domain.product.enums.ProductState;
import com.codingas.gateway.domain.product.enums.ProductType;
import com.codingas.gateway.domain.product.gateway.ProductApiKeyGateway;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.domain.security.service.ApiKeyEncryptionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 供应商元数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderMetadataService {

    private final ProviderMetadataGateway providerMetadataGateway;
    private final ProductMetadataGateway productMetadataGateway;
    private final ModelMetadataGateway modelMetadataGateway;
    private final ProviderGateway providerGateway;
    private final ProductGateway productGateway;
    private final ProductApiKeyGateway productApiKeyGateway;
    private final ModelGateway modelGateway;
    private final ApiKeyEncryptionDomainService encryptionService;

    /**
     * 分页查询供应商元数据
     */
    public Page<ProviderMetadataResponse> listProviderMetadata(
            String keyword, Pageable pageable) {
        Page<ProviderMetadata> page = providerMetadataGateway.findByConditions(
            keyword, pageable);
        return page.map(this::toResponse);
    }

    /**
     * 获取供应商元数据详情
     */
    public ProviderMetadataResponse getProviderMetadata(Long id) {
        ProviderMetadata metadata = providerMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("供应商元数据不存在: id=" + id));
        return toResponse(metadata);
    }

    /**
     * 获取所有供应商元数据
     */
    public List<ProviderMetadataResponse> listAllMetadata() {
        return providerMetadataGateway.findAllMetadata().stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * 创建供应商元数据
     */
    @Transactional
    public ProviderMetadataResponse createMetadata(MetadataCreateRequest request) {
        if (providerMetadataGateway.existsByProviderId(request.getProviderId())) {
            throw new IllegalArgumentException("供应商标识已存在: " + request.getProviderId());
        }

        ProviderMetadata metadata = new ProviderMetadata(
            request.getProviderId(),
            request.getProviderName(),
            request.getProviderConfig()
        );
        metadata.setDescription(request.getDescription());
        metadata.setIconUrl(request.getIconUrl());
        metadata.setTags(request.getTags());
        metadata.setCreatedAt(Instant.now());
        metadata.setUpdatedAt(Instant.now());

        ProviderMetadata saved = providerMetadataGateway.save(metadata);
        log.info("Created provider metadata: providerId={}", saved.getProviderId());
        return toResponse(saved);
    }

    /**
     * 更新供应商元数据
     */
    @Transactional
    public ProviderMetadataResponse updateMetadata(Long id, MetadataUpdateRequest request) {
        ProviderMetadata metadata = providerMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("供应商元数据不存在: id=" + id));

        if (request.getProviderName() != null) metadata.setProviderName(request.getProviderName());
        if (request.getProviderConfig() != null) metadata.setProviderConfig(request.getProviderConfig());
        if (request.getDescription() != null) metadata.setDescription(request.getDescription());
        if (request.getIconUrl() != null) metadata.setIconUrl(request.getIconUrl());
        if (request.getTags() != null) metadata.setTags(request.getTags());
        metadata.setUpdatedAt(Instant.now());

        ProviderMetadata saved = providerMetadataGateway.save(metadata);
        log.info("Updated provider metadata: id={}, providerId={}", saved.getId(), saved.getProviderId());
        return toResponse(saved);
    }

    /**
     * 删除供应商元数据（逻辑删除）
     */
    @Transactional
    public void deleteMetadata(Long id) {
        providerMetadataGateway.deleteById(id);
        log.info("Deleted provider metadata: id={}", id);
    }

    /**
     * 应用元数据：一键创建 Provider + Product + ProductApiKey + Model
     */
    @Transactional
    public ApplyMetadataResult applyMetadata(Long id, ApplyMetadataRequest request) {
        // 1. 查询供应商元数据
        ProviderMetadata metadata = providerMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("供应商元数据不存在: id=" + id));

        // 2. 检查同名 Provider 是否已存在
        if (providerGateway.existsByName(metadata.getProviderName())) {
            throw new IllegalStateException("供应商已存在: " + metadata.getProviderName());
        }

        // 3. 创建 Provider
        Provider provider = new Provider();
        provider.setName(metadata.getProviderName());
        provider.setState(ProviderState.ACTIVE);
        Provider savedProvider = providerGateway.save(provider);
        log.info("Created provider from metadata: id={}, name={}", savedProvider.getId(), savedProvider.getName());

        // 4. 查询关联的产品元数据，创建 Product
        List<ProductMetadata> productMetadatas = productMetadataGateway.findByProviderId(metadata.getProviderId());
        Long defaultProductId = null;
        for (ProductMetadata pm : productMetadatas) {
            Product product = new Product();
            product.setProviderId(savedProvider.getId());
            product.setProviderName(savedProvider.getName());
            product.setName(pm.getProductName());
            product.setProductType(mapProductType(pm.getProductType()));
            product.setEndpoints(pm.getEndpoints());
            product.setState(ProductState.ACTIVE);
            Product savedProduct = productGateway.save(product);
            log.info("Created product from metadata: id={}, name={}", savedProduct.getId(), savedProduct.getName());

            // 记录默认产品 ID（优先选 isDefault=true 的，否则选第一个）
            if (defaultProductId == null || Boolean.TRUE.equals(pm.getIsDefault())) {
                defaultProductId = savedProduct.getId();
            }
        }

        // 5. 创建 ProductApiKey（加密存储 API Key）
        Long targetProductId = defaultProductId;
        if (targetProductId == null) {
            // 没有产品元数据，创建一个默认产品
            Product defaultProduct = new Product();
            defaultProduct.setProviderId(savedProvider.getId());
            defaultProduct.setProviderName(savedProvider.getName());
            defaultProduct.setName(metadata.getProviderName() + " Default");
            defaultProduct.setProductType(ProductType.PAY_AS_YOU_GO);
            defaultProduct.setState(ProductState.ACTIVE);
            targetProductId = productGateway.save(defaultProduct).getId();
        }

        if (targetProductId != null && request.getApiKey() != null && !request.getApiKey().isBlank()) {
            ProductApiKey apiKey = new ProductApiKey();
            apiKey.setProductId(targetProductId);
            apiKey.setApiKeyPlain(request.getApiKey());
            apiKey.setApiKeyPrefix(request.getApiKey().substring(0, Math.min(8, request.getApiKey().length())));
            apiKey.setName(request.getChannelName() != null ? request.getChannelName() : "default");
            apiKey.setState(ProductApiKeyState.ACTIVE);
            productApiKeyGateway.save(apiKey);
            log.info("Created ProductApiKey for product: productId={}", targetProductId);
        }

        // 6. 创建 Model
        List<ModelMetadata> modelMetadatas = modelMetadataGateway.findByProviderId(metadata.getProviderId());
        List<Long> createdModelIds = new ArrayList<>();
        List<String> createdModelNames = new ArrayList<>();
        for (ModelMetadata mm : modelMetadatas) {
            Model model = new Model();
            model.setProviderId(savedProvider.getId());
            model.setProviderName(savedProvider.getName());
            model.setProviderModelId(mm.getProviderModelId());
            model.setDisplayName(mm.getDisplayName());
            model.setContextWindow(mm.getContextWindow());
            model.setInputPrice(mm.getInputPrice());
            model.setOutputPrice(mm.getOutputPrice());
            model.setCapabilities(mm.getCapabilities());
            model.setState(ModelState.ACTIVE);
            Model savedModel = modelGateway.save(model);
            createdModelIds.add(savedModel.getId());
            createdModelNames.add(savedModel.getDisplayName());
        }
        log.info("Created {} models for provider: providerId={}", createdModelIds.size(), savedProvider.getId());

        return ApplyMetadataResult.builder()
            .providerId(savedProvider.getId())
            .providerName(savedProvider.getName())
            .modelIds(createdModelIds)
            .modelNames(createdModelNames)
            .createdAt(Instant.now())
            .build();
    }

    /**
     * 将元数据 ProductType 映射为业务 ProductType
     */
    private ProductType mapProductType(com.codingas.gateway.domain.metadata.enums.ProductType metadataType) {
        if (metadataType == null) return ProductType.PAY_AS_YOU_GO;
        return switch (metadataType) {
            case STANDARD, BATCH, CACHE, FREE_TIER -> ProductType.PAY_AS_YOU_GO;
            case SUBSCRIPTION, PROMOTION -> ProductType.SUBSCRIPTION_CODING;
        };
    }

    private ProviderMetadataResponse toResponse(ProviderMetadata metadata) {
        int modelCount = modelMetadataGateway.findByProviderId(metadata.getProviderId()).size();
        return ProviderMetadataResponse.builder()
            .id(metadata.getId())
            .providerId(metadata.getProviderId())
            .providerName(metadata.getProviderName())
            .providerConfig(metadata.getProviderConfig())
            .iconUrl(metadata.getIconUrl())
            .description(metadata.getDescription())
            .tags(metadata.getTags())
            .state(metadata.getState() != null ? metadata.getState().name() : null)
            .createdAt(metadata.getCreatedAt())
            .updatedAt(metadata.getUpdatedAt())
            .modelCount(modelCount)
            .build();
    }
}
