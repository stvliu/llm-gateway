package com.codingas.gateway.application.catalog;

import com.codingas.gateway.application.catalog.dto.ModelResponse;
import com.codingas.gateway.application.catalog.dto.PlanCatalogResponse;
import com.codingas.gateway.application.catalog.dto.PlanDetailResponse;
import com.codingas.gateway.application.catalog.dto.ProviderCatalogResponse;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.exception.CatalogException;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.entity.Provider;
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
 * 套餐目录查询服务实现
 *
 * <p>提供套餐目录、供应商目录、模型的查询功能。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanCatalogServiceImpl implements PlanCatalogService {

    private final PlanCatalogGateway planCatalogGateway;
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

    @Override
    public List<ProviderCatalogResponse> listProviderCatalogs(String keyword) {
        List<Provider> providers;

        if (keyword != null && !keyword.isBlank()) {
            providers = providerGateway.findByKeyword(keyword);
        } else {
            providers = providerGateway.findAll();
        }

        return providers.stream()
                .filter(p -> true)
                .map(p -> ProviderCatalogResponse.builder()
                        .code(p.getCode())
                        .name(p.getName())
                        .materialized(true) // Provider 本身就是物化后的实体
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
                .filter(c -> true)
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
                .orElseThrow(() -> new CatalogException(
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
    public List<PlanDetailResponse.PricingInfo> getPricing(String planCode) {
        PlanCatalog catalog = planCatalogGateway.findByPlanCode(planCode)
                .orElseThrow(() -> new CatalogException(
                        "PLAN_NOT_FOUND", "套餐目录不存在: " + planCode));
        return parsePricing(catalog.getPricing());
    }

    @Override
    public List<ModelResponse> listModels(String providerCode, String keyword, String capability) {
        List<Model> models;

        if (keyword != null && !keyword.isBlank()) {
            models = modelGateway.findByKeyword(keyword);
        } else if (capability != null && !capability.isBlank()) {
            models = modelGateway.findByCapability(capability);
        } else {
            models = modelGateway.findAll();
        }

        // 如果指定了 providerCode，需要二次过滤
        // 通过 PlanModelCatalog 间接关联
        if (providerCode != null && !providerCode.isBlank()) {
            List<String> modelNames = planModelCatalogGateway.findAll().stream()
                    .filter(pm -> {
                        // 通过 PlanCatalog 找到属于该供应商的套餐
                        return planCatalogGateway.findByProviderCode(providerCode)
                                .stream()
                                .anyMatch(p -> p.getPlanCode().equals(pm.getPlanCode()));
                    })
                    .map(PlanModelCatalog::getModelName)
                    .distinct()
                    .toList();

            models = models.stream()
                    .filter(m -> modelNames.contains(m.getModelName()))
                    .toList();
        }

        return models.stream()
                .filter(Model::isAvailable)
                .map(m -> {
                    // 从 Map<String, Boolean> 中提取能力名称列表
                    List<String> capList = extractCapabilityNames(m.getCapabilities());
                    return ModelResponse.builder()
                            .modelName(m.getModelName())
                            .displayName(m.getDisplayName())
                            .providerCode(findProviderCodeForModel(m.getModelName()))
                            .capabilities(capList)
                            .contextWindow(m.getContextWindow())
                            .maxOutputTokens(m.getMaxOutputTokens())
                            .materialized(true) // Model 本身就是物化后的实体
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
     * 从 capabilities Map 中提取能力名称列表
     */
    private List<String> extractCapabilityNames(Map<String, Boolean> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return Collections.emptyList();
        }
        return capabilities.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();
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