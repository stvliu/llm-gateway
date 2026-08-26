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
package com.codingas.gateway.web.api;

import com.codingas.gateway.provider.model.ModelInstanceManager;
import com.codingas.gateway.web.api.assembler.ModelInstanceFacade;
import com.codingas.gateway.web.api.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模型实例 REST 控制器
 *
 * <p>列表/创建/更新经 {@link ModelInstanceFacade} 组装 DTO；
 * 删除、状态切换与上游模型名更新直接调用领域服务（无跨实体组装）。</p>
 */
@RestController
@RequestMapping("/api/v1/channels/{channelId}/models")
@RequiredArgsConstructor
public class ModelInstanceController {

    private final ModelInstanceFacade modelInstanceFacade;
    private final ModelInstanceManager modelInstanceManager;

    @GetMapping
    public List<ModelInstanceResponse> list(@PathVariable Long channelId) {
        return modelInstanceFacade.list(channelId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModelInstanceResponse create(
            @PathVariable Long channelId,
            @Valid @RequestBody ModelInstanceCreateRequest request) {
        return modelInstanceFacade.create(channelId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long channelId, @PathVariable Long id) {
        modelInstanceManager.delete(channelId, id);
    }

    @PutMapping("/{id}/state")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setEnabled(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @Valid @RequestBody ModelInstanceStateTransitionRequest request) {
        modelInstanceManager.setEnabled(channelId, id, request.getTargetState());
    }

    @PatchMapping("/{id}/upstream-model-name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUpstreamModelName(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String upstreamModelName = body.get("upstreamModelName");
        modelInstanceManager.updateUpstreamModelName(channelId, id, upstreamModelName);
    }

    /**
     * 更新模型实例（支持修改 modelId 和 upstreamModelName）
     *
     * <p>字段为 null 表示不更新该字段。</p>
     */
    @PutMapping("/{id}")
    public ModelInstanceResponse update(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @Valid @RequestBody ModelInstanceUpdateRequest request) {
        return modelInstanceFacade.update(channelId, id, request);
    }
}
