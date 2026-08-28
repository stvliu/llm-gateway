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
package com.codingas.gateway.autoconfigure.settings;

import com.codingas.gateway.settings.SyncInterval;
import com.codingas.gateway.settings.SystemSetting;
import com.codingas.gateway.settings.SystemSettingRepository;
import com.codingas.gateway.settings.SystemSettingService;
import com.codingas.gateway.settings.SystemSettingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 系统设置默认配置项种子加载器测试
 *
 * <p>采用内存版 {@link SystemSettingRepository} 假实现（无 Spring 上下文/无数据库），
 * 直接执行 {@link SettingsDefaultDataInitializer#run(ApplicationArguments)} 验证种子写入，
 * 再经真实 {@link SystemSettingServiceImpl} 类型化解码断言取值。</p>
 */
class SettingsDefaultDataInitializerTest {

    @Test
    @DisplayName("默认种子包含模型废弃自动化配置项")
    void seedContainsDeprecationDefaults() {
        InMemorySystemSettingRepository repository = new InMemorySystemSettingRepository();
        new SettingsDefaultDataInitializer(repository).run(null);
        SystemSettingService settingService = new SystemSettingServiceImpl(repository);

        assertThat(settingService.getBoolean("catalog.deprecation.enabled", false)).isTrue();
        assertThat(settingService.getBoolean("catalog.deprecation.runtime.enabled", false)).isTrue();
        assertThat(settingService.getInt("catalog.deprecation.confirm-count", 0)).isEqualTo(3);
        assertThat(settingService.getBoolean("catalog.deprecation.probe.enabled", false)).isTrue();
        assertThat(settingService.getEnum("catalog.deprecation.probe.interval", SyncInterval.class, null))
                .isEqualTo(SyncInterval.WEEKLY);
    }

    @Test
    @DisplayName("表已有数据时跳过种子加载")
    void run_skipsSeedWhenSettingsExist() {
        InMemorySystemSettingRepository repository = new InMemorySystemSettingRepository();
        repository.save(setting("app.title", "LLM-Gateway", "STRING"));

        new SettingsDefaultDataInitializer(repository).run(null);

        // 已存在数据时不再写入默认种子
        assertThat(repository.findAll()).hasSize(1);
    }

    private static SystemSetting setting(String key, String value, String type) {
        SystemSetting s = new SystemSetting();
        s.setSettingKey(key);
        s.setSettingValue(value);
        s.setValueType(type);
        s.setEditable(true);
        return s;
    }

    /**
     * 内存版系统设置仓储假实现（以 Map 存储，无需 Spring/数据库）
     */
    private static final class InMemorySystemSettingRepository implements SystemSettingRepository {

        private final Map<String, SystemSetting> store = new LinkedHashMap<>();

        @Override
        public Optional<SystemSetting> findByKey(String key) {
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public List<SystemSetting> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public SystemSetting save(SystemSetting setting) {
            store.put(setting.getSettingKey(), setting);
            return setting;
        }
    }
}
