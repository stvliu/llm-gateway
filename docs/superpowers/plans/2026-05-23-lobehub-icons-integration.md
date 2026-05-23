# LobeHub Icons 集成实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 gateway-console 中集成 @lobehub/icons，替换现有远程 iconUrl SVG 渲染方式，使用本地 React 组件渲染供应商图标。

**Architecture:** 创建 `ProviderIcon` 映射组件封装 providerId → LobeHub Icons 组件的映射关系，在所有使用 iconUrl 的地方替换为 ProviderIcon 组件渲染。后端 iconUrl 字段保留但不再作为前端渲染依据。

**Tech Stack:** React 19 + @lobehub/icons + TypeScript + Antd Avatar

---

### 文件变更清单

| 操作 | 文件 | 职责 |
|------|------|------|
| 新建 | `src/components/ui/ProviderIcon.tsx` | providerId → LobeHub 图标组件映射 + 渲染 |
| 新建 | `src/components/ui/ProviderIcon.stories.tsx` | Storybook 预览所有厂商图标变体 |
| 修改 | `src/components/ui/index.ts` | 导出 ProviderIcon |
| 修改 | `src/pages/Providers/ProviderCardView.tsx` | 替换 iconUrl Avatar 为 ProviderIcon |
| 修改 | `src/pages/Metadata/index.tsx` | 替换 iconUrl Avatar 为 ProviderIcon |
| 修改 | `src/types/provider.ts` | 添加 iconUrl 可选注释（标记为仅后端使用） |
| 删除 | `src/assets/images/provider-logos/` | 移除本地 SVG 冗余文件（如有） |

---

### Task 1: 创建 ProviderIcon 映射组件

**Files:**
- Create: `src/components/ui/ProviderIcon.tsx`

- [ ] **Step 1: 创建 ProviderIcon 组件**

```tsx
import { Avatar, type AvatarProps } from 'antd';
import {
  OpenAI,
  Anthropic,
  Gemini,
  DeepSeek,
  Zhipu,
  Qwen,
  Tencent,
  Volcengine,
  Moonshot,
  Minimax,
  IFlyTekCloud,
  Baidu,
} from '@lobehub/icons';
import type { FC } from 'react';

/** providerId 到 LobeHub 图标组件的映射 */
const PROVIDER_ICON_MAP: Record<string, FC<{ size?: number }>> = {
  openai: OpenAI,
  anthropic: Anthropic,
  gemini: Gemini,
  deepseek: DeepSeek,
  zhipu: Zhipu,
  qwen: Qwen,
  tencent: Tencent,
  volcengine: Volcengine,
  moonshot: Moonshot,
  minimax: Minimax,
  xunfei: IFlyTekCloud,
  wenxin: Baidu,
};

export interface ProviderIconProps extends Omit<AvatarProps, 'src'> {
  /** 后端 providerId，用于匹配图标 */
  providerId: string;
  /** 图标尺寸（默认 32） */
  iconSize?: number;
}

/**
 * 根据 providerId 渲染对应的 LobeHub 品牌图标。
 * 未匹配时显示 providerId 首字母的默认 Avatar。
 */
export const ProviderIcon: FC<ProviderIconProps> = ({
  providerId,
  iconSize = 32,
  ...avatarProps
}) => {
  const IconComponent = PROVIDER_ICON_MAP[providerId];

  if (IconComponent) {
    return (
      <Avatar
        {...avatarProps}
        src={<IconComponent size={iconSize} />}
      />
    );
  }

  // 降级：首字母 Avatar
  return (
    <Avatar {...avatarProps}>
      {providerId?.charAt(0)?.toUpperCase() || '?'}
    </Avatar>
  );
};
```

- [ ] **Step 2: 在 ui/index.ts 中导出**

修改 `src/components/ui/index.ts`，添加：

```ts
export { ProviderIcon } from './ProviderIcon';
export type { ProviderIconProps } from './ProviderIcon';
```

- [ ] **Step 3: 验证编译通过**

Run: `cd gateway-console && pnpm tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 4: Commit**

```bash
git add src/components/ui/ProviderIcon.tsx src/components/ui/index.ts
git commit -m "feat(console): 新增 ProviderIcon 组件，封装 providerId 到 LobeHub Icons 映射"
```

---

### Task 2: 替换 ProviderCardView 中的 iconUrl 渲染

**Files:**
- Modify: `src/pages/Providers/ProviderCardView.tsx`

- [ ] **Step 1: 在 ProviderCardView 中替换图标渲染**

找到当前使用 `iconUrl` 渲染 Avatar 的代码，替换为 ProviderIcon。

当前代码（约第 52 行附近）：
```tsx
<Avatar src={provider.iconUrl} size={48} />
```

替换为：
```tsx
import { ProviderIcon } from '@/components/ui';

