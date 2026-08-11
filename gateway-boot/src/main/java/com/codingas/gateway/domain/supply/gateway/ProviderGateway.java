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
package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.Provider;

import java.util.List;
import java.util.Optional;

/**
 * 供应商网关接口
 */
public interface ProviderGateway {

    /**
     * 保存供应商
     */
    Provider save(Provider provider);

    /**
     * 根据ID查找供应商
     */
    Optional<Provider> findById(Long id);

    /**
     * 根据代码查找供应商
     */
    Optional<Provider> findByCode(String code);

    /**
     * 根据名称查找供应商
     */
    Optional<Provider> findByName(String name);

    /**
     * 查找所有供应商
     */
    List<Provider> findAll();

    /**
     * 查找所有活跃供应商
     */
    List<Provider> findAllActive();

    /**
     * 统计供应商总数
     */
    long count();

    /**
     * 删除供应商
     */
    void delete(Provider provider);

    /**
     * 根据ID删除供应商
     */
    void deleteById(Long id);

    /**
     * 检查名称是否已存在
     */
    boolean existsByName(String name);

    /**
     * 检查代码是否已存在
     */
    boolean existsByCode(String code);

    /**
     * 关键词搜索（code 或 name 包含关键字）
     */
    List<Provider> findByKeyword(String keyword);

    /**
     * 获取最大版本号
     */
    default long getMaxVersion() {
        return 0L;
    }
}