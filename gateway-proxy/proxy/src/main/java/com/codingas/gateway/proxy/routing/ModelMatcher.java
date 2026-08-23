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
package com.codingas.gateway.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 模型匹配器 — 根据 modelName 查找 Model
 */
@Component
@RequiredArgsConstructor
public class ModelMatcher {

    private final ModelGateway modelGateway;

    /**
     * 根据 modelName 查找匹配的活跃模型
     *
     * @param modelName 模型名称（对应 Model.modelName）
     * @return 匹配的 Model
     * @throws ResourceNotFoundException 未找到匹配模型
     */
    public Model match(String modelName) {
        return modelGateway.findByModelName(modelName)
                .filter(Model::isAvailable)
                .orElseThrow(() -> new ResourceNotFoundException("Model", modelName));
    }
}
