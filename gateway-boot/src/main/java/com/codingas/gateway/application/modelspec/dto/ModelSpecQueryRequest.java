package com.codingas.gateway.application.modelspec.dto;

import lombok.Data;

/**
 * 模型规格查询请求
 */
@Data
public class ModelSpecQueryRequest {

    private Long providerId;

    private String keyword;

    private String state;
}
