# 系统设置功能设计（gateway-settings 域）

> 日期：2026-08-28
> 状态：设计草案（待确认）
> 需求来源：用户确认——通用配置表 + 动态生效 + 新建 gateway-settings 域（3 模块）；审计日志保留天数配置 + 手动清理 + 定时清理；models.dev 同步自动执行 + 间隔配置（每天/每周/每月）

## 1. 背景与目标

llm-gateway 当前所有配置均为 `@ConfigurationProperties`（静态 `application.yml`，启动时固定、运行时不可变）。用户需要"系统设置"能力：

1. **系统设置**：通用配置表（`system_settings`）+ 运行时动态生效 + 管理 API + 前端设置页
2. **审计日志**：保留天数作为配置项，驱动定时清理；另提供手动清理入口
3. **models.dev 同步**：自动执行机制（定时任务），执行间隔（每天/每周/每月）作为配置项；手工触发保留

## 2. 模块结构（新建 gateway-settings 域，3 模块）

对齐现有域三明治模式（参照 gateway-alert）：

```
gateway-settings/settings          # 域核心（settings 包：实体/接口/服务/配置枚举）
gateway-settings/settings-data     # 持久化绑定（settingsdata 包：DO/JPA Repository）
gateway-settings/settings-starter  # 自动装配（autoconfigure.settings + AutoConfiguration.imports）
```

- 根 pom `<module>` 注册 3 个模块；`gateway-boot` pom 依赖 `gateway-settings-starter`
- 依赖方向：settings 域**只依赖 gateway-common**，不依赖任何业务域（避免循环依赖——audit/provider 将依赖 settings 读配置）

**定时任务落位**（避免 settings ↔ 业务域循环依赖）：
| 任务 | 落位 | 依赖 |
|------|------|------|
| `AuditCleanupTask`（审计定时清理） | gateway-audit/audit 模块 | + settings（读保留天数） |
| `CatalogSyncTask`（同步自动执行） | gateway-provider/provider 模块 | + settings（读间隔/开关） |

对应 pom：gateway-audit/audit 与 gateway-provider/provider 各加 `gateway-settings` 依赖。

## 3. 数据模型（V70 迁移 + 新域实体）

### system_settings 表

| 列 | 类型 | 说明 |
|----|------|------|
| `id` | bigint PK | |
| `setting_key` | varchar(128) UNIQUE | 配置键（如 `audit.retention.days`） |
| `setting_value` | text | 配置值（String 存储） |
| `group_name` | varchar(64) | 分组（AUDIT / CATALOG 等） |
| `description` | varchar(256) | 配置说明 |
| `value_type` | varchar(32) | STRING / NUMBER / BOOLEAN / ENUM |
| `is_editable` | boolean | 是否允许修改（默认 true） |
| 审计字段 | | created_by/created_at/updated_by/updated_at（BaseDo） |

### 实体
- `SystemSetting`（settings 域核心，继承 BaseEntity）：settingKey/settingValue/groupName/description/valueType/isEditable
- `SystemSettingDo`（settings-data，继承 BaseDo）
- `SystemSettingRepository`（域接口）：`Optional<SystemSetting> findByKey(String key)`、`List<SystemSetting> findAll()`、`SystemSetting save(...)`
- `JpaSystemSettingRepository`（settings-data 实现）

### 初始配置项（种子）

| key | 默认值 | 类型 | 分组 | 说明 |
|-----|--------|------|------|------|
| `audit.retention.days` | `90` | NUMBER | AUDIT | 审计日志保留天数，超期定时清理 |
| `catalog.sync.enabled` | `true` | BOOLEAN | CATALOG | models.dev 自动同步开关 |
| `catalog.sync.interval` | `DAILY` | ENUM | CATALOG | 自动同步间隔（DAILY/WEEKLY/MONTHLY） |

种子加载：settings-starter 启动时若表空则插入默认配置（简单 `ApplicationRunner` 或复用现有种子加载模式）。

## 4. 配置服务（动态生效）

### SystemSettingService（settings 域核心）
- `String get(String key)` / `Optional<SystemSetting> getSetting(String key)`
- `int getInt(String key, int default)`、`boolean getBoolean(String key, boolean default)`、`<E extends Enum<E>> E getEnum(String key, Class<E> type, E default)`
- `List<SystemSetting> getAll()`
- `SystemSetting update(String key, String value)`：校验 key 存在 + isEditable + value_type 格式（NUMBER 数字、ENUM 枚举值、BOOLEAN true/false），更新后失效缓存
- **动态生效**：读取直接查库（配置量小、频率低），更新后立即生效；不加缓存（避免一致性复杂度）——若未来高频读取再加简单缓存 + 失效

### API（gateway-web）
| 端点 | 能力 |
|------|------|
| `GET /api/v1/settings` | 返回全部配置（设置页展示） |
| `PUT /api/v1/settings/{key}` | 更新配置值（body `{value}`），立即生效 |

权限：非 USER 白名单 → ADMIN（对齐现有管理端点）。

## 5. 审计日志清理

