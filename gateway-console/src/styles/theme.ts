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
