## Task 5: 版本对比页

**Files:**
- Create: `site/src/data/editionDiff.ts`
- Create: `site/src/data/i18n/editionDiff.ts`
- Create: `site/src/pages/standard-vs-enterprise/index.astro`
- Create: `site/src/pages/en/standard-vs-enterprise/index.astro`
- Create: `site/src/layouts/BaseLayout.astro`

**目标：** `/standard-vs-enterprise/`（中文）+ `/en/standard-vs-enterprise/`（英文）双页，对比表覆盖 README 全部 ✅/🔒 功能项（API 网关/Provider 管理/路由/用户认证/密钥管理/Token 配额/安全风控/可观测性/高级能力/部署/支持），含迁移路径段。

- [ ] **Step 1: 创建 `site/src/data/editionDiff.ts`（版本对比表结构化数据）**

数据覆盖 README 第 41-118 行全部 ✅/🔒 项，以及部署/支持类别。

```ts
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
```

- [ ] **Step 2: 创建 `site/src/data/i18n/editionDiff.ts`（对比页中英文案）**

```ts
// 版本对比页中英文案字典。
export const editionDiffI18n = {
  zh: {
    title: '标准版 vs 企业版',
    intro: 'LLM-Gateway 提供 Apache-2.0 开源标准版与企业版。企业版是标准版的超集，新增高级安全、可观测性与运维能力。',
    standard: '标准版',
    enterprise: '企业版',
    feature: '功能',
    standardDesc: '开源免费，单机即可起步，覆盖全部核心网关能力。',
    enterpriseDesc: '标准版全部能力 + 高级安全、分布式部署、商业支持。',
    migrationTitle: '迁移路径',
    migrationBody: '标准版起步后可平滑升级企业版：将配置 `llm-gateway.edition` 从 `standard` 切换为 `enterprise`，数据完全兼容，无需迁移。企业版专属功能（语义缓存、MCP、国密、WORM 等）通过配置开关启用。',
    getStarted: '如何开始',
    getStartedBody: '从标准版开源仓库开始，业务增长后联系团队获取企业版授权与部署支持。',
    contactCta: '联系我们',
  },
  en: {
    title: 'Standard vs Enterprise',
    intro: 'LLM-Gateway ships as an Apache-2.0 open-source Standard edition and an Enterprise edition. Enterprise is a superset adding advanced security, observability and ops.',
    standard: 'Standard',
    enterprise: 'Enterprise',
    feature: 'Feature',
    standardDesc: 'Free and open-source, single-node to start, covering all core gateway capabilities.',
    enterpriseDesc: 'Everything in Standard plus advanced security, distributed deployment and commercial support.',
    migrationTitle: 'Migration Path',
    migrationBody: 'Start on Standard and upgrade smoothly: flip `llm-gateway.edition` from `standard` to `enterprise`. Data is fully compatible, no migration needed. Enterprise-only features (semantic cache, MCP, SM crypto, WORM) are toggled via config.',
    getStarted: 'Getting Started',
    getStartedBody: 'Begin with the open-source Standard repo; as you grow, contact us for Enterprise licensing and deployment support.',
    contactCta: 'Contact Us',
  },
} as const;
```

- [ ] **Step 3: 创建 `site/src/layouts/BaseLayout.astro`（营销页基础布局）**

营销页（首页/对比页/联系页）共用此布局，复用自定义 Header/Footer。

```astro
---
import Header from '../components/starlight/Header.astro';
import Footer from '../components/starlight/Footer.astro';
import '../styles/global.scss';
interface Props {
  title: string;
  description?: string;
}
const { title, description } = Astro.props;
---
<!doctype html>
<html lang={Astro.url.pathname.startsWith('/en/') ? 'en' : 'zh-CN'}>
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>{title}</title>
    {description && <meta name="description" content={description} />}
    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
  </head>
  <body>
    <Header />
    <main class="container">
      <slot />
    </main>
    <Footer />
  </body>
</html>
```

- [ ] **Step 4: 创建 `site/src/pages/standard-vs-enterprise/index.astro`（中文对比页）**

```astro
---
import BaseLayout from '../../layouts/BaseLayout.astro';
import { editionDiff } from '../../data/editionDiff.ts';
import { editionDiffI18n } from '../../data/i18n/editionDiff.ts';

const t = editionDiffI18n.zh;
---
<BaseLayout title={`${t.title} · LLM-Gateway`} description={t.intro}>
  <section class="edition-hero">
    <h1>{t.title}</h1>
    <p class="intro">{t.intro}</p>
  </section>

  <section class="edition-cards">
    <div class="card standard">
      <h2>{t.standard}</h2>
      <p>{t.standardDesc}</p>
    </div>
    <div class="card enterprise">
      <h2>{t.enterprise}</h2>
      <p>{t.enterpriseDesc}</p>
    </div>
  </section>

  <section class="edition-table">
    <table>
      <thead>
        <tr>
          <th>{t.feature}</th>
          <th>{t.standard}</th>
          <th>{t.enterprise}</th>
        </tr>
      </thead>
      <tbody>
        {editionDiff.map((cat) => (
          <>
            <tr class="category-row">
              <th colspan="3">{cat.category}</th>
            </tr>
            {cat.items.map((item) => (
              <tr>
                <td>{item.name}</td>
                <td>{item.standard ? '✅' : '—'}</td>
                <td>{item.enterprise ? '✅' : '—'}</td>
              </tr>
            ))}
          </>
        ))}
      </tbody>
    </table>
  </section>

  <section class="edition-migration">
    <h2>{t.migrationTitle}</h2>
    <p>{t.migrationBody}</p>
  </section>

  <section class="edition-start">
    <h2>{t.getStarted}</h2>
    <p>{t.getStartedBody}</p>
    <a href="/contact-us/" class="cta">{t.contactCta}</a>
  </section>
</BaseLayout>

<style>
  .edition-hero { padding: 48px 0 24px; }
  .edition-hero h1 { font-size: 2rem; }
  .edition-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin: 24px 0; }
  .card { padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; }
  .card.enterprise { border-color: var(--enterprise-badge, #16a34a); }
  .edition-table { overflow-x: auto; margin: 32px 0; }
  table { width: 100%; border-collapse: collapse; }
  th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
  .category-row th { background: #f9fafb; font-size: 0.95rem; }
  .cta { display: inline-block; margin-top: 12px; padding: 10px 20px; background: var(--brand-primary, #6653e3); color: #fff; border-radius: 8px; text-decoration: none; }
</style>
```

