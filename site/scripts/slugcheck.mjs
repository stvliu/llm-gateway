// slug 检查：src/content/docs/ 下文件名与目录名必须为英文 kebab-case。
// 规则：^[a-z0-9-]+$，不允许中文或大写。非法则退出码 1。
import { readdir } from 'node:fs/promises';
import { join, dirname, extname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const docsDir = resolve(__dirname, '../src/content/docs');
const valid = /^[a-z0-9-]+$/;
const invalid = [];

// 递归遍历 docs 目录，校验每个条目名（去掉 .mdx/.md 后缀）。
async function walk(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  for (const e of entries) {
    const name = e.name;
    // 文件去掉扩展名后校验；目录直接校验目录名。
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
