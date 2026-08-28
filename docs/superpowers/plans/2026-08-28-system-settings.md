# 系统设置功能（gateway-settings 域）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新建 gateway-settings 域（3 模块），实现通用配置表 + 动态生效 + 管理 API + 前端设置页；审计日志保留天数配置驱动定时/手动清理；models.dev 同步自动执行（间隔配置 每天/每周/每月）。

**Architecture:** 新域三明治（settings 核心 + settings-data JPA + settings-starter 自动装配），settings 只依赖 gateway-common。定时任务放业务域避免循环依赖：`AuditCleanupTask`（audit 域，每日，读保留天数删审计日志）、`CatalogSyncTask`（provider 域，每小时检查，读开关+间隔+距上次同步判断触发 sync）。配置动态生效（直接查库，更新立即生效）。

**Tech Stack:** Java 21、Spring Boot 3.5.x、Spring Data JPA（jsonb/审计）、@Scheduled（@EnableScheduling 已在 GatewayApplication 开启）、JUnit5 + Mockito + AssertJ、React。

## Global Constraints

- 新域模块命名对齐现有域（参照 gateway-alert）：`gateway-settings/settings`（根包 `com.codingas.gateway.settings`）、`gateway-settings/settings-data`（根包 `com.codingas.gateway.settingsdata`）、`gateway-settings/settings-starter`（`com.codingas.gateway.autoconfigure.settings`）。
- 分层依赖：域接口/实体在 settings 核心，JPA 实现在 settings-data，Controller 在 gateway-web。
- **settings 域只依赖 gateway-common**（不依赖任何业务域）；audit/provider 模块加 `gateway-settings` 依赖。
- 中文注释/Javadoc；public 方法中文 Javadoc。
- TDD：先写失败测试再实现。
- 提交信息用中文，每任务一次 commit。
- SQL 幂等（IF NOT EXISTS / CREATE TABLE IF NOT EXISTS），兼容 H2(PostgreSQL 模式)/PostgreSQL。
- 审计字段 created_by/created_at/updated_by/updated_at（BaseEntity/BaseDo 约定）。
- 定时任务落位：AuditCleanupTask 在 gateway-audit/audit，CatalogSyncTask 在 gateway-provider/provider。
- 生产代码不得出现 TBD/TODO。

---

### Task 1: gateway-settings 域骨架（3 模块 + V70 迁移 + 实体/DO/Repository）

**Files:**
- Modify: `pom.xml`（根 pom `<modules>` 加 3 项：gateway-settings/settings、gateway-settings/settings-data、gateway-settings/settings-starter）
- Create: `gateway-settings/settings/pom.xml`
- Create: `gateway-settings/settings-data/pom.xml`
- Create: `gateway-settings/settings-starter/pom.xml`
- Create: `gateway-boot/src/main/resources/db/migration/V70__create_system_settings.sql`
- Create: `gateway-settings/settings/src/main/java/com/codingas/gateway/settings/SystemSetting.java`
- Create: `gateway-settings/settings/src/main/java/com/codingas/gateway/settings/SystemSettingRepository.java`
- Create: `gateway-settings/settings-data/src/main/java/com/codingas/gateway/settingsdata/SystemSettingDo.java`
- Create: `gateway-settings/settings-data/src/main/java/com/codingas/gateway/settingsdata/SystemSettingJpaRepository.java`
- Create: `gateway-settings/settings-data/src/main/java/com/codingas/gateway/settingsdata/JpaSystemSettingRepository.java`
- Test: `gateway-settings/settings-data/src/test/java/com/codingas/gateway/settingsdata/JpaSystemSettingRepositoryTest.java`

**Interfaces:**
- Consumes: `BaseEntity`（common.entity）、`BaseDo`（common.data）。
- Produces:
  - `SystemSetting` 实体（继承 BaseEntity）：`String settingKey`、`String settingValue`、`String groupName`、`String description`、`String valueType`、`boolean editable`
  - `SystemSettingRepository` 域接口：`Optional<SystemSetting> findByKey(String key)`、`List<SystemSetting> findAll()`、`SystemSetting save(SystemSetting setting)`
  - `SystemSettingDo`（@Entity @Table system_settings）、`SystemSettingJpaRepository`（Spring Data，findBySettingKey/findAll）、`JpaSystemSettingRepository`（@Component 实现）

- [ ] **Step 1: 写 V70 迁移 SQL**

创建 `gateway-boot/src/main/resources/db/migration/V70__create_system_settings.sql`（顶部版权注释 + 标题注释，参照 V69）：

