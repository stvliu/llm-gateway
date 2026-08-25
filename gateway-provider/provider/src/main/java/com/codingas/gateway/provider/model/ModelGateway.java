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

import com.codingas.gateway.provider.model.Model;

import java.util.List;
import java.util.Optional;

/**
 * 模型持久化接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface ModelGateway {

    /**
     * 保存模型
     */
    Model save(Model model);

    /**
     * 根据 ID 查找模型
     */
    Optional<Model> findById(Long id);

    /**
     * 根据模型名查找模型
     */
    Optional<Model> findByModelName(String modelName);

    /**
     * 查询所有模型
     */
    List<Model> findAll();

    /**
     * 批量查找模型
     */
    List<Model> findByIds(List<Long> ids);

    /**
     * 统计模型总数
     */
    long count();

    /**
     * 删除模型
     */
    void delete(Model model);

    /**
     * 关键词搜索（modelName 或 displayName 包含关键字）
     */
    List<Model> findByKeyword(String keyword);

    /**
     * 按能力过滤（capabilities 包含指定能力）
     */
    List<Model> findByCapability(String capability);

    /**
     * 获取最大版本号
     */
    default long getMaxVersion() {
        return 0L;
    }
}