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
package com.codingas.gateway.infrastructure.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ModelInstanceDo;
import com.codingas.gateway.infrastructure.supply.gateway.database.repository.ModelInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 模型实例持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ModelInstanceGatewayImpl implements ModelInstanceGateway {

    private final ModelInstanceRepository modelInstanceRepository;

    @Override
    public ModelInstance save(ModelInstance instance) {
        ModelInstanceDo doObj = toDo(instance);
        ModelInstanceDo saved = modelInstanceRepository.save(doObj);
        return toEntity(saved);
    }

    @Override
    public Optional<ModelInstance> findById(Long id) {
        return modelInstanceRepository.findById(id).map(this::toEntity);
    }

    @Override
    public List<ModelInstance> findByChannelId(Long channelId) {
        return modelInstanceRepository.findByChannelId(channelId).stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelInstance> findActiveByChannelId(Long channelId) {
        return modelInstanceRepository.findByChannelIdAndState(channelId, ModelInstance.State.ACTIVE.name())
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelInstance> findActiveByModelId(Long modelId) {
        return modelInstanceRepository.findByModelIdAndState(modelId, ModelInstance.State.ACTIVE.name())
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelInstance> findActiveByModelIdOrderByPriority(Long modelId) {
        return modelInstanceRepository.findByModelIdAndStateOrderByPriorityAsc(modelId, ModelInstance.State.ACTIVE.name())
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelInstance> findByChannelIdAndState(Long channelId, String state) {
        return modelInstanceRepository.findByChannelIdAndState(channelId, state)
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelInstance> findByIds(List<Long> ids) {
        return modelInstanceRepository.findByIdIn(ids).stream().map(this::toEntity).toList();
    }

    @Override
    public void deleteById(Long id) {
        modelInstanceRepository.deleteById(id);
    }

    @Override
    public boolean existsByChannelIdAndModelId(Long channelId, Long modelId) {
        return modelInstanceRepository.findByChannelId(channelId).stream()
                .anyMatch(mi -> mi.getModelId().equals(modelId));
    }

    @Override
    public List<ModelInstance> saveAll(List<ModelInstance> instances) {
        List<ModelInstanceDo> doList = instances.stream().map(this::toDo).toList();
        return modelInstanceRepository.saveAll(doList).stream().map(this::toEntity).toList();
    }

    private ModelInstance toEntity(ModelInstanceDo doObj) {
        ModelInstance entity = new ModelInstance();
        entity.setId(doObj.getId());
        entity.setChannelId(doObj.getChannelId());
        entity.setModelId(doObj.getModelId());
        entity.setUpstreamModelName(doObj.getUpstreamModelName());
        entity.setCapabilitiesOverride(doObj.getCapabilitiesOverride());
        entity.setContextWindowOverride(doObj.getContextWindowOverride());
        entity.setPriority(doObj.getPriority() != null ? doObj.getPriority() : 100);
        entity.setWeight(doObj.getWeight() != null ? doObj.getWeight() : 100);
        entity.setQuotaLimit(doObj.getQuotaLimit());
        entity.setState(ModelInstance.State.valueOf(doObj.getState()));
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private ModelInstanceDo toDo(ModelInstance entity) {
        ModelInstanceDo doObj = new ModelInstanceDo();
        doObj.setId(entity.getId());
        doObj.setChannelId(entity.getChannelId());
        doObj.setModelId(entity.getModelId());
        doObj.setUpstreamModelName(entity.getUpstreamModelName());
        doObj.setCapabilitiesOverride(entity.getCapabilitiesOverride());
        doObj.setContextWindowOverride(entity.getContextWindowOverride());
        doObj.setPriority(entity.getPriority() != null ? entity.getPriority() : 100);
        doObj.setWeight(entity.getWeight() != null ? entity.getWeight() : 100);
        doObj.setQuotaLimit(entity.getQuotaLimit());
        doObj.setState(entity.getState() != null ? entity.getState().name() : ModelInstance.State.ACTIVE.name());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}