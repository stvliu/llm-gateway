package com.codingas.gateway.application.userapikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 用户 API Key 创建请求
 *
 * @param teamId 团队 ID
 * @param productId 产品 ID
 * @param name 密钥名称
 * @param models 可访问的模型列表（子集），为空表示全部
 * @param quotaLimit Key 级别的额度限制
 */
public record UserApiKeyCreateRequest(
        @NotNull(message = "团队 ID 不能为空")
        Long teamId,
        @NotNull(message = "产品 ID 不能为空")
        Long productId,
        @NotBlank(message = "密钥名称不能为空")
        String name,
        List<String> models,
        Long quotaLimit
) {
}