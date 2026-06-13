# CHANNEL_LIFECYCLE SSOT 整合迁移审计

> 任务 6.1 产物：列出仓库中 `STATE_CONFIG` / `STATE_TRANSITION_LABELS` 全部引用点，作为后续批量替换 + 删除旧导出的依据。

## 检索方式

由于本仓库 `gateway-console/` 子模块未单独建立 codegraph 索引（仅根目录后端 Java 代码被索引），改用 ripgrep 在 `gateway-console/src/` 内全量扫描，覆盖 `.ts` / `.tsx` 文件。

```bash
# 命令一：定位 STATE_CONFIG
grep -rn "STATE_CONFIG" gateway-console/src/

# 命令二：定位 STATE_TRANSITION_LABELS
grep -rn "STATE_TRANSITION_LABELS" gateway-console/src/

# 命令三：定位 stateTransitions 模块导入
grep -rn "from '@/utils/stateTransitions'" gateway-console/src/
```

## 引用点清单

### 1. `STATE_CONFIG`

| 文件 | 行号 | 性质 |
|------|------|------|
| `gateway-console/src/components/common/ChannelStateTag.tsx` | 5 / 50 / 73 | **定义点 + 自用 + 命名导出** |
| `gateway-console/src/pages/Channels/index.tsx` | 35 | **死引用**（仅 import，全文未消费）|

合计：定义 1 处、外部引用 1 处（且为死引用）。

### 2. `STATE_TRANSITION_LABELS`

| 文件 | 行号 | 性质 |
|------|------|------|
| `gateway-console/src/utils/stateTransitions.ts` | 45 | **定义点 + 命名导出**（仅文件内部 65-71 行通过 `TRANSITION_ACTION_LABELS` 反向引用 key 字面量，但未直接读 `STATE_TRANSITION_LABELS` 值） |

合计：定义 1 处，外部引用 0 处。

### 3. `@/utils/stateTransitions` 模块其他活跃导出

虽不在删除范围（保留），列出便于第 6.3 步替换时不被误删：

| 导出符号 | 引用点 |
|---------|--------|
| `getAvailableTransitions` | `pages/Channels/ChannelCard.tsx:12,52`、`ChannelDetailDrawer.tsx:29`、`ChannelTableView.tsx:11` |
| `getTransitionActionLabel` | `pages/Channels/ChannelCard.tsx:12,57,155`、`ChannelDetailDrawer.tsx:29`、`ChannelTableView.tsx:11` |
| `isTerminalState` / `isRoutableState` / `TRANSITION_ACTION_LABELS` | 文件内部使用，无外部直接引用 |

> 注意：`getAvailableTransitions` 与 `CHANNEL_LIFECYCLE.nextStates` 语义重合，本次不替换 `getAvailableTransitions` 调用点（保持第 6 章最小改动），仅删除 `STATE_TRANSITION_LABELS` 这一未消费的导出，作为彻底清理 SSOT 残余的一部分。后续若有更彻底重构再迁移。

## 替换 / 删除计划（任务 6.3）

1. **`pages/Channels/index.tsx:35`**：删除 `import { STATE_CONFIG } from '@/components/common/ChannelStateTag';` —— 死引用，无替换需求。
2. **`components/common/ChannelStateTag.tsx`**：
   - 删除 `STATE_CONFIG` 常量与 `export { STATE_CONFIG }`。
   - 内部状态展示改为读 `CHANNEL_LIFECYCLE[state]` 派生 `tagColor` / `label`（i18n key）。
3. **`utils/stateTransitions.ts`**：
   - 删除 `STATE_TRANSITION_LABELS` 常量与导出（无外部消费者）。
   - 保留 `getAvailableTransitions / getTransitionActionLabel / isTerminalState / isRoutableState / TRANSITION_ACTION_LABELS`。

## 风险点

- ChannelStateTag.smoke.test.tsx 第 12 行断言 `getByText('运行中')`。改造后 ACTIVE 文案由 i18n 派生，需保证测试环境 i18next 默认 fallback 为 zh-CN（`@/i18n` 模块 init 时已设 `fallbackLng: 'zh-CN'`），且新增 key `channel.state.active = "运行中"`。smoke 文件需要切换为 `I18nextProvider` wrapper 或直接 import `@/i18n` 进行副作用初始化——保持原断言文本不变。
- `pages/Channels/index.tsx` 中 STATE_CONFIG 死 import 删除后，TS 严格模式下会自然消失编译警告。
