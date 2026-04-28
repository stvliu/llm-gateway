package com.codingas.gateway.adapter.admin.dto.model;

import com.codingas.gateway.common.dto.PageRequest;
import com.codingas.gateway.domain.router.entity.Model.ModelStatus;
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

    private ModelStatus status;
}
