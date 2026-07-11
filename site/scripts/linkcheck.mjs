// 链接检查：扫描 dist/ 下 HTML 的内部链接，校验目标文件存在。
// 断链则退出码 1。跳过外部链接、锚点、mailto、tel。
// trailingSlash: always 配置下，以 / 结尾的 href 解析为 /index.html。
import { readdir, readFile, stat } from 'node:fs/promises';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const distDir = resolve(__dirname, '../dist');

const broken = [];

// 递归收集 dist/ 下所有 .html 文件。
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
  // 匹配所有 href="..." 属性值。
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
      // 先直接校验目标；若失败，把 target 当目录尝试其下 index.html（兼容无尾斜杠指向目录）。
      // 注意：fallback 必须基于 target 自身而非其父目录，否则无尾斜杠断链（如 /nonexistent）
      // 会误命中上级目录的 index.html 而漏报。
      const exists = await stat(target).catch(() => null)
        || await stat(join(target, 'index.html')).catch(() => null);
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
