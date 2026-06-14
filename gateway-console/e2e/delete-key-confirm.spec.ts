import { test, expect } from '@playwright/test';
import { Page } from 'playwright';

/**
 * S6: 删除 API Key 确认对话框
 *
 * 覆盖场景：
 * - 删除 API Key 弹出 Modal.confirm 含"删除后无法恢复"
 * - description 包含"请求将立即失败"
 * - 确认/取消按钮交互
 */
test.describe('API Key 删除确认', () => {
  const BASE_URL = 'http://localhost:3000';

  /** 导航到 API Key 管理页面 */
  async function navigateToKeys(page: Page) {
    await page.goto(`${BASE_URL}/api-keys`);
    await page.waitForSelector('text=API Key 管理', { timeout: 10000 });
  }

  test('删除 API Key 弹出确认对话框，含"删除后无法恢复"和"请求将立即失败"', async ({ page }) => {
    await navigateToKeys(page);

    // 等待 API Key 列表加载
    await page.waitForSelector('table tbody tr', { timeout: 10000 });

    // 点击第一个 API Key 的删除按钮
    const deleteButton = page.locator('table tbody tr').first().locator('button:has-text("删除")');
    await deleteButton.click();

    // 确认 Modal.confirm 出现
    const modal = page.locator('.ant-modal-confirm');
    await expect(modal).toBeVisible({ timeout: 5000 });

    // 验证提示信息包含"删除后无法恢复"
    await expect(modal.locator('.ant-modal-confirm-body')).toContainText('删除后无法恢复');

    // 验证 description 包含"请求将立即失败"
    await expect(modal.locator('.ant-modal-confirm-body')).toContainText('请求将立即失败');
  });

  test('删除确认对话框包含确认和取消按钮', async ({ page }) => {
    await navigateToKeys(page);

    // 等待 API Key 列表加载
    await page.waitForSelector('table tbody tr', { timeout: 10000 });

    // 点击第一个 API Key 的删除按钮
    const deleteButton = page.locator('table tbody tr').first().locator('button:has-text("删除")');
    await deleteButton.click();

    // 确认 Modal.confirm 出现
    const modal = page.locator('.ant-modal-confirm');
    await expect(modal).toBeVisible({ timeout: 5000 });

    // 确认按钮存在
    const confirmBtn = modal.locator('.ant-btn-primary:has-text("确定")');
    await expect(confirmBtn).toBeVisible();

    // 取消按钮存在
    const cancelBtn = modal.locator('.ant-btn:has-text("取消")');
    await expect(cancelBtn).toBeVisible();
  });

  test('取消删除操作后对话框关闭', async ({ page }) => {
    await navigateToKeys(page);

    // 等待 API Key 列表加载
    await page.waitForSelector('table tbody tr', { timeout: 10000 });

    // 点击第一个 API Key 的删除按钮
    const deleteButton = page.locator('table tbody tr').first().locator('button:has-text("删除")');
    await deleteButton.click();

    // 确认 Modal.confirm 出现
    const modal = page.locator('.ant-modal-confirm');
    await expect(modal).toBeVisible({ timeout: 5000 });

    // 点击取消按钮
    const cancelBtn = modal.locator('.ant-btn:has-text("取消")');
    await cancelBtn.click();

    // 确认对话框关闭
    await expect(modal).not.toBeVisible({ timeout: 5000 });
  });
});