### Repository 扩展（gateway-audit）
- `AuditLogRepository` 新增 `int deleteBefore(Instant cutoff)`（audit-data 的 `AuditLogJpaRepository` 实现 `DELETE WHERE created_at < cutoff`）

### 手动清理 API（gateway-web AuditController 扩展）
- `DELETE /api/v1/audit-logs`（query `before` 时间或 `days` 天数）→ 删除并返回删除条数
- 受 `AuditLogInterceptor` 自动审计（管理操作）

### 定时清理任务（gateway-audit/audit 模块）
- `AuditCleanupTask`（`@Component` + `@Scheduled` 每日凌晨）：读 `audit.retention.days`（默认 90）→ `deleteBefore(now - days)` → 日志记录清理结果
- `@EnableScheduling` 已在 GatewayApplication 开启

## 6. models.dev 同步自动执行

### 定时任务（gateway-provider/provider 模块）
- `CatalogSyncTask`（`@Component` + `@Scheduled` 每小时检查）：
  1. 读 `catalog.sync.enabled`（默认 true）；false 跳过
  2. 读 `catalog.sync.interval`：DAILY=24h / WEEKLY=7d / MONTHLY=30d
  3. 读最近一次同步时间（`CatalogSyncLogRepository.findLatest()` 的 syncedAt；无记录则视为需要同步）
  4. 距上次同步 ≥ 间隔 → 调 `ModelCatalogSyncService.sync()`（幂等 upsert）
- **间隔语义**："距上次同步的时长阈值"，而非固定 cron——与手工触发天然兼容（手工同步后不会立即重复自动同步）

### 手工同步保留
- 现有 `POST /api/v1/catalog/sync` 与 `GET /api/v1/catalog/sync/status` 不变

## 7. 前端（gateway-console）

1. **菜单**：menuConfig 新增"系统设置"菜单项（`/settings`，icon 用 SettingOutlined，`settings:read` 权限）
2. **设置页**（`src/pages/Settings/index.tsx` + 路由注册）：
   - 审计日志分组：保留天数（NumberInput）+ "立即清理"按钮（调 DELETE /api/v1/audit-logs?days=N，确认弹窗后执行）+ 当前审计总数展示（可选）
   - 模型目录分组：自动同步开关（Switch）+ 间隔选择（Select：每天/每周/每月）+ "立即同步"按钮（调 POST /api/v1/catalog/sync）+ 最近同步状态展示（复用 useCatalogSync）
   - 保存：调 PUT /api/v1/settings/{key}，成功后刷新
3. **类型与 API**：`types/settings.ts` + `services/api/settings.ts` + `services/query/useSettings.ts`（对齐现有模式）

## 8. 测试计划（TDD）

| 层 | 测试 |
|----|------|
| settings 服务 | SystemSettingService：get/getInt/getBoolean/getEnum、update 校验（isEditable/value_type）、不存在 key |
| settings 仓库 | JpaSystemSettingRepository 往返 + findByKey |
| 审计清理 | AuditCleanupTask：读保留天数 + deleteBefore 调用 + disabled 处理；AuditLogRepository.deleteBefore（Mockito） |
| 同步任务 | CatalogSyncTask：开关 false 跳过、间隔判断（未到不触发/到达触发）、无历史记录触发 |
| Web | SettingsController（GET/PUT + 权限）、AuditController 清理端点 |
| 前端 | 类型检查 + 设置页构建 |

## 9. 风险与对策

| 风险 | 对策 |
|------|------|
| settings ↔ 业务域循环依赖 | 任务放业务域（audit/provider），settings 只依赖 common |
| 并发同步重复执行 | sync() 幂等 upsert（已具备），仅多写一条日志 |
| 动态配置一致性 | 直接查库（低频率），更新立即生效；未来高频再加缓存 |
| 定时任务与手工同步竞争 | 间隔基于"距上次同步"，天然去重 |

## 10. 实施范围（模块落位）

| 模块 | 变更 |
|------|------|
| gateway-settings/settings + settings-data + settings-starter | 新域 3 模块（实体/DO/Repository/Service/自动装配/种子） |
| gateway-boot | 根 pom 注册 3 模块 + boot pom 依赖 settings-starter + V70 迁移 |
| gateway-audit/audit | AuditCleanupTask + AuditLogRepository.deleteBefore（audit-data 实现） |
| gateway-provider/provider | CatalogSyncTask + 读间隔配置 |
| gateway-web | SettingsController + AuditController 清理端点 + DTO |
| gateway-console | 设置页 + 菜单 + 类型/API/hook |
| gateway-coverage | 新代码测试覆盖 |

## 11. 里程碑

1. **M1 域骨架**：gateway-settings 3 模块 + V70 迁移 + 实体/DO/Repository + 测试
2. **M2 配置服务**：SystemSettingService + SettingsController + 种子 + 测试
3. **M3 审计清理**：deleteBefore + AuditCleanupTask + 手动清理端点 + 测试
4. **M4 同步自动执行**：CatalogSyncTask + 间隔判断 + 测试
5. **M5 前端**：设置页 + 菜单 + 类型/API/hook
6. **M6 验证**：全量构建 + 设置页功能验证
