package com.codingas.gateway.application.providerapikey.dto;

import com.codingas.gateway.domain.model.entity.ProviderApiKey.ProviderApiKeyStatus;
import lombok.Data;

/**
 * Provider API Key 查询请求
 */
@Data
public class ProviderApiKeyQueryRequest {

    private Long providerId;

    private String keyword;

    private ProviderApiKeyStatus status;

    private Integer page = 1;

    private Integer limit = 20;
}