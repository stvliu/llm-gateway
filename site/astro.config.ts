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
      social: [
        // starlight 0.33+ social 改为数组格式（icon 为内置图标名）。
        { label: 'GitHub', icon: 'github', href: 'https://github.com/codingas/llm-gateway' },
      ],
      editLink: {
        baseUrl: 'https://github.com/codingas/llm-gateway/edit/master/site/src/content/docs',
      },
      customCss: ['./src/styles/global.scss'],
      // 主题覆盖在 Task 4 启用，此处先留空对象。
      components: {},
    }),
  ],
});
