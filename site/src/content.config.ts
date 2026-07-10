import { docsLoader } from '@astrojs/starlight/loaders';
import { docsSchema } from '@astrojs/starlight/schema';
import { defineCollection } from 'astro:content';

// Starlight 文档内容集合配置（Astro 6 Content Layer API）
export const collections = {
	docs: defineCollection({ loader: docsLoader(), schema: docsSchema() }),
};
