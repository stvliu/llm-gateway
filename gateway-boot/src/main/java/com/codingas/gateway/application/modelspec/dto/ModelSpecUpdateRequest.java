package com.codingas.gateway.application.modelspec.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模型规格更新请求
 */
@Data
public class ModelSpecUpdateRequest {

    @Size(max = 128)
    private String providerModelId;

    @Size(max = 128)
    private String displayName;

    @Size(max = 64)
    private String modelFamily;

    @Positive(message = "上下文窗口必须为正数")
    private Integer contextWindow;

    @Positive(message = "最大输入 Token 数必须为正数")
    private Integer maxInputTokens;

    @Positive(message = "最大输出 Token 数必须为正数")
    private Integer maxOutputTokens;

    private Map<String, Boolean> capabilities;

    private List<String> modalities;

    @Min(value = 0, message = "优先级不能为负")
    private Integer priority;

    @Min(value = 1, message = "权重不能小于 1")
    private Integer weight;

    /**
     * 状态变更请使用 PATCH /{id}/state 接口
     */
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "状态值只能是 ACTIVE 或 INACTIVE")
    private String state;
}
