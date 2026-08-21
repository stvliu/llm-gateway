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
package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.ModelInstanceCreateRequest;
import com.codingas.gateway.application.channel.dto.ModelInstanceUpdateRequest;
import com.codingas.gateway.application.channel.dto.ModelInstanceStateTransitionRequest;
import com.codingas.gateway.application.channel.dto.ModelInstanceResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelInstanceGateway;
import com.codingas.gateway.provider.model.ModelGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 模型实例应用服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelInstanceServiceImpl implements ModelInstanceService {

    private final ModelInstanceGateway modelInstanceGateway;
    private final ModelGateway modelGateway;

    /**
     * 查询指定渠道下的所有模型实例
     */
    @Transactional(readOnly = true)
    @Override
    public List<ModelInstanceResponse> getInstancesByChannelId(Long channelId) {
        List<ModelInstance> instances = modelInstanceGateway.findByChannelId(channelId);
        return instances.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 创建模型实例
     */
    @Transactional
    @Override
    public ModelInstanceResponse create(ModelInstanceCreateRequest request) {
        Long channelId = request.getChannelId();
        // 检查是否已关联
        boolean exists = modelInstanceGateway.existsByChannelIdAndModelId(channelId, request.getModelId());
        if (exists) {
            log.warn("模型已关联到该渠道, channelId={}, modelId={}", channelId, request.getModelId());
            throw new DuplicateResourceException("ModelInstance", "modelId");
        }

        ModelInstance instance = new ModelInstance();
        instance.setChannelId(channelId);
        instance.setModelId(request.getModelId());
        instance.setUpstreamModelName(request.getUpstreamModelName());
        instance.setPriority(request.getPriority() != null ? request.getPriority() : 100);
        instance.setWeight(request.getWeight() != null ? request.getWeight() : 100);
        instance.setState(ModelInstance.State.ACTIVE);
        instance = modelInstanceGateway.save(instance);
        log.info("模型实例创建成功, id={}, channelId={}, modelId={}", instance.getId(), channelId, request.getModelId());
        return toResponse(instance);
    }

    /**
     * 删除模型实例
     */
    @Transactional
    @Override
    public void delete(Long channelId, Long id) {
        ModelInstance instance = modelInstanceGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ModelInstance", id));
        if (!instance.getChannelId().equals(channelId)) {
            log.warn("模型实例不属于该渠道, id={}, channelId={}, actualChannelId={}", id, channelId, instance.getChannelId());
            throw new GatewayRequestException("CHANNEL_MISMATCH", "模型实例不属于该渠道");
        }
        modelInstanceGateway.deleteById(id);
        log.info("模型实例删除成功, id={}, channelId={}", id, channelId);
    }

    /**
     * 切换模型实例状态
     * <p>由后端校验 canTransitionTo()。</p>
     */
    @Transactional
    @Override
    public void setEnabled(Long channelId, Long id, ModelInstanceStateTransitionRequest request) {
        ModelInstance instance = modelInstanceGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ModelInstance", id));
        if (!instance.getChannelId().equals(channelId)) {
            log.warn("模型实例不属于该渠道, id={}, channelId={}, actualChannelId={}", id, channelId, instance.getChannelId());
            throw new GatewayRequestException("CHANNEL_MISMATCH", "模型实例不属于该渠道");
        }

        ModelInstance.State currentState = instance.getState();
        ModelInstance.State targetState = ModelInstance.State.valueOf(request.getTargetState());

        // 校验状态转换合法性
        if (!currentState.canTransitionTo(targetState)) {
            throw new GatewayRequestException("INVALID_STATE_TRANSITION",
                String.format("不允许从 %s 转换为 %s", currentState, targetState));
        }

        instance.setState(targetState);
        modelInstanceGateway.save(instance);
        log.info("模型实例状态转换成功, id={}, channelId={}, {}→{}", id, channelId, currentState, targetState);
    }

    /**
     * 将实体转换为响应 DTO
     */
    private ModelInstanceResponse toResponse(ModelInstance instance) {
        ModelInstanceResponse resp = new ModelInstanceResponse();
        resp.setId(instance.getId());
        resp.setChannelId(instance.getChannelId());
        resp.setModelId(instance.getModelId());
        resp.setUpstreamModelName(instance.getUpstreamModelName());
        resp.setPriority(instance.getPriority());
        resp.setWeight(instance.getWeight());
        resp.setState(instance.getState().name());

        modelGateway.findById(instance.getModelId()).ifPresent(spec -> {
            resp.setModelName(spec.getModelName());
            resp.setDisplayName(spec.getDisplayName());
            resp.setModelFamily(spec.getModelFamily());
        });

        return resp;
    }

    /**
     * 更新模型实例的上游模型名
     */
    @Transactional
    @Override
    public void updateUpstreamModelName(Long channelId, Long id, String upstreamModelName) {
        ModelInstance instance = modelInstanceGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ModelInstance", id));
        if (!instance.getChannelId().equals(channelId)) {
            log.warn("模型实例不属于该渠道, id={}, channelId={}, actualChannelId={}", id, channelId, instance.getChannelId());
            throw new GatewayRequestException("CHANNEL_MISMATCH", "模型实例不属于该渠道");
        }
        instance.setUpstreamModelName(upstreamModelName);
        modelInstanceGateway.save(instance);
        log.info("模型实例上游模型名更新成功, id={}, channelId={}, upstreamModelName={}", id, channelId, upstreamModelName);
    }

    /**
     * 更新模型实例（支持修改 modelId 和 upstreamModelName）
     *
     * <p>字段为 null 表示不更新该字段。修改 modelId 时检查是否与渠道下其他实例冲突。</p>
     */
    @Transactional
    @Override
    public ModelInstanceResponse update(Long channelId, Long id, ModelInstanceUpdateRequest request) {
        ModelInstance instance = modelInstanceGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ModelInstance", id));
        if (!instance.getChannelId().equals(channelId)) {
            log.warn("模型实例不属于该渠道, id={}, channelId={}, actualChannelId={}", id, channelId, instance.getChannelId());
            throw new GatewayRequestException("CHANNEL_MISMATCH", "模型实例不属于该渠道");
        }

        // 更新 modelId
        if (request.getModelId() != null && !request.getModelId().equals(instance.getModelId())) {
            // 检查新 modelId 是否已关联到该渠道
            boolean exists = modelInstanceGateway.existsByChannelIdAndModelId(channelId, request.getModelId());
            if (exists) {
                log.warn("模型已关联到该渠道, channelId={}, modelId={}", channelId, request.getModelId());
                throw new DuplicateResourceException("ModelInstance", "modelId");
            }
            instance.setModelId(request.getModelId());
        }

        // 更新 upstreamModelName（null 表示不更新，空字符串表示清除）
        if (request.getUpstreamModelName() != null) {
            instance.setUpstreamModelName(request.getUpstreamModelName());
        }

        instance = modelInstanceGateway.save(instance);
        log.info("模型实例更新成功, id={}, channelId={}, modelId={}", id, channelId, instance.getModelId());
        return toResponse(instance);
    }
}