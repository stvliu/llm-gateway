# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: onboard-inline-provider.spec.ts >> 内联创建供应商 >> 内联创建供应商完整流程（S1）
- Location: e2e\onboard-inline-provider.spec.ts:40:3

# Error details

```
Error: expect(received).toBeTruthy()

Received: false
```

# Page snapshot

```yaml
- generic [ref=e5]:
  - generic [ref=e6]:
    - heading "LLM Gateway" [level=1] [ref=e7]
    - paragraph [ref=e8]: Welcome to LLM Gateway
    - generic [ref=e9]:
      - generic [ref=e12]:
        - generic [ref=e15]:
          - img "user" [ref=e17]:
            - img [ref=e18]
          - textbox "Username" [ref=e20]
        - generic [ref=e23]: Please enter username
      - generic [ref=e26]:
        - generic [ref=e29]:
          - img "lock" [ref=e31]:
            - img [ref=e32]
          - textbox "Password" [ref=e34]
          - img "eye-invisible" [ref=e36] [cursor=pointer]:
            - img [ref=e37]
        - generic [ref=e42]: Please enter password
      - generic [ref=e48]:
        - generic [ref=e49] [cursor=pointer]:
          - checkbox "Remember me" [checked] [ref=e51]
          - generic [ref=e52]: Remember me
        - generic [ref=e53] [cursor=pointer]:
          - generic "English" [ref=e54]:
            - text: English
            - combobox [ref=e55]
          - img "down" [ref=e57]:
            - img [ref=e58]
      - button "Login" [active] [ref=e65] [cursor=pointer]:
        - generic [ref=e66]: Login
  - generic [ref=e67]:
    - text: © 2026 LLM Gateway ·
    - link "帮助文档" [ref=e68] [cursor=pointer]:
      - /url: "#"
```

# Test source

```ts
  66  |         }),
  67  |       });
  68  |     });
  69  | 
  70  |     // 4. Mock 内联创建供应商 API
  71  |     await page.route('**/api/v1/provision/from-plan/**', async (route) => {
  72  |       await route.fulfill({
  73  |         status: 200,
  74  |         contentType: 'application/json',
  75  |         body: JSON.stringify({
  76  |           code: 0,
  77  |           data: {
  78  |             id: 1,
  79  |             providerCode: 'test-provider',
  80  |             providerName: '测试供应商',
  81  |             status: 'active',
  82  |           },
  83  |           message: 'success',
  84  |         }),
  85  |       });
  86  |     });
  87  | 
  88  |     // 5. 访问 /channels 页面
  89  |     await page.goto('/channels');
  90  | 
  91  |     // 等待页面加载完成
  92  |     await page.waitForLoadState('networkidle');
  93  | 
  94  |     // 6. 断言无独立"+ 新增供应商"按钮
  95  |     // 在渠道列表页面上，不应该有独立的"+ 新增供应商"按钮
  96  |     const addProviderButtons = page.getByRole('button', { name: '新增供应商' });
  97  |     await expect(addProviderButtons).toHaveCount(0);
  98  | 
  99  |     // 7. 点"+ 新增渠道"按钮
  100 |     // 使用 text 定位"新增渠道"按钮
  101 |     const addChannelButton = page.getByRole('button', { name: '新增渠道' });
  102 |     // 如果 role 定位不到，尝试 text 定位
  103 |     const addChannelButtonByText = page.locator('text=新增渠道');
  104 |     // 使用 Promise.any 风格等待任一匹配
  105 |     if ((await addChannelButton.count()) > 0) {
  106 |       await addChannelButton.click();
  107 |     } else if ((await addChannelButtonByText.count()) > 0) {
  108 |       await addChannelButtonByText.click();
  109 |     } else {
  110 |       // 回退：查找所有 ant-btn-primary 或 Plus 按钮
  111 |       const plusButton = page.locator('button.ant-btn-primary').first();
  112 |       await plusButton.click();
  113 |     }
  114 | 
  115 |     // 等待 Step 0 弹窗/面板出现
  116 |     await page.waitForTimeout(500);
  117 | 
  118 |     // 8. 点"+ 新建供应商"链接
  119 |     const newProviderLink = page.getByRole('link', { name: '新建供应商' });
  120 |     const newProviderLinkByText = page.locator('text=新建供应商');
  121 | 
  122 |     if ((await newProviderLink.count()) > 0) {
  123 |       await newProviderLink.click();
  124 |     } else if ((await newProviderLinkByText.count()) > 0) {
  125 |       await newProviderLinkByText.click();
  126 |     } else {
  127 |       // 如果链接不存在，可能是直接显示了 ProviderForm，尝试直接填写
  128 |       // 这种情况下跳过点击链接的步骤
  129 |     }
  130 | 
  131 |     await page.waitForTimeout(300);
  132 | 
  133 |     // 9. 填写供应商代码和名称
  134 |     const providerCodeInput = page.locator('input[id="providerCode"], input[name="providerCode"], input[data-testid="providerCode"]').first();
  135 |     const providerNameInput = page.locator('input[id="providerName"], input[name="providerName"], input[data-testid="providerName"]').first();
  136 | 
  137 |     // 如果存在表单字段则填写
  138 |     if ((await providerCodeInput.count()) > 0) {
  139 |       await providerCodeInput.fill('test-provider');
  140 |     }
  141 |     if ((await providerNameInput.count()) > 0) {
  142 |       await providerNameInput.fill('测试供应商');
  143 |     }
  144 | 
  145 |     // 10. 点"下一步"按钮
  146 |     const nextButton = page.getByRole('button', { name: '下一步' });
  147 |     const nextButtonByText = page.locator('text=下一步');
  148 | 
  149 |     if ((await nextButton.count()) > 0) {
  150 |       await nextButton.click();
  151 |     } else if ((await nextButtonByText.count()) > 0) {
  152 |       await nextButtonByText.click();
  153 |     }
  154 | 
  155 |     // 等待请求完成
  156 |     await page.waitForTimeout(1000);
  157 | 
  158 |     // 11. 断言进入 Step 1（端点配置可见）
  159 |     // 检查端点配置相关的 UI 元素
  160 |     const endpointLabel = page.locator('text=端点配置');
  161 |     const endpointInput = page.locator('input[id="endpoint"], input[name="endpoint"], input[data-testid="endpoint"]').first();
  162 | 
  163 |     // 验证进入 Step 1 —— 端点配置可见
  164 |     const step1Visible =
  165 |       (await endpointLabel.count()) > 0 || (await endpointInput.count()) > 0;
> 166 |     expect(step1Visible).toBeTruthy();
      |                          ^ Error: expect(received).toBeTruthy()
  167 |   });
  168 | });
  169 | 
```