# 国内外大模型厂商API Pricing Plans & Base URL 详细调研 (截止2026.05.21)

> 数据来源：各厂商官方文档、GitHub开源项目配置、开发者社区实测，更新日期2026-05-21。

---

## 一、国际厂商

### 1. OpenAI

**API文档**: https://platform.openai.com/docs

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard** | 按量付费 | `https://api.openai.com/v1` | OpenAI原生 | 标准即时计费 |
| **Batch API** | 批量异步 | `https://api.openai.com/v1` | OpenAI原生 | 50%折扣，24h返回 |
| **Enterprise** | 企业定制 | `https://api.openai.com/v1` | OpenAI原生 | 专属支持 |
| **ChatGPT Plus/Pro** | 消费订阅 | — | — | $20/$200/月，非API |

#### 最新模型定价 (per 1M tokens)
| 模型 | Input | Output | Cached Input | Context |
|------|-------|--------|--------------|---------|
| **GPT-5.5** | $5.00 | $30.00 | $0.50 | 270K |
| **GPT-5.4** | $2.50 | $15.00 | $0.25 | 270K |
| **GPT-5.4 mini** | $0.75 | $4.50 | $0.075 | 270K |
| **GPT-5.4 nano** | $0.20 | $1.25 | $0.02 | 270K |
| **o3** | $2.00 | $8.00 | $0.20 | 200K |
| **o3-Pro** | $20.00 | $80.00 | $2.00 | 200K |

---

### 2. Anthropic (Claude)

**API文档**: https://docs.anthropic.com

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard** | 按量付费 | `https://api.anthropic.com/v1` | Anthropic原生 | 标准即时计费 |
| **Batch Processing** | 批量异步 | `https://api.anthropic.com/v1` | Anthropic原生 | 50%折扣 |
| **Model Routing** | 智能路由 | `https://api.anthropic.com/v1` | Anthropic原生 | 自动选最优模型 |
| **OpenAI兼容层** | 兼容模式 | `https://api.anthropic.com/v1` | OpenAI兼容 | 测试用，非生产推荐 |
| **Claude Pro** | 消费订阅 | — | — | $20/月 |

#### 最新模型定价 (per 1M tokens)
| 模型 | Input | Output | Cached Input | Context |
|------|-------|--------|--------------|---------|
| **Claude Opus 4.7** | $5.00 | $25.00 | $0.50 | 1M |
| **Claude Sonnet 4.6** | $3.00 | $15.00 | $0.30 | 1M |
| **Claude Haiku 4.5** | $1.00 | $5.00 | $0.10 | 200K |

---

### 3. Google (Gemini)

**API文档**: https://ai.google.dev/gemini-api/docs

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard** | 按量付费 | `https://generativelanguage.googleapis.com/v1beta` | Gemini原生 | 标准计费 |
| **Batch API** | 批量异步 | `https://generativelanguage.googleapis.com/v1beta` | Gemini原生 | 50%折扣 |
| **OpenAI兼容端点** | 兼容模式 | `https://generativelanguage.googleapis.com/v1beta/openai/` | OpenAI兼容 | 可直接用OpenAI SDK |
| **Vertex AI** | 企业版 | `https://{region}-aiplatform.googleapis.com` | Google Cloud | GCP集成 |
| **Gemini Advanced** | 消费订阅 | — | — | $20/月 |

#### 最新模型定价 (per 1M tokens)
| 模型 | Input | Output | Context |
|------|-------|--------|---------|
| **Gemini 3.1 Pro** (≤200K) | $2.00 | $12.00 | 2M |
| **Gemini 3.1 Pro** (>200K) | $4.00 | $18.00 | 2M |
| **Gemini 3.1 Flash-Lite** | $0.25 | $1.50 | 1M |
| **Gemini 3 Flash** | $0.50 | $3.00 | 1M |
| **Gemini 2.5 Flash-Lite** | $0.10 | $0.40 | 1M |

---

### 4. xAI (Grok)

**API文档**: https://docs.x.ai

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard** | 按量付费 | `https://api.x.ai/v1` | OpenAI兼容 | 默认端点 |
| **X Premium+** | 消费订阅 | — | — | $8/月，含Grok |
| **SuperGrok** | 高级订阅 | — | — | $40/月 |

