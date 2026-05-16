# Hugging Face 与 ModelScope 模型信息 API 对比

基于 2026 年 5 月 15 日的最新公开资料，对两大开源模型托管平台通过 API 获取模型信息的能力进行系统对比分析。

---

## 一、Hugging Face Hub API

**基础端点**: `https://huggingface.co/api`

### 1. 列出模型

```
GET /api/models
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `author` | string | 按组织/用户筛选（如 `author=openai`） |
| `search` | string | 模型名关键词搜索 |
| `sort` | string | 排序字段：`lastModified`、`downloads`、`likes`、`trending` |
| `direction` | string | `-1` 降序 / `1` 升序 |
| `limit` | int | 每页数量（默认 30，最大 500） |
| `full` | bool | `true` 返回完整元数据（含 pipeline_tag、tags、config） |
| `filter` | string | 按标签筛选（如 `text-generation-inference`） |
| `pipeline_tag` | string | 按任务类型筛选（如 `text-generation`、`text2text-generation`） |

**响应示例**（`full=true`）:

```json
{
  "id": "meta-llama/Llama-3.3-70B-Instruct",
  "author": "meta-llama",
  "pipeline_tag": "text-generation",
  "tags": ["pytorch", "safetensors", "llama", "conversational"],
  "downloads": 1234567,
  "likes": 2345,
  "lastModified": "2025-01-15T00:00:00.000Z",
  "siblings": [{"rfilename": "config.json"}, ...],
  "config": { "model_type": "llama", "vocab_size": 128256 }
}
```

### 2. 获取模型详情

```
GET /api/models/{model_id}
```

返回完整的模型卡片信息，包括：

- `config` — 模型架构配置（model_type、vocab_size、hidden_size 等）
- `safetensors` — 权重文件的总参数量
- `cardData` — README 元数据（license、language、widget 等）
- `siblings` — 仓库文件列表
- `gated` — 是否需要申请访问

### 3. Python SDK 用法

```python
from huggingface_hub import HfApi

api = HfApi()

# 列出模型（支持筛选）
models = api.list_models(
    filter="text-generation",
    author="meta-llama",
    sort="downloads",
    direction=-1,
    limit=10
)

# 获取模型详情
info = api.model_info("meta-llama/Llama-3.3-70B-Instruct")
print(f"参数量: {info.safetensors.total / 1e9:.1f}B")
print(f"创建时间: {info.created_at}")
```

### 4. 认证方式

```
Authorization: Bearer hf_xxxxxxxxxxxxx
```

- 公开模型无需认证
- Gated 模型需要认证 + 访问申请
- Token 在 https://huggingface.co/settings/tokens 创建

### 5. 分页方式

采用 **cursor-based 分页**，通过响应 `Link` Header 获取下一页 URL：

```
Link: <https://huggingface.co/api/models?cursor=xxx>; rel="next"
```

---

## 二、ModelScope（魔搭）API

**基础端点**: `https://modelscope.cn/api/v1`

### 1. 列出模型

```
GET /api/v1/models
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `Name` | string | 模型名搜索 |
| `Task` | string | 任务类型（`text-generation`、`chat` 等） |
| `PageNumber` | int | 页码（从 1 开始） |
| `PageSize` | int | 每页数量 |
| `SortBy` | string | 排序字段（`GmtModified`、`Downloads`、`Stars`） |
| `OwnerId` | string | 按组织筛选 |

**响应示例**:

```json
{
  "Code": 200,
  "Data": {
    "Models": [{
      "Id": "qwen/Qwen2.5-72B-Instruct",
      "Name": "Qwen2.5-72B-Instruct",
      "Namespace": "qwen",
      "Task": "text-generation",
      "Downloads": 500000,
      "Stars": 1200,
      "GmtModified": "2025-01-10T00:00:00Z"
    }],
    "TotalCount": 12345
  }
}
```

### 2. 获取模型详情

```
GET /api/v1/models/{namespace}/{name}
```

返回：

- 模型基本信息（名称、描述、任务类型）
- 文件列表：`GET /api/v1/models/{namespace}/{name}/revisions/master/files`
- 模型配置（config.json）

### 3. Python SDK 用法

```python
from modelscope.hub.api import HubApi

api = HubApi()

# 列出模型
result = api.list_models(owner_or_group="Qwen", page_number=1, page_size=10)

# 获取模型详情
model_info = api.get_model("qwen/Qwen2.5-72B-Instruct")

