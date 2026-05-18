package com.codingas.gateway.application.gatewayapikey.dto;

import com.codingas.gateway.common.dto.PageRequest;
import com.codingas.gateway.domain.security.enums.GatewayApiKeyState;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询 API Key 请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
/**
 * @deprecated 旧架构 DTO
 */
@Deprecated(since = "2.0", forRemoval = true)
public class ApiKeyQueryRequest extends PageRequest {

    private String keyword;

    private Long userId;

    private GatewayApiKeyState state;
}