#### 最新模型定价 (per 1M tokens)
| 模型 | Input | Output | Cached Input | Context |
|------|-------|--------|--------------|---------|
| **Grok 4.20** | $2.00 | $6.00 | ~$0.20 | 2M |
| **Grok 4.3** | $1.25 | $2.50 | ~$0.13 | 1M |
| **Grok 4.1 Fast** | $0.20 | $0.50 | ~$0.02 | 2M |

---

### 5. Mistral AI

**API文档**: https://docs.mistral.ai

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard** | 按量付费 | `https://api.mistral.ai/v1` | OpenAI兼容 | 默认端点 |
| **La Plateforme** | 企业版 | `https://api.mistral.ai/v1` | OpenAI兼容 | 企业专属 |
| **Le Chat Pro** | 消费订阅 | — | — | $14.99/月 |
| **Le Chat Team** | 团队版 | — | — | $24.99/用户/月 |

#### 最新模型定价 (per 1M tokens)
| 模型 | Input | Output | Context |
|------|-------|--------|---------|
| **Mistral Large 3** | $0.50 | $1.50 | 128K |
| **Mistral Large 2** | $2.00 | $6.00 | 128K |
| **Mistral Small 4** | $0.15 | $0.60 | 128K |
| **Mistral Small 3.2** | $0.08 | $0.20 | 128K |
| **Devstral** | $0.10 | $0.30 | 32K |
| **Codestral** | $0.30 | $0.90 | 32K |
| **Mistral NeMo** | $0.02 | $0.03 | 128K |

---

### 6. Microsoft (Azure AI Foundry)

**API文档**: https://learn.microsoft.com/azure/ai-foundry

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Global Standard** | 按量付费 | `https://{resource}.openai.azure.com/openai/deployments/{deployment}` | OpenAI兼容 | 最低费率 |
| **PTU (Provisioned)** | 预留容量 | `https://{resource}.openai.azure.com/openai/deployments/{deployment}` | OpenAI兼容 | 最高70%节省 |
| **Batch API** | 批量异步 | 同上 | OpenAI兼容 | 50%折扣 |
| **Marketplace第三方** | 独立计费 | 各厂商自有端点 | 各厂商协议 | 通过Azure Marketplace |
| **Azure订阅** | 消费端 | — | — | Copilot Pro $20/月 |

#### 最新模型定价 (per 1M tokens)
| 模型 | Input | Output | Context | 备注 |
|------|-------|--------|---------|------|
| **GPT-5.5** | $30.00 | $180.00 | 1M | Azure直售 |
| **GPT-4o** | $2.50 | $10.00 | 128K | — |
| **GPT-4o mini** | $0.15 | $0.60 | 128K | — |
| **GPT-4.1** | $2.00 | $8.00 | 1M | — |
| **Phi-4-mini** | $0.07 | $0.23 | 128K | 微软自研 |
| **Claude Opus 4.7** (第三方) | — | — | — | Marketplace |
| **Llama-4-Maverick** (第三方) | — | — | — | Marketplace |
| **DeepSeek-V3/R1** (第三方) | — | — | — | Marketplace |

---

### 7. Amazon (AWS Bedrock)

**API文档**: https://docs.aws.amazon.com/bedrock

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **On-Demand** | 按量付费 | `https://bedrock-runtime.{region}.amazonaws.com` | Bedrock原生 | 标准即时计费 |
| **Batch** | 批量异步 | 同上 | Bedrock原生 | 50%折扣 |
| **Provisioned Throughput** | 预留吞吐量 | 同上 | Bedrock原生 | 15-40%折扣 |
| **Flex** | 灵活调度 | 同上 | Bedrock原生 | 50%折扣 |
| **Priority** | 优先保障 | 同上 | Bedrock原生 | +75%溢价 |
| **Prompt Caching** | 缓存 | 同上 | Bedrock原生 | 最高90%折扣 |

