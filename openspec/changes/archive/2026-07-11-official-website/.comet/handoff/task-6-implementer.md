## Task 6: 首页

**Files:**
- Create: `site/src/data/i18n/home.ts`
- Create: `site/src/data/homeFeatures.ts`
- Create: `site/src/data/homeProducts.ts`
- Create: `site/src/data/homeEcosystem.ts`
- Create: `site/src/components/Hero.astro`
- Create: `site/src/components/ProductCard.astro`
- Create: `site/src/components/FeatureCard.astro`
- Create: `site/src/components/EcosystemCard.astro`
- Create: `site/src/components/Carousel.astro`
- Create: `site/src/pages/index.astro`
- Create: `site/src/pages/en/index.astro`

**目标：** 对标 thingsboard.io 的首页区块顺序：Hero -> 信任背书 -> 四差异化 -> 能力叙事 -> 三产品卡 -> 生态组件 -> 控制台轮播（P0 占位图）-> 8 能力域功能网格 -> 底部 CTA。中英双语。

- [ ] **Step 1: 创建 `site/src/data/i18n/home.ts`（首页中英文案字典）**

```ts
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
      security: 'Security & compliance: auth, rate-limit, masking, audit — four-layer checks, encrypted keys.',
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
```

- [ ] **Step 2: 创建 `site/src/data/homeFeatures.ts`（8 能力域功能卡数据）**

```ts
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
```

- [ ] **Step 3: 创建 `site/src/data/homeProducts.ts` 与 `homeEcosystem.ts`**

`site/src/data/homeProducts.ts`：

```ts
// 三产品卡数据（中英 label 由 i18n/home.ts 提供，此处仅结构）。
export interface ProductCardData {
  key: 'standard' | 'cloud' | 'enterprise';
}

export const homeProducts: ProductCardData[] = [
  { key: 'standard' },
  { key: 'cloud' },
  { key: 'enterprise' },
];
```

`site/src/data/homeEcosystem.ts`：

```ts
// 生态组件数据。
export interface EcosystemItem {
  name: string;
  desc: string;
}

export const homeEcosystem: EcosystemItem[] = [
  { name: 'PostgreSQL', desc: '主数据库 + pgvector 向量存储' },
  { name: 'Redis', desc: '分布式缓存与限流' },
  { name: 'OpenTelemetry', desc: '全链路追踪标准' },
  { name: 'Prometheus', desc: '指标采集与导出' },
  { name: 'Spring Boot 3.5', desc: '后端框架（Java 21）' },
  { name: 'Cloudflare Pages', desc: '官网与文档部署' },
];
```

- [ ] **Step 4: 创建 `site/src/components/Hero.astro`**

```astro
---
interface Props {
  title: string;
  subtitle: string;
  cta: string;
  secondaryCta: string;
}
const { title, subtitle, cta, secondaryCta } = Astro.props;
---
<section class="hero">
  <h1>{title}</h1>
  <p class="subtitle">{subtitle}</p>
  <div class="cta-group">
    <a href="/docs/" class="btn primary">{cta}</a>
    <a href="/standard-vs-enterprise/" class="btn secondary">{secondaryCta}</a>
  </div>
</section>

<style>
  .hero { text-align: center; padding: 80px 0 48px; }
  .hero h1 { font-size: 2.5rem; line-height: 1.3; max-width: 900px; margin: 0 auto 16px; }
  .subtitle { font-size: 1.15rem; color: #6b7280; max-width: 700px; margin: 0 auto 32px; }
  .cta-group { display: flex; gap: 16px; justify-content: center; }
  .btn { padding: 12px 28px; border-radius: 8px; text-decoration: none; font-weight: 600; }
  .btn.primary { background: var(--brand-primary, #6653e3); color: #fff; }
  .btn.secondary { border: 1px solid var(--brand-primary, #6653e3); color: var(--brand-primary, #6653e3); }
</style>
```

- [ ] **Step 5: 创建 `site/src/components/ProductCard.astro`、`FeatureCard.astro`、`EcosystemCard.astro`、`Carousel.astro`**

`ProductCard.astro`：

```astro
---
interface Props { name: string; desc: string; href: string; }
const { name, desc, href } = Astro.props;
---
<a href={href} class="product-card">
  <h3>{name}</h3>
  <p>{desc}</p>
</a>

<style>
  .product-card { display: block; padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; text-decoration: none; color: inherit; transition: box-shadow .2s; }
  .product-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,.08); }
  .product-card h3 { margin: 0 0 8px; font-size: 1.2rem; }
  .product-card p { margin: 0; color: #6b7280; font-size: 0.95rem; }
</style>
```

`FeatureCard.astro`：

