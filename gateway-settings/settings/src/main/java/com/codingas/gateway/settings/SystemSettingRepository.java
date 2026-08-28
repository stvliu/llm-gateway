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
package com.codingas.gateway.settings;

import java.util.List;
import java.util.Optional;

/**
 * 系统设置仓储接口（域端口，由绑定模块实现）
 */
public interface SystemSettingRepository {

    /**
     * 按设置键查询
     *
     * @param key 设置键
     * @return 命中返回设置，未命中返回空
     */
    Optional<SystemSetting> findByKey(String key);

    /**
     * 查询全部设置
     *
     * @return 设置列表
     */
    List<SystemSetting> findAll();

    /**
     * 保存设置（新增或更新，按设置键唯一）
     *
     * @param setting 待保存的设置
     * @return 保存后的设置（含回填 id 与审计字段）
     */
    SystemSetting save(SystemSetting setting);
}
