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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 系统设置服务实现
 *
 * <p>读取类方法统一委托 {@link SystemSettingRepository#findByKey(String)}，
 * 类型化解析（数值/布尔/枚举）对非法值容错回退默认值；更新走存在性、可编辑性、
 * 值类型三道校验后落库。</p>
 */
@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository repository;

    @Override
    public Optional<SystemSetting> getSetting(String key) {
        return repository.findByKey(key);
    }

    @Override
    public String get(String key) {
        return repository.findByKey(key).map(SystemSetting::getSettingValue).orElse(null);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return repository.findByKey(key)
                .map(SystemSetting::getSettingValue)
                .map(value -> parseInt(value, defaultValue))
                .orElse(defaultValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return repository.findByKey(key)
                .map(SystemSetting::getSettingValue)
                .map(value -> value != null ? Boolean.parseBoolean(value) : defaultValue)
                .orElse(defaultValue);
    }

    @Override
    public <E extends Enum<E>> E getEnum(String key, Class<E> enumType, E defaultValue) {
        return repository.findByKey(key)
                .map(SystemSetting::getSettingValue)
                .map(value -> parseEnum(value, enumType, defaultValue))
                .orElse(defaultValue);
    }

    @Override
    public List<SystemSetting> getAll() {
        return repository.findAll();
    }

    @Override
    public SystemSetting update(String key, String value) {
        SystemSetting setting = repository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("配置项不存在: " + key));
        if (!setting.isEditable()) {
            throw new IllegalArgumentException("配置项不可编辑: " + key);
        }
        validateValue(value, setting.getValueType(), key);
        setting.setSettingValue(value);
        return repository.save(setting);
    }

    /**
     * 按值类型校验更新值
     *
     * @param value     待校验的值
     * @param valueType 值类型（NUMBER / BOOLEAN / ENUM，其余按 STRING 放行）
     * @param key       设置键（用于错误信息）
     */
    private void validateValue(String value, String valueType, String key) {
        if (valueType == null) {
            return;
        }
        switch (valueType.toUpperCase()) {
            case "NUMBER" -> {
                try {
                    Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("配置项 " + key + " 需要数值: " + value);
                }
            }
            case "BOOLEAN" -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException("配置项 " + key + " 需要布尔值: " + value);
                }
            }
            case "ENUM" -> {
                if (value == null || value.isBlank()) {
                    throw new IllegalArgumentException("配置项 " + key + " 枚举值不能为空");
                }
            }
            default -> {
                // STRING 及其它类型不校验格式
            }
        }
    }

    /**
     * 容错解析整数，解析失败回退默认值
     */
    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    /**
     * 容错解析枚举，缺失/非法回退默认值
     */
    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumType, E defaultValue) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return defaultValue;
        }
    }
}
