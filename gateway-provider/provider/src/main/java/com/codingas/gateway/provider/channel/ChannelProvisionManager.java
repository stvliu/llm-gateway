/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.provider.channel;

import com.codingas.gateway.provider.catalog.BatchProvisionResult;
import com.codingas.gateway.provider.catalog.ProvisionResult;
import com.codingas.gateway.provider.catalog.PlanCatalog;
import com.codingas.gateway.provider.catalog.CatalogException;
import com.codingas.gateway.provider.catalog.PlanCatalogRepository;
import com.codingas.gateway.provider.catalog.PlanModelCatalogRepository;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.upstream.Protocol;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.model.ModelInstanceRepository;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 渠道开通服务
 *
 * <p>负责将套餐目录（PlanCatalog）转化为运营实体（Channel、ChannelEndpoint、ModelInstance）。</p>
 * <p>替代原 CatalogMaterializeService 的核心物化功能。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelProvisionManager {

    private final PlanCatalogRepository planCatalogRepository;
    private final PlanModelCatalogRepository planModelCatalogRepository;
    private final ProviderRepository providerRepository;
    private final ChannelRepository channelRepository;
    private final ChannelEndpointRepository channelEndpointRepository;
    private final ModelInstanceRepository modelInstanceRepository;
    private final ChannelCredentialRepository channelCredentialRepository;
    private final ModelRepository modelRepository;
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
     * 开通套餐
     *
     * <p>从 PlanCatalog 创建 Channel + ChannelEndpoint + ModelInstance 运营实体。</p>
     * <p>开通时如果 Model 不存在，自动级联创建。</p>
     * <p>如果 Provider 尚未开通，自动先创建 Provider。</p>
     *
     * @param planCode 套餐编码
     * @return 开通结果
     */
    @Transactional
    public ProvisionResult provisionFromPlan(String planCode) {
        return provisionFromPlan(planCode, null);
    }

    /**
     * 开通套餐（扩展版）
     *
     * <p>支持批量创建 API Key 凭证。</p>
     *
     * @param planCode 套餐编码
     * @param request  扩展请求（apiKeys）
     * @return 开通结果
     */
    @Transactional
    public ProvisionResult provisionFromPlan(String planCode, ProvisionCommand command) {
        PlanCatalog catalog = planCatalogRepository.findByPlanCode(planCode)
                .orElseThrow(() -> new CatalogException("CATALOG_NOT_FOUND",
                        "套餐目录不存在: " + planCode));

        // 入口校验：若提供 inlineProvider，其 code 必须与套餐解析的 providerCode 一致
        ProvisionCommand.InlineProviderCommand inline =
                command != null ? command.inlineProvider() : null;
        if (inline != null && inline.code() != null
                && !Objects.equals(inline.code(), catalog.getProviderCode())) {
            throw new CatalogException("INLINE_PROVIDER_CODE_MISMATCH",
                    "INLINE_PROVIDER_CODE_MISMATCH: inlineProvider.code 与套餐 providerCode 不一致, inline="
                            + inline.code() + ", plan=" + catalog.getProviderCode());
        }

        // 自动创建 Provider（如果尚未存在）；inline 仅在新建路径生效
        Provider provider = ensureProvider(catalog.getProviderCode(), inline);

        // 检查 Channel 是否已存在
        if (channelRepository.existsByProviderIdAndName(provider.getId(), planCode)) {
            return ProvisionResult.skipped(planCode, "套餐已开通");
        }

        // 创建 Channel
        Channel channel = new Channel();
        channel.setProviderId(provider.getId());
        channel.setName(planCode);
        channel.setBillingMode(catalog.getBillingMode());
        channel.setTimeout(30);
        channel.setMaxRetries(3);
        channel.setState(ChannelState.ACTIVE);

        Channel savedChannel = channelRepository.save(channel);
        log.info("开通套餐-创建渠道成功: planCode={}, channelId={}", planCode, savedChannel.getId());

        // 创建 ChannelEndpoint
        List<Map<String, String>> endpoints = parseEndpoints(catalog.getEndpoints());
        for (Map<String, String> ep : endpoints) {
            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setChannelId(savedChannel.getId());
            endpoint.setProtocol(Protocol.valueOf(ep.get("protocol")));
            endpoint.setEndpointUrl(ep.get("url"));
            channelEndpointRepository.save(endpoint);
        }
        log.info("开通套餐-创建端点成功: planCode={}, count={}", planCode, endpoints.size());

        // 创建 ModelInstance
        List<Map<String, Object>> pricing = parsePricing(catalog.getPricing());
        for (Map<String, Object> p : pricing) {
            String modelName = (String) p.get("providerModelId");

            Model model = ensureModel(modelName);

            ModelInstance modelInstance = new ModelInstance();
            modelInstance.setChannelId(savedChannel.getId());
            modelInstance.setModelId(model.getId());
            // 预填上游模型名
            String resolved = resolveUpstreamModelName(provider.getCode(), modelName);
            modelInstance.setUpstreamModelName(resolved);
            modelInstance.setPriority(100);
            modelInstance.setWeight(100);
            modelInstance.setState(ModelInstance.State.ACTIVE);
            modelInstanceRepository.save(modelInstance);
        }
        log.info("开通套餐-创建模型实例成功: planCode={}, count={}", planCode, pricing.size());

        // 批量创建 API Key 凭证
        if (command != null && command.apiKeys() != null && !command.apiKeys().isEmpty()) {
            int priority = 1;
            for (String apiKey : command.apiKeys()) {
                if (apiKey == null || apiKey.isBlank()) {
                    continue;
                }

                ChannelCredential credential = new ChannelCredential();
                credential.setChannelId(savedChannel.getId());
                credential.setApiKeyPlain(apiKey);
                // 提取前缀（最多 8 位）
                String keyPrefix = apiKey.substring(0, Math.min(8, apiKey.length()));
                credential.setApiKeyPrefix(keyPrefix);
                credential.setPriority(priority);
                credential.setWeight(100);

                // Gateway 内部处理加密存储
                channelCredentialRepository.save(credential);
                priority++;
            }
            log.info("开通套餐-批量创建凭证成功: planCode={}, count={}", planCode, command.apiKeys().size());
        }

        return ProvisionResult.created(planCode, savedChannel.getId(), endpoints.size(), pricing.size());
    }

    /**
     * 级联开通供应商（含关联套餐）
     *
     * <p>创建 Provider（如不存在），并级联开通该供应商下所有（或指定）的套餐。</p>
     * <p>如果套餐已开通，自动跳过并计入 SKIPPED 统计。</p>
     *
     * @param providerCode 供应商编码
     * @param request      批量开通请求（可选 planCodes）
     * @return 批量开通结果
     */
    @Transactional(timeout = 30)
    public BatchProvisionResult provisionBatch(String providerCode, BatchProvisionCommand command) {
        // 1. 确保 Provider 存在
        ensureProvider(providerCode);

        // 2. 查询关联套餐
        List<PlanCatalog> allPlans = planCatalogRepository.findByProviderCode(providerCode);
        List<String> targetPlanCodes;

        if (command != null && command.planCodes() != null && !command.planCodes().isEmpty()) {
            targetPlanCodes = command.planCodes();
        } else {
            targetPlanCodes = allPlans.stream()
                    .map(PlanCatalog::getPlanCode)
                    .toList();
        }

        // 3. 逐条开通套餐
        List<ProvisionResult> results = new ArrayList<>();
        int successCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (String planCode : targetPlanCodes) {
            try {
                ProvisionResult result = provisionFromPlan(planCode);
                results.add(result);

                switch (result.getStatus()) {
                    case "CREATED" -> successCount++;
                    case "SKIPPED" -> skippedCount++;
                    case "FAILED" -> failedCount++;
                }
            } catch (CatalogException e) {
                ProvisionResult result = ProvisionResult.failed(planCode, e.getMessage());
                results.add(result);
                failedCount++;
            } catch (Exception e) {
                ProvisionResult result = ProvisionResult.failed(planCode, e.getMessage());
                results.add(result);
                failedCount++;
                log.error("开通套餐失败: planCode={}", planCode, e);
            }
        }

        // 4. 汇总
        return BatchProvisionResult.builder()
                .providerCode(providerCode)
                .totalCount(targetPlanCodes.size())
                .successCount(successCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .results(results)
                .build();
    }

    /**
     * 开通模型
     *
     * <p>创建 Model 运营实体。</p>
     *
     * @param modelName 模型名称
     * @return 开通结果
     */
    @Transactional
    public ProvisionResult provisionModel(String modelName) {
        if (modelRepository.findByModelName(modelName).isPresent()) {
            return ProvisionResult.skipped(modelName, "模型已存在");
        }

        Model model = new Model();
        model.setModelName(modelName);
        model.setDisplayName(modelName);

        Model saved = modelRepository.save(model);
        log.info("开通模型成功: modelName={}, id={}", modelName, saved.getId());

        return ProvisionResult.builder()
                .planCode(modelName)
                .channelId(saved.getId())
                .status("CREATED")
                .build();
    }

    // ===== 辅助方法 =====

    /**
     * 查找或创建 Provider（无 inline 入参的旧默认行为）
     *
     * <p>用于内部级联场景（如 provisionBatch），等价于 {@code ensureProvider(providerCode, null)}。</p>
     */
    private Provider ensureProvider(String providerCode) {
        return ensureProvider(providerCode, null);
    }

    /**
     * 查找或创建 Provider，支持 inlineProvider 字段填充
     *
     * <p>三路径行为：</p>
     * <ul>
     *     <li>providerCode 已存在 → 直接返回，inline 被忽略，不触发 save</li>
     *     <li>providerCode 不存在 + inline 非空 → 用 inline 字段填充 name/description/websiteUrl/apiDocUrl</li>
     *     <li>providerCode 不存在 + inline 为空 → 走旧默认级联（name=providerCode）</li>
     * </ul>
     *
     * @param providerCode 供应商程序标识
     * @param inline       内联供应商参数；为空时走默认级联
     * @return 现有或新建的 Provider
     */
    private Provider ensureProvider(String providerCode, ProvisionCommand.InlineProviderCommand inline) {
        return providerRepository.findByCode(providerCode).orElseGet(() -> {
            Provider provider = new Provider();
            provider.setCode(providerCode);
            provider.setPriority(100);
            if (inline != null) {
                // inline.name 为空时回退 providerCode，避免 name 列为 null 违反约束
                provider.setName(Optional.ofNullable(inline.name()).orElse(providerCode));
                provider.setDescription(inline.description());
                provider.setWebsiteUrl(inline.websiteUrl());
                provider.setApiDocUrl(inline.apiDocUrl());
            } else {
                provider.setName(providerCode);
            }
            Provider saved = providerRepository.save(provider);
            log.info("自动创建供应商: code={}, inline={}, id={}", providerCode, inline != null, saved.getId());
            return saved;
        });
    }

    /**
     * 查找或创建 Model（级联开通）
     */
    private Model ensureModel(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            throw new CatalogException("INVALID_CATALOG_DATA", "模型名称不能为空，请检查套餐 pricing 数据");
        }

        Optional<Model> existing = modelRepository.findByModelName(modelName);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 创建基础 Model
        Model model = new Model();
        model.setModelName(modelName);
        model.setDisplayName(modelName);

        Model saved = modelRepository.save(model);
        log.info("级联创建模型: modelName={}, id={}", modelName, saved.getId());
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
}