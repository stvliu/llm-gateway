package com.codingas.gateway.application.catalog;

import com.codingas.gateway.application.catalog.dto.MaterializeBatchRequest;
import com.codingas.gateway.application.catalog.dto.MaterializeBatchResult;
import com.codingas.gateway.application.catalog.dto.MaterializePlanRequest;
import com.codingas.gateway.application.catalog.dto.MaterializeResult;
import com.codingas.gateway.application.catalog.dto.PlanResult;
import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.exception.CatalogException;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.enums.ModelState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
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
 * <p>将 catalog 条目一次性转化为运营实体（Provider、Channel、ChannelEndpoint、ChannelModel、Model）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogMaterializeService {

    private final ProviderCatalogGateway providerCatalogGateway;
    private final PlanCatalogGateway planCatalogGateway;
    private final PlanModelCatalogGateway planModelCatalogGateway;
    private final ModelCatalogGateway modelCatalogGateway;
    private final ProviderGateway providerGateway;
    private final ChannelGateway channelGateway;
    private final ChannelEndpointGateway channelEndpointGateway;
    private final ChannelModelGateway channelModelGateway;
    private final ChannelCredentialGateway channelCredentialGateway;
    private final ModelGateway modelGateway;
    private final ObjectMapper objectMapper;

    /** 内置上游模型名映射规则表 */
    private static final Map<String, Map<String, String>> UPSTREAM_MODEL_NAME_RULES = Map.of(
            "aws-bedrock", Map.ofEntries(
                    Map.entry("claude-opus-4-7", "anthropic.claude-opus-4-7"),
                    Map.entry("claude-sonnet-4-6", "anthropic.claude-sonnet-4-6"),
                    Map.entry("claude-haiku-4-5", "anthropic.claude-haiku-4-5-20251001-v1:0"),
                    Map.entry("claude-3-opus-20240229", "anthropic.claude-3-opus-20240229-v1:0"),
                    Map.entry("claude-3-sonnet-20240229", "anthropic.claude-3-sonnet-20240229-v1:0"),
                    Map.entry("claude-3-haiku-20240307", "anthropic.claude-3-haiku-20240307-v1:0")
            ),
            "azure-openai", Map.of(
                    "chat-latest", "gpt-chat-latest"
            )
    );

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
        if (providerGateway.findByCode(providerCode).isPresent()) {
            throw new CatalogException("ALREADY_MATERIALIZED",
                    "供应商已物化: " + providerCode);
        }

        ProviderCatalog catalog = providerCatalogGateway.findByProviderCode(providerCode)
                .orElseThrow(() -> new CatalogException("CATALOG_NOT_FOUND",
                        "供应商目录不存在: " + providerCode));

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
     * 级联物化供应商（含关联 Plans）
     *
     * <p>从 ProviderCatalog 创建 Provider，并级联物化该供应商下所有（或指定）的 Plans。</p>
     * <p>如果 Provider 已物化、Plan 已物化，自动跳过并计入 SKIPPED 统计。</p>
     *
     * @param providerCode 供应商编码
     * @param request      批量物化请求（可选 planCodes）
     * @return 批量物化结果
     */
    @Transactional(timeout = 30)
    public MaterializeBatchResult materializeProviderWithPlans(String providerCode, MaterializeBatchRequest request) {
        // 1. 物化 Provider（已存在则跳过）
        boolean providerAlreadyExists = providerGateway.findByCode(providerCode).isPresent();
        if (!providerAlreadyExists) {
            materializeProvider(providerCode);
        }

        // 2. 查询关联 Plans
        List<PlanCatalog> allPlans = planCatalogGateway.findByProviderCode(providerCode);
        List<String> targetPlanCodes;

        if (request != null && request.getPlanCodes() != null && !request.getPlanCodes().isEmpty()) {
            targetPlanCodes = request.getPlanCodes();
        } else {
            targetPlanCodes = allPlans.stream()
                    .filter(p -> p.getState() == null || p.getState() == CatalogState.ACTIVE)
                    .map(PlanCatalog::getPlanCode)
                    .toList();
        }

        // 3. 逐条物化 Plan
        List<PlanResult> results = new java.util.ArrayList<>();
        int successCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (String planCode : targetPlanCodes) {
            PlanResult.PlanResultBuilder builder = PlanResult.builder()
                    .type("PLAN")
                    .planCode(planCode);

            try {
                MaterializeResult result = materializePlan(planCode);
                builder.entityId(result.getEntityId());
                builder.status(result.getStatus());
                if ("CREATED".equals(result.getStatus())) {
                    successCount++;
                } else {
                    skippedCount++;
                }
            } catch (CatalogException e) {
                if ("ALREADY_MATERIALIZED".equals(e.getCode())) {
                    builder.status("SKIPPED");
                    skippedCount++;
                } else {
                    builder.status("FAILED");
                    builder.errorMessage(e.getMessage());
                    failedCount++;
                }
            } catch (Exception e) {
                builder.status("FAILED");
                builder.errorMessage(e.getMessage());
                failedCount++;
            }

            results.add(builder.build());
        }

        // 4. 汇总
        return MaterializeBatchResult.builder()
                .providerCode(providerCode)
                .totalCount(targetPlanCodes.size())
                .successCount(successCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .results(results)
                .build();
    }

    /**
     * 物化套餐
     *
     * <p>从 PlanCatalog 创建 Channel + ChannelEndpoint + ChannelModel 运营实体。</p>
     * <p>物化 Plan 时如果 Model 不存在，自动级联物化。</p>
     */
    @Transactional
    public MaterializeResult materializePlan(String planCode) {
        PlanCatalog catalog = planCatalogGateway.findByPlanCode(planCode)
                .orElseThrow(() -> new CatalogException("CATALOG_NOT_FOUND",
                        "套餐目录不存在: " + planCode));

        Provider provider = providerGateway.findByCode(catalog.getProviderCode())
                .orElseThrow(() -> new CatalogException("PROVIDER_NOT_MATERIALIZED",
                        "供应商尚未物化: " + catalog.getProviderCode() + "，请先物化供应商"));

        if (channelGateway.existsByProviderIdAndName(provider.getId(), planCode)) {
            throw new CatalogException("ALREADY_MATERIALIZED",
                    "套餐已物化: " + planCode);
        }

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

        List<Map<String, Object>> pricing = parsePricing(catalog.getPricing());
        for (Map<String, Object> p : pricing) {
            String modelName = (String) p.get("modelName");

            Model model = findOrCreateModel(modelName);

            ChannelModel channelModel = new ChannelModel();
            channelModel.setChannelId(savedChannel.getId());
            channelModel.setModelId(model.getId());
            // 预填上游模型名
            String resolved = resolveUpstreamModelName(provider.getCode(), modelName);
            channelModel.setUpstreamModelName(resolved);
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
     * 物化套餐（扩展版）
     *
     * <p>支持批量创建 API Key 凭证和自定义端点/模型配置。</p>
     * <p>先完成基础物化，再根据 request 中的扩展配置进行补充。</p>
     *
     * @param planCode 套餐编码
     * @param request  扩展请求（apiKeys / endpoints / models）
     * @return 物化结果
     */
    @Transactional
    public MaterializeResult materializePlan(String planCode, MaterializePlanRequest request) {
        // 1. 先完成基础物化
        MaterializeResult baseResult = materializePlan(planCode);

        // 2. 如果不是 CREATED（已有），直接返回
        if (!"CREATED".equals(baseResult.getStatus())) {
            return baseResult;
        }

        Long channelId = baseResult.getEntityId();

        // 3. 批量创建 API Key 凭证
        if (request != null && request.getApiKeys() != null && !request.getApiKeys().isEmpty()) {
            int priority = 1; // 从 1 开始递增
            for (String apiKey : request.getApiKeys()) {
                if (apiKey == null || apiKey.isBlank()) {
                    continue;
                }

                ChannelCredential credential = new ChannelCredential();
                credential.setChannelId(channelId);
                credential.setApiKeyPlain(apiKey);
                // 提取前缀（最多 8 位）
                String keyPrefix = apiKey.substring(0, Math.min(8, apiKey.length()));
                credential.setApiKeyPrefix(keyPrefix);
                credential.setPriority(priority);
                credential.setWeight(100);
                credential.setState(CredentialState.ACTIVE);

                // Gateway 内部处理加密存储
                channelCredentialGateway.save(credential);
                priority++;
            }
            log.info("物化套餐-批量创建凭证成功: planCode={}, count={}", planCode, request.getApiKeys().size());
        }

        // 4. 自定义端点（暂时跳过，等后续扩展）
        // TODO: 实现自定义端点逻辑

        // 5. 自定义模型（暂时跳过，等后续扩展）
        // TODO: 实现自定义模型逻辑

        return baseResult;
    }

    /**
     * 物化模型
     *
     * <p>从 ModelCatalog 创建 Model 运营实体。</p>
     */
    @Transactional
    public MaterializeResult materializeModel(String modelName) {
        if (modelGateway.findByModelName(modelName).isPresent()) {
            throw new CatalogException("ALREADY_MATERIALIZED",
                    "模型已物化: " + modelName);
        }

        ModelCatalog catalog = modelCatalogGateway.findByModelName(modelName)
                .orElseThrow(() -> new CatalogException("CATALOG_NOT_FOUND",
                        "模型目录不存在: " + modelName));

        Model model = new Model();
        model.setModelName(catalog.getModelName());
        model.setDisplayName(catalog.getDisplayName());
        model.setModelFamily(catalog.getModelFamily());
        model.setContextWindow(catalog.getContextWindow());
        model.setMaxInputTokens(catalog.getMaxInputTokens());
        model.setMaxOutputTokens(catalog.getMaxOutputTokens());
        model.setCapabilities(parseCapabilitiesMap(catalog.getCapabilities()));
        model.setModalities(parseModalitiesList(catalog.getModalities()));
        model.setState(ModelState.ACTIVE);

        Model saved = modelGateway.save(model);
        log.info("物化模型成功: modelName={}, id={}", modelName, saved.getId());

        return MaterializeResult.builder()
                .type("MODEL")
                .code(modelName)
                .entityId(saved.getId())
                .status("CREATED")
                .build();
    }

    // ===== 辅助方法 =====

    /**
     * 查找或创建 Model（级联物化）
     */
    private Model findOrCreateModel(String modelName) {
        Optional<Model> existing = modelGateway.findByModelName(modelName);
        if (existing.isPresent()) {
            return existing.get();
        }

        ModelCatalog catalog = modelCatalogGateway.findByModelName(modelName)
                .orElse(null);

        Model model = new Model();
        model.setModelName(modelName);
        if (catalog != null) {
            model.setDisplayName(catalog.getDisplayName());
            model.setModelFamily(catalog.getModelFamily());
            model.setContextWindow(catalog.getContextWindow());
            model.setMaxInputTokens(catalog.getMaxInputTokens());
            model.setMaxOutputTokens(catalog.getMaxOutputTokens());
            model.setCapabilities(parseCapabilitiesMap(catalog.getCapabilities()));
            model.setModalities(parseModalitiesList(catalog.getModalities()));
        } else {
            model.setDisplayName(modelName);
        }
        model.setState(ModelState.ACTIVE);

        Model saved = modelGateway.save(model);
        log.info("级联物化模型: modelName={}, id={}", modelName, saved.getId());
        return saved;
    }

    /**
     * 解析上游模型名
     *
     * <p>根据供应商编码和模型名，在内置映射表中查找对应的上游模型名。
     * 未命中则返回 null（走默认值 = Model.modelName）。</p>
     *
     * @param providerCode 供应商编码
     * @param modelName    用户面模型名
     * @return 上游模型名，null 表示与 modelName 相同
     */
    private String resolveUpstreamModelName(String providerCode, String modelName) {
        Map<String, String> rules = UPSTREAM_MODEL_NAME_RULES.get(providerCode);
        if (rules == null) {
            return null;
        }
        return rules.get(modelName);
    }

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

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return new BigDecimal(value.toString());
    }

    private Long findProviderIdForModel(String modelName) {
        List<PlanModelCatalog> associations =
                planModelCatalogGateway.findByModelName(modelName);
        if (associations.isEmpty()) return null;
        String planCode = associations.get(0).getPlanCode();
        return planCatalogGateway.findByPlanCode(planCode)
                .flatMap(plan -> providerGateway.findByCode(plan.getProviderCode())
                        .map(Provider::getId))
                .orElse(null);
    }
}