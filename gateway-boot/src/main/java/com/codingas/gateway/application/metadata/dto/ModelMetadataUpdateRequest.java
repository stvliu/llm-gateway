package com.codingas.gateway.application.metadata.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 更新模型元数据请求
 * <p>
 * 所有字段可选，仅更新非空字段
 */
public record ModelMetadataUpdateRequest(
        String displayName,
        String modelFamily,
        Integer contextWindow,
        Integer maxInputTokens,
        Integer maxOutputTokens,
        String knowledgeCutoff,
        LocalDate releaseDate,
        Boolean openWeights,
        List<String> modalities,
        Map<String, Boolean> capabilities
) {
}