```sql
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(128) NOT NULL UNIQUE,
    setting_value TEXT,
    group_name VARCHAR(64),
    description VARCHAR(256),
    value_type VARCHAR(32) DEFAULT 'STRING',
    is_editable BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP
);
```

- [ ] **Step 2: 创建 3 个模块 pom**

参照 `gateway-alert/alert/pom.xml`（parent = gateway-project，relativePath ../../pom.xml）：

- `gateway-settings/settings/pom.xml`：artifactId `gateway-settings`，name `Gateway Settings`，依赖：gateway-common、lombok、jackson-databind、junit-jupiter、mockito-junit-jupiter、assertj-core（对照 alert 核心依赖集合）。
- `gateway-settings/settings-data/pom.xml`：artifactId `gateway-settings-data`，依赖：gateway-settings、spring-boot-starter-data-jpa、lombok、测试依赖。
- `gateway-settings/settings-starter/pom.xml`：artifactId `gateway-settings-starter`，依赖：gateway-settings、gateway-settings-data、spring-boot-autoconfigure、lombok。

`settings-starter` 建目录 `gateway-settings/settings-starter/src/main/resources/META-INF/spring/`（本任务先建目录，AutoConfiguration 内容在 Task 2）。

根 pom `<modules>` 在 `gateway-alert` 相关模块后追加 3 个 `<module>`。

- [ ] **Step 3: 写失败测试**

`gateway-settings/settings-data/src/test/java/com/codingas/gateway/settingsdata/JpaSystemSettingRepositoryTest.java`（Mockito 单测风格，对齐现有 JpaModelRepositoryTest）：

```java
@ExtendWith(MockitoExtension.class)
class JpaSystemSettingRepositoryTest {

    @Mock
    private SystemSettingJpaRepository jpaRepository;

    private JpaSystemSettingRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaSystemSettingRepository(jpaRepository);
    }

    @Test
    @DisplayName("保存并往返查询配置")
    void save_roundTrip_mapsAllFields() {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey("audit.retention.days");
        setting.setSettingValue("90");
        setting.setGroupName("AUDIT");
        setting.setValueType("NUMBER");
        setting.setEditable(true);

        SystemSettingDo doObj = new SystemSettingDo();
        doObj.setId(1L);
        doObj.setSettingKey("audit.retention.days");
        doObj.setSettingValue("90");
        doObj.setGroupName("AUDIT");
        doObj.setValueType("NUMBER");
        doObj.setEditable(true);
        when(jpaRepository.save(any())).thenReturn(doObj);

        SystemSetting saved = repository.save(setting);

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getSettingKey()).isEqualTo("audit.retention.days");
        assertThat(saved.getSettingValue()).isEqualTo("90");
        assertThat(saved.getGroupName()).isEqualTo("AUDIT");
        assertThat(saved.getValueType()).isEqualTo("NUMBER");
        assertThat(saved.isEditable()).isTrue();
    }

    @Test
    @DisplayName("findByKey 命中与未命中")
    void findByKey_hitAndMiss() {
        SystemSettingDo doObj = new SystemSettingDo();
        doObj.setSettingKey("audit.retention.days");
        doObj.setSettingValue("90");
        when(jpaRepository.findBySettingKey("audit.retention.days")).thenReturn(Optional.of(doObj));
        when(jpaRepository.findBySettingKey("missing")).thenReturn(Optional.empty());

        assertThat(repository.findByKey("audit.retention.days")).isPresent();
        assertThat(repository.findByKey("audit.retention.days").get().getSettingValue()).isEqualTo("90");
        assertThat(repository.findByKey("missing")).isEmpty();
    }
}
```

- [ ] **Step 4: 运行测试确认失败**

Run: `./mvnw test -pl gateway-settings/settings-data -Dtest=JpaSystemSettingRepositoryTest`（需先 `-am` 或 install 依赖）
Expected: 编译失败（模块/类不存在）。

- [ ] **Step 5: 实现实体、DO、Repository**

`SystemSetting.java`（settings 核心，继承 BaseEntity，@Data @EqualsAndHashCode(callSuper=true)，中文 Javadoc）：
- `settingKey`、`settingValue`、`groupName`、`description`、`valueType`、`editable`（boolean）

`SystemSettingRepository.java`（settings 核心接口）：
```java
public interface SystemSettingRepository {
    Optional<SystemSetting> findByKey(String key);
    List<SystemSetting> findAll();
    SystemSetting save(SystemSetting setting);
}
```

`SystemSettingDo.java`（settings-data，继承 BaseDo，@Entity @Table(name="system_settings")）：列映射 setting_key/setting_value/group_name/description/value_type/is_editable。

