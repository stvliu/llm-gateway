package com.codingas.gateway.application.metadata.dto;

import com.codingas.gateway.domain.metadata.entity.MetadataSource;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 创建模型元数据请求
 */
public record ModelMetadataCreateRequest(
        @NotBlank(message = "供应商ID不能为空")
        String providerId,

        @NotBlank(message = "供应商模型ID不能为空")
        String providerModelId,

        @NotBlank(message = "显示名称不能为空")
        String displayName,

        String modelFamily,
        Integer contextWindow,
        Integer maxInputTokens,
        Integer maxOutputTokens,
        BigDecimal inputPrice,
        BigDecimal outputPrice,
        BigDecimal reasoningPrice,
        BigDecimal cacheReadPrice,
        BigDecimal cacheWritePrice,
        BigDecimal inputAudioPrice,
        BigDecimal outputAudioPrice,
        String knowledgeCutoff,
        LocalDate releaseDate,
        Boolean openWeights,
        List<String> modalities,
        Map<String, Boolean> capabilities
) {
}