# 获取模型文件列表
files = api.get_model_files("qwen/Qwen2.5-72B-Instruct")
```

### 4. 认证方式

```
Authorization: Bearer xxxxxxxx
```

- 公开模型无需认证
- Token 在 https://modelscope.cn/my/myaccesstoken 创建
- 支持 OAuth 授权接入

### 5. 分页方式

采用传统 **PageNumber / PageSize 分页**，响应中包含 `TotalCount` 总数。

### 6. OpenAPI 规范

ModelScope 提供标准 OpenAPI 规范文件：

- 文档入口：https://modelscope.cn/docs/openapi
- OpenAPI JSON：https://modelscope.cn/.well-known/openapi.json

已覆盖的核心板块：

| 板块 | 能力 |
|------|------|
| 用户信息 | 获取登录用户公开基本信息 |
| 模型管理 | 按名称、作者、任务类型、框架、许可证等多维度检索，获取详情、下载量、参数量、标签等 |
| 数据集管理 | 查询和获取数据集信息，包括许可协议、任务类型、更新时间等 |
| MCP 服务管理 | 列出、查询、部署与解除部署 MCP 服务 |

---

## 三、关键差异对比

| 维度 | Hugging Face | ModelScope |
|------|-------------|------------|
| **模型数量** | ~90 万+ | ~6 万+ |
| **API 成熟度** | 高，OpenAPI 规范完善 | 中，文档较分散 |
| **元数据丰富度** | 高（pipeline_tag、tags、config、safetensors 参数量） | 中（基础信息 + 任务类型） |
| **分页方式** | cursor-based（`Link` Header） | 传统 PageNumber/PageSize |
| **搜索能力** | 支持全文搜索 + 标签 + 任务类型组合筛选 | 支持名称搜索 + 任务类型筛选 |
| **国内访问** | 较慢/不稳定 | 快速稳定 |
| **特色** | safetensors 元数据可获取精确参数量 | 国内模型生态（Qwen、ChatGLM 等首发） |
| **SDK 语言** | Python、JavaScript、CLI | Python、Swift |
| **认证方式** | Bearer Token（hf_xxx） | Bearer Token + OAuth |
| **Gated 模型** | 支持（需申请访问） | 不适用 |

---

## 四、对 LLM-Gateway 的集成价值

在 metadata 重构背景下，这两个平台可作为**模型元数据的自动同步源**：

### 1. 自动填充模型信息

创建供应商时，输入模型 ID 即可自动拉取：

- 模型名称
- 参数量（HF 通过 safetensors 获取）
- 任务类型（pipeline_tag / Task）
- 上下文长度（从 config.json 推导）
- 框架（PyTorch / TensorFlow / vLLM 等）
- 许可证（license）

### 2. 双源互补策略

| 场景 | 推荐数据源 |
|------|-----------|
| 国际模型（Llama、Mistral、Gemma 等） | Hugging Face |
| 国内模型（Qwen、ChatGLM、Baichuan 等） | ModelScope |
| 国内网络环境 | ModelScope（访问稳定） |
| 需要精确参数量 | Hugging Face（safetensors 元数据） |

### 3. 建议的集成方式

- 在 `ProviderMetadataController` 中增加"从平台导入"接口
- 在 `metadata/` 资源目录中维护平台映射配置（模型 ID → 平台来源）
- 前端 `ProviderMetadataSelector` 增加"从 HuggingFace/ModelScope 搜索导入"功能
- 通过定时任务定期同步模型元数据（下载量、更新时间等）

### 4. 数据同步流程

```
用户输入模型 ID
    ↓
判断平台来源（HF / ModelScope）
    ↓
调用对应平台 API 获取元数据
    ↓
映射为内部 Metadata 实体
    ↓
持久化到 model_metadata 表
    ↓
前端展示自动填充的模型信息
```

---

## 五、参考链接

| 资源 | 链接 |
|------|------|
| Hugging Face Hub API 文档 | https://huggingface.co/docs/hub/en/api |
| Hugging Face OpenAPI 规范 | https://huggingface.co/.well-known/openapi.json |
| Hugging Face Python SDK | https://github.com/huggingface/huggingface_hub |
| ModelScope OpenAPI 文档 | https://modelscope.cn/docs/openapi |
| ModelScope OpenAPI 规范 | https://modelscope.cn/.well-known/openapi.json |
| ModelScope OAuth 文档 | https://modelscope.cn/docs/accounts/oauth |
