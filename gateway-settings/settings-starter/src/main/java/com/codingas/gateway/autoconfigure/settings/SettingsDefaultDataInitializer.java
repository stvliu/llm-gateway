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

import com.codingas.gateway.settings.SystemSetting;
import com.codingas.gateway.settings.SystemSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * 系统设置默认配置项种子加载器
 *
 * <p>应用启动时，若 {@code system_settings} 表为空则写入 3 个默认配置项
 * （审计保留天数、目录自动同步开关、目录自动同步周期），
 * 供审计清理（Task 4）与同步自动执行（Task 5）等后续功能消费。</p>
 *
 * <p>优先级取最低（{@link Ordered#LOWEST_PRECEDENCE}），确保在业务数据加载
 * （如 BuiltinDataLoader）之后执行，避免与其它种子逻辑竞争。</p>
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class SettingsDefaultDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SettingsDefaultDataInitializer.class);

    private final SystemSettingRepository repository;

    /**
     * 构造器注入仓储
     *
     * @param repository 系统设置仓储
     */
    public SettingsDefaultDataInitializer(SystemSettingRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<SystemSetting> existing = repository.findAll();
        if (!existing.isEmpty()) {
            log.info("系统设置已存在 {} 条，跳过默认配置项种子加载", existing.size());
            return;
        }
        seedDefaults();
    }

    /**
     * 写入默认配置项
     */
    private void seedDefaults() {
        List<SystemSetting> defaults = List.of(
                setting("audit.retention.days", "90", "NUMBER", "AUDIT", "审计日志保留天数", true),
                setting("catalog.sync.enabled", "true", "BOOLEAN", "CATALOG", "是否启用模型目录自动同步", true),
                setting("catalog.sync.interval", "DAILY", "ENUM", "CATALOG", "模型目录自动同步周期", true)
        );
        defaults.forEach(repository::save);
        log.info("已写入 {} 条默认配置项", defaults.size());
    }

    /**
     * 构造设置项实体
     *
     * @param key       设置键
     * @param value     设置值
     * @param valueType 值类型
     * @param group     分组名
     * @param desc      描述
     * @param editable  是否可编辑
     * @return 设置项实体
     */
    private SystemSetting setting(String key, String value, String valueType, String group,
                                  String desc, boolean editable) {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setValueType(valueType);
        setting.setGroupName(group);
        setting.setDescription(desc);
        setting.setEditable(editable);
        return setting;
    }
}
