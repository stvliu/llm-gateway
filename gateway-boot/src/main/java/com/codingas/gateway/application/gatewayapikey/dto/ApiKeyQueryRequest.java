package com.codingas.gateway.application.gatewayapikey.dto;

import com.codingas.gateway.common.dto.PageRequest;
import com.codingas.gateway.domain.security.entity.GatewayApiKey.ApiKeyStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询 API Key 请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiKeyQueryRequest extends PageRequest {

    private String keyword;

    private Long userId;

    private ApiKeyStatus status;
}
