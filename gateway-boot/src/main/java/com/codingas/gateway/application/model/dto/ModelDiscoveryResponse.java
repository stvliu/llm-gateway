/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 模型发现响应（兼容 OpenAI /v1/models 格式）
 */
@Data
@AllArgsConstructor
public class ModelDiscoveryResponse {
    private String object;
    private List<ModelItem> data;

    @Data
    @AllArgsConstructor
    public static class ModelItem {
        private String id;
        private String object;
        private long created;
        private String ownedBy;
    }
}