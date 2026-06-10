package com.codingas.gateway.application.userapikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建用户 API Key 请求
 */
public record UserApiKeyCreateRequest(
        @NotNull(message = "用户 ID 不能为空")
        Long userId,
        @NotBlank(message = "密钥名称不能为空")
        String name
) {
}
