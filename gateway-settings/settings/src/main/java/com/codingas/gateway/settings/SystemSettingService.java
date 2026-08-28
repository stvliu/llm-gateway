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
 * 系统设置服务（域核心用例门面）
 *
 * <p>提供按 key 读取（含类型化解析：数值/布尔/枚举）与更新配置项的能力；
 * 更新时执行存在性、可编辑性与值类型三类校验。</p>
 */
public interface SystemSettingService {

    /**
     * 按设置键查询设置项
     *
     * @param key 设置键
     * @return 命中返回设置项，未命中返回空
     */
    Optional<SystemSetting> getSetting(String key);

    /**
     * 按设置键读取字符串值
     *
     * @param key 设置键
     * @return 设置值；未命中返回 null
     */
    String get(String key);

    /**
     * 按设置键读取整数值
     *
     * @param key          设置键
     * @param defaultValue 缺失或解析失败时的默认值
     * @return 解析后的整数值
     */
    int getInt(String key, int defaultValue);

    /**
     * 按设置键读取布尔值
     *
     * @param key          设置键
     * @param defaultValue 缺失时的默认值
     * @return 布尔值
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * 按设置键读取枚举值
     *
     * @param key          设置键
     * @param enumType     枚举类型
     * @param defaultValue 缺失或非法时的默认值
     * @param <E>          枚举类型参数
     * @return 枚举值
     */
    <E extends Enum<E>> E getEnum(String key, Class<E> enumType, E defaultValue);

    /**
     * 查询全部设置项
     *
     * @return 设置项列表
     */
    List<SystemSetting> getAll();

    /**
     * 更新设置项的值
     *
     * <p>校验规则：key 不存在抛出 {@link IllegalArgumentException}；配置项不可编辑抛出
     * {@link IllegalArgumentException}；按 valueType 校验值格式（NUMBER 需整数、
     * BOOLEAN 需 true/false、ENUM 非空）。</p>
     *
     * @param key   设置键
     * @param value 新值
     * @return 保存后的设置项
     * @throws IllegalArgumentException 校验失败时抛出
     */
    SystemSetting update(String key, String value);
}
