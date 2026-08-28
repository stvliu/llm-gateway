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
}
