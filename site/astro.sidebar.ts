// 企业版 badge：Starlight 原生，variant 'success' = 绿色。
const enterpriseBadge = { text: '企业版', variant: 'success' as const };

// 8 大能力域分组 + 快速开始 + 高级能力 + 模型广场 + 参考。
// 全部条目均渲染（不做条件隐藏）；企业版专属项附 badge。
// 类型由 astro.config.ts 的 starlight({ sidebar }) 推断，无需显式导入 SidebarItem。
export const sidebar = [
  {
    label: '快速开始',
    collapsed: true,
    items: [
      { label: '快速开始', slug: 'quickstart' },
      { label: '安装部署', slug: 'deploy' },
    ],
  },
  {
    label: 'API 网关',
    collapsed: true,
    items: [
      { label: 'API 网关概览', slug: 'api-gateway' },
      { label: 'API 参考', slug: 'api/api-spec' },
      { label: '调用示例', slug: 'api/examples' },
    ],
  },
  {
    label: 'Provider 管理',
    collapsed: true,
    items: [
      { label: 'Provider 管理概览', slug: 'provider-management' },
      { label: '代理配置', slug: 'provider-management', badge: enterpriseBadge },
    ],
  },
  {
    label: '路由',
    collapsed: true,
    items: [
      { label: '路由设计', slug: 'features/routing' },
    ],
  },
  {
    label: '用户与认证',
    collapsed: true,
    items: [
      { label: '用户与认证概览', slug: 'auth' },
    ],
  },
  {
    label: '密钥管理',
    collapsed: true,
    items: [
      { label: '密钥管理概览', slug: 'apikey-management' },
    ],
  },
  {
    label: 'Token 计量与配额',
    collapsed: true,
    items: [
      { label: 'Token 计量与配额概览', slug: 'token-quota' },
    ],
  },
  {
    label: '安全与风控',
    collapsed: true,
    items: [
      { label: '安全与风控概览', slug: 'security' },
      { label: '容灾方案', slug: 'features/resilience' },
    ],
  },
  {
    label: '可观测性',
    collapsed: true,
    items: [
      { label: '可观测性概览', slug: 'observability' },
    ],
  },
  {
    label: '高级能力',
    collapsed: true,
    items: [
      { label: '语义缓存', slug: 'features/semantic-cache', badge: enterpriseBadge },
      { label: 'MCP 协议', slug: 'features/mcp-protocol', badge: enterpriseBadge },
    ],
  },
  {
    label: '模型广场',
    collapsed: true,
    items: [
      { label: '模型广场', slug: 'features/model-plaza' },
    ],
  },
  {
    label: '参考',
    collapsed: true,
    items: [
      { label: '功能特性总览', slug: 'features' },
      { label: '配置项参考', slug: 'reference/config' },
      { label: '常见问题', slug: 'faq' },
      { label: '故障排查', slug: 'troubleshooting' },
      { label: '升级指南', slug: 'migration' },
      { label: '更新日志', slug: 'changelog' },
      { label: 'SDK 使用', slug: 'sdk' },
    ],
  },
];
