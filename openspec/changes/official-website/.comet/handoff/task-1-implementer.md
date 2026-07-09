## Task 1: 项目脚手架与 i18n 架构

**Files:**
- Create: `site/package.json`
- Create: `site/.nvmrc`
- Create: `site/tsconfig.json`
- Create: `site/astro.config.ts`
- Create: `site/astro.redirects.ts`
- Create: `site/src/env.d.ts`
- Create: `site/src/styles/global.scss`
- Create: `site/public/favicon.svg`
- Create: `site/.gitignore`

- [ ] **Step 1: 创建 `site/.nvmrc` 锁定 Node 版本**

文件内容：

```
20
```

- [ ] **Step 2: 创建 `site/.gitignore`**

```
node_modules/
dist/
.astro/
.output/
*.log
.DS_Store
.env
.env.production
```

- [ ] **Step 3: 创建 `site/package.json`**

```json
{
  "name": "llm-gateway-site",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "engines": { "node": ">=20.0.0" },
  "scripts": {
    "dev": "astro dev",
    "build": "astro build",
    "preview": "astro preview",
    "astro": "astro",
    "lint:linkcheck": "node scripts/linkcheck.mjs",
    "lint:slugcheck": "node scripts/slugcheck.mjs"
  },
  "dependencies": {
    "@astrojs/sitemap": "^3.2.1",
    "@astrojs/starlight": "^0.30.0",
    "astro": "^6.0.0",
    "sass": "^1.80.0",
    "sharp": "^0.33.5"
  }
}
```

> 注：执行 `pnpm install` 时若 starlight 对 astro 6 有 peer 警告，以实际可安装的兼容版本为准（starlight 0.30+ 支持 Astro 5/6）。若 Astro 6 尚未稳定，降级为 `astro: "^5.0.0"` + 对应 starlight，配置不变。

- [ ] **Step 4: 创建 `site/tsconfig.json`**

```json
{
  "extends": "astro/tsconfigs/strict",
  "include": [".astro/types.d.ts", "**/*"],
  "exclude": ["dist"]
}
```

- [ ] **Step 5: 创建 `site/src/env.d.ts`**

```ts
/// <reference path="../.astro/types.d.ts" />
```

- [ ] **Step 6: 创建 `site/astro.redirects.ts`（P0 预留，导出空对象）**

```ts
// 旧 docs/ 链接 -> 新 slug 的重定向映射。
// P0 预留：原 docs/ 保留不删，如需重定向在此追加。
// 键为旧路径（相对站点根），值为新 slug。
export const redirects: Record<string, string> = {};

export default redirects;
```

- [ ] **Step 7: 创建 `site/src/styles/global.scss`（营销页全局样式占位）**

```scss
// 营销页全局样式。Starlight 文档站样式由 Starlight 自带，此处仅影响营销页。
:root {
  --brand-primary: #6653e3;
  --brand-bg: #ffffff;
  --brand-text: #1a1a2e;
  --enterprise-badge: #16a34a;
}

html {
  scroll-behavior: smooth;
}

body {
  margin: 0;
  font-family: system-ui, -apple-system, "Segoe UI", "PingFang SC",
    "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  color: var(--brand-text);
  background: var(--brand-bg);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}
```

- [ ] **Step 8: 创建 `site/public/favicon.svg`**

```xml
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">
  <rect width="32" height="32" rx="6" fill="#6653e3"/>
  <text x="16" y="22" font-size="18" font-family="sans-serif" font-weight="bold" fill="#fff" text-anchor="middle">G</text>
</svg>
```

- [ ] **Step 9: 创建 `site/astro.config.ts`**

```ts
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import sitemap from '@astrojs/sitemap';
import { sidebar } from './astro.sidebar.ts';

// 站点根 URL，P0 占位，域名确定后一处修改。
const SITE_URL = process.env.PUBLIC_SITE_URL ?? 'https://llm-gateway.dev';

// https://astro.build/config
export default defineConfig({
  site: SITE_URL,
  trailingSlash: 'always',
  integrations: [
    sitemap(),
    starlight({
      title: 'LLM-Gateway',
      // P0 仅 root locale（简体中文），不配 en，避免未翻译 fallback 噪音。
      locales: {
        root: { label: '简体中文', lang: 'zh-CN' },
      },
      sidebar,
      social: {
        github: 'https://github.com/codingas/llm-gateway',
      },
      editLink: {
        baseUrl: 'https://github.com/codingas/llm-gateway/edit/master/site/src/content/docs',
      },
      customCss: ['./src/styles/global.scss'],
      // 主题覆盖在 Task 4 启用，此处先留空对象。
      components: {},
    }),
  ],
});
```

- [ ] **Step 10: 安装依赖**

Run:
```bash
cd site && pnpm install
```
Expected: 依赖安装成功，生成 `node_modules/` 与 `pnpm-lock.yaml`。若 astro 6 不可用，按 Step 3 注释降级 astro 主版本后重装。

- [ ] **Step 11: 验证脚手架可启动**

Run:
```bash
cd site && pnpm dev
```
Expected: 开发服务在 `http://localhost:4321/` 启动，无报错（此时无文档内容，Starlight 会渲染空文档站首页）。Ctrl+C 停止。

- [ ] **Step 12: Commit**

```bash
git add site/
git commit -m "feat(site): 初始化 Astro+Starlight 脚手架与 i18n 架构（root locale）"
```

---
