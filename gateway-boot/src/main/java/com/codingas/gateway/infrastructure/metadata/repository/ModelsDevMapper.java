package com.codingas.gateway.infrastructure.metadata.repository;

import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import com.codingas.gateway.domain.metadata.entity.ModelMetadata;
import com.codingas.gateway.domain.metadata.service.ModelMetadataDomainService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Models.dev 数据映射器
 * <p>
 * 负责将 Models.dev API 的 JSON 数据转换为 ModelMetadata 实体
 * </p>
 */
@Component
@RequiredArgsConstructor
public class ModelsDevMapper {

    private final ModelMetadataDomainService modelMetadataDomainService;

    /**
     * 从 Models.dev JSON 映射为新的 ModelMetadata
     */
    public ModelMetadata mapFromModelsDev(String providerId, String modelId, JsonNode node) {
        ModelMetadata metadata = new ModelMetadata(providerId, modelId,
            node.path("name").asText(modelId), MetadataSource.MODELS_DEV);
        modelMetadataDomainService.markSynced(metadata);

        populateLimits(metadata, node);
        populateMetadata(metadata, node);
        populateCapabilities(metadata, node);
        populateModalities(metadata, node);

        return metadata;
    }

    /**
     * 用 Models.dev 数据更新已有 ModelMetadata
     */
    public void updateFromModelsDev(ModelMetadata existing, JsonNode node) {
        existing.setDisplayName(node.path("name").asText(existing.getDisplayName()));
        modelMetadataDomainService.markSynced(existing);

        populateLimits(existing, node);
        existing.setKnowledgeCutoff(node.path("knowledge_cutoff").asText(null));
    }

    /**
     * 填充限制信息（上下文窗口、token 限制等）
     */
    private void populateLimits(ModelMetadata metadata, JsonNode node) {
        metadata.setContextWindow(parseInteger(node, "context_length"));
        metadata.setMaxInputTokens(parseInteger(node, "max_input_tokens"));
        metadata.setMaxOutputTokens(parseInteger(node, "max_output_tokens"));
    }

    /**
     * 填充元数据（知识截止日期、开源状态、模型家族等）
     */
    private void populateMetadata(ModelMetadata metadata, JsonNode node) {
        metadata.setKnowledgeCutoff(node.path("knowledge_cutoff").asText(null));
        metadata.setOpenWeights(node.path("open_weights").asBoolean(false));
        metadata.setModelFamily(node.path("family").asText(null));
    }

    /**
     * 填充能力信息
     */
    private void populateCapabilities(ModelMetadata metadata, JsonNode node) {
        Map<String, Boolean> capabilities = new HashMap<>();
        capabilities.put("vision", node.path("vision").asBoolean(false));
        capabilities.put("function_calling", node.path("function_calling").asBoolean(false));
        capabilities.put("streaming", true); // 默认支持流式
        metadata.setCapabilities(capabilities);
    }

    /**
     * 填充模态信息
     */
    private void populateModalities(ModelMetadata metadata, JsonNode node) {
        List<String> modalities = new ArrayList<>();
        if (node.path("vision").asBoolean(false)) {
            modalities.add("image");
        }
        modalities.add("text");
        metadata.setModalities(modalities);
    }

    /**
     * 解析 Integer 字段
     */
    private Integer parseInteger(JsonNode parent, String field) {
        JsonNode node = parent.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asInt();
    }
}
