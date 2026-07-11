## Task 7: 工具链与部署

**Files:**
- Create: `site/scripts/linkcheck.mjs`
- Create: `site/scripts/slugcheck.mjs`
- Create: `.github/workflows/deploy-site.yml`（根仓库工作流目录）
- Modify: `site/package.json`（lint 脚本已在 Task 1 定义，此处无需改）

**目标：** linkcheck（无断链）+ slugcheck（英文 kebab-case）lint 脚本；GitHub Actions -> Cloudflare Pages 部署流水线（PR 预览 + master 生产）。lunaria P0 不引入。

- [ ] **Step 1: 创建 `site/scripts/linkcheck.mjs`**

构建后扫描 `dist/` 产物中的 `<a href>`，校验内部链接是否指向存在的文件。断链则退出码 1。

```js
// 链接检查：扫描 dist/ 下 HTML 的内部链接，校验目标文件存在。
import { readdir, readFile, stat } from 'node:fs/promises';
import { join, dirname, resolve, extname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const distDir = resolve(__dirname, '../dist');

const broken = [];

async function walk(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = [];
  for (const e of entries) {
    const full = join(dir, e.name);
    if (e.isDirectory()) files.push(...await walk(full));
    else if (e.name.endsWith('.html')) files.push(full);
  }
  return files;
}

async function check() {
  if (!await stat(distDir).catch(() => null)) {
    console.error('linkcheck: dist/ 不存在，请先运行 pnpm build');
    process.exit(1);
  }
  const htmlFiles = await walk(distDir);
  const hrefRe = /href="([^"]+)"/g;
  for (const file of htmlFiles) {
    const html = await readFile(file, 'utf8');
    let m;
    while ((m = hrefRe.exec(html)) !== null) {
      let href = m[1];
      // 跳过外部链接、锚点、mailto、tel。
      if (/^(https?:|mailto:|tel:|#)/.test(href)) continue;
      // 去掉查询参数与锚点。
      href = href.split('#')[0].split('?')[0];
      if (!href) continue;
      // trailingSlash always：以 / 结尾则解析为 /index.html。
      const target = href.endsWith('/')
        ? join(distDir, href, 'index.html')
        : join(distDir, href);
      const exists = await stat(target).catch(() => null)
        || await stat(join(dirname(target), 'index.html')).catch(() => null);
      if (!exists) {
        broken.push(`${file.replace(distDir, '')} -> ${href}`);
      }
    }
  }
  if (broken.length) {
    console.error(`linkcheck: 发现 ${broken.length} 个断链：`);
    broken.forEach((b) => console.error('  ' + b));
    process.exit(1);
  }
  console.log('linkcheck: 无断链 ✅');
}

check();
```

- [ ] **Step 2: 创建 `site/scripts/slugcheck.mjs`**

校验 `src/content/docs/` 下所有 `.mdx` 文件名与目录名为英文 kebab-case（`^[a-z0-9-]+$`），不允许中文或大写。

```js
// slug 检查：src/content/docs/ 下文件名与目录名必须为英文 kebab-case。
import { readdir, stat } from 'node:fs/promises';
import { join, dirname, extname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const docsDir = resolve(__dirname, '../src/content/docs');
const valid = /^[a-z0-9-]+$/;
const invalid = [];

async function walk(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  for (const e of entries) {
    const name = e.name;
    const stem = extname(name) ? name.replace(/\.(mdx|md)$/, '') : name;
    if (!valid.test(stem)) {
      invalid.push(join(dir.replace(docsDir, ''), name));
    }
    if (e.isDirectory()) await walk(join(dir, name));
  }
}

async function check() {
  await walk(docsDir);
  if (invalid.length) {
    console.error(`slugcheck: 发现 ${invalid.length} 个非法 slug（需英文 kebab-case）：`);
    invalid.forEach((s) => console.error('  ' + s));
    process.exit(1);
  }
  console.log('slugcheck: 所有 slug 合规 ✅');
}

check();
```

- [ ] **Step 3: 验证 lint 脚本可运行**

Run:
```bash
cd site && pnpm build && pnpm lint:slugcheck && pnpm lint:linkcheck
```
Expected:
- `pnpm build` 生成 `dist/`，无错误。
- `slugcheck` 输出「所有 slug 合规 ✅」。
- `linkcheck` 输出「无断链 ✅」。若有断链，修复对应 sidebar 条目或文档 href 后重跑。

- [ ] **Step 4: 创建 `.github/workflows/deploy-site.yml`（根仓库工作流）**

```yaml
name: Deploy Site

on:
  push:
    branches: [master]
    paths:
      - 'site/**'
      - '.github/workflows/deploy-site.yml'
  pull_request:
    paths:
      - 'site/**'
      - '.github/workflows/deploy-site.yml'

jobs:
  build-deploy:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: site
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version-file: 'site/.nvmrc'

      - name: Setup pnpm
        uses: pnpm/action-setup@v4
        with:
          version: 9

      - name: Install dependencies
        run: pnpm install --frozen-lockfile

      - name: Build
        run: pnpm build
        env:
          PUBLIC_SITE_URL: https://llm-gateway.dev

      - name: Slug check
        run: pnpm lint:slugcheck

      - name: Link check
        run: pnpm lint:linkcheck

      - name: Deploy to Cloudflare Pages
        uses: cloudflare/wrangler-action@v3
        with:
          apiToken: ${{ secrets.CLOUDFLARE_API_TOKEN }}
          accountId: ${{ secrets.CLOUDFLARE_ACCOUNT_ID }}
          command: pages deploy dist --project-name=llm-gateway
```

> 注：PR 触发时 Cloudflare Pages 会自动生成预览部署（preview），master 触发为生产部署（production）。`CLOUDFLARE_API_TOKEN` 与 `CLOUDFLARE_ACCOUNT_ID` 需在 GitHub 仓库 Secrets 配置。wrangler-action 会以 `site` 为工作目录（defaults.run 已设），`dist` 路径相对于 `site/`。

- [ ] **Step 5: Commit**

```bash
git add site/scripts/ .github/workflows/deploy-site.yml
git commit -m "feat(site): linkcheck/slugcheck lint 脚本+GitHub Actions→Cloudflare Pages 部署"
```

---