`SystemSettingJpaRepository.java`（settings-data）：
```java
public interface SystemSettingJpaRepository extends JpaRepository<SystemSettingDo, Long> {
    Optional<SystemSettingDo> findBySettingKey(String settingKey);
}
```

`JpaSystemSettingRepository.java`（settings-data，@Component）：toEntity/toDo 双向转换（参照 JpaModelRepository 模式，含审计字段 createdAt/updatedAt 双向拷贝）。

- [ ] **Step 6: 运行测试确认通过**

Run: `./mvnw test -pl gateway-settings/settings-data`（必要时 `-am`）
Expected: PASS。

- [ ] **Step 7: 提交**

```bash
git add pom.xml gateway-settings/ gateway-boot/src/main/resources/db/migration/V70__create_system_settings.sql
git commit -m "feat(settings): 新建 gateway-settings 域骨架（3 模块 + system_settings 表 + 实体/Repository）
Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: SystemSettingService + 种子加载 + 自动装配

**Files:**
- Create: `gateway-settings/settings/src/main/java/com/codingas/gateway/settings/SystemSettingService.java`
- Create: `gateway-settings/settings/src/main/java/com/codingas/gateway/settings/SystemSettingServiceImpl.java`
- Create: `gateway-settings/settings-starter/src/main/java/com/codingas/gateway/autoconfigure/settings/SettingsAutoConfiguration.java`
- Create: `gateway-settings/settings-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `gateway-settings/settings-starter/src/main/java/com/codingas/gateway/autoconfigure/settings/SettingsDefaultDataInitializer.java`
- Modify: `gateway-boot/pom.xml`（依赖 gateway-settings-starter）
- Test: `gateway-settings/settings/src/test/java/com/codingas/gateway/settings/SystemSettingServiceImplTest.java`

**Interfaces:**
- Consumes: Task 1 的 `SystemSettingRepository`。
- Produces:
  - `SystemSettingService` 接口：`Optional<SystemSetting> getSetting(String key)`、`String get(String key)`、`int getInt(String key, int defaultValue)`、`boolean getBoolean(String key, boolean defaultValue)`、`<E extends Enum<E>> E getEnum(String key, Class<E> enumType, E defaultValue)`、`List<SystemSetting> getAll()`、`SystemSetting update(String key, String value)`
  - `SettingsAutoConfiguration`（@AutoConfiguration + @ComponentScan 扫 settings/settingsdata 包）+ imports 文件
  - `SettingsDefaultDataInitializer`（ApplicationRunner：表空时插入默认配置项）
  - `SystemSettingService.update` 校验：key 不存在抛 IllegalArgumentException；!editable 抛 IllegalArgumentException；value_type 校验（NUMBER 数字 / BOOLEAN true|false / ENUM 需在枚举候选内；ENUM 候选通过 update 的枚举类参数或 value_type 内嵌，本期简化：ENUM 校验非空即可，具体枚举校验由调用方 getEnum 容错）

- [ ] **Step 1: 写失败测试**

`SystemSettingServiceImplTest.java`（Mockito）：

```java
@ExtendWith(MockitoExtension.class)
class SystemSettingServiceImplTest {

    @Mock private SystemSettingRepository repository;
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
```

