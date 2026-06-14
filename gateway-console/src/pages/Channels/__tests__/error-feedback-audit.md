# Channels 页面 mutation catch 块审计清单

> 范围：`gateway-console/src/pages/Channels/` 下 4 个目标 Section 文件
> 任务来源：OpenSpec change `channel-ux-overhaul` 第 5 章 / Plan 行 822-911
> 审计日期：2026-06-14

## 严重度定义

- **高（Critical）**：空 catch 或仅注释的 catch — 静默吞掉所有错误（含 5xx），用户感知不到失败
- **中（Major）**：已有 `message.error`，但消息为固定常量，未携带后端 / 网络错误原因
- **低（Minor）**：catch 内有用户反馈且包含原因传递

## 4 个目标 Section 内 catch 块清单

### EndpointSection.tsx

| 行号 | 函数 | 当前实现 | 严重度 | 备注 |
|------|------|----------|--------|------|
| 61-63 | `handleSaveEdit` | `} catch { /* 校验失败或 API 错误 */ }` | **高** | 静默吞掉 API 5xx |
| 71-73 | `handleDelete` | `message.error(t('drawer.endpointDeleteFailed'))` | 中 | 无原因 |
| 88-90 | `handleAdd` | `} catch { /* 校验失败 */ }` | **高** | 静默吞掉 API 5xx |

### CredentialSection.tsx

| 行号 | 函数 | 当前实现 | 严重度 | 备注 |
|------|------|----------|--------|------|
| 90-92 | renderItem 内测试按钮 | `message.error(t('credential.testRequestFail'))` | 中 | 无原因；测试按钮非典型 mutation 保存，本批保留原状 |
| 124-126 | renderEditForm.handleSave | `message.error(t('credential.updateFail'))` | 中 | 无原因 |
| 192-194 | renderAddForm.handleSave | `message.error(t('credential.addFail'))` | 中 | 无原因 |
| 244-246 | `handleDelete` | `message.error(t('credential.deleteFail'))` | 中 | 无原因 |

### ModelMappingSection.tsx

| 行号 | 函数 | 当前实现 | 严重度 | 备注 |
|------|------|----------|--------|------|
| 72-74 | renderEditForm.handleSave | `message.error(t('modelMapping.updateFail'))` | 中 | 无原因 |
| 118-120 | renderAddForm.handleSave | `message.error(t('modelMapping.addFail'))` | 中 | 无原因 |
| 157-159 | `handleDelete` | `message.error(t('modelMapping.deleteFail'))` | 中 | 无原因 |

### QuotaSettingsSection.tsx

| 行号 | 函数 | 当前实现 | 严重度 | 备注 |
|------|------|----------|--------|------|
| 58-60 | `handleSave` | `message.error(t('quota.updateFail'))` | 中 | 无原因 |

## 改造策略

1. **新增工具** `src/utils/errorMessage.ts`：`extractErrorMessage(err: unknown): string`
   - 识别 AntD `Form.validateFields()` 抛出的 `{ errorFields: [...] }` —— 返回空字符串，调用方据此跳过 toast（行内提示已存在）
   - 识别 `AxiosError`：取 `response.data.message` / `response.data.error` / `response.statusText`
   - 识别原生 `Error`：取 `message`
   - 兜底：`String(err)` 或 i18n 通用文案
2. **每个 catch 块统一改造为：**
   ```ts
   } catch (err) {
     const reason = extractErrorMessage(err);
     if (!reason) return; // 表单校验失败，AntD 已就地展示
     message.error(t('common.saveFailed', { reason }));
   }
   ```
   保留各处原有更具语义的 i18n key（如 `drawer.endpointDeleteFailed`）作为兜底文案，可在 `reason` 为通用时使用 — 但 plan 明确要求统一为 `common.saveFailed`，因此改造后均经由 `common.saveFailed` 输出。

3. **i18n 新增**：`common.saveFailed` —— 中文 "保存失败：{{reason}}"，英文 "Save failed: {{reason}}"。

## 不在本批改造范围

下列文件 / catch 块属本章之外（如批量导入、卡片层、抽屉 deleteChannel），不在第 5 章勾选范围：

- `ChannelDetailDrawer.tsx`、`ChannelOverviewTab.tsx`、`BatchImportModal.tsx`、`index.tsx`、`QuickOnboardMode.tsx`、`ApiKeyEditModal.tsx`

后续章节 / 后续批次再统一处理。
