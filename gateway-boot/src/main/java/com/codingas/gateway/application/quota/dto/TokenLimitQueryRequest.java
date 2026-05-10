package com.codingas.gateway.application.quota.dto;

import com.codingas.gateway.common.dto.PageRequest;
import com.codingas.gateway.domain.usage.entity.TokenLimit.TokenLimitState;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询 Token 限额请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TokenLimitQueryRequest extends PageRequest {

    private String keyword;

    private Long userId;

    private Long providerId;

    private Long modelId;

    private TokenLimitState state;
}
