根据您的要求，我已详细调研了各厂商的定价方案（Pricing Plans），并补充了不同套餐对应的 **API 端点 (BaseUrl)** 及 **协议兼容性** 信息。以下为截至 **2026年5月21日** 的完整数据汇总。

### 📊 国内大模型厂商定价与接入详情

| 厂商 | 核心定价模式 | 具体套餐/计费细则 (截至2026.05.21) | 关键价格锚点 (每百万Token) | **API 端点 (BaseUrl)** | **协议兼容性** | 备注/最新动态 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **华为云 (盘古)** | 1. 按Token计费<br>2. 包年/包月订阅<br>3. 按需(时长)计费 | • 自研盘古模型：企业长期商务折扣7折，输入约 **0.8元**，输出约 **2.4元**。<br>• 第三方模型：如DeepSeek-V3.2约 **10元**。新用户赠20元无门槛代金券。<br>• 资源订购：数据、训练、推理资源支持**包年/包月**或**按需(时长)计费**。 | 输入: ¥0.8<br>输出: ¥2.4 | • **V2推理接口 (OpenAI兼容)**：`https://{endpoint}/api/v2/chat/completions`<br>• **V1推理接口 (原生)**：`https://{endpoint}/v1/{project_id}/deployments/{deployment_id}/chat/completions` | 兼容 OpenAI API 规范 | 2026年4月价格未涨，主打国产化低价洼地。API端点需在部署后于控制台获取。 |
| **360智脑** | 1. 按量后付费 (Token)<br>2. 智能路由 (虚拟模型) | • **360zhinao-turbo**：输入 **¥1**，输出 **¥2**，上下文32K。<br>• **DeepSeek-V4**：输入 **¥3**，输出 **¥6**，上下文1M。<br>• **Kimi K2.6**：输入 **¥6.5**，输出 **¥27**。<br>• **GLM-5.1**：输入 **¥6**，输出 **¥24**。 | 各模型差异大，详见模型广场。 | `https://api.360.cn/v1` | 完全兼容 OpenAI API | 平台提供虚拟模型，智能路由至最佳供应商，支持自动容灾。 |
| **网易有道 (子曰)** | 1. 按Token/字符实时付费<br>2. 资源包预付费<br>3. 分层服务包 (体验/标准/企业) | • **子曰翻译Pro模型**：**0.03元/千tokens** (即¥30/百万)。<br>• **子曰翻译Lite模型**：**0.01元/千tokens** (即¥10/百万)。<br>• **资源包**：1000万tokens/90天，**285元**。<br>• **服务包**：体验版(免费)、标准版(¥49/月)、企业版(¥39/月)。 | Pro: ¥30<br>Lite: ¥10 | `https://openapi.youdao.com/llmgateway/api/v1` | 兼容 OpenAI SDK | 聚焦翻译与教育垂类。新用户赠50元体验金。 |
| **商汤 (日日新)** | 1. Token Plan (限时免费)<br>2. 按量后付费<br>3. 多档位套餐 (即将推出) | • **公测期免费**：**¥0/月**，每模型 **1,500次调用/5小时**，支持SenseNova 6.7 Flash-Lite等。<br>• **SenseNova 6.7 Flash-Lite**：Lite档 **0.001元/千Token** (即¥1/百万)，Pro档 **0.008元/千Token** (即¥8/百万)。 | Lite: ¥1<br>Pro: ¥8 | • **Token Plan (免费套餐)**：`https://token.sensenova.cn/v1`<br>• **通用 OpenAI 兼容接口**：`https://api.sensenova.cn/compatible-mode/v2` | 兼容 OpenAI API 规范 | 2026年5月发布Token Plan并开启限时免费活动，后续将推出Lite、Pro等档位。 |
| **月之暗面 (Kimi)** | 1. 按量后付费<br>2. 预付费资源包 | • **Kimi K2.6**：输入 **¥6.5**，输出 **¥27** (通过360平台数据)。<br>• 具体套餐详情需参考其官方平台。 | 输入: ¥6.5<br>输出: ¥27 | • **国内**：`https://api.moonshot.cn/v1`<br>• **境外**：`https://api.moonshot.ai/v1` | 完全兼容 OpenAI API | 以其超长上下文能力著称。 |
| **MiniMax** | 1. 按量后付费<br>2. Platform订阅套餐<br>3. 群聊/Agent专属包 | • **M2.7**等模型具体价格需参考其官方平台文档。 | 待补充 | • **OpenAI 兼容**：`https://api.minimaxi.com/v1`<br>• **Anthropic 兼容 (推荐)**：`https://api.minimaxi.com/anthropic` | 同时兼容 OpenAI 和 Anthropic 协议 | 提供深度思考模式及多Agent协同的专项算力包。 |
| **阶跃星辰 (StepFun)** | 1. 按量计费 (Token/次数)<br>2. 尊享版订阅<br>3. 企业定制合约 | • 具体价格需参考其官方平台文档。 | 待补充 | • **Step Plan (OpenAI 兼容)**：`https://api.stepfun.com/step_plan/v1`<br>• **Step Plan (Anthropic 兼容)**：`https://api.stepfun.com/step_plan`<br>• **通用 API (OpenAI 兼容)**：`https://api.stepfun.com/v1` | 同时兼容 OpenAI 和 Anthropic 协议 | 支持文本、视觉、音频模型，提供更高并发权限的订阅。 |
| **零一万物 (01.AI)** | 1. 按量付费<br>2. 企业级Token Plan<br>3. 私有化部署许可 | • 支持上下文缓存抵扣。具体价格需参考其官方平台。 | 待补充 | • **平台一**：`https://api.lingyiwanwu.com/v1`<br>• **平台二**：`https://api.01.ai/v1` | 高度兼容 OpenAI API | 针对金融、政企提供本地化授权年费。 |
| **智谱AI (GLM)** | 1. GLM Coding Plan (订阅制)<br>2. 按量计费 | • **Coding Plan**：Lite(¥49/月)、Pro(¥149/月)、Max(¥469/月)，限制请求次数。<br>• **GLM-5.1**：输入 **¥6**，输出 **¥24** (通过360平台数据)。 | 输入: ¥6<br>输出: ¥24 | • **通用 API (OpenAI 兼容)**：`https://open.bigmodel.cn/api/paas/v4/`<br>• **Coding Plan 专属端点 (OpenAI 兼容)**：`https://open.bigmodel.cn/api/coding/paas/v4/`<br>• **Coding Plan (Anthropic 兼容)**：`https://open.bigmodel.cn/api/anthropic` | 同时兼容 OpenAI 和 Anthropic 协议 | 订阅制套餐是其特色。 |
| **深度求索 (DeepSeek)** | 1. 按量付费<br>2. 新用户礼包 | • **DeepSeek-V4**：输入 **¥3**，输出 **¥6** (通过360平台数据)。<br>• 新用户赠500万免费Token。 | 输入: ¥3<br>输出: ¥6 | • **OpenAI 兼容**：`https://api.deepseek.com`<br>• **Anthropic 兼容**：`https://api.deepseek.com/anthropic` | 同时兼容 OpenAI 和 Anthropic 协议 | 输入缓存命中价格低至¥0.02/百万Token。 |
| **阿里云 (通义千问)** | 1. Coding Plan<br>2. 按量后付费<br>3. 预付费资源包 | • **Coding Plan**：Lite(¥200/月)、Pro($50/月)。<br>• **Qwen3.6-Plus**：输入 **¥1.4**，输出 **¥8.4** (通过360平台数据)。<br>• 提供按月包年折扣，最高省60%。 | 输入: ¥1.4<br>输出: ¥8.4 | • **OpenAI 兼容接口 (北京)**：`https://dashscope.aliyuncs.com/compatible-mode/v1`<br>• **OpenAI 兼容接口 (弗吉尼亚)**：`https://dashscope-us.aliyuncs.com/compatible-mode/v1`<br>• **OpenAI 兼容接口 (新加坡)**：`https://dashscope-intl.aliyuncs.com/compatible-mode/v1` | 兼容 OpenAI 接口规范 | 提供按模型单元(MU)购买的资源包。 |
| **百度智能云 (文心一言)** | 1. Token福利包<br>2. Coding Plan<br>3. Agent Plan (按次付费) | • **Coding Plan**：Lite版 ¥40/月；Pro版 ¥200/月。<br>• **Agent Plan**：智能搜索生成 **¥0.036/次**。 | 待补充 | `https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions` | 自定义 RESTful API，未明确声明兼容 OpenAI。 | Token福利包为预购积分包，可抵扣调用。 |
| **腾讯云 (混元)** | 1. Hy Token Plan (Agent导向)<br>2. 通用Token Plan<br>3. 后付费 | • **Hy Token Plan**：个人版Lite体验包（3500万Tokens限时特惠）。 | 待补充 | `https://api.hunyuan.cloud.tencent.com/v1` | 兼容 OpenAI 接口规范 | 计划集合多家模型，支持按需切换。 |
| **字节跳动 (豆包)** | 1. Coding Plan<br>2. 按量后付费<br>3. App订阅 | • **Coding Plan**：Lite版（月均1.8万次请求）；Pro版（月均9万次请求）。<br>• **豆包虚拟模型**：输入 **¥0.8**，输出 **¥2** (通过360平台数据)。 | 输入: ¥0.8<br>输出: ¥2 | 待补充 (官方文档未明确给出兼容 OpenAI 的 BaseUrl) | 待补充 | App订阅有标准版(¥68/月)和专业版(¥500/月)。 |

