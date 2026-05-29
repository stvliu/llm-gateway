# 供应商/接入点/API Key/模型 配置体验设计

> 日期：2026-05-29
> 状态：已定稿

---

## 1. 设计原则

### 1.1 用户思维模型优先

界面按用户的思维模型组织，而非后端领域模型：

| 用户思维 | 后端实体 |
|---------|---------|
| "我要接入哪个 AI 公司" | Provider |
| "我拿到什么 Key" | ChannelCredential |
| "我能用哪些模型" | ChannelModel → Model |
| "谁可以访问" | User, Team |

用户不需要理解 `Provider → Channel → ChannelEndpoint → ChannelCredential → ChannelModel → Model` 的层级关系。

### 1.2 双模式设计

| 维度 | 快速模式 | 专家模式 |
|------|---------|---------|
| 目标用户 | 初次配置 / 快速新增 | 高阶管理员 / 批量变更 |
| 操作路径 | 3步向导 | Tab 面板 + YAML 编辑 |
| 默认值 | 自动推断（内置模板） | 全部字段可编辑 |
| 批量能力 | 无 | 导入导出 / 批量轮换 |

---

## 2. 页面架构

### 2.1 侧边栏导航

按用户任务组织 7 个一级入口：

| 导航项 | 目标用户 | 说明 |
|--------|---------|------|
| ⚡ 快速接入 | Admin | 默认首页，展示已接入供应商概览 + "添加供应商"入口 |
| 🏢 供应商 | Admin | 供应商列表与管理 |
| 🧠 模型目录 | Admin | 全局模型注册表 |
| 🔑 API Key 管理 | Admin | 上游 Key + 下游 Key 统一视图 |
| 👥 团队与权限 | Admin | 团队管理、访问控制 |
| 📖 开发者门户 | Developer | 模型浏览 + 自助 Key + 代码示例 |
| ⚙️ 系统设置 | Admin | 全局配置 |

右上角设置 **快速/专家** 模式切换开关。

### 2.2 首页仪表盘

- 已接入供应商卡片一览（每个卡片显示供应商名 + Key 数量 + 状态）
- "+ 添加供应商"大按钮
- 快速查看近期异常、用量概览

---

## 3. 快速接入流程

### 3.1 三步向导

