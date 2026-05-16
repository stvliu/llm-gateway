package com.codingas.gateway.infrastructure.metadata.gateway;

import com.codingas.gateway.domain.metadata.gateway.ModelsDevDataGateway;
import com.codingas.gateway.infrastructure.metadata.config.MetadataSyncConfig;
import com.codingas.gateway.common.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Models.dev 数据获取网关实现
 * <p>
 * 纯技术实现：HTTP 调用、JSON 解析、格式转换。
 * 不含业务逻辑。
 * </p>
 */
@Slf4j
@Component
public class ModelsDevDataGatewayImpl implements ModelsDevDataGateway {

    private final MetadataSyncConfig config;
    private final RestClient restClient;
    private final Set<String> supportedProviders;

    public ModelsDevDataGatewayImpl(MetadataSyncConfig config) {
        this.config = config;
        this.supportedProviders = config.getModelsDev().getSupportedProviders();

        // 创建带超时配置的 RestClient
        this.restClient = RestClient.builder()
            .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(config.getModelsDev().getConnectTimeoutSeconds()))
                    .build()
            ))
            .build();
    }

    @Override
    public Map<String, List<ModelData>> fetchAllSupportedModels() {
        if (!config.getModelsDev().isEnabled()) {
            log.info("Models.dev sync is disabled");
            return Map.of();
        }

        String apiUrl = config.getModelsDev().getApiUrl();
        log.info("Fetching model data from Models.dev: {}", apiUrl);

        try {
            JsonNode root = fetchApiData(apiUrl);
            Map<String, List<ModelData>> result = new HashMap<>();

            root.fieldNames().forEachRemaining(providerId -> {
                if (!supportedProviders.contains(providerId)) {
                    return;
                }
                List<ModelData> models = parseProviderModels(providerId, root);
                if (!models.isEmpty()) {
                    result.put(providerId, models);
                }
            });

            log.info("Fetched models for {} providers from Models.dev", result.size());
            return result;
        } catch (ModelsDevApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch data from Models.dev API", e);
            throw new ModelsDevApiException("Models.dev 数据获取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取 API 数据
     */
    private JsonNode fetchApiData(String apiUrl) {
        String response;
        try {
            response = restClient.get()
                .uri(apiUrl)
                .retrieve()
                .body(String.class);
        } catch (RestClientException e) {
            throw new ModelsDevApiException("Models.dev API 请求失败: " + e.getMessage(), e, true);
        }

        if (response == null || response.isBlank()) {
            throw new ModelsDevApiException("Models.dev API 返回空响应", null, false);
        }

        try {
            return JsonUtils.readTree(response);
        } catch (Exception e) {
            throw new ModelsDevApiException("Models.dev API 响应解析失败: " + e.getMessage(), e, false);
        }
    }

    /**
     * 解析指定供应商的模型列表
     */
    private List<ModelData> parseProviderModels(String providerId, JsonNode root) {
        JsonNode providerNode = root.path(providerId);
        JsonNode models = providerNode.path("models");

        if (models.isMissingNode()) {
            return List.of();
        }

        List<ModelData> result = new ArrayList<>();
        models.fieldNames().forEachRemaining(modelId -> {
            try {
                JsonNode modelNode = models.get(modelId);
                result.add(parseModelData(modelId, modelNode));
            } catch (Exception e) {
                log.warn("Failed to parse model {}:{}", providerId, modelId, e);
            }
        });

        return result;
    }

    /**
     * 解析单个模型数据
     */
    private ModelData parseModelData(String modelId, JsonNode node) {
        JsonNode pricing = node.path("pricing");

        return new ModelData(
            modelId,
            node.path("name").asText(modelId),
            parseDecimal(pricing, "prompt"),
            parseDecimal(pricing, "completion"),
            parseDecimal(pricing, "reasoning"),
            parseDecimal(pricing, "cache_read"),
            parseDecimal(pricing, "cache_write"),
            parseDecimal(pricing, "input_audio"),
            parseDecimal(pricing, "output_audio"),
            parseInteger(node, "context_length"),
            parseInteger(node, "max_input_tokens"),
            parseInteger(node, "max_output_tokens"),
            node.path("knowledge_cutoff").asText(null),
            node.path("open_weights").asBoolean(false),
            node.path("family").asText(null),
            node.path("vision").asBoolean(false),
            node.path("function_calling").asBoolean(false)
        );
    }

    private BigDecimal parseDecimal(JsonNode parent, String field) {
        JsonNode node = parent.path(field);
        if (node.isMissingNode() || node.isNull()) return null;
        try {
            return BigDecimal.valueOf(node.asDouble());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(JsonNode parent, String field) {
        JsonNode node = parent.path(field);
        if (node.isMissingNode() || node.isNull()) return null;
        return node.asInt();
    }

    /**
     * Models.dev API 异常
     * <p>
     * 区分可重试（网络错误）和不可重试（解析错误）的异常。
     * </p>
     */
    public static class ModelsDevApiException extends RuntimeException {
        private final boolean retryable;

        public ModelsDevApiException(String message, Throwable cause, boolean retryable) {
            super(message, cause);
            this.retryable = retryable;
        }

        public ModelsDevApiException(String message, Throwable cause) {
            this(message, cause, false);
        }

        public boolean isRetryable() {
            return retryable;
        }
    }
}