// ...

<ProviderIcon providerId={provider.providerId} iconSize={48} size={48} />
```

移除该文件中与 `iconUrl` 相关的 import 或引用。

- [ ] **Step 2: 验证编译通过**

Run: `cd gateway-console && pnpm tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 3: Commit**

```bash
git add src/pages/Providers/ProviderCardView.tsx
git commit -m "feat(console): ProviderCardView 使用 ProviderIcon 替代远程 iconUrl"
```

---

### Task 3: 替换 Metadata 页面中的 iconUrl 渲染

**Files:**
- Modify: `src/pages/Metadata/index.tsx`

- [ ] **Step 1: 在 Metadata 页面替换图标渲染**

找到当前使用 `iconUrl` 渲染的代码。在 Provider 列表项中：

当前代码：
```tsx
<Avatar src={provider.iconUrl} size={24} />
```

替换为：
```tsx
import { ProviderIcon } from '@/components/ui';

// ...

<ProviderIcon providerId={provider.providerId} iconSize={24} size={24} />
```

对页面中所有 `iconUrl` Avatar 引用都做同样的替换。同时移除该文件中不再使用的 `iconUrl` 相关引用。

- [ ] **Step 2: 验证编译通过**

Run: `cd gateway-console && pnpm tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 3: Commit**

```bash
git add src/pages/Metadata/index.tsx
git commit -m "feat(console): Metadata 页面使用 ProviderIcon 替代远程 iconUrl"
```

---

### Task 4: 标记 iconUrl 字段为后端专用 & 清理冗余 SVG

**Files:**
- Modify: `src/types/provider.ts`
- Delete: `src/assets/images/provider-logos/` 目录（如包含冗余 SVG）

- [ ] **Step 1: 在 provider 类型中标记 iconUrl 为后端专用**

在 `src/types/provider.ts` 中，找到 `iconUrl` 字段，添加 JSDoc 注释：

```ts
/** @deprecated 前端不再使用，图标渲染已由 ProviderIcon 组件接管 */
iconUrl?: string;
```

- [ ] **Step 2: 删除本地冗余 SVG 文件**

检查 `src/assets/images/provider-logos/` 目录。如果其中仅包含 OpenAI/Google/Anthropic 三个 SVG 且不再被其他代码引用，删除整个目录。

Run: `grep -r "provider-logos" src/` 确认无其他引用后：
```bash
rm -rf src/assets/images/provider-logos/
```

- [ ] **Step 3: 验证编译通过**

Run: `cd gateway-console && pnpm tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 4: Commit**

```bash
git add src/types/provider.ts
git commit -m "chore(console): 标记 iconUrl 为 deprecated，清理冗余本地 SVG"
```

---

### Task 5: 端到端验证

**Files:** 无文件变更

- [ ] **Step 1: 启动开发服务器**

Run: `cd gateway-console && pnpm dev`

- [ ] **Step 2: 验证供应商页面图标渲染**

在浏览器中访问供应商管理页面，确认：
- 所有 12 个供应商的图标正确显示为 LobeHub 品牌图标
- 图标尺寸与原有 Avatar 一致
- 无控制台报错

- [ ] **Step 3: 验证元数据页面图标渲染**

在浏览器中访问元数据页面，确认：
- Provider 列表中图标正确显示
- 无 iconUrl 404 错误

- [ ] **Step 4: 验证降级逻辑**

临时将某个 providerId 改为不存在的值，确认显示首字母 Avatar 降级方案。

---

### 自查清单

**Spec 覆盖度：**
- ✅ 12 个 provider 全部有对应 LobeHub 图标组件
- ✅ ProviderCardView 和 Metadata 两个页面均替换
- ✅ 降级方案（未知 providerId → 首字母 Avatar）
- ✅ iconUrl 字段标记 deprecated
- ✅ 清理冗余本地 SVG

**Placeholder 扫描：**
- ✅ 无 TBD / TODO / implement later
- ✅ 所有代码步骤包含完整实现
- ✅ 无 "Similar to Task N" 引用

**类型一致性：**
- ✅ `ProviderIconProps` 在组件定义和导出中一致
- ✅ `providerId` 字段名与后端 `provider_id` 映射一致
- ✅ `PROVIDER_ICON_MAP` 的 key 与后端 12 个 providerId 完全对齐