- [ ] **Step 5: 创建 `site/src/pages/en/standard-vs-enterprise/index.astro`（英文镜像）**

```astro
---
import BaseLayout from '../../../layouts/BaseLayout.astro';
import { editionDiff } from '../../../data/editionDiff.ts';
import { editionDiffI18n } from '../../../data/i18n/editionDiff.ts';

const t = editionDiffI18n.en;
---
<BaseLayout title={`${t.title} · LLM-Gateway`} description={t.intro}>
  <section class="edition-hero">
    <h1>{t.title}</h1>
    <p class="intro">{t.intro}</p>
  </section>

  <section class="edition-cards">
    <div class="card standard">
      <h2>{t.standard}</h2>
      <p>{t.standardDesc}</p>
    </div>
    <div class="card enterprise">
      <h2>{t.enterprise}</h2>
      <p>{t.enterpriseDesc}</p>
    </div>
  </section>

  <section class="edition-table">
    <table>
      <thead>
        <tr>
          <th>{t.feature}</th>
          <th>{t.standard}</th>
          <th>{t.enterprise}</th>
        </tr>
      </thead>
      <tbody>
        {editionDiff.map((cat) => (
          <>
            <tr class="category-row">
              <th colspan="3">{cat.category}</th>
            </tr>
            {cat.items.map((item) => (
              <tr>
                <td>{item.name}</td>
                <td>{item.standard ? '✅' : '—'}</td>
                <td>{item.enterprise ? '✅' : '—'}</td>
              </tr>
            ))}
          </>
        ))}
      </tbody>
    </table>
  </section>

  <section class="edition-migration">
    <h2>{t.migrationTitle}</h2>
    <p>{t.migrationBody}</p>
  </section>

  <section class="edition-start">
    <h2>{t.getStarted}</h2>
    <p>{t.getStartedBody}</p>
    <a href="/en/contact-us/" class="cta">{t.contactCta}</a>
  </section>
</BaseLayout>

<style>
  .edition-hero { padding: 48px 0 24px; }
  .edition-hero h1 { font-size: 2rem; }
  .edition-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin: 24px 0; }
  .card { padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; }
  .card.enterprise { border-color: var(--enterprise-badge, #16a34a); }
  .edition-table { overflow-x: auto; margin: 32px 0; }
  table { width: 100%; border-collapse: collapse; }
  th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
  .category-row th { background: #f9fafb; font-size: 0.95rem; }
  .cta { display: inline-block; margin-top: 12px; padding: 10px 20px; background: var(--brand-primary, #6653e3); color: #fff; border-radius: 8px; text-decoration: none; }
</style>
```

- [ ] **Step 6: 创建联系页占位 `site/src/pages/contact-us.astro`**

```astro
---
import BaseLayout from '../layouts/BaseLayout.astro';
---
<BaseLayout title="联系我们 · LLM-Gateway" description="联系 LLM-Gateway 团队获取企业版授权与部署支持">
  <section style="padding: 64px 0;">
    <h1>联系我们</h1>
    <p>如需企业版授权、部署支持或商务合作，请通过以下方式联系我们：</p>
    <ul>
      <li>GitHub Issues：<a href="https://github.com/codingas/llm-gateway/issues">提交 Issue</a></li>
      <li>邮件：business@llm-gateway.dev（占位，待确定）</li>
    </ul>
  </section>
</BaseLayout>
```

- [ ] **Step 7: 验证对比页中英双页**

Run:
```bash
cd site && pnpm dev
```
Expected:
- `http://localhost:4321/standard-vs-enterprise/` 显示中文对比页，表格覆盖 11 个类别，企业版专属项标准版列显示「—」、企业版列显示「✅」，含迁移路径段。
- `http://localhost:4321/en/standard-vs-enterprise/` 显示英文对比页，文案为英文，表格数据一致。

Ctrl+C 停止。

- [ ] **Step 8: Commit**

```bash
git add site/src/data/editionDiff.ts site/src/data/i18n/editionDiff.ts site/src/pages/standard-vs-enterprise/ site/src/pages/en/standard-vs-enterprise/ site/src/pages/contact-us.astro site/src/layouts/BaseLayout.astro
git commit -m "feat(site): 版本对比页中英双语（覆盖 README 全部功能项+迁移路径）"
```

---

