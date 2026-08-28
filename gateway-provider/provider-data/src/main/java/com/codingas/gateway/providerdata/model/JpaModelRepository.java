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
package com.codingas.gateway.providerdata.model;

import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelRepository;
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
public class JpaModelRepository implements ModelRepository {

    private final ModelJpaRepository modelRepository;

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
    public List<Model> findAll() {
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

    @Override
    public Optional<Model> findByExternalId(String externalId) {
        return modelRepository.findByExternalId(externalId).map(this::toEntity);
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
        entity.setDescription(doObj.getDescription());
        entity.setReleaseDate(doObj.getReleaseDate());
        entity.setLastUpdated(doObj.getLastUpdated());
        entity.setLicense(doObj.getLicense());
        entity.setOpenWeights(doObj.getOpenWeights());
        entity.setBenchmarks(doObj.getBenchmarks());
        entity.setWeights(doObj.getWeights());
        entity.setSource(doObj.getSource());
        entity.setExternalId(doObj.getExternalId());
        entity.setLockedFields(doObj.getLockedFields());
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
        doObj.setDescription(entity.getDescription());
        doObj.setReleaseDate(entity.getReleaseDate());
        doObj.setLastUpdated(entity.getLastUpdated());
        doObj.setLicense(entity.getLicense());
        doObj.setOpenWeights(entity.getOpenWeights());
        doObj.setBenchmarks(entity.getBenchmarks());
        doObj.setWeights(entity.getWeights());
        doObj.setSource(entity.getSource());
        doObj.setExternalId(entity.getExternalId());
        doObj.setLockedFields(entity.getLockedFields());
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