#### 最新模型定价 (per 1M tokens)
| 模型 | Input | Output | Context |
|------|-------|--------|---------|
| **OpenAI GPT-5.5** | $5.00 | $15.00 | 1M |
| **Claude Opus 4.7** | $5.00 | $25.00 | 1M |
| **Claude Sonnet 4.6** | $3.00 | $15.00 | 1M |
| **Claude Haiku 4.5** | $0.80 | $4.00 | 200K |
| **Amazon Nova 2 Lite** | $0.06 | $0.24 | 1M |
| **Amazon Nova Pro** | $0.80 | $3.20 | 300K |
| **Amazon Nova Micro** | $0.035 | $0.14 | 128K |
| **Meta Llama 3.3 70B** | $0.72 | $0.72 | 128K |
| **Mistral Large 3** | $0.50 | $1.50 | 128K |
| **DeepSeek V3.2** | $0.62 | $1.85 | 128K |

---

## 二、国内厂商

### 1. DeepSeek (深度求索)

**API文档**: https://api-docs.deepseek.com

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard (标准按量)** | 按量付费 | `https://api.deepseek.com` | OpenAI兼容 | 默认端点，OpenAI SDK直接接入 |
| **Standard (Anthropic兼容)** | 按量付费 | `https://api.deepseek.com/anthropic` | Anthropic兼容 | 支持Claude SDK直接接入 |
| **Context Caching** | 自动缓存 | 同上 | 同上 | 缓存命中98%折扣 |
| **赠送余额** | 新用户优惠 | 同上 | 同上 | 500万免费Tokens |

#### 最新模型定价 (per 1M tokens)
| 模型 | Cache Hit | Cache Miss | Output | Context | Max Output |
|------|-----------|------------|--------|---------|------------|
| **DeepSeek-V4-Pro** | $0.0145 | $1.74 | $3.48 | 1M | 384K |
| **DeepSeek-V4-Flash** | $0.0028 | $0.14 | $0.28 | 1M | 384K |

> **注意**: V4-Pro当前运行75%发布折扣(至2026-05-31 15:59 UTC)，上表为促销后稳态价格。

---

### 2. 月之暗面 (Kimi)

**API文档**: https://platform.kimi.com/docs

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard (标准按量)** | 按量付费 | `https://api.moonshot.cn/v1` | OpenAI兼容 | 默认端点，OpenAI SDK直接接入 |
| **Context Caching** | 自动缓存 | 同上 | OpenAI兼容 | 缓存命中80-85%折扣 |
| **Web Search** | 联网搜索 | 同上 | OpenAI兼容 | $0.005/call |
| **Tiered Rate Limits** | 分层速率 | 同上 | OpenAI兼容 | 充值解锁更高并发 |
| **新用户代金券** | 优惠 | 同上 | OpenAI兼容 | 充值$5返$5代金券 |

#### 速率限制层级
| 层级 | 累计充值 | 并发 | RPM | TPM |
|------|----------|------|-----|-----|
| Tier 0 | $1 | 1 | 3 | 500K |
| Tier 1 | $10 | 50 | 200 | 2M |
| Tier 2 | $20 | 100 | 500 | 3M |
| Tier 3 | $100 | 200 | 5,000 | 3M |
| Tier 4 | $1,000 | 400 | 5,000 | 4M |
| Tier 5 | $3,000 | 1,000 | 10,000 | 5M |

#### 最新模型定价 (per 1M tokens)
| 模型 | Input | Output | Cached Input | Context | 备注 |
|------|-------|--------|--------------|---------|------|
| **kimi-k2.6** | $0.95 | $4.00 | $0.16 | 262K | 最新旗舰 |
| **kimi-k2.5** | $0.60 | $3.00 | $0.10 | 262K | 多模态+推理 |
| **kimi-k2-thinking** | $0.60 | $2.50 | $0.15 | 262K | **2026.05.25 EOL** |
| **moonshot-v1-8k** | $0.20 | $2.00 | — | 8K | 基础版 |
| **moonshot-v1-32k** | $1.00 | $3.00 | — | 32K | 标准版 |
| **moonshot-v1-128k** | $2.00 | $5.00 | — | 131K | 长上下文 |

---

### 3. 阿里巴巴 (通义千问/Qwen)

**API文档**: https://help.aliyun.com/zh/dashscope

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard (标准按量)** | 按量付费 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | OpenAI兼容 | 默认端点 |
| **Standard (原生协议)** | 按量付费 | `https://dashscope.aliyuncs.com/api/v1` | 阿里云原生 | 百炼平台原生协议 |
| **阿里云百炼免费额度** | 免费试用 | 同上 | 同上 | 每模型100万Tokens，3个月 |
| **Token包预购** | 批量购买 | 同上 | 同上 | 折扣价 |

