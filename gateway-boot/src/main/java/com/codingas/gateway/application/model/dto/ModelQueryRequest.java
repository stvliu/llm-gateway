package com.codingas.gateway.application.model.dto;

import com.codingas.gateway.common.dto.PageRequest;
import com.codingas.gateway.domain.supply.enums.ModelState;
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

    private ModelState state;
}