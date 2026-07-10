// 版本对比表结构化数据。standard/enterprise 为 true 表示该版本具备该能力。
export interface DiffItem {
  name: string;
  standard: boolean;
  enterprise: boolean;
}
export interface DiffCategory {
  category: string;
  items: DiffItem[];
}

export const editionDiff: DiffCategory[] = [
  {
    category: 'API 网关',
    items: [
      { name: 'OpenAI 兼容端点（/v1/chat/completions、/v1/completions）', standard: true, enterprise: true },
      { name: 'Anthropic 兼容端点（/v1/messages）', standard: true, enterprise: true },
      { name: 'SSE 流式转发（首 token ≤100ms）', standard: true, enterprise: true },
      { name: '协议转换（OpenAI ↔ Anthropic 互转）', standard: true, enterprise: true },
      { name: '图像生成端点（/v1/images/generations）', standard: true, enterprise: true },
      { name: '语音合成端点（/v1/audio/speech）', standard: true, enterprise: true },
      { name: '语音识别端点（/v1/audio/transcriptions）', standard: true, enterprise: true },
      { name: '内容审核端点（/v1/moderations）', standard: true, enterprise: true },
    ],
  },
  {
    category: 'Provider 管理',
    items: [
      { name: 'Provider CRUD', standard: true, enterprise: true },
      { name: '多 Key 管理（自动轮换、调度、故障切换）', standard: true, enterprise: true },
      { name: '默认 Key 设置', standard: true, enterprise: true },
      { name: 'Key 统计展示', standard: true, enterprise: true },
      { name: '负载均衡（优先级 + 权重）', standard: true, enterprise: true },
      { name: '渠道分组', standard: true, enterprise: true },
      { name: '渠道级故障转移', standard: true, enterprise: true },
      { name: '熔断超时（防雪崩）', standard: true, enterprise: true },
      { name: '代理配置（HTTP/S、Socket5）', standard: false, enterprise: true },
    ],
  },
  {
    category: '路由',
    items: [
      { name: '模型级智能降级', standard: true, enterprise: true },
      { name: '场景路由（CODE/CREATIVE/SUMMARY 等）', standard: true, enterprise: true },
      { name: '模型别名映射', standard: true, enterprise: true },
      { name: '可视化策略编排', standard: false, enterprise: true },
      { name: '自定义脚本扩展', standard: false, enterprise: true },
    ],
  },
  {
    category: '用户与认证',
    items: [
      { name: '用户 CRUD', standard: true, enterprise: true },
      { name: '用户名密码登录/登出', standard: true, enterprise: true },
      { name: 'OAuth 登录（GitHub、Gitee、QQ、企业微信）', standard: true, enterprise: true },
      { name: '企业 OAuth（飞书、钉钉、GitHub Enterprise）', standard: false, enterprise: true },
    ],
  },
  {
    category: '密钥管理',
    items: [
      { name: 'API Key CRUD', standard: true, enterprise: true },
      { name: '额度限制', standard: true, enterprise: true },
      { name: '模型白名单', standard: true, enterprise: true },
      { name: 'IP 限制', standard: true, enterprise: true },
      { name: '过期时间', standard: true, enterprise: true },
    ],
  },
  {
    category: 'Token 计量与配额',
    items: [
      { name: 'Token 计量（输入/输出分别统计）', standard: true, enterprise: true },
      { name: 'Token 限额（用户级/API Key 级）', standard: true, enterprise: true },
      { name: '用户×渠道限额', standard: false, enterprise: true },
      { name: '请求次数配额', standard: false, enterprise: true },
    ],
  },
  {
    category: '安全与风控',
    items: [
      { name: '认证中间件（Token 验证）', standard: true, enterprise: true },
      { name: 'IP 白/黑名单', standard: true, enterprise: true },
      { name: 'UA 过滤', standard: true, enterprise: true },
      { name: 'PII 脱敏（手机号、身份证、邮箱、银行卡等）', standard: true, enterprise: true },
      { name: '数据掩码策略', standard: true, enterprise: true },
      { name: '审计日志', standard: true, enterprise: true },
      { name: '密钥加密存储（AES-256）', standard: true, enterprise: true },
      { name: '国密算法（SM2/SM3/SM4）', standard: false, enterprise: true },
      { name: '完整审计链（WORM）', standard: false, enterprise: true },
    ],
  },
  {
    category: '可观测性',
    items: [
      { name: 'Trace ID（全链路追踪）', standard: true, enterprise: true },
      { name: '结构化日志（JSON 格式）', standard: true, enterprise: true },
      { name: '实时指标（延迟/QPS/Token/费用）', standard: true, enterprise: true },
      { name: 'Prometheus 导出', standard: true, enterprise: true },
      { name: 'Grafana 仪表盘', standard: false, enterprise: true },
      { name: 'Jaeger 追踪', standard: false, enterprise: true },
    ],
  },
  {
    category: '高级能力',
    items: [
      { name: '语义缓存（相似请求命中，降本 30%+）', standard: false, enterprise: true },
      { name: 'MCP 协议（Resources/Prompts/Tools）', standard: false, enterprise: true },
    ],
  },
  {
    category: '部署',
    items: [
      { name: '单机部署', standard: true, enterprise: true },
      { name: 'Kubernetes 分布式部署', standard: false, enterprise: true },
      { name: '高可用集群', standard: false, enterprise: true },
    ],
  },
  {
    category: '支持',
    items: [
      { name: '社区支持', standard: true, enterprise: true },
      { name: '商业 SLA 保障', standard: false, enterprise: true },
      { name: '专属技术支持', standard: false, enterprise: true },
    ],
  },
];
