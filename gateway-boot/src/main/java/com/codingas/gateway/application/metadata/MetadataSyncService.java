package com.codingas.gateway.application.metadata;

import com.codingas.gateway.application.metadata.dto.MetadataSyncResult;
import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.entity.ModelMetadata;
import com.codingas.gateway.domain.metadata.entity.ProductMetadata;
import com.codingas.gateway.domain.metadata.entity.ProviderMetadata;
import com.codingas.gateway.domain.metadata.gateway.ModelMetadataGateway;
import com.codingas.gateway.domain.metadata.gateway.ProductMetadataGateway;
import com.codingas.gateway.domain.metadata.gateway.ProviderMetadataGateway;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 元数据同步服务
 *
 * <p>负责从外部元数据源同步 Provider / Model / Product 元数据，
 * 并将它们物化为 supply 域的实体。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataSyncService {

    private final ProviderMetadataGateway providerMetadataGateway;
    private final ModelMetadataGateway modelMetadataGateway;
    private final ProductMetadataGateway productMetadataGateway;
    private final ProviderGateway providerGateway;
    private final ModelSpecGateway modelSpecGateway;
    private final ChannelGateway channelGateway;
    private final ChannelCredentialGateway channelCredentialGateway;

    /**
     * 同步所有元数据
     */
    @Transactional
    public MetadataSyncResult syncAll() {
        log.info("Starting full metadata sync");
        int providers = syncProviders();
        int models = syncModels();
        int channels = syncChannelCredentials();
        log.info("Full metadata sync completed");
        return MetadataSyncResult.builder()
            .syncedCount(providers + models + channels)
            .addedCount(providers + models + channels)
            .updatedCount(0)
            .syncedAt(Instant.now())
            .build();
    }

    /**
     * 同步内置元数据（启动时调用）
     */
    @Transactional
    public MetadataSyncResult syncBuiltinMetadata() {
        log.info("Starting builtin metadata sync");
        int providers = syncProviders();
        int models = syncModels();
        log.info("Builtin metadata sync completed");
        return MetadataSyncResult.builder()
            .syncedCount(providers + models)
            .addedCount(providers + models)
            .updatedCount(0)
            .syncedAt(Instant.now())
            .build();
    }

    /**
     * 从 models.dev 同步模型元数据（定时或手动触发）
     */
    @Transactional
    public MetadataSyncResult syncModelsDev() {
        log.info("Starting models.dev sync");
        // 从 models.dev API 拉取并同步
        List<ModelMetadata> models = modelMetadataGateway.findBySource(MetadataSource.MODELS_DEV);
        int count = 0;
        for (ModelMetadata model : models) {
            applyOrCreateModelSpec(model);
            count++;
        }
        log.info("models.dev sync completed, {} models processed", count);
        return MetadataSyncResult.builder()
            .syncedCount(count)
            .addedCount(count)
            .updatedCount(0)
            .syncedAt(Instant.now())
            .build();
    }

    private int syncProviders() {
        List<ProviderMetadata> providerMetas = providerMetadataGateway.findAllMetadata();
        int count = 0;
        for (ProviderMetadata meta : providerMetas) {
            // 根据 name 查找是否已存在
            if (providerGateway.findByName(meta.getProviderName()).isEmpty()) {
                Provider provider = new Provider();
                provider.setName(meta.getProviderName());
                provider.setDescription(meta.getDescription());
                provider.setState(ProviderState.ACTIVE);
                providerGateway.save(provider);
                count++;
            }
        }
        return count;
    }

    private int syncModels() {
        List<ModelMetadata> modelMetas = modelMetadataGateway.findBySource(MetadataSource.BUILTIN);
        int count = 0;
        for (ModelMetadata meta : modelMetas) {
            applyOrCreateModelSpec(meta);
            count++;
        }
        return count;
    }

    private int syncChannelCredentials() {
        List<ProductMetadata> productMetas = productMetadataGateway.findAll();
        int count = 0;
        for (ProductMetadata meta : productMetas) {
            // 从元数据端点推断 Channel 信息
            Map<String, String> endpoints = meta.getEndpoints();
            Channel channel = new Channel();
            channel.setProviderId(1L); // 默认 provider
            channel.setName(meta.getProviderId() + "-" + meta.getProductName());
            if (endpoints != null && !endpoints.isEmpty()) {
                String protocolKey = endpoints.containsKey("openai") ? "openai" : endpoints.keySet().iterator().next();
                channel.setEndpointUrl(endpoints.get(protocolKey));
                channel.setProtocol(Protocol.fromCode(protocolKey));
            }
            channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
            channel.setState(ChannelState.ACTIVE);
            channelGateway.save(channel);
            count++;
        }
        return count;
    }

    private void applyOrCreateModelSpec(ModelMetadata meta) {
        // 查找是否已存在同名 ModelSpec
        modelSpecGateway.findByProviderModelId(meta.getProviderModelId())
            .ifPresentOrElse(
                existing -> log.debug("ModelSpec already exists: {}", meta.getProviderModelId()),
                () -> {
                    ModelSpec spec = new ModelSpec();
                    spec.setProviderModelId(meta.getProviderModelId());
                    spec.setDisplayName(meta.getDisplayName());
                    spec.setContextWindow(meta.getContextWindow());
                    spec.setCapabilities(meta.getCapabilities());
                    spec.setState(ModelSpecState.ACTIVE);
                    modelSpecGateway.save(spec);
                }
            );
    }
}