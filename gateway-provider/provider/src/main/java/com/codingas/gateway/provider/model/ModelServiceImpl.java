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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模型管理服务实现
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
    public Model create(Model model) {
        // 创建模型（业务字段已由 DTO.toEntity 承载）
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
    public Model update(Long id, Model model) {
        Model existing = modelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));

        // 记录本次人工修改的字段，加入锁定集合（同步时跳过这些字段，避免覆盖人工编辑）
        List<String> changedFields = new ArrayList<>();
        if (model.getModelName() != null) {
            existing.setModelName(model.getModelName());
            changedFields.add("modelName");
        }
        if (model.getDisplayName() != null) {
            existing.setDisplayName(model.getDisplayName());
            changedFields.add("displayName");
        }
        if (model.getModelFamily() != null) {
            existing.setModelFamily(model.getModelFamily());
            changedFields.add("modelFamily");
        }
        if (model.getContextWindow() != null) {
            existing.setContextWindow(model.getContextWindow());
            changedFields.add("contextWindow");
        }
        if (model.getMaxInputTokens() != null) {
            existing.setMaxInputTokens(model.getMaxInputTokens());
            changedFields.add("maxInputTokens");
        }
        if (model.getMaxOutputTokens() != null) {
            existing.setMaxOutputTokens(model.getMaxOutputTokens());
            changedFields.add("maxOutputTokens");
        }
        if (model.getCapabilities() != null) {
            existing.setCapabilities(model.getCapabilities());
            changedFields.add("capabilities");
        }
        if (model.getModalities() != null) {
            existing.setModalities(model.getModalities());
            changedFields.add("modalities");
        }
        if (model.getKnowledgeCutoff() != null) {
            existing.setKnowledgeCutoff(model.getKnowledgeCutoff());
            changedFields.add("knowledgeCutoff");
        }

        if (!changedFields.isEmpty()) {
            Set<String> locked = new HashSet<>(existing.getLockedFields() == null
                    ? List.of() : existing.getLockedFields());
            locked.addAll(changedFields);
            existing.setLockedFields(new ArrayList<>(locked));
        }

        Model saved = modelRepository.save(existing);
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