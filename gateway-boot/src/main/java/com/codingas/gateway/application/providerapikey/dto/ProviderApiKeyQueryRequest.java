package com.codingas.gateway.application.providerapikey.dto;

import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;
import lombok.Data;

/**
 * Provider API Key 查询请求
 */
@Data
/**
 * @deprecated 旧架构 DTO
 */
@Deprecated(since = "2.0", forRemoval = true)
public class ProviderApiKeyQueryRequest {

    private Long providerId;

    private String keyword;

    private ProviderApiKeyState state;

    private Integer page = 1;

    private Integer limit = 20;
}