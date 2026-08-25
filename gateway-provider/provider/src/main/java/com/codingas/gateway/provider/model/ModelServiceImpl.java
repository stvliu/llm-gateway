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

import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型应用服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final ModelRepository modelRepository;

    /**
     * 创建模型
     */
    @Override
    @Transactional
    public Model create(ModelCreateCommand command) {
        // 创建模型
        Model model = new Model();
        model.setModelName(command.getModelName());
        model.setDisplayName(command.getDisplayName());
        model.setModelFamily(command.getModelFamily());
        model.setContextWindow(command.getContextWindow());
        model.setMaxInputTokens(command.getMaxInputTokens());
        model.setMaxOutputTokens(command.getMaxOutputTokens());
        model.setCapabilities(command.getCapabilities());
        model.setModalities(command.getModalities());

        Model savedModel = modelRepository.save(model);
        log.info("模型创建成功, id={}, modelName={}", savedModel.getId(), savedModel.getModelName());
        return savedModel;
    }

    /**
     * 根据 ID 获取模型
     */
    @Override
    public Model getById(Long id) {
        return modelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
    }

    /**
     * 查询模型列表
     */
    @Override
    public PageResponse<Model> query(ModelQuery query) {
        List<Model> models = modelRepository.findAll();

        // 过滤
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String keyword = query.getKeyword().toLowerCase();
            models = models.stream()
                .filter(m -> (m.getDisplayName() != null && m.getDisplayName().toLowerCase().contains(keyword))
                    || m.getModelName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        // 状态过滤：ACTIVE=未废弃（deprecatedAt 为空），INACTIVE=已废弃（deprecatedAt 非空）
        if (query.getState() != null && !query.getState().isBlank()) {
            String state = query.getState().toUpperCase();
            models = models.stream()
                .filter(m -> "ACTIVE".equals(state) ? m.isAvailable() : !m.isAvailable())
                .collect(Collectors.toList());
        }

        // 统计
        long total = models.size();

        // 分页
        int offset = query.getOffset();
        int limit = query.getLimit();
        List<Model> pagedModels = models.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        return PageResponse.of(pagedModels, query.getPage(), limit, total);
    }

    /**
     * 更新模型
     */
    @Override
    @Transactional
    public Model update(Long id, ModelUpdateCommand command) {
        Model model = modelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));

        if (command.getModelName() != null) {
            model.setModelName(command.getModelName());
        }
        if (command.getDisplayName() != null) {
            model.setDisplayName(command.getDisplayName());
        }
        if (command.getModelFamily() != null) {
            model.setModelFamily(command.getModelFamily());
        }
        if (command.getContextWindow() != null) {
            model.setContextWindow(command.getContextWindow());
        }
        if (command.getMaxInputTokens() != null) {
            model.setMaxInputTokens(command.getMaxInputTokens());
        }
        if (command.getMaxOutputTokens() != null) {
            model.setMaxOutputTokens(command.getMaxOutputTokens());
        }
        if (command.getCapabilities() != null) {
            model.setCapabilities(command.getCapabilities());
        }
        if (command.getModalities() != null) {
            model.setModalities(command.getModalities());
        }

        Model saved = modelRepository.save(model);
        log.info("模型更新成功, id={}, modelName={}", id, saved.getModelName());
        return saved;
    }

    /**
     * 删除模型
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Model model = modelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        modelRepository.delete(model);
    }

    /**
     * 启用/禁用模型
     */
    @Override
    @Transactional
    public Model setEnabled(Long id, boolean enabled) {
        Model model = modelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        if (enabled) {
            model.setDeprecatedAt(null);
        } else {
            model.setDeprecatedAt(java.time.Instant.now());
        }
        return modelRepository.save(model);
    }
}