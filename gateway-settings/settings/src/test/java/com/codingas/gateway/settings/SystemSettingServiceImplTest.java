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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 系统设置服务实现测试
 */
@ExtendWith(MockitoExtension.class)
class SystemSettingServiceImplTest {

    @Mock
    private SystemSettingRepository repository;

    private SystemSettingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SystemSettingServiceImpl(repository);
    }

    private SystemSetting setting(String key, String value, String type, boolean editable) {
        SystemSetting s = new SystemSetting();
        s.setSettingKey(key);
        s.setSettingValue(value);
        s.setValueType(type);
        s.setEditable(editable);
        return s;
    }

    @Test
    @DisplayName("getInt 解析数值，缺失回退默认值")
    void getInt_parsesAndFallsBack() {
        when(repository.findByKey("audit.retention.days")).thenReturn(Optional.of(setting("audit.retention.days", "90", "NUMBER", true)));
        when(repository.findByKey("missing")).thenReturn(Optional.empty());
        assertThat(service.getInt("audit.retention.days", 30)).isEqualTo(90);
        assertThat(service.getInt("missing", 30)).isEqualTo(30);
    }

    @Test
    @DisplayName("getEnum 解析枚举，缺失/非法回退默认值")
    void getEnum_parsesAndFallsBack() {
        when(repository.findByKey("catalog.sync.interval")).thenReturn(Optional.of(setting("catalog.sync.interval", "WEEKLY", "ENUM", true)));
        when(repository.findByKey("missing")).thenReturn(Optional.empty());
        assertThat(service.getEnum("catalog.sync.interval", SyncInterval.class, SyncInterval.DAILY))
                .isEqualTo(SyncInterval.WEEKLY);
        assertThat(service.getEnum("missing", SyncInterval.class, SyncInterval.DAILY))
                .isEqualTo(SyncInterval.DAILY);
    }

    @Test
    @DisplayName("update 校验：不存在 / 不可编辑 / 类型非法")
    void update_validates() {
        when(repository.findByKey("nope")).thenReturn(Optional.empty());
        when(repository.findByKey("audit.retention.days")).thenReturn(Optional.of(setting("audit.retention.days", "90", "NUMBER", true)));
        when(repository.findByKey("locked")).thenReturn(Optional.of(setting("locked", "x", "STRING", false)));

        assertThatThrownBy(() -> service.update("nope", "1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.update("audit.retention.days", "abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.update("locked", "y")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("update 合法值保存并返回")
    void update_validValue_saves() {
        SystemSetting existing = setting("audit.retention.days", "90", "NUMBER", true);
        when(repository.findByKey("audit.retention.days")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SystemSetting updated = service.update("audit.retention.days", "120");
        assertThat(updated.getSettingValue()).isEqualTo("120");
    }

    @Test
    @DisplayName("getBoolean 解析布尔值，缺失回退默认值")
    void getBoolean_parsesAndFallsBack() {
        when(repository.findByKey("catalog.sync.enabled"))
                .thenReturn(Optional.of(setting("catalog.sync.enabled", "true", "BOOLEAN", true)));
        when(repository.findByKey("catalog.sync.disabled"))
                .thenReturn(Optional.of(setting("catalog.sync.disabled", "false", "BOOLEAN", true)));
        when(repository.findByKey("missing")).thenReturn(Optional.empty());

        assertThat(service.getBoolean("catalog.sync.enabled", false)).isTrue();
        assertThat(service.getBoolean("catalog.sync.disabled", true)).isFalse();
        assertThat(service.getBoolean("missing", true)).isTrue();
    }

    @Test
    @DisplayName("get 存在返回原始值，缺失返回 null")
    void get_returnsValueOrNull() {
        when(repository.findByKey("app.title"))
                .thenReturn(Optional.of(setting("app.title", "LLM-Gateway", "STRING", true)));
        when(repository.findByKey("missing")).thenReturn(Optional.empty());

        assertThat(service.get("app.title")).isEqualTo("LLM-Gateway");
        assertThat(service.get("missing")).isNull();
    }

    @Test
    @DisplayName("getAll 返回仓储全部设置")
    void getAll_returnsAll() {
        when(repository.findAll()).thenReturn(List.of(
                setting("a", "1", "NUMBER", true),
                setting("b", "2", "NUMBER", true)));

        assertThat(service.getAll()).hasSize(2);
    }

    @Test
    @DisplayName("getInt 值为非数字回退默认值")
    void getInt_invalidValueFallsBack() {
        when(repository.findByKey("bad.number"))
                .thenReturn(Optional.of(setting("bad.number", "not-a-number", "NUMBER", true)));

        assertThat(service.getInt("bad.number", 30)).isEqualTo(30);
    }

    @Test
    @DisplayName("getEnum 非法枚举名回退默认值")
    void getEnum_invalidNameFallsBack() {
        when(repository.findByKey("bad.enum"))
                .thenReturn(Optional.of(setting("bad.enum", "EVERY_HOUR", "ENUM", true)));

        assertThat(service.getEnum("bad.enum", SyncInterval.class, SyncInterval.DAILY))
                .isEqualTo(SyncInterval.DAILY);
    }

    @Test
    @DisplayName("update BOOLEAN 非法值抛异常")
    void update_booleanInvalid_throws() {
        when(repository.findByKey("catalog.sync.enabled"))
                .thenReturn(Optional.of(setting("catalog.sync.enabled", "true", "BOOLEAN", true)));

        assertThatThrownBy(() -> service.update("catalog.sync.enabled", "maybe"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("update ENUM 空值抛异常")
    void update_enumBlank_throws() {
        when(repository.findByKey("catalog.sync.interval"))
                .thenReturn(Optional.of(setting("catalog.sync.interval", "DAILY", "ENUM", true)));

        assertThatThrownBy(() -> service.update("catalog.sync.interval", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("update STRING 放行并保存")
    void update_string_allowed() {
        SystemSetting existing = setting("app.title", "old", "STRING", true);
        when(repository.findByKey("app.title")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SystemSetting updated = service.update("app.title", "new");

        assertThat(updated.getSettingValue()).isEqualTo("new");
    }

    @Test
    @DisplayName("update 不可编辑（isEditable=false）抛异常")
    void update_notEditable_throws() {
        when(repository.findByKey("locked"))
                .thenReturn(Optional.of(setting("locked", "x", "STRING", false)));

        assertThatThrownBy(() -> service.update("locked", "y"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
