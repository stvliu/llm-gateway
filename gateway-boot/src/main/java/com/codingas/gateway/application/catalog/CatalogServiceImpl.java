package com.codingas.gateway.application.catalog;

import com.codingas.gateway.application.catalog.dto.ModelCatalogResponse;
import com.codingas.gateway.application.catalog.dto.PlanCatalogResponse;
import com.codingas.gateway.application.catalog.dto.PlanDetailResponse;
import com.codingas.gateway.application.catalog.dto.ProviderCatalogResponse;
import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 目录查询服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final ProviderCatalogGateway providerCatalogGateway;
    private final PlanCatalogGateway planCatalogGateway;
    private final ModelCatalogGateway modelCatalogGateway;
    private final PlanModelCatalogGateway planModelCatalogGateway;
    private final ProviderGateway providerGateway;
    private final ChannelGateway channelGateway;
    private final ModelGateway modelGateway;
    private final ObjectMapper objectMapper;

    /** endpoints JSON 类型引用 */
    private static final TypeReference<List<Map<String, String>>> ENDPOINTS_TYPE =
            new TypeReference<>() {};

    /** pricing JSON 类型引用 */
    private static final TypeReference<List<Map<String, Object>>> PRICING_TYPE =
            new TypeReference<>() {};

    /** capabilities JSON 类型引用 */
    private static final TypeReference<Map<String, Boolean>> CAPABILITIES_TYPE =
            new TypeReference<>() {};

    @Override
    public List<ProviderCatalogResponse> listProviderCatalogs(String keyword) {
        List<ProviderCatalog> catalogs;

        if (keyword != null && !keyword.isBlank()) {
            catalogs = providerCatalogGateway.findByKeyword(keyword);
        } else {
            catalogs = providerCatalogGateway.findAll();
        }

        return catalogs.stream()
                .filter(c -> c.getState() == CatalogState.ACTIVE)
                .map(c -> ProviderCatalogResponse.builder()
                        .code(c.getProviderCode())
                        .name(c.getProviderName())
                        .materialized(isProviderMaterialized(c.getProviderCode()))
                        .build())
                .toList();
    }

    @Override
    public List<PlanCatalogResponse> listPlanCatalogs(String providerCode) {
        List<PlanCatalog> catalogs;

        if (providerCode != null && !providerCode.isBlank()) {
            catalogs = planCatalogGateway.findByProviderCode(providerCode);
        } else {
            catalogs = planCatalogGateway.findAll();
        }

        return catalogs.stream()
                .filter(c -> c.getState() == CatalogState.ACTIVE)
                .map(c -> PlanCatalogResponse.builder()
                        .planCode(c.getPlanCode())
                        .providerCode(c.getProviderCode())
                        .planName(c.getPlanName())
                        .billingMode(c.getBillingMode() != null ? c.getBillingMode().name() : null)
                        .materialized(isPlanMaterialized(c.getPlanCode()))
                        .build())
                .toList();
    }

    @Override
    public PlanDetailResponse getPlanDetail(String planCode) {
        PlanCatalog catalog = planCatalogGateway.findByPlanCode(planCode)
                .orElseThrow(() -> new com.codingas.gateway.domain.supply.catalog.exception.CatalogException(
                        "PLAN_NOT_FOUND", "套餐目录不存在: " + planCode));

        List<PlanDetailResponse.EndpointInfo> endpoints = parseEndpoints(catalog.getEndpoints());
        List<PlanDetailResponse.PricingInfo> pricing = parsePricing(catalog.getPricing());

        return PlanDetailResponse.builder()
                .planCode(catalog.getPlanCode())
                .providerCode(catalog.getProviderCode())
                .planName(catalog.getPlanName())
                .billingMode(catalog.getBillingMode() != null ? catalog.getBillingMode().name() : null)
                .description(catalog.getDescription())
                .endpoints(endpoints)
                .pricing(pricing)
                .materialized(isPlanMaterialized(planCode))
                .build();
    }

    @Override
    public List<ModelCatalogResponse> listModelCatalogs(String providerCode, String keyword, String capability) {
        List<ModelCatalog> catalogs;

        if (keyword != null && !keyword.isBlank()) {
            catalogs = modelCatalogGateway.findByKeyword(keyword);
        } else if (capability != null && !capability.isBlank()) {
            catalogs = modelCatalogGateway.findByCapability(capability);
        } else {
            catalogs = modelCatalogGateway.findAll();
        }

        // 如果指定了 providerCode，需要二次过滤
        // 由于 ModelCatalog 没有 providerCode 字段，需要通过 PlanModelCatalog 间接关联
        if (providerCode != null && !providerCode.isBlank()) {
            List<String> modelIds = planModelCatalogGateway.findAll().stream()
                    .filter(pm -> {
                        // 通过 PlanCatalog 找到属于该供应商的套餐
                        return planCatalogGateway.findByProviderCode(providerCode)
                                .stream()
                                .anyMatch(p -> p.getPlanCode().equals(pm.getPlanCode()));
                    })
                    .map(PlanModelCatalog::getModelName)
                    .distinct()
                    .toList();

            catalogs = catalogs.stream()
                    .filter(c -> modelIds.contains(c.getModelName()))
                    .toList();
        }

        return catalogs.stream()
                .filter(c -> c.getState() == CatalogState.ACTIVE)
                .map(c -> {
                    // 解析 capabilities 为能力名称列表
                    List<String> capList = parseCapabilities(c.getCapabilities());
                    return ModelCatalogResponse.builder()
                            .modelName(c.getModelName())
                            .displayName(c.getDisplayName())
                            .providerCode(findProviderCodeForModel(c.getModelName()))
                            .capabilities(capList)
                            .contextWindow(c.getContextWindow())
                            .maxOutputTokens(c.getMaxOutputTokens())
                            .materialized(isModelMaterialized(c.getModelName()))
                            .build();
                })
                .toList();
    }

    // ===== 解析辅助方法 =====

    /**
     * 解析 endpoints JSON 为结构化列表
     */
    private List<PlanDetailResponse.EndpointInfo> parseEndpoints(String endpointsJson) {
        if (endpointsJson == null || endpointsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, String>> raw = objectMapper.readValue(endpointsJson, ENDPOINTS_TYPE);
            return raw.stream()
                    .map(e -> PlanDetailResponse.EndpointInfo.builder()
                            .protocol(e.get("protocol"))
                            .url(e.get("url"))
                            .build())
                    .toList();
        } catch (Exception e) {
            log.warn("解析 endpoints JSON 失败: {}", endpointsJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析 pricing JSON 为结构化列表
     */
    private List<PlanDetailResponse.PricingInfo> parsePricing(String pricingJson) {
        if (pricingJson == null || pricingJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(pricingJson, PRICING_TYPE);
            return raw.stream()
                    .map(p -> PlanDetailResponse.PricingInfo.builder()
                            .providerModelId((String) p.get("providerModelId"))
                            .inputPrice(toBigDecimal(p.get("inputPrice")))
                            .outputPrice(toBigDecimal(p.get("outputPrice")))
                            .cacheReadPrice(toBigDecimal(p.get("cacheReadPrice")))
                            .build())
                    .toList();
        } catch (Exception e) {
            log.warn("解析 pricing JSON 失败: {}", pricingJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析 capabilities JSON 为能力名称列表
     */
    private List<String> parseCapabilities(String capabilitiesJson) {
        if (capabilitiesJson == null || capabilitiesJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            Map<String, Boolean> caps = objectMapper.readValue(capabilitiesJson, CAPABILITIES_TYPE);
            return caps.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .toList();
        } catch (Exception e) {
            log.warn("解析 capabilities JSON 失败: {}", capabilitiesJson, e);
            return Collections.emptyList();
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

    // ===== 物化状态检查 =====

    /**
     * 检查供应商是否已物化
     */
    private boolean isProviderMaterialized(String providerCode) {
        return providerGateway.findByCode(providerCode).isPresent();
    }

    /**
     * 检查套餐是否已物化（通过 Channel 名称关联）
     *
     * <p>套餐物化后创建的 Channel 名称规则为 planCode。</p>
     */
    private boolean isPlanMaterialized(String planCode) {
        return planCatalogGateway.findByPlanCode(planCode)
                .flatMap(plan -> providerGateway.findByCode(plan.getProviderCode())
                        .map(provider -> channelGateway.existsByProviderIdAndName(
                                provider.getId(), planCode)))
                .orElse(false);
    }

    /**
     * 检查模型是否已物化
     */
    private boolean isModelMaterialized(String modelName) {
        return modelGateway.findByModelName(modelName).isPresent();
    }

    /**
     * 通过 PlanModelCatalog 反查模型所属供应商
     */
    private String findProviderCodeForModel(String modelName) {
        List<PlanModelCatalog> associations =
                planModelCatalogGateway.findByModelName(modelName);
        if (associations.isEmpty()) {
            return null;
        }
        // 取第一个关联的套餐对应的供应商
        String planCode = associations.get(0).getPlanCode();
        return planCatalogGateway.findByPlanCode(planCode)
                .map(PlanCatalog::getProviderCode)
                .orElse(null);
    }
}