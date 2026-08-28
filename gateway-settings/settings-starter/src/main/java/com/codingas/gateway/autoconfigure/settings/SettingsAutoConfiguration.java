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

import com.codingas.gateway.settings.SystemSettingRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Settings 域自动装配（纯装配，零业务逻辑）
 *
 * <p>通过 {@code gateway.settings.enabled}（默认开启）控制域装配开关；
 * 组件扫描覆盖本域核心包 {@code com.codingas.gateway.settings} 与绑定包
 * {@code com.codingas.gateway.settingsdata}（SystemSettingService 实现、
 * JpaSystemSettingRepository 等由扫描装配）。</p>
 *
 * <p>默认配置项种子加载器位于本自动装配包（不在扫描包内），
 * 故以 {@link Bean} 方法显式注册，避免靠包扫描漏装配。</p>
 */
@AutoConfiguration
@ComponentScan(basePackages = {
        "com.codingas.gateway.settings",
        "com.codingas.gateway.settingsdata"
})
@ConditionalOnProperty(prefix = "gateway.settings", name = "enabled", matchIfMissing = true)
public class SettingsAutoConfiguration {

    /**
     * 默认配置项种子加载器（ApplicationRunner：表空时插入 3 个默认配置项）
     *
     * @param repository 系统设置仓储
     * @return 种子加载器实例
     */
    @Bean
    public SettingsDefaultDataInitializer settingsDefaultDataInitializer(SystemSettingRepository repository) {
        return new SettingsDefaultDataInitializer(repository);
    }
}
