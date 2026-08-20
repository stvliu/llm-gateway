# Catalog Cascade Materialize
## Summary

## Purpose

定义 Catalog 供应商级联物化的行为契约：Platform Admin 物化一个供应商时，系统按指定范围（全部或过滤列表）事务性级联物化其下的 Plans/Endpoints/Models，保证部分失败整体回滚，并为前端提供一致的批量结果反馈。

## Requirements

### Requirement: Provider cascade materialization

Platform Admin 物化一个供应商时，系统 SHALL 级联物化该 Provider 下所有处于 `ACTIVE` 状态的 Plans（含 Endpoints + Models），除非用户指定了 Plans 过滤列表。

#### Scenario: Cascade all plans when no filter specified
- **WHEN** Platform Admin 调用 `POST /api/v1/catalog/materialize/provider/{code}/with-plans` 且请求体无 `planCodes` 字段
- **THEN** 系统 SHALL 查询该 Provider 下所有 `ACTIVE` 状态的 PlanCatalog，逐条执行物化
- **THEN** 系统 SHALL 返回 `MaterializeBatchResult`，包含每条计划的结果（code + status + entityId）

#### Scenario: Cascade only specified plans
- **WHEN** Platform Admin 调用 `POST /api/v1/catalog/materialize/provider/{code}/with-plans` 且请求体包含 `planCodes: ["deepseek_std", "deepseek_pro"]`
- **THEN** 系统 SHALL 仅物化列表中存在的 Plan
- **THEN** 系统 SHALL 自动跳过列表中不存在的 planCode

#### Scenario: All plans already materialized
- **WHEN** 该 Provider 所有 Plans 已被逐一物化，Admin 再次调用级联物化
- **THEN** 系统 SHALL 对每个已物化 Plan 返回 `status: "SKIPPED"`（非错误）
- **THEN** 系统 SHALL 返回 HTTP 200，整体操作视为成功

#### Scenario: Existing provider but no plans
- **WHEN** 某个 Provider 在 Catalog 中 `plan_catalogs` 表里无关联 Plans
- **THEN** 系统 SHALL 仅创建 Provider 实体
- **THEN** 系统 SHALL 返回空的 `results` 列表

### Requirement: Cascade consistency

级联物化 SHALL 是事务性的：Provider 创建和所有 Plan 物化在同一事务中，任何 Plan 物化失败则整体回滚。

#### Scenario: Partial plan failure triggers rollback
- **WHEN** 级联物化 3 个 Plans，第 3 个因内部错误（如 JSON 解析异常）失败
- **THEN** 系统 SHALL 回滚整个事务
- **THEN** Provider 实体 SHALL NOT 被创建
- **THEN** 已成功物化的 Plan SHALL NOT 残留运营实体

### Requirement: Frontend cascade confirmation dialog

ProviderCatalogView 中点击「物化」按钮时，前端 SHALL 弹出确认弹窗，展示将自动创建的 Plans 清单，允许用户选择性取消。

#### Scenario: Show plan preview in dialog
- **WHEN** Admin 在 ProviderCatalogView 点击供应商卡片的「物化」按钮
- **THEN** 前端 SHALL 向后端查询该 Provider 的可用 Plans 列表
- **THEN** 弹窗 SHALL 展示 Provider 名称、关联 Plan 数量、每条 Plan 的名称/编码/计费模式
- **THEN** 弹窗 SHALL 包含「确认物化」和「取消」按钮

#### Scenario: User deselects plans before confirming
- **WHEN** 弹窗中取消勾选部分 Plans 并点击「确认物化」
- **THEN** 前端 SHALL 调用 `POST /api/v1/catalog/materialize/provider/{code}/with-plans` 并传入包含已勾选 Plans 的 `planCodes`
- **THEN** 物化完成后，弹窗 SHALL 关闭，页面刷新状态

#### Scenario: User cancels dialog
- **WHEN** Admin 在确认弹窗点击「取消」
- **THEN** 弹窗关闭，不执行任何物化操作
- **THEN** 页面状态不变

### Requirement: Provider materialization endpoint backward compatibility

已有 `POST /api/v1/catalog/materialize/provider/{code}` 端点 SHALL 保持不变，仅创建 Provider 实体，不级联 Plans。

#### Scenario: Legacy materialize still works
- **WHEN** Admin 调用快路径 `POST /api/v1/catalog/materialize/provider/{code}`
- **THEN** 系统行为与变更前一致：仅创建 Provider 实体
- **THEN** 关联 Plans 不会被物化

### Requirement: Cascade materialize result structure

`MaterializeBatchResult` SHALL 包含批量操作的整体状态，以及每条 Plan 的独立物化结果。

#### Scenario: Successful materialize with mixed results
- **WHEN** 级联物化 5 个 Plans，其中 3 个成功创建、1 个已存在（SKIPPED）、1 个失败
- **THEN** 整体 HTTP 状态码 SHALL 为 200
- **THEN** `MaterializeBatchResult.results` SHALL 包含各 Plan 的独立 status
- **THEN** `MaterializeBatchResult.failedCount` SHALL = 1