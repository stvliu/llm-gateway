/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.provider.model;

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