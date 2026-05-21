package com.codingas.gateway.application.experience.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 体验聊天请求
 *
 * <p>已迁移到新架构，使用 productId 替代 providerId。</p>
 */
@Data
public class ExperienceChatRequest {

    @NotBlank(message = "Model is required")
    private String model;

    /** 协议名称（如 openai、anthropic），必填 */
    @NotBlank(message = "Protocol name is required")
    private String protocolName;

    private List<Map<String, String>> messages;

    private Double temperature;

    private Integer maxTokens;

    private Boolean stream;

    /** 产品 ID（使用已保存配置时必填） */
    private Long productId;

    /** API Key ID（使用已保存配置时可选，默认使用产品的默认 Key） */
    private Long apiKeyId;

    /** 直接传入的 API Key（不使用已保存配置时） */
    private String apiKey;

    /** 直接传入的 Base URL（不使用已保存配置时，可选，默认使用协议默认 URL） */
    private String baseUrl;

    /** 是否使用已保存的产品配置 */
    private Boolean useSavedConfig;

    /**
     * 判断是否使用已保存的产品配置
     *
     * @return true 如果使用已保存配置
     */
    public boolean useSavedConfig() {
        return useSavedConfig != null && useSavedConfig;
    }

    /**
     * 验证请求是否有效
     *
     * @return true 如果请求参数完整
     */
    public boolean isValid() {
        if (model == null || model.isBlank()) {
            return false;
        }
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        if (useSavedConfig()) {
            return productId != null;
        } else {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}