`SyncInterval` 枚举（Task 5 将使用，定义在本任务 settings 核心）：
```java
public enum SyncInterval {
    DAILY, WEEKLY, MONTHLY
}
```
放 `gateway-settings/settings/src/main/java/com/codingas/gateway/settings/SyncInterval.java`（中文 Javadoc：DAILY=每天、WEEKLY=每周、MONTHLY=每月）。

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -pl gateway-settings/settings -Dtest=SystemSettingServiceImplTest`
Expected: 编译失败（类不存在）。

- [ ] **Step 3: 实现 Service + 枚举 + 自动装配 + 种子**

`SystemSettingServiceImpl`（@Service）：
- getSetting/get/getInt/getBoolean/getEnum 委托 repository.findByKey，getInt 用 Integer.parseInt 容错（parse 失败回退默认值），getBoolean 用 Boolean.parseBoolean，getEnum 用 Enum.valueOf 容错
- update：findByKey 不存在 → IllegalArgumentException("配置项不存在: " + key)；!isEditable → IllegalArgumentException("配置项不可编辑: " + key)；valueType 校验（NUMBER 需 Integer.parseInt 成功、BOOLEAN 需 "true"/"false"、ENUM 非空）→ 设置值 + save + 返回

`SettingsAutoConfiguration.java`：
```java
@AutoConfiguration
@ComponentScan(basePackages = {"com.codingas.gateway.settings", "com.codingas.gateway.settingsdata"})
public class SettingsAutoConfiguration {
}
```

`org.springframework.boot.autoconfigure.AutoConfiguration.imports` 内容：
```
com.codingas.gateway.autoconfigure.settings.SettingsAutoConfiguration
```

`SettingsDefaultDataInitializer`（@Component implements ApplicationRunner，@Order 低）：repository.findAll() 为空时插入 3 个默认配置项（audit.retention.days=90/NUMBER/AUDIT、catalog.sync.enabled=true/BOOLEAN/CATALOG、catalog.sync.interval=DAILY/ENUM/CATALOG），含中文 description。

`gateway-boot/pom.xml` 加 `gateway-settings-starter` 依赖（对照 gateway-alert-starter 的依赖块）。

- [ ] **Step 4: 运行测试确认通过 + boot 上下文**

Run: `./mvnw test -pl gateway-settings/settings -Dtest=SystemSettingServiceImplTest`
Expected: PASS。
Run: `./mvnw install -pl gateway-settings -DskipTests -q` 后 `./mvnw test -pl gateway-boot -Dtest=FullContextIntegrationTest`（确认新 starter 装配不破坏上下文）。
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add gateway-settings/ gateway-boot/pom.xml
git commit -m "feat(settings): 配置服务 + 默认配置项种子 + 自动装配
Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: SettingsController（web）

**Files:**
- Create: `gateway-web/src/main/java/com/codingas/gateway/web/api/SettingsController.java`
- Create: `gateway-web/src/main/java/com/codingas/gateway/web/api/dto/SystemSettingResponse.java`
- Create: `gateway-web/src/main/java/com/codingas/gateway/web/api/dto/SettingUpdateRequest.java`
- Test: `gateway-web/src/test/java/com/codingas/gateway/web/api/SettingsControllerTest.java`

**Interfaces:**
- Consumes: Task 2 的 `SystemSettingService`。
- Produces:
  - `GET /api/v1/settings` → `List<SystemSettingResponse>`（settingKey/settingValue/groupName/description/valueType/editable）
  - `PUT /api/v1/settings/{key}`（body `{value}`）→ `SystemSettingResponse`；参数非法返回 400

- [ ] **Step 1: 写失败测试**

`SettingsControllerTest.java`（standalone MockMvc + setControllerAdvice(ApiResponseWrapperAdvice, GlobalExceptionHandler)，对齐 CatalogSyncControllerTest）：

```java
class SettingsControllerTest {
    private MockMvc mockMvc;
    @Mock private SystemSettingService settingService;

