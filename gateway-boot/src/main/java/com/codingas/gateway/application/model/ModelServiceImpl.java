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
package com.codingas.gateway.application.model;

import com.codingas.gateway.application.model.dto.ModelCreateRequest;
import com.codingas.gateway.application.model.dto.ModelQueryRequest;
import com.codingas.gateway.application.model.dto.ModelResponse;
import com.codingas.gateway.application.model.dto.ModelUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
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

    private final ModelGateway modelGateway;

    /**
     * 创建模型
     */
    @Override
    @Transactional
    public ModelResponse create(ModelCreateRequest request) {
        // 创建模型
        Model model = new Model();
        model.setModelName(request.getModelName());
        model.setDisplayName(request.getDisplayName());
        model.setModelFamily(request.getModelFamily());
        model.setContextWindow(request.getContextWindow());
        model.setMaxInputTokens(request.getMaxInputTokens());
        model.setMaxOutputTokens(request.getMaxOutputTokens());
        model.setCapabilities(request.getCapabilities());
        model.setModalities(request.getModalities());

        Model savedModel = modelGateway.save(model);
        log.info("模型创建成功, id={}, modelName={}", savedModel.getId(), savedModel.getModelName());
        return toResponse(savedModel);
    }

    /**
     * 根据 ID 获取模型
     */
    @Override
    public ModelResponse getById(Long id) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        return toResponse(model);
    }

    /**
     * 查询模型列表
     */
    @Override
    public PageResponse<ModelResponse> query(ModelQueryRequest request) {
        List<Model> models = modelGateway.findAll();

        // 过滤
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().toLowerCase();
            models = models.stream()
                .filter(m -> (m.getDisplayName() != null && m.getDisplayName().toLowerCase().contains(keyword))
                    || m.getModelName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        // 状态过滤：ACTIVE=未废弃（deprecatedAt 为空），INACTIVE=已废弃（deprecatedAt 非空）
        if (request.getState() != null && !request.getState().isBlank()) {
            String state = request.getState().toUpperCase();
            models = models.stream()
                .filter(m -> "ACTIVE".equals(state) ? m.isAvailable() : !m.isAvailable())
                .collect(Collectors.toList());
        }

        // 统计
        long total = models.size();

        // 分页
        int offset = request.getOffset();
        int limit = request.getLimit();
        List<Model> pagedModels = models.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        List<ModelResponse> responses = pagedModels.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return PageResponse.of(responses, request.getPage(), limit, total);
    }

    /**
     * 更新模型
     */
    @Override
    @Transactional
    public ModelResponse update(Long id, ModelUpdateRequest request) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));

        if (request.getModelName() != null) {
            model.setModelName(request.getModelName());
        }
        if (request.getDisplayName() != null) {
            model.setDisplayName(request.getDisplayName());
        }
        if (request.getModelFamily() != null) {
            model.setModelFamily(request.getModelFamily());
        }
        if (request.getContextWindow() != null) {
            model.setContextWindow(request.getContextWindow());
        }
        if (request.getMaxInputTokens() != null) {
            model.setMaxInputTokens(request.getMaxInputTokens());
        }
        if (request.getMaxOutputTokens() != null) {
            model.setMaxOutputTokens(request.getMaxOutputTokens());
        }
        if (request.getCapabilities() != null) {
            model.setCapabilities(request.getCapabilities());
        }
        if (request.getModalities() != null) {
            model.setModalities(request.getModalities());
        }

        Model saved = modelGateway.save(model);
        log.info("模型更新成功, id={}, modelName={}", id, saved.getModelName());
        return toResponse(saved);
    }

    /**
     * 删除模型
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        modelGateway.delete(model);
    }

    /**
     * 启用/禁用模型
     */
    @Override
    @Transactional
    public ModelResponse setEnabled(Long id, boolean enabled) {
        Model model = modelGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        if (enabled) {
            model.setDeprecatedAt(null);
        } else {
            model.setDeprecatedAt(java.time.Instant.now());
        }
        return toResponse(modelGateway.save(model));
    }

    /**
     * 转换为响应 DTO
     */
    private ModelResponse toResponse(Model model) {
        ModelResponse response = new ModelResponse();
        response.setId(model.getId());
        response.setModelName(model.getModelName());
        response.setDisplayName(model.getDisplayName());
        response.setModelFamily(model.getModelFamily());
        response.setContextWindow(model.getContextWindow());
        response.setMaxInputTokens(model.getMaxInputTokens());
        response.setMaxOutputTokens(model.getMaxOutputTokens());
        response.setCapabilities(model.getCapabilities());
        response.setModalities(model.getModalities());
        response.setDeprecatedAt(model.getDeprecatedAt());
        response.setDeprecationMessage(model.getDeprecationMessage());
        // 状态：未废弃为 ACTIVE，已废弃为 INACTIVE
        response.setState(model.isAvailable() ? "ACTIVE" : "INACTIVE");
        response.setCreatedAt(model.getCreatedAt());
        response.setUpdatedAt(model.getUpdatedAt());
        return response;
    }
}