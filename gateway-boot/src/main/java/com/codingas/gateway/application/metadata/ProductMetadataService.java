package com.codingas.gateway.application.metadata;

import com.codingas.gateway.application.metadata.dto.ApplyMetadataRequest;
import com.codingas.gateway.application.metadata.dto.ApplyMetadataResult;
import com.codingas.gateway.application.metadata.dto.MetadataCreateRequest;
import com.codingas.gateway.application.metadata.dto.MetadataUpdateRequest;
import com.codingas.gateway.application.metadata.dto.ProductMetadataResponse;
import com.codingas.gateway.domain.metadata.entity.ProductMetadata;
import com.codingas.gateway.domain.metadata.enums.ProductType;
import com.codingas.gateway.domain.metadata.gateway.ProductMetadataGateway;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 渠道元数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMetadataService {

    private final ProductMetadataGateway productMetadataGateway;
    private final ProviderGateway providerGateway;
    private final ChannelGateway channelGateway;
    private final ChannelCredentialGateway channelCredentialGateway;

    /**
     * 查询渠道元数据列表
     */
    public List<ProductMetadataResponse> listProductMetadata(String providerId) {
        List<ProductMetadata> metadatas;
        if (providerId != null && !providerId.isBlank()) {
            metadatas = productMetadataGateway.findByProviderId(providerId);
        } else {
            metadatas = productMetadataGateway.findAll();
        }
        return metadatas.stream().map(this::toResponse).toList();
    }

    /**
     * 获取渠道元数据详情
     */
    public ProductMetadataResponse getProductMetadata(Long id) {
        ProductMetadata metadata = productMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("渠道元数据不存在: id=" + id));
        return toResponse(metadata);
    }

    /**
     * 创建渠道元数据
     */
    @Transactional
    public ProductMetadataResponse createMetadata(MetadataCreateRequest request) {
        ProductMetadata metadata = new ProductMetadata(
            request.getProviderId(),
            request.getProviderName(),
            ProductType.STANDARD
        );
        metadata.setCreatedAt(Instant.now());
        metadata.setUpdatedAt(Instant.now());

        ProductMetadata saved = productMetadataGateway.save(metadata);
        log.info("Created product metadata: providerId={}, name={}", saved.getProviderId(), saved.getProductName());
        return toResponse(saved);
    }

    /**
     * 更新渠道元数据
     */
    @Transactional
    public ProductMetadataResponse updateMetadata(Long id, MetadataUpdateRequest request) {
        ProductMetadata metadata = productMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("渠道元数据不存在: id=" + id));

        if (request.getProviderName() != null) metadata.setProductName(request.getProviderName());
        metadata.setUpdatedAt(Instant.now());

        ProductMetadata saved = productMetadataGateway.save(metadata);
        log.info("Updated product metadata: id={}", saved.getId());
        return toResponse(saved);
    }

    /**
     * 删除渠道元数据
     */
    @Transactional
    public void deleteMetadata(Long id) {
        productMetadataGateway.deleteById(id);
        log.info("Deleted product metadata: id={}", id);
    }

    /**
     * 应用渠道元数据：创建 Channel + ChannelCredential
     */
    @Transactional
    public ApplyMetadataResult applyMetadata(Long id, ApplyMetadataRequest request) {
        ProductMetadata metadata = productMetadataGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("渠道元数据不存在: id=" + id));

        // 查找对应的 Provider
        Provider provider = providerGateway.findAll().stream()
            .filter(p -> p.getName().equals(metadata.getProviderId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未找到供应商: " + metadata.getProviderId()));

        // 创建 Channel
        Channel channel = new Channel();
        channel.setProviderId(provider.getId());
        channel.setName(metadata.getProductName());
        channel.setBillingMode(mapBillingMode(metadata.getProductType()));

        Map<String, String> endpoints = metadata.getEndpoints();
        if (endpoints != null && !endpoints.isEmpty()) {
            String protocolName = endpoints.containsKey("openai") ? "openai" : endpoints.keySet().iterator().next();
            channel.setEndpointUrl(endpoints.get(protocolName));
            channel.setProtocol(Protocol.valueOf(protocolName.toUpperCase()));
        }

        channel.setState(ChannelState.ACTIVE);
        Channel savedChannel = channelGateway.save(channel);

        // 创建 ChannelCredential
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            ChannelCredential credential = new ChannelCredential();
            credential.setChannelId(savedChannel.getId());
            credential.setApiKeyPlain(request.getApiKey());
            credential.setApiKeyPrefix(request.getApiKey().substring(0, Math.min(8, request.getApiKey().length())));
            credential.setName(request.getChannelName() != null ? request.getChannelName() : "default");
            credential.setState(CredentialState.ACTIVE);
            channelCredentialGateway.save(credential);
        }

        log.info("Applied product metadata: channel={}, provider={}", savedChannel.getName(), provider.getName());

        return ApplyMetadataResult.builder()
            .providerId(provider.getId())
            .providerName(provider.getName())
            .createdAt(Instant.now())
            .build();
    }

    private BillingMode mapBillingMode(ProductType metadataType) {
        if (metadataType == null) return BillingMode.PAY_AS_YOU_GO;
        return switch (metadataType) {
            case STANDARD, BATCH, CACHE, FREE_TIER -> BillingMode.PAY_AS_YOU_GO;
            case SUBSCRIPTION, PROMOTION -> BillingMode.SUBSCRIPTION_CODING;
        };
    }

    private ProductMetadataResponse toResponse(ProductMetadata metadata) {
        return ProductMetadataResponse.builder()
            .id(metadata.getId())
            .providerId(metadata.getProviderId())
            .productName(metadata.getProductName())
            .productType(metadata.getProductType() != null ? metadata.getProductType().name() : null)
            .endpoints(metadata.getEndpoints())
            .isDefault(metadata.getIsDefault())
            .createdAt(metadata.getCreatedAt())
            .updatedAt(metadata.getUpdatedAt())
            .build();
    }
}