    @BeforeEach
    void setUp() {
        SettingsController controller = new SettingsController(settingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiResponseWrapperAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/settings 返回全部配置")
    void getAll_returnsSettings() throws Exception {
        SystemSetting s = new SystemSetting();
        s.setSettingKey("audit.retention.days");
        s.setSettingValue("90");
        s.setGroupName("AUDIT");
        s.setValueType("NUMBER");
        s.setEditable(true);
        when(settingService.getAll()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/v1/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].settingKey").value("audit.retention.days"))
                .andExpect(jsonPath("$.data[0].settingValue").value("90"));
    }

    @Test
    @DisplayName("PUT /api/v1/settings/{key} 更新配置")
    void update_returnsUpdated() throws Exception {
        SystemSetting s = new SystemSetting();
        s.setSettingKey("audit.retention.days");
        s.setSettingValue("120");
        s.setGroupName("AUDIT");
        s.setValueType("NUMBER");
        s.setEditable(true);
        when(settingService.update("audit.retention.days", "120")).thenReturn(s);

        mockMvc.perform(put("/api/v1/settings/audit.retention.days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"120\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingValue").value("120"));
    }

    @Test
    @DisplayName("PUT 非法参数返回 400")
    void update_invalidValue_badRequest() throws Exception {
        when(settingService.update(anyString(), anyString())).thenThrow(new IllegalArgumentException("配置项不存在"));
        mockMvc.perform(put("/api/v1/settings/nope").contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"1\"}"))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -pl gateway-web -Dtest=SettingsControllerTest`
Expected: 编译失败。

- [ ] **Step 3: 实现 Controller + DTO**

`SettingsController`（@RestController @RequestMapping("/api/v1/settings")）：GET 返回 from 列表；PUT 调 service.update，IllegalArgumentException 由 GlobalExceptionHandler 映射 400。

`SystemSettingResponse`（@Data + from(SystemSetting)）、`SettingUpdateRequest`（@Data + @NotNull String value）。

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -pl gateway-web -Dtest=SettingsControllerTest`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add gateway-web/src/main/java/com/codingas/gateway/web/api/SettingsController.java \
        gateway-web/src/main/java/com/codingas/gateway/web/api/dto/SystemSettingResponse.java \
        gateway-web/src/main/java/com/codingas/gateway/web/api/dto/SettingUpdateRequest.java \
        gateway-web/src/test/java/com/codingas/gateway/web/api/SettingsControllerTest.java
git commit -m "feat(web): 系统设置管理接口（GET 全部 + PUT 更新配置）
Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: 审计日志清理（deleteBefore + 定时任务 + 手动清理端点）

**Files:**
- Modify: `gateway-audit/audit/src/main/java/com/codingas/gateway/audit/AuditLogRepository.java`（加 deleteBefore）
- Modify: `gateway-audit/audit-data/src/main/java/com/codingas/gateway/auditdata/auditlog/AuditLogJpaRepository.java`（加 @Modifying delete）
- Modify: `gateway-audit/audit-data/src/main/java/com/codingas/gateway/auditdata/auditlog/JpaAuditLogRepository.java`（实现 deleteBefore）
- Create: `gateway-audit/audit/src/main/java/com/codingas/gateway/audit/task/AuditCleanupTask.java`
- Modify: `gateway-audit/audit/pom.xml`（加 gateway-settings 依赖）
- Modify: `gateway-web/src/main/java/com/codingas/gateway/web/api/AuditController.java`（加 DELETE 清理端点）
- Test: `gateway-audit/audit/src/test/java/com/codingas/gateway/audit/task/AuditCleanupTaskTest.java`
- Test: `gateway-web/src/test/java/com/codingas/gateway/web/api/AuditControllerTest.java`（追加清理端点用例）

**Interfaces:**
- Consumes: Task 2 的 `SystemSettingService.getInt`；现有 `AuditLogRepository`。
- Produces:
  - `AuditLogRepository.deleteBefore(Instant cutoff)` → int 删除条数
  - `AuditCleanupTask`（@Component + @Scheduled(cron 每日凌晨 3:00)）：读 `audit.retention.days`（默认 90）→ deleteBefore(now - days) → log 清理结果
  - `DELETE /api/v1/audit-logs?days=N`（手动清理，N 天前）或 `?before=ISO时间` → 返回 {deleted: N}

- [ ] **Step 1: 写失败测试**

`AuditCleanupTaskTest.java`（Mockito）：

```java
@ExtendWith(MockitoExtension.class)
class AuditCleanupTaskTest {
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private SystemSettingService settingService;
    private AuditCleanupTask task;

    @BeforeEach
    void setUp() {
        task = new AuditCleanupTask(auditLogRepository, settingService);
    }

    @Test
    @DisplayName("每日清理：按保留天数删除截止时间前的审计日志")
    void cleanup_deletesBeforeCutoff() {
        when(settingService.getInt("audit.retention.days", 90)).thenReturn(90);
        when(auditLogRepository.deleteBefore(any(Instant.class))).thenReturn(42);

        task.cleanup();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(auditLogRepository).deleteBefore(captor.capture());
        assertThat(captor.getValue()).isBefore(Instant.now());
        assertThat(captor.getValue()).isAfter(Instant.now().minus(91, ChronoUnit.DAYS));
    }
}
```

`AuditControllerTest` 追加：
```java
@Test
@DisplayName("DELETE /api/v1/audit-logs?days=30 手动清理")
void deleteAuditLogs_returnsDeletedCount() throws Exception {
    when(auditService.deleteBefore(any())).thenReturn(10);
    mockMvc.perform(delete("/api/v1/audit-logs").param("days", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.deleted").value(10));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -pl gateway-audit/audit -Dtest=AuditCleanupTaskTest`
Expected: 编译失败。

- [ ] **Step 3: 实现清理能力**

`AuditLogRepository.deleteBefore(Instant cutoff)` 接口方法（中文 Javadoc）；`AuditLogJpaRepository` 加：
```java
@Modifying
@Query("DELETE FROM AuditLogDo a WHERE a.createdAt < :cutoff")
int deleteBefore(@Param("cutoff") Instant cutoff);
```
（先确认 AuditLogDo 的 createdAt 字段名与类型；若 AuditLogDo 无 createdAt，用 created_at 的原生 SQL `DELETE FROM audit_logs WHERE created_at < :cutoff`）

`JpaAuditLogRepository.deleteBefore` 委托。

`AuditCleanupTask`（audit 模块）：
```java
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditCleanupTask {
    private final AuditLogRepository auditLogRepository;
    private final SystemSettingService settingService;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup() {
        int retentionDays = settingService.getInt("audit.retention.days", 90);
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = auditLogRepository.deleteBefore(cutoff);
        log.info("审计日志定时清理完成: 保留 {} 天, 删除 {} 条", retentionDays, deleted);
    }
}
```

`gateway-audit/audit/pom.xml` 加 gateway-settings 依赖。

`AuditController` 加 `DELETE /api/v1/audit-logs`（@RequestParam(required=false) Integer days 或 @RequestParam(required=false) Instant before → 计算 cutoff → 调 AuditService 或直接 repository.deleteBefore → 返回 `{deleted}`）。需在 AuditService 加 `int deleteBefore(Instant cutoff)` 委托（或 Controller 直接注入 repository——按项目模式，Controller 走 Service）。

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -pl gateway-audit/audit -Dtest=AuditCleanupTaskTest` 与 `./mvnw test -pl gateway-web -Dtest=AuditControllerTest`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add gateway-audit/ gateway-web/src/main/java/com/codingas/gateway/web/api/AuditController.java gateway-web/src/test/java/com/codingas/gateway/web/api/AuditControllerTest.java
git commit -m "feat(audit): 审计日志清理（保留天数配置 + 每日定时 + 手动清理端点）
Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 5: models.dev 同步自动执行（CatalogSyncTask）

**Files:**
- Create: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/catalog/sync/CatalogSyncTask.java`
- Modify: `gateway-provider/provider/pom.xml`（加 gateway-settings 依赖）
- Test: `gateway-provider/provider/src/test/java/com/codingas/gateway/provider/catalog/sync/CatalogSyncTaskTest.java`

**Interfaces:**
- Consumes: Task 2 的 `SystemSettingService`（getBoolean/getEnum）与 `SyncInterval` 枚举；现有 `ModelCatalogSyncService.sync()` 与 `CatalogSyncLogRepository.findLatest()`。
- Produces:
  - `CatalogSyncTask`（@Component + @Scheduled 每小时检查）：读 `catalog.sync.enabled`（默认 true，false 跳过）→ 读 `catalog.sync.interval`（DAILY=24h/WEEKLY=7d/MONTHLY=30d）→ 读 `findLatest()` 的 syncedAt（无记录视为需要同步）→ 距上次同步 ≥ 间隔 → 调 `sync()`；否则跳过（debug 日志）

- [ ] **Step 1: 写失败测试**

`CatalogSyncTaskTest.java`（Mockito）：

```java
@ExtendWith(MockitoExtension.class)
class CatalogSyncTaskTest {
    @Mock private ModelCatalogSyncService syncService;
    @Mock private CatalogSyncLogRepository logRepository;
    @Mock private SystemSettingService settingService;
    private CatalogSyncTask task;

    @BeforeEach
    void setUp() {
        task = new CatalogSyncTask(syncService, logRepository, settingService);
    }

    @Test
    @DisplayName("开关关闭时跳过同步")
    void check_syncDisabled_skips() {
        when(settingService.getBoolean("catalog.sync.enabled", true)).thenReturn(false);
        task.check();
        verify(syncService, never()).sync();
    }

    @Test
    @DisplayName("达到间隔时触发同步")
    void check_intervalElapsed_triggersSync() {
        when(settingService.getBoolean("catalog.sync.enabled", true)).thenReturn(true);
        when(settingService.getEnum("catalog.sync.interval", SyncInterval.class, SyncInterval.DAILY))
                .thenReturn(SyncInterval.DAILY);
        CatalogSyncLog log = new CatalogSyncLog();
        log.setSyncedAt(Instant.now().minus(25, ChronoUnit.HOURS));
        when(logRepository.findLatest()).thenReturn(Optional.of(log));
        when(syncService.sync()).thenReturn(CatalogSyncReport.builder().success(true).build());

        task.check();

        verify(syncService).sync();
    }

    @Test
    @DisplayName("未达到间隔时跳过同步")
    void check_intervalNotElapsed_skips() {
        when(settingService.getBoolean("catalog.sync.enabled", true)).thenReturn(true);
        when(settingService.getEnum("catalog.sync.interval", SyncInterval.class, SyncInterval.DAILY))
                .thenReturn(SyncInterval.DAILY);
        CatalogSyncLog log = new CatalogSyncLog();
        log.setSyncedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(logRepository.findLatest()).thenReturn(Optional.of(log));

        task.check();

        verify(syncService, never()).sync();
    }

    @Test
    @DisplayName("无历史同步记录时触发首次同步")
    void check_noHistory_triggers() {
        when(settingService.getBoolean("catalog.sync.enabled", true)).thenReturn(true);
        when(settingService.getEnum("catalog.sync.interval", SyncInterval.class, SyncInterval.DAILY))
                .thenReturn(SyncInterval.DAILY);
        when(logRepository.findLatest()).thenReturn(Optional.empty());
        when(syncService.sync()).thenReturn(CatalogSyncReport.builder().success(true).build());

        task.check();

        verify(syncService).sync();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -pl gateway-provider/provider -Dtest=CatalogSyncTaskTest`
Expected: 编译失败。

- [ ] **Step 3: 实现任务**

`CatalogSyncTask`（provider 模块）：

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogSyncTask {

    private final ModelCatalogSyncService syncService;
    private final CatalogSyncLogRepository logRepository;
    private final SystemSettingService settingService;

    @Scheduled(fixedRate = 3600_000) // 每小时检查一次
    public void check() {
        if (!settingService.getBoolean("catalog.sync.enabled", true)) {
            log.debug("模型目录自动同步已关闭，跳过");
            return;
        }
        SyncInterval interval = settingService.getEnum(
                "catalog.sync.interval", SyncInterval.class, SyncInterval.DAILY);
        long thresholdHours = switch (interval) {
            case DAILY -> 24;
            case WEEKLY -> 24 * 7;
            case MONTHLY -> 24 * 30;
        };
        Optional<CatalogSyncLog> latest = logRepository.findLatest();
        boolean shouldSync = latest.isEmpty()
                || latest.get().getSyncedAt() == null
                || latest.get().getSyncedAt().isBefore(Instant.now().minus(thresholdHours, ChronoUnit.HOURS));
        if (!shouldSync) {
            log.debug("距上次同步未达间隔({}), 跳过", interval);
            return;
        }
        try {
            CatalogSyncReport report = syncService.sync();
            log.info("自动同步完成: added={}, updated={}", report.getAddedCount(), report.getUpdatedCount());
        } catch (RuntimeException e) {
            log.error("自动同步失败: {}", e.getMessage(), e);
        }
    }
}
```

`gateway-provider/provider/pom.xml` 加 gateway-settings 依赖。

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -pl gateway-provider/provider -Dtest=CatalogSyncTaskTest`
Expected: PASS。回归 `./mvnw test -pl gateway-provider/provider`（全绿）。

- [ ] **Step 5: 提交**

```bash
git add gateway-provider/provider/src/main/java/com/codingas/gateway/provider/catalog/sync/CatalogSyncTask.java \
        gateway-provider/provider/src/test/java/com/codingas/gateway/provider/catalog/sync/CatalogSyncTaskTest.java \
        gateway-provider/provider/pom.xml
git commit -m "feat(sync): models.dev 同步自动执行任务（开关 + 间隔 DAILY/WEEKLY/MONTHLY）
Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 6: 前端系统设置页

**Files:**
- Modify: `gateway-console/src/constants/menuConfig.tsx`（加"系统设置"菜单项）
- Modify: `gateway-console/src/router/index.tsx`（注册 /settings 路由）
- Create: `gateway-console/src/pages/Settings/index.tsx`
- Create: `gateway-console/src/types/settings.ts`
- Create: `gateway-console/src/services/api/settings.ts`
- Create: `gateway-console/src/services/query/useSettings.ts`

**Interfaces:**
- Consumes: Task 2/3 的 `GET/PUT /api/v1/settings`、Task 4 的 `DELETE /api/v1/audit-logs?days=N`、现有 `POST /api/v1/catalog/sync` 与 `GET /api/v1/catalog/sync/status`（catalogSync.ts 已有）。
- Produces: 设置页（审计分组 + 模型目录分组）。

- [ ] **Step 1: 菜单 + 路由**

`menuConfig.tsx` operations 分组追加（保留 AuditLogs 项）：
```tsx
{
  key: '/settings',
  icon: <SettingOutlined />,
  label: 'menu.settings',
  permission: 'settings:read',
},
```
（SettingOutlined 加 import；权限 `settings:read` 需在 `constants/permissions.ts` 确认存在或新增。）

`router/index.tsx` 注册 `/settings` → Settings 页（对齐现有懒加载路由模式）。

- [ ] **Step 2: 类型 + API + hook**

`types/settings.ts`：
```ts
export interface SystemSetting {
  settingKey: string;
  settingValue?: string;
  groupName?: string;
  description?: string;
  valueType?: string;
  editable?: boolean;
}
```

`services/api/settings.ts`（对齐 catalogSync.ts 的 api 封装）：
```ts
export function getSettings() { return api.get('/api/v1/settings'); }
export function updateSetting(key: string, value: string) { return api.put(`/api/v1/settings/${key}`, { value }); }
export function cleanupAuditLogs(days: number) { return api.delete('/api/v1/audit-logs', { params: { days } }); }
```

`services/query/useSettings.ts`：`useSettings()`（useQuery，key `['settings']`）+ `useUpdateSetting()`（useMutation，成功 invalidate `['settings']` + `['catalog-sync']` 等）。

- [ ] **Step 3: 设置页**

`pages/Settings/index.tsx`（对齐现有页面结构：useTranslation + Card + Form）：
- 审计日志分组 Card：保留天数（InputNumber，默认 90，保存调 updateSetting('audit.retention.days', String(v))）+ "立即清理"按钮（Popconfirm 确认后调 cleanupAuditLogs(天数)，message.success('已清理 N 条')，展示返回数）
- 模型目录分组 Card：自动同步开关（Switch，绑定 catalog.sync.enabled）+ 同步间隔（Select：每天 DAILY/每周 WEEKLY/每月 MONTHLY，绑定 catalog.sync.interval）+ "立即同步"按钮（调 useCatalogSync 的 syncModelCatalog，复用现有）+ 最近同步状态（复用 useCatalogSyncStatus 显示时间/结果）
- 保存逻辑：修改后立即调 updateSetting（或统一"保存"按钮），成功后 invalidate 刷新

- [ ] **Step 4: 前端验证**

Run: `cd gateway-console && npx tsc -b`（无类型错误）与 `npm run build`（构建通过）。

- [ ] **Step 5: 提交**

```bash
git add gateway-console/src/constants/menuConfig.tsx gateway-console/src/router/index.tsx \
        gateway-console/src/pages/Settings/ gateway-console/src/types/settings.ts \
        gateway-console/src/services/api/settings.ts gateway-console/src/services/query/useSettings.ts
git commit -m "feat(console): 系统设置页（审计保留天数/清理 + 模型目录同步开关/间隔）
Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 7: 全流程集成验证

**Files:** 无源码改动（验证任务）

- [ ] **Step 1: 全量构建**

Run: `./mvnw clean install`
Expected: BUILD SUCCESS（全模块测试绿）。

- [ ] **Step 2: 配置服务验证**

启动应用或集成测试确认：
- `GET /api/v1/settings` 返回 3 个默认配置项（audit.retention.days=90、catalog.sync.enabled=true、catalog.sync.interval=DAILY）
- `PUT /api/v1/settings/audit.retention.days` body `{"value":"120"}` → 返回 120；`GET` 确认已更新（动态生效）
- `PUT` 非法值（如 `abc`）→ 400

- [ ] **Step 3: 审计清理验证**

- `DELETE /api/v1/audit-logs?days=0`（清 0 天前=全部）→ 返回删除条数（有数据则 >0）
- 手动清理后 `GET /api/v1/audit-logs` 变空或减少

- [ ] **Step 4: 同步自动执行验证**

- 配置 `catalog.sync.enabled=false` → 确认 CatalogSyncTask 跳过（日志 debug）
- 配置 `catalog.sync.interval=WEEKLY` → 距上次同步未达 7 天 → 跳过
- （自动执行触发路径已由单测覆盖；真实触发依赖系统时间，不强制等待）

- [ ] **Step 5: 前端验证**

- 设置页显示 3 个配置项；修改保留天数保存成功；立即清理按钮执行
- `npx tsc -b` 无错误

- [ ] **Step 6: 提交（若有修复项）**

如有修复，按任务各自提交；无修复则验证通过即完成。

---

## Self-Review 结论

- **Spec 覆盖**：设计 §3（表/实体）→ Task 1；§4（服务/API）→ Task 2+3；§5（审计清理）→ Task 4；§6（同步自动执行）→ Task 5；§7（前端）→ Task 6；§8（测试）→ 各任务 TDD；§9（风险）→ Task 4/5 实现；§11 里程碑 → Task 1-7。
- **占位符扫描**：无 TBD/TODO。Task 4 中 AuditLogDo 的 createdAt 字段名需 implementer 读文件确认（已注明两种实现路径）。
- **类型一致性**：`SyncInterval`（DAILY/WEEKLY/MONTHLY）在 Task 2/5/6 一致；`SystemSettingService` 方法签名（getInt/getBoolean/getEnum/getAll/update）在 Task 2/3/4/5 一致；`AuditLogRepository.deleteBefore(Instant)` 在 Task 4 一致。
- **已知待执行者确认点**：settings 模块 pom 参照 gateway-alert 具体内容；AuditLogDo 的 createdAt 列名；前端权限 `settings:read` 在 permissions.ts 的登记；router 懒加载模式。
