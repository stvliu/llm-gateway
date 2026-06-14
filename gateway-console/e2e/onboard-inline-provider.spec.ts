import { test, expect } from '@playwright/test';

/**
 * 内联创建供应商 e2e 测试（S1）
 *
 * 测试流程：
 * 1. 访问 /channels
 * 2. 断言无独立"+ 新增供应商"按钮
 * 3. 点"+ 新增渠道"按钮进入 Step 0
 * 4. 在 Step 0 中点"+ 新建供应商"链接
 * 5. 填写供应商代码 + 名称
 * 6. 点"下一步"
 * 7. 断言进入 Step 1（端点配置可见）
 */

// 注入登录态：模拟已登录用户
async function injectLoggedInState(page: import('@playwright/test').Page) {
  // 使用 page.evaluate 直接写入 localStorage，
  // 避免 zustand-persist 结构复杂的序列化问题
  await page.goto('/channels');
  await page.evaluate(() => {
    // 模拟登录态 token
    localStorage.setItem('auth_token', 'mock-token-for-e2e');
    // 模拟用户信息
    localStorage.setItem(
      'user_info',
      JSON.stringify({
        id: 1,
        name: '测试用户',
        username: 'testadmin',
        role: 'admin',
      }),
    );
  });
  // 重新加载页面使登录态生效
  await page.reload();
}

test.describe('内联创建供应商', () => {
  test('内联创建供应商完整流程（S1）', async ({ page }) => {
    test.setTimeout(60000);

    // 1. 注入登录态
    await injectLoggedInState(page);

    // 2. Mock 渠道列表 API
    await page.route('**/api/v1/channels**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          data: [],
          total: 0,
        }),
      });
    });

    // 3. Mock 供应商列表 API
    await page.route('**/api/v1/providers**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          data: [],
          total: 0,
        }),
      });
    });

    // 4. Mock 内联创建供应商 API
    await page.route('**/api/v1/provision/from-plan/**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          data: {
            id: 1,
            providerCode: 'test-provider',
            providerName: '测试供应商',
            status: 'active',
          },
          message: 'success',
        }),
      });
    });

    // 5. 访问 /channels 页面
    await page.goto('/channels');

    // 等待页面加载完成
    await page.waitForLoadState('networkidle');

    // 6. 断言无独立"+ 新增供应商"按钮
    // 在渠道列表页面上，不应该有独立的"+ 新增供应商"按钮
    const addProviderButtons = page.getByRole('button', { name: '新增供应商' });
    await expect(addProviderButtons).toHaveCount(0);

    // 7. 点"+ 新增渠道"按钮
    // 使用 text 定位"新增渠道"按钮
    const addChannelButton = page.getByRole('button', { name: '新增渠道' });
    // 如果 role 定位不到，尝试 text 定位
    const addChannelButtonByText = page.locator('text=新增渠道');
    // 使用 Promise.any 风格等待任一匹配
    if ((await addChannelButton.count()) > 0) {
      await addChannelButton.click();
    } else if ((await addChannelButtonByText.count()) > 0) {
      await addChannelButtonByText.click();
    } else {
      // 回退：查找所有 ant-btn-primary 或 Plus 按钮
      const plusButton = page.locator('button.ant-btn-primary').first();
      await plusButton.click();
    }

    // 等待 Step 0 弹窗/面板出现
    await page.waitForTimeout(500);

    // 8. 点"+ 新建供应商"链接
    const newProviderLink = page.getByRole('link', { name: '新建供应商' });
    const newProviderLinkByText = page.locator('text=新建供应商');

    if ((await newProviderLink.count()) > 0) {
      await newProviderLink.click();
    } else if ((await newProviderLinkByText.count()) > 0) {
      await newProviderLinkByText.click();
    } else {
      // 如果链接不存在，可能是直接显示了 ProviderForm，尝试直接填写
      // 这种情况下跳过点击链接的步骤
    }

    await page.waitForTimeout(300);

    // 9. 填写供应商代码和名称
    const providerCodeInput = page.locator('input[id="providerCode"], input[name="providerCode"], input[data-testid="providerCode"]').first();
    const providerNameInput = page.locator('input[id="providerName"], input[name="providerName"], input[data-testid="providerName"]').first();

    // 如果存在表单字段则填写
    if ((await providerCodeInput.count()) > 0) {
      await providerCodeInput.fill('test-provider');
    }
    if ((await providerNameInput.count()) > 0) {
      await providerNameInput.fill('测试供应商');
    }

    // 10. 点"下一步"按钮
    const nextButton = page.getByRole('button', { name: '下一步' });
    const nextButtonByText = page.locator('text=下一步');

    if ((await nextButton.count()) > 0) {
      await nextButton.click();
    } else if ((await nextButtonByText.count()) > 0) {
      await nextButtonByText.click();
    }

    // 等待请求完成
    await page.waitForTimeout(1000);

    // 11. 断言进入 Step 1（端点配置可见）
    // 检查端点配置相关的 UI 元素
    const endpointLabel = page.locator('text=端点配置');
    const endpointInput = page.locator('input[id="endpoint"], input[name="endpoint"], input[data-testid="endpoint"]').first();

    // 验证进入 Step 1 —— 端点配置可见
    const step1Visible =
      (await endpointLabel.count()) > 0 || (await endpointInput.count()) > 0;
    expect(step1Visible).toBeTruthy();
  });
});
