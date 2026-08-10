/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { defineConfig } from '@playwright/test';

/**
 * Playwright E2E 配置
 *
 * <p>注意：webServer.command 使用 pnpm dev，本地与 CI 均会自动拉起
 * Vite 开发服务器；CI 环境下不复用既有服务，避免端口残留。</p>
 */
export default defineConfig({
  testDir: './e2e',
  // 单条 smoke 用例的默认超时
  timeout: 30_000,
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    trace: 'on-first-retry',
  },
  webServer: {
    command: 'pnpm dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    // 冷启动 Vite 在 Windows 上可能略慢，给到 120s
    timeout: 120_000,
  },
});
