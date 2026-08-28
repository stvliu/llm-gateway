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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

/**
 * 系统设置默认配置项种子加载器
 *
 * <p>应用启动时，若 {@code system_settings} 表为空则写入 8 个默认配置项
 * （审计保留天数、目录自动同步开关/周期、模型废弃自动化开关/运行检查/
 * 废弃确认次数/上游探测开关/探测周期），供审计清理（Task 4）、同步自动执行
 * （Task 5）与模型生命周期管理（模型废弃自动化）等后续功能消费。</p>
 *
 * <p>优先级取最低（{@link Ordered#LOWEST_PRECEDENCE}），确保在业务数据加载
 * （如 BuiltinDataLoader）之后执行，避免与其它种子逻辑竞争。</p>
 *
 * <p>写入逐条对唯一约束冲突容错，多实例并发首启时第二个实例撞
 * {@code setting_key} 唯一约束不会导致启动失败（见 {@link #seedDefaults()}）。</p>
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
     *
     * <p>逐条插入并对唯一约束冲突容错：多实例并发首启时，多个实例都可能通过
     * {@link #run(ApplicationArguments)} 的表空检查，后插入者会撞
     * {@code setting_key} 唯一约束。捕获数据完整性异常（JPA 场景下 Hibernate
     * {@code ConstraintViolationException} 被 Spring 翻译为
     * {@link DataIntegrityViolationException}，其子类
     * {@link org.springframework.dao.DuplicateKeyException} 为 JDBC 直译路径）
     * 后记录 warn 忽略，保证实例不因竞态启动失败。</p>
     */
    private void seedDefaults() {
        List<SystemSetting> defaults = List.of(
                setting("audit.retention.days", "90", "NUMBER", "AUDIT", "审计日志保留天数", true),
                setting("catalog.sync.enabled", "true", "BOOLEAN", "CATALOG", "是否启用模型目录自动同步", true),
                setting("catalog.sync.interval", "DAILY", "ENUM", "CATALOG", "模型目录自动同步周期", true),
                setting("catalog.deprecation.enabled", "true", "BOOLEAN", "CATALOG", "模型废弃自动化总开关", true),
                setting("catalog.deprecation.runtime.enabled", "true", "BOOLEAN", "CATALOG", "调用中自动检查模型废弃", true),
                setting("catalog.deprecation.confirm-count", "3", "NUMBER", "CATALOG", "废弃确认次数（防误判）", true),
                setting("catalog.deprecation.probe.enabled", "true", "BOOLEAN", "CATALOG", "上游列表探测（提前预警）", true),
                setting("catalog.deprecation.probe.interval", "WEEKLY", "ENUM", "CATALOG", "上游列表探测周期", true)
        );
        defaults.forEach(this::saveWithRaceTolerance);
        log.info("已写入 {} 条默认配置项", defaults.size());
    }

    /**
     * 带多实例竞态容错地写入单条默认配置项
     *
     * <p>捕获数据完整性异常（唯一约束冲突），记录 warn 后忽略；
     * 默认值均为固定已知合法值，完整性异常只会来自并发首启的键冲突，
     * 不吞掉其它类型的启动错误。</p>
     *
     * @param setting 默认配置项
     */
    private void saveWithRaceTolerance(SystemSetting setting) {
        try {
            repository.save(setting);
        } catch (DataIntegrityViolationException e) {
            // 多实例并发首启时另一实例已写入同一 setting_key，命中唯一约束属正常竞态，
            // 记录 warn 忽略即可，无需回滚或导致实例启动失败
            log.warn("默认配置项 {} 已存在（多实例并发首启竞态），跳过写入", setting.getSettingKey());
        }
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