#### 最新模型定价 (per 1M tokens)
| 模型 | Input | Output | Context | 备注 |
|------|-------|--------|---------|------|
| **Qwen3-Max** | $0.36–$1.00 | $1.43–$4.01 | 262K | 旗舰 |
| **Qwen3.5-Plus** | $0.12–$0.57 | $0.69–$3.44 | 1M | 平衡 |
| **Qwen-Flash** | $0.05–$0.25 | $0.40–$2.00 | 1M | 轻量 |
| **Qwen3-VL** | $0.520 | $2.08 | 131K | 视觉 |
| **Qwen3-Coder** | — | — | — | 代码专用 |

---

### 4. 字节跳动 (火山方舟/豆包)

**API文档**: https://www.volcengine.com/docs/82379

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard (在线推理)** | 按量付费 | `https://ark.cn-beijing.volces.com/api/v3` | OpenAI兼容 | 默认端点，需Endpoint ID作为模型名 |
| **Coding Plan Lite** | 订阅制 | `https://ark.cn-beijing.volces.com/api/coding/v3` | OpenAI兼容 | AI编码专用，Lite套餐 |
| **Coding Plan Pro** | 订阅制 | `https://ark.cn-beijing.volces.com/api/coding/v3` | OpenAI兼容 | AI编码专用，Pro套餐 |
| **Coding Plan (Anthropic兼容)** | 订阅制 | `https://ark.cn-beijing.volces.com/api/coding` | Anthropic兼容 | 支持Claude Code等工具 |
| **安心体验** | 免费试用 | 同上 | 同上 | 每模型50万Tokens |
| **协作奖励计划** | 免费额度 | 同上 | 同上 | 每日200万Tokens |

> **重要**: Coding Plan套餐额度仅支持AI编程工具使用，不可用于API调用。企业级API调用需使用火山方舟模型API。

#### 模型配置说明
- 火山方舟使用 **Endpoint ID** (如 `ep-20240xxxxx`) 作为模型名，而非通用模型名
- 支持 `ark-code-latest` 自动切换模型

---

### 5. 百度 (千帆/文心)

**API文档**: https://cloud.baidu.com/doc/qianfan

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard (常规MaaS)** | 按量付费 | `https://qianfan.baidubce.com/v2` | OpenAI兼容 | 默认端点 |
| **Coding Plan (OpenAI兼容)** | 订阅制 | `https://qianfan.baidubce.com/v2/coding` | OpenAI兼容 | 编码专用，完整路径: `/v2/coding/chat/completions` |
| **Coding Plan (Anthropic兼容)** | 订阅制 | `https://qianfan.baidubce.com/anthropic/coding` | Anthropic兼容 | 编码专用，完整路径: `/anthropic/coding/v1/messages` |
| **新用户免费额度** | 免费试用 | 同上 | 同上 | 每模型100万Tokens，3个月 |
| **Token包预购** | 批量购买 | 同上 | 同上 | 折扣价 |

#### Coding Plan专属说明
- Coding Plan生成 **专属API Key**，仅可用于Coding Plan专属接口
- 支持模型: `kimi-k2.5`, `deepseek-v3.2`, `glm-5`, `minimax-m2.5`, `ernie-4.5-turbo`, `deepseek-v4-flash`, `glm-5.1`
- 配置文件指定 `qianfan-code-latest` 可实时切换模型

---

### 6. 智谱AI (GLM)

**API文档**: https://docs.bigmodel.cn

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard (标准按量)** | 按量付费 | `https://open.bigmodel.cn/api/paas/v4/` | OpenAI兼容 | 默认端点 |
| **GLM Coding Plan Lite** | 订阅制 | `https://open.bigmodel.cn/api/coding/paas/v4` | OpenAI兼容 | 编码专用，Lite套餐 |
| **GLM Coding Plan Pro** | 订阅制 | `https://open.bigmodel.cn/api/coding/paas/v4` | OpenAI兼容 | 编码专用，Pro套餐 |
| **GLM Coding Plan Max** | 订阅制 | `https://open.bigmodel.cn/api/coding/paas/v4` | OpenAI兼容 | 编码专用，Max套餐 |
| **新用户体验包** | 免费试用 | 同上 | 同上 | 2000万Tokens，3个月 |

