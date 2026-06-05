# Sidebar 菜单重设计

## 背景

现有 Sidebar 菜单存在以下问题：

1. **菜单项缺失**：Models、API Keys、Users 三个页面有路由但未出现在菜单中
2. **分组语义弱**："系统设置"下把"目录管理"和"修改密码"放在一起，逻辑关联弱
3. **路由命名不当**：`/developer` 页面实际功能是"快速开始"，名不副实
4. **缺乏业务域组织**：菜单项平铺，未按业务域分组

## 设计目标

按业务域重新分组，让管理员和开发者都能快速找到功能入口。

## 菜单结构

采用三域精简分组方案，使用 Ant Design `Menu.ItemGroup` 渲染分组：

```
📊 仪表盘                        /dashboard
────────── 模型供给 ──────────
  🔌 渠道                        /channels
  🧩 模型                        /models
  📋 目录                        /catalog
────────── 身份与权限 ──────────
  🔑 API Key                     /keys
  👥 团队                        /teams
  👤 用户管理                     /users
────────── 运营与系统 ──────────
  📈 统计                        /stats        （预留）
  📝 审计日志                     /audit-logs   （预留）
```

折叠时分组标签隐藏，只显示图标。

## 变更清单

### 新增菜单项

| 路由 | 菜单名 | 图标 | 分组 | 权限 |
|------|--------|------|------|------|
| `/models` | 模型 | `AppstoreOutlined` | 模型供给 | `model:read` |
| `/keys` | API Key | `KeyOutlined` | 身份与权限 | `key:read` |
| `/users` | 用户管理 | `UserSwitchOutlined` | 身份与权限 | `user:read` |
| `/stats` | 统计 | `BarChartOutlined` | 运营与系统 | `dashboard:admin` |
| `/audit-logs` | 审计日志 | `FileSearchOutlined` | 运营与系统 | `audit:read` |

### 变更菜单项

| 项目 | 变更 |
|------|------|
| 渠道图标 | `ThunderboltOutlined` → `ApiOutlined` |
| 目录 | 从"系统设置"子菜单移至"模型供给"分组 |

### 移除菜单项

| 项目 | 去向 |
|------|------|
| 快速开始 | 从 Sidebar 移除，Header "快速开始"按钮路由改为 `/quickstart` |
| 修改密码 | 从 Sidebar 移除，移至头像下拉菜单 |

### 路由变更

| 旧路由 | 新路由 | 说明 |
|--------|--------|------|
| `/developer` | `/quickstart` | 语义更准确 |

### 权限变更

| 旧权限 | 新权限 |
|--------|--------|
| `developer:access` | `quickstart:access` |

## 头像下拉菜单调整

"修改密码"从 Sidebar 移至用户头像下拉菜单，位于"退出登录"上方：

```
┌──────────────┐
│ 👤 admin      │
│──────────────│
│ 🔒 修改密码    │
│ 🚪 退出登录    │
└──────────────┘
```

## i18n 新增 Key

### common.json

```json
{
  "menu": {
    "models": "模型 / Models",
    "apiKeys": "API Key",
    "users": "用户管理 / User Management",
    "stats": "统计 / Statistics",
    "auditLogs": "审计日志 / Audit Logs",
    "changePassword": "修改密码 / Change Password",
    "group": {
      "supply": "模型供给 / Model Supply",
      "identity": "身份与权限 / Identity & Access",
      "operations": "运营与系统 / Operations & System"
    }
  }
}
```

## menuConfig 数据结构

```typescript
export interface MenuGroupConfig {
  key: string;
  /** i18n key for group label */
  label: string;
  items: MenuItemConfig[];
}

export interface MenuItemConfig {
  key: string;
  icon: React.ReactNode;
  /** i18n key, namespace: common */
  label: string;
  permission?: Permission;
  /** 预留项显示为禁用状态 */
  reserved?: boolean;
}
```

顶层由 `MenuItemConfig[]`（无分组的独立项）和 `MenuGroupConfig[]`（分组项）组成。

## 实现范围

1. 修改 `menuConfig.tsx` — 新数据结构，三域分组
2. 修改 `Sidebar.tsx` — 渲染 ItemGroup 分组
3. 修改 `Header.tsx` — 头像下拉加"修改密码"，快速开始按钮路由改 `/quickstart`
4. 修改路由 `/developer` → `/quickstart`
5. 修改权限常量 `developer:access` → `quickstart:access`
6. 更新 i18n 文件
7. 预留项（统计/审计日志）渲染为禁用菜单项，点击无跳转
