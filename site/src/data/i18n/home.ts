// 首页全部文案，中英双语，组件按 locale 取值。
export const home = {
  zh: {
    hero: {
      title: '企业级大模型网关 · 更合规、更安全、更智能、更易用',
      subtitle: 'OpenAI / Anthropic 双标准 API，统一接入 50+ 主流大模型，智能路由、语义缓存、全链路可观测。',
      cta: '快速开始',
      secondaryCta: '版本对比',
    },
    trust: { title: '已被多家团队信赖' },
    differentiators: [
      { title: '更易用', desc: '开箱即用（预置模板）+ 零学习成本（OpenAI 兼容）+ 运维友好（智能诊断）' },
      { title: '更合规', desc: '国密算法（SM2/SM3/SM4）+ 完整审计链（WORM）+ 等保 2.0 三级合规' },
      { title: '更安全', desc: 'Prompt 注入防护 + PII 智能脱敏 + 内容安全审核 + 零信任架构' },
      { title: '更智能', desc: '智能降级（业务连续性）+ 语义缓存（降本 30%+）+ 场景路由' },
    ],
    narrative: {
      title: '能力叙事',
      dualProtocol: '双协议：同时支持 OpenAI 与 Anthropic API 标准，协议互转零成本。',
      routing: '智能路由与降级：额度不足或模型不可用时自动切换，保障业务连续性。',
      security: '安全合规：认证、限流、脱敏、审计四层检查，密钥加密存储。',
    },
    products: {
      title: '三产品形态',
      items: [
        { name: '标准版', desc: 'Apache-2.0 开源，单机即可起步', href: '/docs/' },
        { name: '托管云', desc: '免运维的云托管服务（即将推出）', href: '/contact-us/' },
        { name: '企业版', desc: '分布式部署 + 高级安全 + 商业支持', href: '/standard-vs-enterprise/' },
      ],
    },
    ecosystem: { title: '生态组件' },
    console: { title: '控制台预览', placeholder: '控制台截图（P0 占位，待补充真实截图）' },
    features: { title: '8 大能力域' },
    cta: { title: '开始使用 LLM-Gateway', button: '查看文档' },
  },
  en: {
    hero: {
      title: 'Enterprise LLM Gateway · Compliant, Secure, Intelligent, Easy',
      subtitle: 'OpenAI / Anthropic dual-standard APIs, unified access to 50+ models, smart routing, semantic cache, full observability.',
      cta: 'Get Started',
      secondaryCta: 'Compare Editions',
    },
    trust: { title: 'Trusted by teams' },
    differentiators: [
      { title: 'Easier', desc: 'Out-of-the-box templates + zero learning curve (OpenAI-compatible) + ops-friendly diagnostics' },
      { title: 'More Compliant', desc: 'SM2/SM3/SM4 crypto + WORM audit chain + MLPS 2.0 Level 3' },
      { title: 'More Secure', desc: 'Prompt injection defense + PII masking + content moderation + zero-trust' },
      { title: 'Smarter', desc: 'Smart failover + semantic cache (30%+ savings) + scenario routing' },
    ],
    narrative: {
      title: 'Capabilities',
      dualProtocol: 'Dual protocol: OpenAI and Anthropic API standards, zero-cost protocol conversion.',
      routing: 'Smart routing & failover: auto-switch on quota exhaustion or model unavailability.',
      security: 'Security & compliance: auth, rate-limit, masking, audit - four-layer checks, encrypted keys.',
    },
    products: {
      title: 'Three Editions',
      items: [
        { name: 'Standard', desc: 'Apache-2.0 open-source, single-node to start', href: '/en/docs/' },
        { name: 'Managed Cloud', desc: 'Managed cloud service (coming soon)', href: '/en/contact-us/' },
        { name: 'Enterprise', desc: 'Distributed deployment + advanced security + support', href: '/en/standard-vs-enterprise/' },
      ],
    },
    ecosystem: { title: 'Ecosystem' },
    console: { title: 'Console Preview', placeholder: 'Console screenshots (placeholder, real shots pending)' },
    features: { title: '8 Capability Domains' },
    cta: { title: 'Start with LLM-Gateway', button: 'View Docs' },
  },
} as const;