### 🌍 国外大模型厂商定价与接入详情

| 厂商 | 核心定价模式 | 具体套餐/计费细则 (截至2026.05.21) | 关键价格锚点 (每百万Token) | **API 端点 (BaseUrl)** | **协议兼容性** | 备注/最新动态 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **OpenAI** | 1. 按Token计费 (多档位)<br>2. ChatGPT订阅<br>3. Scale Tiers | • 分标准、批量(Batch)、弹性(Flex)、优先(Priority)四档。<br>• **ChatGPT**：Business ($20/月)、Enterprise(按消息点数)。<br>• **Scale Tiers**：专属计算资源 **$750/天/单元**。 | 随模型和档位变化 | `https://api.openai.com/v1` | 原生协议 | GPT-5.5系列已发布。 |
| **Microsoft (Azure AI)** | 1. 按量付费 (Token)<br>2. 预配吞吐量<br>3. 智能体服务 | • 按Token日结/月结，分标准、灵活(Flex)、优先(Priority)档。<br>• 企业级包月/包年订阅，保底算力+超额按量。<br>• 智能体服务按执行次数和工具调用耗时计费。 | 与OpenAI价格联动，略有溢价 | `https://{YOUR-RESOURCE-NAME}.openai.azure.com/openai/v1/` | 兼容 OpenAI API 规范 | 深度集成OpenAI、Meta、Mistral等第三方模型。 |
| **Amazon (AWS Bedrock)** | 1. 按Token计费<br>2. 模型微调/定制<br>3. 批量推理 | • 调用第三方模型（Claude, Llama等）按输入/输出Token付费。<br>• 微调按训练时长（每小时）和存储计费。<br>• 批量推理费用为实时推理的一半。 | 取决于所选模型 | • **OpenAI/Anthropic 兼容接口**：`bedrock-mantle.{region}.api.aws`<br>• **原生 API**：`bedrock-runtime.{region}.amazonaws.com` | 同时支持 OpenAI 兼容接口、Anthropic Messages API 及原生 API | 提供大批量离线任务的经济选择。 |
| **Google (Gemini)** | 1. 免费层<br>2. 付费层<br>3. 企业层 | • 免费层提供有限速率访问。<br>• 付费层提供更高RPM/TPM，支持Context Caching。<br>• 企业层提供专属支持、高级合规和预留吞吐量。 | Gemini 3.5 Flash已正式发布(GA) | `https://generativelanguage.googleapis.com` | 自定义 REST API (非 OpenAI 格式) | 价格未在搜索结果中明确列出。 |
| **Anthropic (Claude)** | 1. 按Token计费<br>2. 终端订阅 | • 支持Prompt Caching（降价90%）和Batch处理（5折）。<br>• 终端订阅：Pro, Max, Team, Enterprise。 | Claude Opus 4.7、Sonnet 4.6已发布 | `https://api.anthropic.com/v1` | 原生 Messages API | 以其强大的推理能力和长上下文著称。 |
| **Meta (Llama)** | 1. 免费开源<br>2. Cloud API<br>3. Llama API | • 权重下载自托管免费。<br>• 通过AWS/Azure等云厂商按Token计费调用。<br>• 官方托管API（具体计费视部署规模而定）。 | 主要通过云厂商定价 | • **官方 API (预览)**：`https://api.llama.com/v1`<br>• **第三方云服务商**：如 Together AI (`https://api.together.xyz/v1`) | 官方 API 兼容 OpenAI 格式 | Llama 4 Maverick/Scout已发布。 |
| **xAI (Grok)** | 1. API 按量计费<br>2. X Premium+ | • 开发者按Token调用Grok模型。<br>• 终端用户通过订阅X(Twitter)高级账号使用。 | 待补充 | • **OpenAI 兼容接口**：`https://api.x.ai/v1`<br>• **gRPC 服务**：`api.x.ai` | 兼容 OpenAI API 规范 | 与X平台深度绑定。 |
| **Mistral AI** | 1. 按量计费<br>2. 企业级合约 | • 按Token数计费，支持批量处理。<br>• 针对私有化部署或专属云资源的年度协议。 | 待补充 | `https://api.mistral.ai/v1` | 兼容 OpenAI API 规范 | Mistral Large 2和Codestral代码模型已发布。 |