#### 最新模型定价 (per 1M tokens)
| 模型 | Input | Output | Context | 备注 |
|------|-------|--------|---------|------|
| **GLM-5.1** | ¥8.00 | ¥28.00 | 198K | 旗舰 |
| **GLM-5** | — | — | 198K | 稳定版 |
| **GLM-5-Turbo** | — | — | 198K | 高速版 |
| **GLM-4.7** | — | — | 200K | 前代 |
| **GLM-4.6V** | — | — | — | 视觉 |

---

### 7. MiniMax

**API文档**: https://platform.minimax.io/docs

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard (国际版)** | 按量付费 | `https://api.minimax.io/v1` | OpenAI兼容 | 国际端点 |
| **Standard (国内版)** | 按量付费 | `https://api.minimaxi.com/v1` | OpenAI兼容 | 国内端点(注意拼写: minimaxi) |
| **Anthropic兼容** | 按量付费 | `https://api.minimaxi.com/anthropic` | Anthropic兼容 | 支持Claude SDK |
| **OpenAI兼容(旧)** | 按量付费 | `https://api.minimaxi.chat/v1` | OpenAI兼容 | 旧端点，注意域名拼写 |

> **注意**: MiniMax国内版域名为 `minimaxi.com` (多一个i)，非 `minimax.com`。

#### 最新模型定价 (per 1M tokens)
| 模型 | Input | Output | Context | 备注 |
|------|-------|--------|---------|------|
| **MiniMax-M2.7** | — | — | 200K | 文本旗舰 |
| **MiniMax-M2.5** | — | — | 200K | 编程SOTA |
| **MiniMax-M2.1** | — | — | 200K | 编程专家 |
| **MiniMax-Hailuo-2.3** | — | — | — | 视频生成 |
| **MiniMax-Speech-2.8** | — | — | — | 语音合成 |
| **Music-2.6** | — | — | — | 音乐生成 |

---

### 8. 科大讯飞 (讯飞星火)

**API文档**: https://www.xfyun.cn/solutions/xinghuoAPI

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard (套餐一~四)** | 按量付费 | `https://spark-api.xf-yun.com/v1` | OpenAI兼容 | 标准端点 |
| **简享包** | 轻量套餐 | 同上 | OpenAI兼容 | 轻量用户 |
| **新用户测试额度** | 免费试用 | 同上 | OpenAI兼容 | 需申请 |

#### 最新模型
- 星火X2-Flash (MoE，30B，256K上下文)
- 星火X2/X1.5
- 星火Ultra
- 星火Max (批推理)
- 星火Pro / Pro-128K

---

### 9. 腾讯 (混元)

**API文档**: https://cloud.tencent.com/document/product/1729

#### Pricing Plans & Base URLs
| Plan名称 | 类型 | Base URL | 协议兼容 | 说明 |
|----------|------|----------|----------|------|
| **Standard (标准按量)** | 按量付费 | `https://hunyuan.tencentcloudapi.com` | 腾讯云原生 | 标准端点 |
| **通用资源包** | 共享额度 | 同上 | 同上 | 10款主力模型共享100万Tokens，1年有效 |
| **Embedding赠送** | 免费额度 | 同上 | 同上 | 额外100万Tokens |
| **Lite版本** | 部分免费 | 同上 | 同上 | 部分模型免费 |

#### 最新模型
- Hunyuan-T1
- Hunyuan-TurboS
- Hunyuan-a13b
- Tencent Vision 1.5 Instruct (视觉)
- Hunyuan-t1-vision
- Hunyuan-large-role
- Hunyuan-translation

---

## 三、第三方聚合平台 (OpenAI兼容)

