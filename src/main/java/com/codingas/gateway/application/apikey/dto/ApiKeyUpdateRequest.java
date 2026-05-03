package com.codingas.gateway.application.apikey.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 更新 API Key 请求
 */
@Data
public class ApiKeyUpdateRequest {

    @Size(max = 64, message = "Name must not exceed 64 characters")
    private String name;

    private Instant expiresAt;

    private List<String> ipWhitelist;

    private Boolean enabled;
}