### 💡 核心洞察与趋势总结

1.  **协议兼容性成为标配**：绝大多数国内厂商（智谱、阿里、腾讯、360、网易有道、商汤、月之暗面、MiniMax、阶跃星辰、深度求索、零一万物）均提供 **OpenAI 兼容接口**，极大降低了开发者的迁移成本。部分厂商（如智谱、MiniMax、阶跃星辰、深度求索）还额外提供 **Anthropic 兼容接口**，以覆盖更广泛的生态。
2.  **套餐与端点分离**：部分厂商为不同的计费套餐设置了独立的 API 端点。例如：
    *   **智谱AI**：通用 API (`/api/paas/v4/`) 与 **Coding Plan** 专属端点 (`/api/coding/paas/v4/`) 分离。
    *   **商汤**：通用 OpenAI 兼容接口 (`/compatible-mode/v2`) 与 **Token Plan** 免费套餐端点 (`/token.sensenova.cn/v1`) 分离。
    *   **阶跃星辰**：**Step Plan** 订阅用户需使用专属端点 (`/step_plan/v1`)，而非通用端点 (`/v1`)。
3.  **价格透明化与平台化**：以 **360智脑开放平台** 为代表，提供了实时、透明的各模型Token价格和供应商可用性看板，极大方便了比价和选型。
4.  **垂类模型低价策略**：教育、翻译等垂类模型价格显著低于通用大模型。如**网易有道“子曰”翻译模型**低至¥10/百万Token，**商汤Flash-Lite**轻量多模态模型低至¥1/百万Token。
5.  **“Coding Plan”成为B端入口**：**智谱AI、阿里云、百度**等均推出包月制的Coding Plan，以固定价格提供一定量的请求次数，降低了开发者的初始使用门槛和成本不确定性。
6.  **“Token Plan”与免费体验成为获客手段**：**商汤**在2026年5月高调推出 **限时免费Token Plan**（每5小时1500次调用），旨在快速吸引开发者生态。**华为云、网易有道**也为新用户提供无门槛代金券或体验金。
7.  **企业级套餐与合规性**：**华为云**强调企业长期商务折扣（7折）和完整的国产化合规体系。**网易有道**提供不同QPS和日志留存期的标准版、企业版服务包。
8.  **计费模式多元化**：除了按Token计费这一主流方式，**华为云**还保留着面向算力资源的**包年/包月**和**按需(时长)计费**。**百度**则创新性地推出了按次付费的**Agent Plan**。

> **请注意**：以上价格及API端点信息主要基于截至2026年5月21日的网络公开资料，部分厂商（如阶跃星辰、零一万物、字节跳动豆包）的详细价目表未在搜索结果中直接呈现。实际计费可能因促销活动、商务谈判等因素变动，建议在接入前查阅各厂商官方定价页面以获取最准确信息。