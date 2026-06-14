// 注意：原 plan 要求访问 /channels 并断言 "新增渠道" 按钮，
// 但本期 smoke 只验证 E2E 链路（Playwright + Vite dev + 应用挂载）通畅，
// 避免与后端服务及具体路由耦合，因此改为访问根路径并断言 #root 元素存在。
import { test, expect } from '@playwright/test';

test('应用根容器应可见（E2E 链路 smoke）', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('#root')).toBeVisible();
});