```
步骤 1：选择供应商      步骤 2：配置接入         步骤 3：选择模型
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│ ○ OpenAI        │   │ API Key: [____] │   │ ☑ gpt-4o       │
│ ○ Anthropic     │   │ 测试连通性      │   │ ☑ gpt-4o-mini  │
│ ○ Azure OpenAI  │   │                 │   │ ☐ o3           │
│ ○ Google Gemini │   │ + 添加备用 Key  │   │ ☐ o4-mini      │
│ ○ 自定义...     │   │ (负载均衡)      │   │                 │
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

### 3.2 智能默认值

选择主流供应商后，系统自动填充：

| 供应商 | 协议 | 默认 Endpoint | 推荐模型 |
|--------|------|--------------|---------|
| OpenAI | OPENAI | https://api.openai.com/v1 | gpt-4o, gpt-4o-mini, o3, o4-mini |
| Anthropic | ANTHROPIC | https://api.anthropic.com/v1 | claude-sonnet-4-6, claude-haiku-4-5 |
| Azure OpenAI | OPENAI | {resource}.openai.azure.com | 需用户指定 |
| Google Gemini | GEMINI | https://generativelanguage.googleapis.com | gemini-2.0-flash |

### 3.3 Key 输入交互

- 支持同时粘贴多个 Key（用换行/逗号分隔）
- 自动去重
- 内置连通性测试按钮
- 支持单通道配置多 Key（负载均衡），可视化展示优先组和权重

---

## 4. 专家模式

### 4.1 Tab 布局

切换到专家模式时，快速模式下填入的信息自动展开到完整视图。

| Tab | 包含字段 |
|-----|---------|
| 基础信息 | 供应商、显示名称、计费模式、优先级、权重 |
| 接入点 | 协议（OPENAI/ANTHROPIC/GEMINI/NATIVE）、Base URL、多接入点配置 |
| API Key | Key 管理（CRUD）、优先级、权重、状态、过期监控 |
| 模型映射 | 关联模型、upstream_model_name（别名映射）、定价覆盖 |
| 限流与配额 | RPM/TPM、Token 配额、并发限制 |
| 高级设置 | 超时、重试策略、断路器参数、自定义 Header |

### 4.2 YAML 双屏联动

- 表单修改实时同步到右侧 YAML 预览
- 支持直接编辑 YAML 反写表单
- 配置版本历史与回滚

---

## 5. 开发者门户

### 5.1 模型目录

- 展示当前团队已开通的所有模型
- 每个模型卡片包含：名称、供应商、能力标签（文本/图像）、价格透明
- 支持按供应商/能力搜索筛选

### 5.2 自助 Key 生成

- 一键创建 API Key
- 自动关联到当前用户和团队
- 创建时选择权限范围（可用模型、速率限制）
- 仅在创建时展示一次完整 Key

### 5.3 内嵌代码示例

- 自动填入新生成的 API Key
- 支持多语言切换：cURL / Python / Node.js / Java
- 一键复制

---

## 6. 模板与批量操作

### 6.1 预置模板

- OpenAI 标准、Anthropic 标准、Azure OpenAI、Google Gemini
- 每个模板预配置了默认 Endpoint、协议、推荐模型
- 支持"使用此模板" + 自定义覆盖

### 6.2 自定义模板

- 管理员可将已有配置保存为自定义模板
- 支持团队内共享

### 6.3 批量操作

- 批量导入：拖放或选择文件（YAML / JSON / CSV）
- 批量导出：导出全部配置或选定范围
- 从剪贴板粘贴配置文本

---

## 7. 统一 API Key 管理

### 7.1 双 Tab 视角

| 视角 | 内容 | 展示信息 |
|------|------|---------|
| 上游 Key | 供应商凭证（ChannelCredential） | 前缀、供应商、关联通道、状态、优先级/权重、最后使用时间 |
| 下游 Key | 用户密钥（UserApiKey） | 前缀、所属用户、关联通道、状态、最后使用时间、创建时间 |

### 7.2 关键交互

- 搜索/筛选：按 Key 前缀、供应商、状态筛选
- 状态标签：活跃（绿）/ 已降级（黄）/ 过期（红）
- 批量轮换：选择多个 Key 一键轮换
- 过期告警：Key 到期前自动通知，支持设置提前告警天数

---

## 8. 页面间流转关系

```
快速接入（向导）
    │ 完成
    ▼
供应商详情页 ←────────── 供应商列表
    │                       │
    │ 切换专家模式           │
    ▼                       ▼
专家配置面板 ←─────────── API Key 管理（统一视图）
    │                       
    ▼
开发者门户 ←─── 开发者自助获取 Key
```

---

## 9. 技术实现建议

### 9.1 模板系统

- 内置模板以 JSON/YAML 格式存储在 `gateway-boot/src/main/resources/templates/`
- 模板数据结构对齐 `Channel` + `ChannelEndpoint` + `ChannelModel` 聚合根
- 自定义模板存储在数据库 `provider_templates` 表中

### 9.2 配置导入导出格式

```yaml
version: "1.0"
providers:
  - name: OpenAI
    channels:
      - name: 主通道
        billing_mode: pay_as_you_go
        endpoints:
          - protocol: OPENAI
            base_url: https://api.openai.com/v1
        credentials:
          - api_key_prefix: sk-proj-xxx
            priority: 1
            weight: 100
        models:
          - model_name: gpt-4o
            upstream_model_name: gpt-4o
          - model_name: gpt-4o-mini
            upstream_model_name: gpt-4o-mini
```

### 9.3 前端路由

```
/                   → 仪表盘（快速接入首页）
/providers          → 供应商列表
/providers/:id      → 供应商详情（快速/专家模式）
/models             → 模型目录
/keys               → API Key 管理（上游/下游 Tab）
/keys/upstream      → 上游 Key 视图
/keys/downstream    → 下游 Key 视图
/teams              → 团队管理
/developer          → 开发者门户
/settings           → 系统设置
```

---

## 10. 未涵盖范围（后续迭代）

- 用量分析仪表盘（统计图表）
- 告警规则配置 UI
- 多区域/多集群部署配置
- 审计日志查询界面
- 自定义角色 RBAC 配置