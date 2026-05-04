package com.codingas.gateway.application.apikey.dto;

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
public class ApiKeyCreateRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Name is required")
    @Size(max = 64, message = "Name must not exceed 64 characters")
    private String name;

    private Instant expiresAt;

    private List<String> ipWhitelist;
}
