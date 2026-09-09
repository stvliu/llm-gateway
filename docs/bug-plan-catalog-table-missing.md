# Bug 单:PlanCatalog 表已删除但全链路代码仍在运行路径

- **编号**:BUG-2026-0909-01
- **提出日期**:2026-09-09
- **发现来源**:架构文档全量复核(对齐 commit 11b6a155)
- **严重度**:高(PostgreSQL 生产环境无法启动)
- **类型**:代码-库漂移(迁移删表未同步清理代码)

## 缺陷描述

`V47__supply_domain_merge.sql:133-136` 已删除 `plan_catalogs`、`plan_model_catalogs`(连同 `model_catalogs`、`provider_catalogs`)共 4 张表,后续迁移无重建;但 PlanCatalog 全链路代码未清理,仍处于运行路径:

```
ChannelProvisionController (gateway-web)          ── 活跃 ADMIN API
  └─ ChannelProvisionService (provider/channel)   ── 活跃:套餐开通渠道流程
       └─ PlanCatalogRepository (provider/catalog 端口)
            └─ JpaPlanCatalogRepository (providerdata/catalog)
                 └─ PlanCatalogDo @Table("plan_catalogs")   ── 表已不存在
BuiltinDataLoader.loadPlans() (provider)          ── 启动时 upsert plan_catalogs
PlanCatalogController (/api/v1/plan-catalogs)     ── 活跃管理 API
PlanCatalogService/Impl、PlanModelCatalog、PlanCatalogResponse
PlanModelCatalogDo / JpaPlanModelCatalogRepository / PlanModelCatalogJpaRepository
```

## 影响(按环境分化)

| 环境 | ddl-auto | 表现 |
|------|----------|------|
| H2(local/dev,应用默认) | `update`(application.yml:49) | Hibernate 按 PlanCatalogDo **重建**两张表,启动"正常",BuiltinDataLoader 写入数据,provision API 表面可用——**缺陷被掩盖** |
| PostgreSQL(生产 profile) | `validate`(application-postgresql.yml:39) | schema 校验发现实体对应表不存在,**启动即失败** |

## 证据链

- `gateway-boot/src/main/resources/db/migration/V47__supply_domain_merge.sql:133-136`:`DROP TABLE IF EXISTS plan_model_catalogs / plan_catalogs / model_catalogs / provider_catalogs;`,V48~V70 无重建
- 实体被显式扫描:`gateway-boot/.../boot/GatewayApplication.java:52` `@EntityScan(basePackages = {...})` 含 providerdata
- provision 链路活跃:`gateway-web/.../web/api/ChannelProvisionController.java` → `ChannelProvisionService.java:55,106,213`(注入并查询 `PlanCatalogRepository`)
- 启动加载器活跃:`BuiltinDataLoader.java:176` 加载生产 classpath `catalog/plans.json`(文件存在于 `gateway-boot/src/main/resources/catalog/`)逐条 upsert
- 管理端点存在:`PlanCatalogController.java:35` `@RequestMapping("/api/v1/plan-catalogs")`
- 目录同步新口径:`CatalogSyncTask`/`CatalogProbeTask` 已落 `models` 表 + `catalog_sync_logs`(V69),不经过 PlanCatalog

## 修复方案(需决策,二选一或分步)

### 方案 A:恢复建表(最小止血)
新增 `V71__recreate_plan_catalogs.sql` 按 PlanCatalogDo/PlanModelCatalogDo 字段重建两张表。
- 优点:改动最小,provision 流程、BuiltinDataLoader、PlanCatalog API 全部恢复
- 适用前提:**套餐目录(PlanCatalog)作为 provision 输入载体仍是产品功能**
- 注意:需评估 V47 之前的历史数据是否需要迁移回填(原表数据已丢,只能从 `catalog/plans.json` 重载)

### 方案 B:彻底清理(与 V47 意图一致)
删除 PlanCatalog 全链路(约 11 个生产文件 + 6 个测试文件,见上清单与下测试清单);provision 流程改造为不依赖 PlanCatalog(以请求体/JSON 直输),或随产品决策废弃 PlanCatalogController/Provision API。
- 优点:消除僵尸概念,供给域彻底收敛到 Channel/ModelInstance 口径
- 适用前提:**V47 删表即代表套餐目录概念废弃**,provision 流程改造成本可接受

### 建议
若 PostgreSQL 生产启动是近期风险,可先执行方案 A 止血,再以独立 change 推进方案 B;若已确定废弃套餐目录概念,直接执行方案 B 并在 provision 改造时补集成测试。

## 测试文件清单(清理/改造时同步处理)

- `gateway-provider/provider/src/test/.../catalog/PlanCatalogServiceImplTest.java`
- `gateway-provider/provider/src/test/.../catalog/PlanCatalogTest.java`
- `gateway-provider/provider/src/test/.../channel/ChannelProvisionServiceInlineProviderTest.java`
- `gateway-provider/provider/src/test/.../channel/ChannelProvisionServiceTest.java`
- `gateway-provider/provider/src/test/.../service/BuiltinDataLoaderTest.java`
- `gateway-provider/provider-data/src/test/.../catalog/JpaPlanCatalogRepositoryTest.java`
- `gateway-boot/src/test/.../integration/ChannelProvisionTransactionalIntegrationTest.java`

## 验收清单(修复后逐项验证)

1. PostgreSQL profile(`ddl-auto: validate`)应用可正常启动
2. H2 环境表数量回归 Flyway 口径 26 张(不再出现 Hibernate 重建表)
3. provision 端到端流程(按方案 A/B 各自的目标语义)有集成测试覆盖
4. `grep -r "PlanCatalog"` 仅剩方案选定的存留(方案 B 应为 0)
5. 架构文档(数据架构.md/信息架构.md)"遗留实体待清理"标注同步更新为最终状态
