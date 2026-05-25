package com.codingas.gateway.application.catalog;

import com.codingas.gateway.application.catalog.dto.MaterializeResult;
import com.codingas.gateway.domain.supply.catalog.entity.ModelSpecCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.exception.CatalogException;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelSpecCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 目录物化服务
 *
 * <p>将 catalog 条目一次性转化为运营实体（Provider、Channel、ChannelEndpoint、ChannelModel、ModelSpec）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogMaterializeService {

    private final ProviderCatalogGateway providerCatalogGateway;
    private final PlanCatalogGateway planCatalogGateway;
    private final PlanModelCatalogGateway planModelCatalogGateway;
    private final ModelSpecCatalogGateway modelSpecCatalogGateway;
    private final ProviderGateway providerGateway;
    private final ChannelGateway channelGateway;
    private final ChannelEndpointGateway channelEndpointGateway;
    private final ChannelModelGateway channelModelGateway;
    private final ModelSpecGateway modelSpecGateway;
    private final ObjectMapper objectMapper;

    /**
     * 物化供应商
     *
     * <p>从 ProviderCatalog 创建 Provider 运营实体。</p>
     *
     * @param providerCode 供应商编码
     * @return 物化结果
     */
    @Transactional
    public MaterializeResult materializeProvider(String providerCode) {
        // 检查是否已物化
        if (providerGateway.findByCode(providerCode).isPresent()) {
            throw new CatalogException("ALREADY_MATERIALIZED",
                    "供应商已物化: " + providerCode);
        }

        // 查找目录条目
        ProviderCatalog catalog = providerCatalogGateway.findByProviderCode(providerCode)
                .orElseThrow(() -> new CatalogException("CATALOG_NOT_FOUND",
                        "供应商目录不存在: " + providerCode));

        // 创建 Provider 运营实体
        Provider provider = new Provider();
        provider.setCode(catalog.getProviderCode());
        provider.setName(catalog.getProviderName());
        provider.setLogoUrl(catalog.getLogoUrl());
        provider.setWebsiteUrl(catalog.getWebsiteUrl());
        provider.setDescription(catalog.getDescription());
        provider.setPriority(100);
        provider.setState(ProviderState.ACTIVE);

        Provider saved = providerGateway.save(provider);
        log.info("物化供应商成功: code={}, id={}", providerCode, saved.getId());

        return MaterializeResult.builder()
                .type("PROVIDER")
                .code(providerCode)
                .entityId(saved.getId())
                .status("CREATED")
                .build();
    }

    /**
     * 物化套餐
     *
     * <p>从 PlanCatalog 创建 Channel + ChannelEndpoint + ChannelModel 运营实体。</p>
     * <p>物化 Plan 时如果 ModelSpec 不存在，自动级联物化。</p>
     *
     * @param planCode 套餐编码
     * @return 物化结果
     */
    @Transactional
    public MaterializeResult materializePlan(String planCode) {
        // 查找目录条目
        PlanCatalog catalog = planCatalogGateway.findByPlanCode(planCode)
                .orElseThrow(() -> new CatalogException("CATALOG_NOT_FOUND",
                        "套餐目录不存在: " + planCode));

        // 确保供应商已物化
        Provider provider = providerGateway.findByCode(catalog.getProviderCode())
                .orElseThrow(() -> new CatalogException("PROVIDER_NOT_MATERIALIZED",
                        "供应商尚未物化: " + catalog.getProviderCode() + "，请先物化供应商"));

        // 检查是否已物化（以 planCode 为名称查找 Channel）
        if (channelGateway.existsByProviderIdAndName(provider.getId(), planCode)) {
            throw new CatalogException("ALREADY_MATERIALIZED",
                    "套餐已物化: " + planCode);
        }

        // 创建 Channel
        Channel channel = new Channel();
        channel.setProviderId(provider.getId());
        channel.setName(planCode);
        channel.setBillingMode(mapBillingMode(catalog.getBillingMode()));
        channel.setPriority(100);
        channel.setWeight(100);
        channel.setTimeout(30);
        channel.setMaxRetries(3);
        channel.setState(ChannelState.ACTIVE);

        Channel savedChannel = channelGateway.save(channel);
        log.info("物化套餐-创建渠道成功: planCode={}, channelId={}", planCode, savedChannel.getId());

        // 创建 ChannelEndpoint
        List<Map<String, String>> endpoints = parseEndpoints(catalog.getEndpoints());
        for (Map<String, String> ep : endpoints) {
            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setChannelId(savedChannel.getId());
            endpoint.setProtocol(Protocol.valueOf(ep.get("protocol")));
            endpoint.setEndpointUrl(ep.get("url"));
            endpoint.setState(ChannelEndpointState.ACTIVE);
            channelEndpointGateway.save(endpoint);
        }
        log.info("物化套餐-创建端点成功: planCode={}, count={}", planCode, endpoints.size());

        // 创建 ChannelModel（从 pricing JSON 解析）
        List<Map<String, Object>> pricing = parsePricing(catalog.getPricing());
        for (Map<String, Object> p : pricing) {
            String providerModelId = (String) p.get("providerModelId");

            // 级联物化 ModelSpec（如果不存在）
            ModelSpec modelSpec = findOrCreateModelSpec(providerModelId, provider.getId());

            ChannelModel channelModel = new ChannelModel();
            channelModel.setChannelId(savedChannel.getId());
            channelModel.setModelSpecId(modelSpec.getId());
            channelModel.setInputPrice(toBigDecimal(p.get("inputPrice")));
            channelModel.setOutputPrice(toBigDecimal(p.get("outputPrice")));
            channelModel.setCacheReadPrice(toBigDecimal(p.get("cacheReadPrice")));
            channelModel.setReasoningPrice(toBigDecimal(p.get("reasoningPrice")));
            channelModel.setCacheWritePrice(toBigDecimal(p.get("cacheWritePrice")));
            channelModel.setInputAudioPrice(toBigDecimal(p.get("inputAudioPrice")));
            channelModel.setOutputAudioPrice(toBigDecimal(p.get("outputAudioPrice")));
            channelModel.setState(ChannelModelState.ACTIVE);
            channelModelGateway.save(channelModel);
        }
        log.info("物化套餐-创建渠道模型成功: planCode={}, count={}", planCode, pricing.size());

        return MaterializeResult.builder()
                .type("PLAN")
                .code(planCode)
                .entityId(savedChannel.getId())
                .status("CREATED")
                .build();
    }

    /**
     * 物化模型规格
     *
     * <p>从 ModelSpecCatalog 创建 ModelSpec 运营实体。</p>
     *
     * @param providerModelId 供应商模型标识
     * @return 物化结果
     */
    @Transactional
    public MaterializeResult materializeModelSpec(String providerModelId) {
        // 检查是否已物化
        if (modelSpecGateway.findByProviderModelId(providerModelId).isPresent()) {
            throw new CatalogException("ALREADY_MATERIALIZED",
                    "模型规格已物化: " + providerModelId);
        }

        // 查找目录条目
        ModelSpecCatalog catalog = modelSpecCatalogGateway.findByProviderModelId(providerModelId)
                .orElseThrow(() -> new CatalogException("CATALOG_NOT_FOUND",
                        "模型规格目录不存在: " + providerModelId));

        // 查找供应商 ID（通过 PlanModelCatalog 反查）
        Long providerId = findProviderIdForModel(providerModelId);

        // TODO: providerId 已从 ModelSpec 移除，后续通过 Supply 实体关联
        // 创建 ModelSpec 运营实体
        ModelSpec spec = new ModelSpec();
        spec.setProviderModelId(catalog.getProviderModelId());
        spec.setDisplayName(catalog.getDisplayName());
        spec.setModelFamily(catalog.getModelFamily());
        spec.setContextWindow(catalog.getContextWindow());
        spec.setMaxInputTokens(catalog.getMaxInputTokens());
        spec.setMaxOutputTokens(catalog.getMaxOutputTokens());
        spec.setCapabilities(parseCapabilitiesMap(catalog.getCapabilities()));
        spec.setModalities(parseModalitiesList(catalog.getModalities()));
        spec.setPriority(100);
        spec.setWeight(100);
        spec.setState(ModelSpecState.ACTIVE);

        ModelSpec saved = modelSpecGateway.save(spec);
        log.info("物化模型规格成功: providerModelId={}, id={}", providerModelId, saved.getId());

        return MaterializeResult.builder()
                .type("MODEL_SPEC")
                .code(providerModelId)
                .entityId(saved.getId())
                .status("CREATED")
                .build();
    }

    // ===== 辅助方法 =====

    /**
     * 查找或创建 ModelSpec（级联物化）
     */
    private ModelSpec findOrCreateModelSpec(String providerModelId, Long providerId) {
        Optional<ModelSpec> existing = modelSpecGateway.findByProviderModelId(providerModelId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 从目录创建
        ModelSpecCatalog catalog = modelSpecCatalogGateway.findByProviderModelId(providerModelId)
                .orElse(null);

        ModelSpec spec = new ModelSpec();
        // TODO: providerId 已从 ModelSpec 移除，后续通过 Supply 实体关联
        spec.setProviderModelId(providerModelId);
        if (catalog != null) {
            spec.setDisplayName(catalog.getDisplayName());
            spec.setModelFamily(catalog.getModelFamily());
            spec.setContextWindow(catalog.getContextWindow());
            spec.setMaxInputTokens(catalog.getMaxInputTokens());
            spec.setMaxOutputTokens(catalog.getMaxOutputTokens());
            spec.setCapabilities(parseCapabilitiesMap(catalog.getCapabilities()));
            spec.setModalities(parseModalitiesList(catalog.getModalities()));
        } else {
            spec.setDisplayName(providerModelId);
        }
        spec.setPriority(100);
        spec.setWeight(100);
        spec.setState(ModelSpecState.ACTIVE);

        ModelSpec saved = modelSpecGateway.save(spec);
        log.info("级联物化模型规格: providerModelId={}, id={}", providerModelId, saved.getId());
        return saved;
    }

    /**
     * 映射计费模式：catalog BillingMode → supply BillingMode
     */
    private BillingMode mapBillingMode(com.codingas.gateway.domain.supply.catalog.enums.BillingMode catalogMode) {
        if (catalogMode == null) {
            return BillingMode.PAY_AS_YOU_GO;
        }
        return switch (catalogMode) {
            case PAY_AS_YOU_GO -> BillingMode.PAY_AS_YOU_GO;
            case SUBSCRIPTION -> BillingMode.SUBSCRIPTION_CODING;
            case PACKAGE -> BillingMode.SUBSCRIPTION_TOKEN;
        };
    }

    /**
     * 解析 endpoints JSON
     */
    private List<Map<String, String>> parseEndpoints(String endpointsJson) {
        if (endpointsJson == null || endpointsJson.isBlank()) {
            return List.of();
        }
        try {
            var typeRef = new TypeReference<List<Map<String, String>>>() {};
            return objectMapper.readValue(endpointsJson, typeRef);
        } catch (Exception e) {
            log.warn("解析 endpoints JSON 失败: {}", endpointsJson, e);
            return List.of();
        }
    }

    /**
     * 解析 pricing JSON
     */
    private List<Map<String, Object>> parsePricing(String pricingJson) {
        if (pricingJson == null || pricingJson.isBlank()) {
            return List.of();
        }
        try {
            var typeRef = new TypeReference<List<Map<String, Object>>>() {};
            return objectMapper.readValue(pricingJson, typeRef);
        } catch (Exception e) {
            log.warn("解析 pricing JSON 失败: {}", pricingJson, e);
            return List.of();
        }
    }

    /**
     * 解析 capabilities JSON 为 Map
     */
    private Map<String, Boolean> parseCapabilitiesMap(String capabilitiesJson) {
        if (capabilitiesJson == null || capabilitiesJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(capabilitiesJson, new TypeReference<Map<String, Boolean>>() {});
        } catch (Exception e) {
            log.warn("解析 capabilities JSON 失败: {}", capabilitiesJson, e);
            return Map.of();
        }
    }

    /**
     * 解析 modalities JSON 为 List
     */
    private List<String> parseModalitiesList(String modalitiesJson) {
        if (modalitiesJson == null || modalitiesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(modalitiesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 modalities JSON 失败: {}", modalitiesJson, e);
            return List.of();
        }
    }

    /**
     * 将 Object 转为 BigDecimal
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    /**
     * 通过 PlanModelCatalog 反查模型所属供应商 ID
     */
    private Long findProviderIdForModel(String providerModelId) {
        List<com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog> associations =
                planModelCatalogGateway.findByProviderModelId(providerModelId);
        if (associations.isEmpty()) {
            return null;
        }
        String planCode = associations.get(0).getPlanCode();
        return planCatalogGateway.findByPlanCode(planCode)
                .flatMap(plan -> providerGateway.findByCode(plan.getProviderCode())
                        .map(Provider::getId))
                .orElse(null);
    }
}