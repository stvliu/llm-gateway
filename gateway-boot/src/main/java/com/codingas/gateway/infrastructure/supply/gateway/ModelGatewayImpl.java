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

import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ModelDo;
import com.codingas.gateway.infrastructure.supply.gateway.database.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 模型持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ModelGatewayImpl implements ModelGateway {

    private final ModelRepository modelRepository;

    @Override
    public Model save(Model model) {
        ModelDo doObj = toDo(model);
        ModelDo saved = modelRepository.save(doObj);
        return toEntity(saved);
    }

    @Override
    public Optional<Model> findById(Long id) {
        return modelRepository.findById(id).map(this::toEntity);
    }

    @Override
    public Optional<Model> findByModelName(String modelName) {
        return modelRepository.findByModelName(modelName).map(this::toEntity);
    }

    @Override
    public List<Model> findActiveByModelName(String modelName) {
        return modelRepository.findByModelName(modelName)
                .map(this::toEntity)
                .filter(Model::isAvailable)
                .map(List::of)
                .orElseGet(List::of);
    }

    @Override
    public List<Model> findAll() {
        return modelRepository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public List<Model> findAllActive() {
        return modelRepository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public List<Model> findByIds(List<Long> ids) {
        return modelRepository.findByIdIn(ids).stream().map(this::toEntity).toList();
    }

    @Override
    public long count() {
        return modelRepository.count();
    }

    @Override
    public void delete(Model model) {
        modelRepository.deleteById(model.getId());
    }

    @Override
    public boolean existsByModelName(String modelName) {
        return modelRepository.existsByModelName(modelName);
    }

    @Override
    public List<Model> findByKeyword(String keyword) {
        return modelRepository.findByModelNameContainingOrDisplayNameContaining(keyword, keyword)
                .stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<Model> findByCapability(String capability) {
        return modelRepository.findByCapability(capability)
                .stream()
                .map(this::toEntity)
                .toList();
    }

    private Model toEntity(ModelDo doObj) {
        Model entity = new Model();
        entity.setId(doObj.getId());
        entity.setModelName(doObj.getModelName());
        entity.setDisplayName(doObj.getDisplayName());
        entity.setModelFamily(doObj.getModelFamily());
        entity.setContextWindow(doObj.getContextWindow());
        entity.setMaxInputTokens(doObj.getMaxInputTokens());
        entity.setMaxOutputTokens(doObj.getMaxOutputTokens());
        entity.setKnowledgeCutoff(doObj.getKnowledgeCutoff());
        entity.setCapabilities(doObj.getCapabilities());
        entity.setModalities(doObj.getModalities());
        entity.setDeprecatedAt(doObj.getDeprecatedAt());
        entity.setScheduledRetiredAt(doObj.getScheduledRetiredAt());
        entity.setDeprecationMessage(doObj.getDeprecationMessage());
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private ModelDo toDo(Model entity) {
        ModelDo doObj = new ModelDo();
        doObj.setId(entity.getId());
        doObj.setModelName(entity.getModelName());
        doObj.setDisplayName(entity.getDisplayName());
        doObj.setModelFamily(entity.getModelFamily());
        doObj.setContextWindow(entity.getContextWindow());
        doObj.setMaxInputTokens(entity.getMaxInputTokens());
        doObj.setMaxOutputTokens(entity.getMaxOutputTokens());
        doObj.setKnowledgeCutoff(entity.getKnowledgeCutoff());
        doObj.setCapabilities(entity.getCapabilities());
        doObj.setModalities(entity.getModalities());
        doObj.setDeprecatedAt(entity.getDeprecatedAt());
        doObj.setScheduledRetiredAt(entity.getScheduledRetiredAt());
        doObj.setDeprecationMessage(entity.getDeprecationMessage());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}