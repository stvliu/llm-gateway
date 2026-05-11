/**
 * Ant Design 主题配置
 * 完全使用官方默认主题
 */

import type { ThemeConfig } from 'antd';
import { theme } from 'antd';

const { defaultAlgorithm, darkAlgorithm } = theme;

/**
 * 浅色主题
 */
export const lightTheme: ThemeConfig = {
  algorithm: defaultAlgorithm,
};

/**
 * 深色主题
 */
export const darkTheme: ThemeConfig = {
  algorithm: darkAlgorithm,
};

/**
 * 获取主题配置
 */
export function getThemeConfig(isDark: boolean): ThemeConfig {
  return isDark ? darkTheme : lightTheme;
}
