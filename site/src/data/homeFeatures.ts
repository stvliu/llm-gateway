// 首页 8 能力域功能网格卡片数据（含跳转文档 slug）。
export interface FeatureCardData {
  title: string;
  desc: string;
  href: string;
  enterprise?: boolean;
}

export const homeFeatures: FeatureCardData[] = [
  { title: 'API 网关', desc: 'OpenAI / Anthropic 双标准端点，SSE 流式，协议互转', href: '/docs/api-gateway/' },
  { title: 'Provider 管理', desc: '多 Key 轮换、负载均衡、熔断故障转移', href: '/docs/provider-management/' },
  { title: '路由', desc: '模型级智能降级、场景路由、别名映射', href: '/docs/features/routing/' },
  { title: '用户与认证', desc: '用户 CRUD、OAuth 登录、企业 OAuth', href: '/docs/auth/' },
  { title: '密钥管理', desc: 'API Key CRUD、额度限制、模型白名单、IP 限制', href: '/docs/apikey-management/' },
  { title: 'Token 计量与配额', desc: '输入/输出分别统计、二级预算控制', href: '/docs/token-quota/' },
  { title: '安全与风控', desc: 'PII 脱敏、内容审核、密钥加密、国密', href: '/docs/security/' },
  { title: '可观测性', desc: 'Trace ID 全链路、结构化日志、Prometheus', href: '/docs/observability/' },
];
