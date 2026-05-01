package com.codingas.gateway.adapter.admin.dto.tokenlimit;

import com.codingas.gateway.adapter.admin.dto.model.ModelQueryRequest;
import com.codingas.gateway.common.dto.PageRequest;
import com.codingas.gateway.domain.quota.entity.TokenLimit.TokenLimitStatus;
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

    private TokenLimitStatus status;
}
