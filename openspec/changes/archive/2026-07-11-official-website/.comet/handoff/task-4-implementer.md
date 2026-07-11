## Task 4: Starlight 主题覆盖

**Files:**
- Create: `site/src/components/starlight/Header.astro`
- Create: `site/src/components/starlight/Footer.astro`
- Create: `site/src/components/starlight/SiteTitle.astro`
- Create: `site/src/data/navigation.ts`
- Modify: `site/astro.config.ts`（启用 components 覆盖）

**目标：** 覆盖 Starlight 的 Header（顶部导航：产品/文档/定价/版本对比）、Footer、SiteTitle，使其承载营销导航，文档站与营销页视觉统一。

- [ ] **Step 1: 创建 `site/src/data/navigation.ts`**

```ts
// 顶部导航项（中英双语，营销页与文档站 Header 共用）。
export interface NavItem {
  label: string;
  href: string;
  enLabel: string;
}

export const navigation: NavItem[] = [
  { label: '产品', href: '/#products', enLabel: 'Product' },
  { label: '文档', href: '/docs/', enLabel: 'Docs' },
  { label: '版本对比', href: '/standard-vs-enterprise/', enLabel: 'Editions' },
  { label: '联系我们', href: '/contact-us/', enLabel: 'Contact' },
];
```

- [ ] **Step 2: 创建 `site/src/components/starlight/SiteTitle.astro`**

```astro
---
// 覆盖 Starlight 默认 SiteTitle，链接回首页。
---
<a href="/" class="site-title">
  <span class="logo">LLM-Gateway</span>
</a>

<style>
  .site-title {
    text-decoration: none;
    font-weight: 700;
    font-size: 1.25rem;
    color: var(--sl-color-white);
  }
</style>
```

- [ ] **Step 3: 创建 `site/src/components/starlight/Header.astro`**

```astro
---
// 覆盖 Starlight Header，追加营销导航。
import { navigation } from '../../data/navigation.ts';
// 当前路径判断：/en/ 前缀用英文 label。
const pathname = Astro.url.pathname;
const isEn = pathname.startsWith('/en/');
---
<header class="header">
  <div class="header-inner">
    <a href="/" class="brand">LLM-Gateway</a>
    <nav class="nav">
      {navigation.map((item) => (
        <a href={item.href} class="nav-item">
          {isEn ? item.enLabel : item.label}
        </a>
      ))}
    </nav>
  </div>
</header>

<style>
  .header {
    position: sticky;
    top: 0;
    z-index: 100;
    background: var(--brand-bg, #fff);
    border-bottom: 1px solid #e5e7eb;
  }
  .header-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .brand {
    font-weight: 700;
    font-size: 1.25rem;
    text-decoration: none;
    color: var(--brand-text, #1a1a2e);
  }
  .nav {
    display: flex;
    gap: 24px;
  }
  .nav-item {
    text-decoration: none;
    color: var(--brand-text, #1a1a2e);
    font-size: 0.95rem;
  }
  .nav-item:hover {
    color: var(--brand-primary, #6653e3);
  }
</style>
```

- [ ] **Step 4: 创建 `site/src/components/starlight/Footer.astro`**

```astro
---
// 覆盖 Starlight Footer，含版权与 GitHub 链接。
const year = new Date().getFullYear();
---
<footer class="footer">
  <div class="footer-inner">
    <div class="copyright">© {year} LLM-Gateway · Apache-2.0 开源</div>
    <div class="links">
      <a href="https://github.com/codingas/llm-gateway">GitHub</a>
      <a href="/docs/">文档</a>
      <a href="/standard-vs-enterprise/">版本对比</a>
    </div>
  </div>
</footer>

<style>
  .footer {
    border-top: 1px solid #e5e7eb;
    padding: 24px 0;
    margin-top: 48px;
  }
  .footer-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
  }
  .copyright {
    color: #6b7280;
    font-size: 0.875rem;
  }
  .links {
    display: flex;
    gap: 20px;
  }
  .links a {
    color: #6b7280;
    text-decoration: none;
    font-size: 0.875rem;
  }
  .links a:hover {
    color: var(--brand-primary, #6653e3);
  }
</style>
```

- [ ] **Step 5: 修改 `site/astro.config.ts` 启用组件覆盖**

将 Task 1 Step 9 中 `starlight({...})` 内的 `components: {}` 替换为：

```ts
      components: {
        Header: './src/components/starlight/Header.astro',
        Footer: './src/components/starlight/Footer.astro',
        SiteTitle: './src/components/starlight/SiteTitle.astro',
      },
```

- [ ] **Step 6: 验证主题覆盖生效**

Run:
```bash
cd site && pnpm dev
```
Expected: 访问 `http://localhost:4321/docs/`，顶部显示自定义 Header（品牌名 + 产品/文档/版本对比/联系我们导航），底部显示自定义 Footer（版权 + GitHub/文档/版本对比链接），SiteTitle 显示「LLM-Gateway」。Ctrl+C 停止。

- [ ] **Step 7: Commit**

```bash
git add site/src/components/starlight/ site/src/data/navigation.ts site/astro.config.ts
git commit -m "feat(site): 覆盖 Starlight Header/Footer/SiteTitle 主题组件"
```

---

