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
package com.codingas.gateway.iam.application;

import com.codingas.gateway.iam.application.Application;

import java.util.List;

/**
 * 应用领域网关接口
 *
 * <p>应用聚合根的持久化抽象。domain 层仅依赖此接口，
 * 实现位于 infrastructure 层（COLA Light 依赖倒置）。</p>
 */
public interface ApplicationGateway {

    /**
     * 按主键查找应用
     *
     * @param id 应用 ID
     * @return 命中的应用实体；不存在时返回 null
     */
    Application findById(Long id);

    /**
     * 按应用编码查找
     *
     * @param code 应用编码（全局唯一）
     * @return 命中的应用实体；不存在时返回 null
     */
    Application findByCode(String code);

    /**
     * 查询全部应用
     *
     * @return 应用列表
     */
    List<Application> findAll();

    /**
     * 保存应用
     *
     * @param app 应用实体
     * @return 保存后的应用实体（含生成的 ID 与审计字段）
     */
    Application save(Application app);

    /**
     * 删除应用
     *
     * @param id 应用 ID
     */
    void deleteById(Long id);
}
