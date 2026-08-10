/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
// Vitest 全局测试启动配置
// - 引入 jest-dom 自定义匹配器（toBeInTheDocument 等）
// - 每个测试用例后清理 RTL 渲染的 DOM，防止用例间相互污染
import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

afterEach(() => {
  cleanup();
});
