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


import com.codingas.gateway.provider.channel.ModelInstanceCreateRequest;
import com.codingas.gateway.provider.channel.ModelInstanceUpdateRequest;
import com.codingas.gateway.provider.channel.ModelInstanceStateTransitionRequest;
import com.codingas.gateway.provider.channel.ModelInstanceResponse;
import java.util.List;

/**
 * 模型实例应用服务接口
 */
public interface ModelInstanceService {

    /**
     * 查询指定渠道下的所有模型实例
     */
    List<ModelInstanceResponse> getInstancesByChannelId(Long channelId);

    /**
     * 创建模型实例
     */
    ModelInstanceResponse create(ModelInstanceCreateRequest request);

    /**
     * 删除模型实例
     */
    void delete(Long channelId, Long id);

    /**
     * 切换模型实例状态
     * <p>由后端校验 canTransitionTo()。</p>
     */
    void setEnabled(Long channelId, Long id, ModelInstanceStateTransitionRequest request);

    /**
     * 更新模型实例的上游模型名
     *
     * @param upstreamModelName 新的上游模型名，null 表示走默认（= Model.modelName）
     */
    void updateUpstreamModelName(Long channelId, Long id, String upstreamModelName);

    /**
     * 更新模型实例（支持修改 modelId 和 upstreamModelName）
     *
     * <p>字段为 null 表示不更新该字段。</p>
     */
    ModelInstanceResponse update(Long channelId, Long id, ModelInstanceUpdateRequest request);
}