```astro
---
interface Props { title: string; desc: string; href: string; }
const { title, desc, href } = Astro.props;
---
<a href={href} class="feature-card">
  <h4>{title}</h4>
  <p>{desc}</p>
</a>

<style>
  .feature-card { display: block; padding: 20px; border: 1px solid #e5e7eb; border-radius: 8px; text-decoration: none; color: inherit; }
  .feature-card:hover { border-color: var(--brand-primary, #6653e3); }
  .feature-card h4 { margin: 0 0 6px; font-size: 1rem; }
  .feature-card p { margin: 0; color: #6b7280; font-size: 0.875rem; }
</style>
```

`EcosystemCard.astro`：

```astro
---
interface Props { name: string; desc: string; }
const { name, desc } = Astro.props;
---
<div class="ecosystem-card">
  <strong>{name}</strong>
  <span>{desc}</span>
</div>

<style>
  .ecosystem-card { padding: 16px; border: 1px solid #e5e7eb; border-radius: 8px; display: flex; flex-direction: column; gap: 4px; }
  .ecosystem-card strong { font-size: 0.95rem; }
  .ecosystem-card span { color: #6b7280; font-size: 0.8rem; }
</style>
```

`Carousel.astro`（P0 占位）：

```astro
---
interface Props { title: string; placeholder: string; }
const { title, placeholder } = Astro.props;
---
<section class="carousel">
  <h2>{title}</h2>
  <div class="placeholder">{placeholder}</div>
</section>

<style>
  .carousel { padding: 48px 0; text-align: center; }
  .placeholder { margin: 24px auto; max-width: 800px; height: 300px; display: flex; align-items: center; justify-content: center; background: #f3f4f6; border-radius: 12px; color: #9ca3af; }
</style>
```

- [ ] **Step 6: 创建 `site/src/pages/index.astro`（中文首页）**

```astro
---
import BaseLayout from '../layouts/BaseLayout.astro';
import Hero from '../components/Hero.astro';
import ProductCard from '../components/ProductCard.astro';
import FeatureCard from '../components/FeatureCard.astro';
import EcosystemCard from '../components/EcosystemCard.astro';
import Carousel from '../components/Carousel.astro';
import { home } from '../data/i18n/home.ts';
import { homeFeatures } from '../data/homeFeatures.ts';
import { homeProducts } from '../data/homeProducts.ts';
import { homeEcosystem } from '../data/homeEcosystem.ts';

const t = home.zh;
const products = homeProducts.map((p) => ({
  ...home.zh.products.items.find((i, idx) => idx === homeProducts.indexOf(p)),
  ...p,
}));
---
<BaseLayout title={`${t.hero.title} · LLM-Gateway`} description={t.hero.subtitle}>
  <Hero title={t.hero.title} subtitle={t.hero.subtitle} cta={t.hero.cta} secondaryCta={t.hero.secondaryCta} />

  <section class="trust"><h3>{t.trust.title}</h3></section>

  <section class="differentiators">
    {t.differentiators.map((d) => (
      <div class="diff-item"><h4>{d.title}</h4><p>{d.desc}</p></div>
    ))}
  </section>

  <section class="narrative">
    <h2>{t.narrative.title}</h2>
    <p>{t.narrative.dualProtocol}</p>
    <p>{t.narrative.routing}</p>
    <p>{t.narrative.security}</p>
  </section>

  <section class="products">
    <h2>{t.products.title}</h2>
    <div class="grid-3">
      {t.products.items.map((p) => <ProductCard name={p.name} desc={p.desc} href={p.href} />)}
    </div>
  </section>

  <section class="ecosystem">
    <h2>{t.ecosystem.title}</h2>
    <div class="grid-3">
      {homeEcosystem.map((e) => <EcosystemCard name={e.name} desc={e.desc} />)}
    </div>
  </section>

  <Carousel title={t.console.title} placeholder={t.console.placeholder} />

  <section class="features">
    <h2>{t.features.title}</h2>
    <div class="grid-4">
      {homeFeatures.map((f) => <FeatureCard title={f.title} desc={f.desc} href={f.href} />)}
    </div>
  </section>

  <section class="bottom-cta">
    <h2>{t.cta.title}</h2>
    <a href="/docs/" class="btn primary">{t.cta.button}</a>
  </section>
</BaseLayout>

<style>
  .trust { text-align: center; padding: 24px 0; color: #9ca3af; }
  .differentiators { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; padding: 32px 0; }
  .diff-item h4 { margin: 0 0 8px; color: var(--brand-primary, #6653e3); }
  .diff-item p { margin: 0; font-size: 0.875rem; color: #6b7280; }
  .narrative, .products, .ecosystem, .features { padding: 32px 0; }
  .narrative h2, .products h2, .ecosystem h2, .features h2 { font-size: 1.5rem; margin-bottom: 24px; }
  .grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
  .grid-4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
  .bottom-cta { text-align: center; padding: 64px 0; }
  .bottom-cta .btn { display: inline-block; padding: 12px 28px; border-radius: 8px; background: var(--brand-primary, #6653e3); color: #fff; text-decoration: none; font-weight: 600; }
  @media (max-width: 768px) { .differentiators, .grid-3, .grid-4 { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 7: 创建 `site/src/pages/en/index.astro`（英文首页镜像）**

结构与中文首页一致，`const t = home.en;`，导航 href 已在 `home.en` 字典中带 `/en/` 前缀。完整内容：

```astro
---
import BaseLayout from '../../layouts/BaseLayout.astro';
import Hero from '../../components/Hero.astro';
import ProductCard from '../../components/ProductCard.astro';
import FeatureCard from '../../components/FeatureCard.astro';
import EcosystemCard from '../../components/EcosystemCard.astro';
import Carousel from '../../components/Carousel.astro';
import { home } from '../../data/i18n/home.ts';
import { homeFeatures } from '../../data/homeFeatures.ts';
import { homeEcosystem } from '../../data/homeEcosystem.ts';

