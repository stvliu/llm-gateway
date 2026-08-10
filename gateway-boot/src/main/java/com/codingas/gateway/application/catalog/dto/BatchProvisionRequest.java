/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 批量开通请求
 *
 * <p>供应商级联开通时，可选择性指定套餐编码列表。不传则开通所有 ACTIVE 套餐。</p>
 */
@Getter
@Setter
public class BatchProvisionRequest {

    /** 需要开通的套餐编码列表（可选，不传则全部开通） */
    private List<String> planCodes;
}