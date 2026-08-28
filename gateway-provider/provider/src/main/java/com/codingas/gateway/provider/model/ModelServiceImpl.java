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
import com.codingas.gateway.common.util.SortSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模型管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    /** 模型可排序字段白名单 */
    private static final Set<String> MODEL_SORT_FIELDS = Set.of("modelName", "displayName", "id");

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

        // 排序：按白名单字段（modelName/displayName/id）字母序排列，非法 sortBy 回退默认，防注入
        models = models.stream().sorted(comparator(query.getSortBy(), query.getSortOrder())).toList();

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
     * 构建排序比较器
     *
     * <p>排序字段白名单校验（仅允许 modelName/displayName/id，其余回退默认 modelName），
     * 防止通过查询参数注入任意字段；排序方向仅识别 ASC/DESC，其余回退升序。</p>
     *
     * @param sortBy    排序字段
     * @param sortOrder 排序方向
     * @return 模型比较器
     */
    private Comparator<Model> comparator(String sortBy, String sortOrder) {
        String field = SortSupport.normalize(sortBy, MODEL_SORT_FIELDS, "modelName");
        boolean desc = SortSupport.isDesc(sortOrder);
        return switch (field) {
            case "displayName" -> SortSupport.byString(Model::getDisplayName, desc);
            case "id" -> SortSupport.byLong(Model::getId, desc);
            default -> SortSupport.byString(Model::getModelName, desc);
        };
    }

    /**
     * 更新模型
     */
    @Override
    @Transactional
    public Model update(Long id, Model model) {
        Model existing = modelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));

        // 记录本次人工修改的字段，加入锁定集合（同步时跳过这些字段，避免覆盖人工编辑）。
        // 仅当请求值非 null 且与原值不同才更新实体并锁定；值相同或未提交的字段既不更新也不锁定，
        // 避免前端编辑抽屉一次全量提交 6 个字段就永久锁定全部字段。
        List<String> changedFields = new ArrayList<>();
        if (model.getModelName() != null
                && !Objects.equals(model.getModelName(), existing.getModelName())) {
            existing.setModelName(model.getModelName());
            changedFields.add("modelName");
        }
        if (model.getDisplayName() != null
                && !Objects.equals(model.getDisplayName(), existing.getDisplayName())) {
            existing.setDisplayName(model.getDisplayName());
            changedFields.add("displayName");
        }
        if (model.getModelFamily() != null
                && !Objects.equals(model.getModelFamily(), existing.getModelFamily())) {
            existing.setModelFamily(model.getModelFamily());
            changedFields.add("modelFamily");
        }
        if (model.getContextWindow() != null
                && !Objects.equals(model.getContextWindow(), existing.getContextWindow())) {
            existing.setContextWindow(model.getContextWindow());
            changedFields.add("contextWindow");
        }
        if (model.getMaxInputTokens() != null
                && !Objects.equals(model.getMaxInputTokens(), existing.getMaxInputTokens())) {
            existing.setMaxInputTokens(model.getMaxInputTokens());
            changedFields.add("maxInputTokens");
        }
        if (model.getMaxOutputTokens() != null
                && !Objects.equals(model.getMaxOutputTokens(), existing.getMaxOutputTokens())) {
            existing.setMaxOutputTokens(model.getMaxOutputTokens());
            changedFields.add("maxOutputTokens");
        }
        // capabilities 采用语义比较：models.dev 同步存全键位 Map（含 false 键），
        // 前端编辑只提交 true 键 Map，直接 equals 几乎恒不等；改比较 true 键集合
        if (model.getCapabilities() != null
                && !sameCapabilitySet(model.getCapabilities(), existing.getCapabilities())) {
            existing.setCapabilities(model.getCapabilities());
            changedFields.add("capabilities");
        }
        if (model.getModalities() != null
                && !Objects.equals(model.getModalities(), existing.getModalities())) {
            existing.setModalities(model.getModalities());
            changedFields.add("modalities");
        }
        if (model.getKnowledgeCutoff() != null
                && !Objects.equals(model.getKnowledgeCutoff(), existing.getKnowledgeCutoff())) {
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
     * 语义比较能力集合：只比较 value 为 true 的键集合，忽略 false 键差异。
     * 任一为 null 时按引用/值相等处理（null 与 null 视为相同）。
     *
     * @param a 能力 Map A（可能为 null）
     * @param b 能力 Map B（可能为 null）
     * @return true 键集合相同返回 true
     */
    private boolean sameCapabilitySet(Map<String, Boolean> a, Map<String, Boolean> b) {
        if (a == null || b == null) {
            return a == b;
        }
        return trueKeys(a).equals(trueKeys(b));
    }

    /**
     * 提取能力 Map 中 value 为 true 的键集合
     *
     * @param map 能力 Map
     * @return 启用能力键集合
     */
    private Set<String> trueKeys(Map<String, Boolean> map) {
        return map.entrySet().stream()
            .filter(e -> Boolean.TRUE.equals(e.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    /**
     * 清除模型字段人工锁定（清空 lockedFields，恢复 models.dev 同步对全部字段的覆盖权限）
     *
     * @param id 模型 ID
     * @return 清除锁定后的模型实体
     */
    @Override
    @Transactional
    public Model unlockFields(Long id) {
        Model model = modelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Model", id));
        model.setLockedFields(null);
        return modelRepository.save(model);
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