| 平台 | Base URL | 协议兼容 | 特点 |
|------|----------|----------|------|
| **Together AI** | `https://api.together.ai/v1` | OpenAI兼容 | 开源模型聚合 |
| **OpenRouter** | `https://openrouter.ai/api/v1` | OpenAI兼容 | 多模型路由 |
| **Groq** | `https://api.groq.com/openai/v1` | OpenAI兼容 | 低延迟推理 |
| **SiliconFlow (硅基流动)** | `https://api.siliconflow.cn/v1` | OpenAI兼容 | 国产模型聚合 |
| **Ollama (本地)** | `http://localhost:11434/v1` | OpenAI兼容 | 本地部署 |
| **vLLM (本地)** | Custom | OpenAI兼容 | 自托管 |
| **LiteLLM Proxy** | Custom | OpenAI兼容 | 自托管网关 |

---

## 四、全厂商 Base URL & 协议兼容汇总表

| 厂商 | Plan名称 | Base URL | 协议兼容 | 适用场景 |
|------|----------|----------|----------|----------|
| **OpenAI** | Standard | `https://api.openai.com/v1` | OpenAI原生 | 通用 |
| **Anthropic** | Standard | `https://api.anthropic.com/v1` | Anthropic原生 | 通用 |
| **Anthropic** | OpenAI兼容 | `https://api.anthropic.com/v1` | OpenAI兼容 | 测试/迁移 |
| **Google** | Standard | `https://generativelanguage.googleapis.com/v1beta` | Gemini原生 | 通用 |
| **Google** | OpenAI兼容 | `https://generativelanguage.googleapis.com/v1beta/openai/` | OpenAI兼容 | OpenAI SDK迁移 |
| **xAI** | Standard | `https://api.x.ai/v1` | OpenAI兼容 | 通用 |
| **Mistral** | Standard | `https://api.mistral.ai/v1` | OpenAI兼容 | 通用 |
| **Azure** | Global Standard | `https://{resource}.openai.azure.com/openai/deployments/{deployment}` | OpenAI兼容 | 企业 |
| **AWS** | On-Demand | `https://bedrock-runtime.{region}.amazonaws.com` | Bedrock原生 | 企业 |
| **DeepSeek** | Standard | `https://api.deepseek.com` | OpenAI兼容 | 通用 |
| **DeepSeek** | Anthropic兼容 | `https://api.deepseek.com/anthropic` | Anthropic兼容 | Claude SDK迁移 |
| **Kimi** | Standard | `https://api.moonshot.cn/v1` | OpenAI兼容 | 通用 |
| **Qwen** | Standard | `https://dashscope.aliyuncs.com/compatible-mode/v1` | OpenAI兼容 | 通用 |
| **Qwen** | 原生 | `https://dashscope.aliyuncs.com/api/v1` | 阿里云原生 | 百炼平台 |
| **火山方舟** | Standard | `https://ark.cn-beijing.volces.com/api/v3` | OpenAI兼容 | 通用 |
| **火山方舟** | Coding Plan | `https://ark.cn-beijing.volces.com/api/coding/v3` | OpenAI兼容 | AI编码 |
| **火山方舟** | Coding Plan | `https://ark.cn-beijing.volces.com/api/coding` | Anthropic兼容 | Claude Code |
| **百度千帆** | Standard | `https://qianfan.baidubce.com/v2` | OpenAI兼容 | 通用 |
| **百度千帆** | Coding Plan | `https://qianfan.baidubce.com/v2/coding` | OpenAI兼容 | AI编码 |
| **百度千帆** | Coding Plan | `https://qianfan.baidubce.com/anthropic/coding` | Anthropic兼容 | Claude Code |
| **智谱AI** | Standard | `https://open.bigmodel.cn/api/paas/v4/` | OpenAI兼容 | 通用 |
| **智谱AI** | Coding Plan | `https://open.bigmodel.cn/api/coding/paas/v4` | OpenAI兼容 | AI编码 |
| **MiniMax** | 国际版 | `https://api.minimax.io/v1` | OpenAI兼容 | 国际 |
| **MiniMax** | 国内版 | `https://api.minimaxi.com/v1` | OpenAI兼容 | 国内 |
| **MiniMax** | Anthropic兼容 | `https://api.minimaxi.com/anthropic` | Anthropic兼容 | Claude SDK |
| **讯飞星火** | Standard | `https://spark-api.xf-yun.com/v1` | OpenAI兼容 | 通用 |
| **腾讯混元** | Standard | `https://hunyuan.tencentcloudapi.com` | 腾讯云原生 | 通用 |

---

*所有信息截止2026年5月21日，以各厂商官方最新文档为准。*
