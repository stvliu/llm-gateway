/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Ant Design 主题配置
 * 启用 CSS 变量模式，确保 CSS 文件也能引用主题 token
 */

import type { ThemeConfig } from 'antd';
import { theme } from 'antd';

const { defaultAlgorithm, darkAlgorithm } = theme;

/**
 * 浅色主题
 */
export const lightTheme: ThemeConfig = {
  algorithm: defaultAlgorithm,
  cssVar: { prefix: 'ant' },
};

/**
 * 深色主题
 */
export const darkTheme: ThemeConfig = {
  algorithm: darkAlgorithm,
  cssVar: { prefix: 'ant' },
};

/**
 * 获取主题配置
 */
export function getThemeConfig(isDark: boolean): ThemeConfig {
  return isDark ? darkTheme : lightTheme;
}
