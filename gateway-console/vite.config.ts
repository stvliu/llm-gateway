/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath, URL } from 'node:url';
import { execSync } from 'node:child_process';

// 获取后端代理目标地址
// 优先级：VITE_BACKEND_URL 环境变量（容器环境） > WSL2 自动探测 > localhost
function getBackendTarget(): string {
  // 容器环境：优先使用显式配置的后端 URL（如 docker-compose 中设为 http://gateway:8080）
  if (process.env.VITE_BACKEND_URL) {
    return process.env.VITE_BACKEND_URL;
  }
  // WSL2 下获取 Windows 主机 IP
  if (process.env.WSL_DISTRO_NAME) {
    try {
      const hostIp = execSync("hostname -I | awk '{print $1}'", { encoding: 'utf-8' }).trim();
      return `http://${hostIp}:8080`;
    } catch {
      return 'http://localhost:8080';
    }
  }
  return 'http://localhost:8080';
}

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    historyApiFallback: true,
    proxy: {
      '/api/v1': {
        target: getBackendTarget(),
        changeOrigin: true,
      },
    },
  },
  build: {
    // 构建输出到 Spring Boot 的 static 目录
    outDir: '../gateway-boot/src/main/resources/static',
    emptyOutDir: true,
    sourcemap: false,
    rollupOptions: {
      output: {
        // 静态资源文件名
        assetFileNames: 'assets/[name]-[hash][extname]',
        chunkFileNames: 'assets/[name]-[hash].js',
        entryFileNames: 'assets/[name]-[hash].js',
      },
    },
  },
  // 单元/组件测试配置（Vitest）
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    css: true,
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    // 排除 e2e 目录，避免 Vitest 误抓取 Playwright 用例
    exclude: ['node_modules/**', 'e2e/**', 'dist/**'],
  },
});
