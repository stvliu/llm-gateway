package com.codingas.gateway.application.metadata;

import com.codingas.gateway.application.metadata.dto.ApplyMetadataRequest;
import com.codingas.gateway.application.metadata.dto.ApplyMetadataResult;
import com.codingas.gateway.application.metadata.dto.MetadataCreateRequest;
import com.codingas.gateway.application.metadata.dto.MetadataUpdateRequest;
import com.codingas.gateway.application.metadata.dto.ProviderMetadataResponse;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.domain.metadata.entity.ModelMetadata;
import com.codingas.gateway.domain.metadata.entity.ProductMetadata;
import com.codingas.gateway.domain.metadata.entity.ProductModelMetadata;
import com.codingas.gateway.domain.metadata.entity.ProviderMetadata;
import com.codingas.gateway.domain.metadata.enums.ProductType;
import com.codingas.gateway.domain.metadata.gateway.ModelMetadataGateway;
import com.codingas.gateway.domain.metadata.gateway.ProductMetadataGateway;
import com.codingas.gateway.domain.metadata.gateway.ProviderMetadataGateway;
import com.codingas.gateway.domain.metadata.gateway.ProductModelMetadataGateway;
import com.codingas.gateway.domain.iam.service.ApiKeyEncryptionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final ProductModelMetadataGateway productModelMetadataGateway;
    private final ProviderGateway providerGateway;
    private final ChannelGateway channelGateway;
    private final ChannelCredentialGateway channelCredentialGateway;
    private final ModelSpecGateway modelSpecGateway;
    private final ChannelModelGateway channelModelGateway;
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
     * 应用元数据：一键创建 Provider + Channel + ChannelCredential + ModelSpec
     */
    @Transactional
    public ApplyMetadataResult applyMetadata(Long id, ApplyMetadataRequest request) {
        // 1. 查询供应商元数据
        ProviderMetadata metadata = providerMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("供应商元数据不存在: id=" + id));

        // 2. 检查同名 Provider 是否已存在
        if (providerGateway.findByName(metadata.getProviderName()).isPresent()) {
            throw new IllegalStateException("供应商已存在: " + metadata.getProviderName());
        }

        // 3. 创建 Provider
        Provider provider = new Provider();
        provider.setName(metadata.getProviderName());
        provider.setState(ProviderState.ACTIVE);
        Provider savedProvider = providerGateway.save(provider);
        log.info("Created provider from metadata: id={}, name={}", savedProvider.getId(), savedProvider.getName());

        // 4. 查询关联的产品元数据，创建 Channel
        List<ProductMetadata> productMetadatas = productMetadataGateway.findByProviderId(metadata.getProviderId());
        Long defaultChannelId = null;
        for (ProductMetadata pm : productMetadatas) {
            Channel channel = new Channel();
            channel.setProviderId(savedProvider.getId());
            channel.setName(pm.getProductName());
            channel.setBillingMode(mapBillingMode(pm.getProductType()));
            // 从元数据端点推断 endpointUrl 和 protocol
            Map<String, String> endpoints = pm.getEndpoints();
            if (endpoints != null && !endpoints.isEmpty()) {
                String protocolName = endpoints.containsKey("openai") ? "openai" : endpoints.keySet().iterator().next();
                channel.setEndpointUrl(endpoints.get(protocolName));
                channel.setProtocol(com.codingas.gateway.domain.supply.enums.Protocol.valueOf(protocolName.toUpperCase()));
            }
            channel.setState(ChannelState.ACTIVE);
            Channel savedChannel = channelGateway.save(channel);
            log.info("Created channel from metadata: id={}, name={}", savedChannel.getId(), savedChannel.getName());

            // 记录默认渠道 ID（优先选 isDefault=true 的，否则选第一个）
            if (defaultChannelId == null || Boolean.TRUE.equals(pm.getIsDefault())) {
                defaultChannelId = savedChannel.getId();
            }
        }

        // 5. 创建 ChannelCredential（加密存储 API Key）
        Long targetChannelId = defaultChannelId;
        if (targetChannelId == null) {
            // 没有产品元数据，创建一个默认渠道
            Channel defaultChannel = new Channel();
            defaultChannel.setProviderId(savedProvider.getId());
            defaultChannel.setName(metadata.getProviderName() + " Default");
            defaultChannel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
            defaultChannel.setState(ChannelState.ACTIVE);
            targetChannelId = channelGateway.save(defaultChannel).getId();
        }

        if (targetChannelId != null && request.getApiKey() != null && !request.getApiKey().isBlank()) {
            ChannelCredential credential = new ChannelCredential();
            credential.setChannelId(targetChannelId);
            credential.setApiKeyPlain(request.getApiKey());
            credential.setApiKeyPrefix(request.getApiKey().substring(0, Math.min(8, request.getApiKey().length())));
            credential.setName(request.getChannelName() != null ? request.getChannelName() : "default");
            credential.setState(CredentialState.ACTIVE);
            channelCredentialGateway.save(credential);
            log.info("Created ChannelCredential for channel: channelId={}", targetChannelId);
        }

        // 6. 创建 ModelSpec
        List<ModelMetadata> modelMetadatas = modelMetadataGateway.findByProviderId(metadata.getProviderId());
        List<Long> createdModelIds = new ArrayList<>();
        List<String> createdModelNames = new ArrayList<>();
        for (ModelMetadata mm : modelMetadatas) {
            ModelSpec modelSpec = new ModelSpec();
            modelSpec.setProviderModelId(mm.getProviderModelId());
            modelSpec.setDisplayName(mm.getDisplayName());
            modelSpec.setContextWindow(mm.getContextWindow());
            modelSpec.setCapabilities(mm.getCapabilities());
            modelSpec.setState(ModelSpecState.ACTIVE);
            ModelSpec savedModel = modelSpecGateway.save(modelSpec);
            createdModelIds.add(savedModel.getId());
            createdModelNames.add(savedModel.getDisplayName());
        }
        log.info("Created {} models for provider: providerId={}", createdModelIds.size(), savedProvider.getId());

        // 7. 创建 ChannelModel 关联（批量方式）
        // 构建 元数据模型ID -> 业务模型ID 映射
        Map<Long, Long> metadataModelIdToModelId = new HashMap<>();
        List<ModelMetadata> allModelMetas = modelMetadataGateway.findByProviderId(metadata.getProviderId());
        List<ModelSpec> savedModels = modelSpecGateway.findAll();
        for (ModelMetadata mm : allModelMetas) {
            savedModels.stream()
                .filter(m -> m.getProviderModelId().equals(mm.getProviderModelId()))
                .findFirst()
                .ifPresent(m -> metadataModelIdToModelId.put(mm.getId(), m.getId()));
        }

        // 构建 元数据产品ID -> 业务渠道ID 映射（批量加载）
        Map<Long, Long> metadataProductIdToChannelId = new HashMap<>();
        List<ProductMetadata> allProductMetas = productMetadataGateway.findByProviderId(metadata.getProviderId());
        List<Channel> savedChannels = channelGateway.findByProviderId(savedProvider.getId());
        for (ProductMetadata pm : allProductMetas) {
            savedChannels.stream()
                .filter(c -> c.getName().equals(pm.getProductName()))
                .findFirst()
                .ifPresent(c -> metadataProductIdToChannelId.put(pm.getId(), c.getId()));
        }

        // 批量创建 ChannelModel 关联
        List<com.codingas.gateway.domain.supply.entity.ChannelModel> toCreate = new ArrayList<>();
        for (ModelMetadata mm : allModelMetas) {
            List<ProductModelMetadata> associations = productModelMetadataGateway.findByModelId(mm.getId());
            for (ProductModelMetadata assoc : associations) {
                Long businessModelId = metadataModelIdToModelId.get(mm.getId());
                Long businessChannelId = metadataProductIdToChannelId.get(assoc.getProductId());
                if (businessChannelId != null && businessModelId != null) {
                    if (!channelModelGateway.existsByChannelIdAndModelId(businessChannelId, businessModelId)) {
                        com.codingas.gateway.domain.supply.entity.ChannelModel channelModel = new com.codingas.gateway.domain.supply.entity.ChannelModel();
                        channelModel.setChannelId(businessChannelId);
                        channelModel.setModelSpecId(businessModelId);
                        toCreate.add(channelModel);
                    }
                }
            }
        }
        if (!toCreate.isEmpty()) {
            channelModelGateway.saveAll(toCreate);
            log.info("Created {} ChannelModel associations for provider: providerId={}", toCreate.size(), savedProvider.getId());
        }

        return ApplyMetadataResult.builder()
            .providerId(savedProvider.getId())
            .providerName(savedProvider.getName())
            .modelIds(createdModelIds)
            .modelNames(createdModelNames)
            .createdAt(Instant.now())
            .build();
    }

    /**
     * 将元数据 ProductType 映射为业务 BillingMode
     */
    private BillingMode mapBillingMode(com.codingas.gateway.domain.metadata.enums.ProductType metadataType) {
        if (metadataType == null) return BillingMode.PAY_AS_YOU_GO;
        return switch (metadataType) {
            case STANDARD, BATCH, CACHE, FREE_TIER -> BillingMode.PAY_AS_YOU_GO;
            case SUBSCRIPTION, PROMOTION -> BillingMode.SUBSCRIPTION_CODING;
        };
    }

    /**
     * 将元数据 ProductType 映射为业务 BillingMode（旧方法保留兼容）
     */
    @SuppressWarnings("unused")
    private BillingMode mapProductType(com.codingas.gateway.domain.metadata.enums.ProductType metadataType) {
        return mapBillingMode(metadataType);
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