package com.codingas.gateway.application.gatewayapikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 创建 API Key 请求
 */
@Data
/**
 * @deprecated 旧架构 DTO
 */
@Deprecated(since = "2.0", forRemoval = true)
public class ApiKeyCreateRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Name is required")
    @Size(max = 64, message = "Name must not exceed 64 characters")
    private String name;

    private Instant expiresAt;

    private List<String> ipWhitelist;
}
