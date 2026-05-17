package com.codingas.gateway.application.experience.dto;

import com.codingas.gateway.domain.model.enums.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 模型体验聊天请求
 *
 * <p>支持两种模式：</p>
 * <ol>
 *   <li>使用已保存配置：传入 providerId，可选 apiKeyId（不传则使用默认 Key）</li>
 *   <li>临时配置：传入 providerType, apiKey, 可选 baseUrl</li>
 * </ol>
 */
public record ExperienceChatRequest(
    /**
     * 供应商 ID（使用已保存配置时必填）
     */
    Long providerId,

    /**
     * API Key ID（可选，不传则使用供应商的默认 Key）
     */
    Long apiKeyId,

    /**
     * 供应商类型（临时配置时必填）
     */
    ProviderType providerType,

    /**
     * Base URL（可选，使用默认值）
     */
    String baseUrl,

    /**
     * API Key（临时配置时必填）
     */
    String apiKey,

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空")
    String model,

    /**
     * 对话消息列表
     */
    @NotNull(message = "消息列表不能为空")
    @Size(min = 1, max = 20, message = "消息数量必须在 1-20 条之间")
    List<ChatMessage> messages,

    /**
     * 最大输出 Token（默认 1024，最大 2048）
     */
    Integer maxTokens,

    /**
     * 温度参数（默认 0.7，范围 0-2）
     */
    Double temperature
) {

    /**
     * 聊天消息
     */
    public record ChatMessage(
        String role,     // user, assistant, system
        String content
    ) {}

    /**
     * 判断是否使用已保存配置
     */
    public boolean useSavedConfig() {
        return providerId != null;
    }

    /**
     * 判断请求是否有效
     */
    public boolean isValid() {
        if (useSavedConfig()) {
            // 使用已保存配置：providerId 必须存在
            return true;
        } else {
            // 临时配置：providerType 和 apiKey 必须存在
            return providerType != null && apiKey != null && !apiKey.isBlank();
        }
    }

    /**
     * 获取有效的 maxTokens
     */
    public int getEffectiveMaxTokens() {
        return maxTokens != null ? Math.min(maxTokens, 2048) : 1024;
    }

    /**
     * 获取有效的 temperature
     */
    public double getEffectiveTemperature() {
        return temperature != null ? Math.max(0, Math.min(2, temperature)) : 0.7;
    }
}
