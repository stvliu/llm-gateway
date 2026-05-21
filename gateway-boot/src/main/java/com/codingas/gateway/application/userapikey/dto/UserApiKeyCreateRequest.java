package com.codingas.gateway.application.userapikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 创建用户 API Key 请求
 */
public record UserApiKeyCreateRequest(
        @NotNull(message = "团队 ID 不能为空")
        Long teamId,
        @NotNull(message = "用户 ID 不能为空")
        Long userId,
        @NotEmpty(message = "至少需要关联一个产品")
        List<Long> productIds,
        @NotBlank(message = "密钥名称不能为空")
        String name,
        List<String> models,
        Long quotaLimit
) {
}