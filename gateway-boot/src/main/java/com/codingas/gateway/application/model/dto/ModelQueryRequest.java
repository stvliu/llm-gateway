package com.codingas.gateway.application.model.dto;

import com.codingas.gateway.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询模型请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelQueryRequest extends PageRequest {

    private String keyword;

    private Long providerId;

    /**
     * 状态过滤：ACTIVE（启用）/ INACTIVE（禁用），为空则不过滤
     */
    private String state;
}