const t = home.en;
---
<BaseLayout title={`${t.hero.title} · LLM-Gateway`} description={t.hero.subtitle}>
  <Hero title={t.hero.title} subtitle={t.hero.subtitle} cta={t.hero.cta} secondaryCta={t.hero.secondaryCta} />

  <section class="trust"><h3>{t.trust.title}</h3></section>

  <section class="differentiators">
    {t.differentiators.map((d) => (
      <div class="diff-item"><h4>{d.title}</h4><p>{d.desc}</p></div>
    ))}
  </section>

  <section class="narrative">
    <h2>{t.narrative.title}</h2>
    <p>{t.narrative.dualProtocol}</p>
    <p>{t.narrative.routing}</p>
    <p>{t.narrative.security}</p>
  </section>

  <section class="products">
    <h2>{t.products.title}</h2>
    <div class="grid-3">
      {t.products.items.map((p) => <ProductCard name={p.name} desc={p.desc} href={p.href} />)}
    </div>
  </section>

  <section class="ecosystem">
    <h2>{t.ecosystem.title}</h2>
    <div class="grid-3">
      {homeEcosystem.map((e) => <EcosystemCard name={e.name} desc={e.desc} />)}
    </div>
  </section>

  <Carousel title={t.console.title} placeholder={t.console.placeholder} />

  <section class="features">
    <h2>{t.features.title}</h2>
    <div class="grid-4">
      {homeFeatures.map((f) => <FeatureCard title={f.title} desc={f.desc} href={f.href} />)}
    </div>
  </section>

  <section class="bottom-cta">
    <h2>{t.cta.title}</h2>
    <a href="/en/docs/" class="btn primary">{t.cta.button}</a>
  </section>
</BaseLayout>

<style>
  .trust { text-align: center; padding: 24px 0; color: #9ca3af; }
  .differentiators { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; padding: 32px 0; }
  .diff-item h4 { margin: 0 0 8px; color: var(--brand-primary, #6653e3); }
  .diff-item p { margin: 0; font-size: 0.875rem; color: #6b7280; }
  .narrative, .products, .ecosystem, .features { padding: 32px 0; }
  .narrative h2, .products h2, .ecosystem h2, .features h2 { font-size: 1.5rem; margin-bottom: 24px; }
  .grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
  .grid-4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
  .bottom-cta { text-align: center; padding: 64px 0; }
  .bottom-cta .btn { display: inline-block; padding: 12px 28px; border-radius: 8px; background: var(--brand-primary, #6653e3); color: #fff; text-decoration: none; font-weight: 600; }
  @media (max-width: 768px) { .differentiators, .grid-3, .grid-4 { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 8: 验证首页中英双语渲染**

Run:
```bash
cd site && pnpm dev
```
Expected:
- `http://localhost:4321/` 显示中文首页，依次呈现 Hero、信任背书、四差异化、能力叙事、三产品卡、生态组件、控制台占位、8 能力域功能网格、底部 CTA。
- `http://localhost:4321/en/` 显示英文首页，文案为英文，区块结构一致。
- 功能网格卡片链接指向 `/docs/*`，可跳转。

Ctrl+C 停止。

- [ ] **Step 9: Commit**

```bash
git add site/src/data/ site/src/components/ site/src/pages/index.astro site/src/pages/en/index.astro
git commit -m "feat(site): 首页中英双语（8 区块结构+数据化文案）"
```

---

