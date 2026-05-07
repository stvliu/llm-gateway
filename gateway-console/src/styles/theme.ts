/**
 * Ant Design 主题配置
 * 基于 Ant Design 官网设计风格
 */

import type { ThemeConfig } from 'antd';

/**
 * 品牌色 - Ant Design 官网蓝
 */
export const PRIMARY_COLOR = '#1677ff';

/**
 * 圆角配置
 */
export const BORDER_RADIUS = {
  xs: 2,
  sm: 4,
  base: 6,
  lg: 8,
  xl: 12,
} as const;

/**
 * 动画缓动曲线
 */
export const MOTION_EASING = {
  easeInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
  easeOut: 'cubic-bezier(0, 0, 0.2, 1)',
  easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
  easeOutQuint: 'cubic-bezier(0.23, 1, 0.32, 1)',
} as const;

/**
 * 基础主题 Token
 */
export const baseToken = {
  // 品牌色
  colorPrimary: PRIMARY_COLOR,
  // 圆角
  borderRadius: BORDER_RADIUS.base,
  borderRadiusSM: BORDER_RADIUS.sm,
  borderRadiusLG: BORDER_RADIUS.lg,
  // 字体
  fontSize: 14,
  fontSizeHeading1: 38,
  fontSizeHeading2: 30,
  fontSizeHeading3: 24,
  fontSizeHeading4: 20,
  fontSizeHeading5: 16,
  // 线宽
  lineWidth: 1,
  // 动画
  motionDurationFast: '0.1s',
  motionDurationMid: '0.2s',
  motionDurationSlow: '0.3s',
  motionEaseInOut: MOTION_EASING.easeInOut,
  motionEaseOut: MOTION_EASING.easeOut,
  motionEaseIn: MOTION_EASING.easeIn,
} as const;

/**
 * 浅色主题配置
 */
export const lightTheme: ThemeConfig = {
  token: {
    ...baseToken,
    // 浅色背景
    colorBgContainer: '#ffffff',
    colorBgLayout: '#f5f5f5',
    colorBgElevated: '#ffffff',
    // 文字颜色
    colorText: 'rgba(0, 0, 0, 0.88)',
    colorTextSecondary: 'rgba(0, 0, 0, 0.65)',
    colorTextTertiary: 'rgba(0, 0, 0, 0.45)',
    colorTextQuaternary: 'rgba(0, 0, 0, 0.25)',
    // 边框
    colorBorder: '#d9d9d9',
    colorBorderSecondary: '#f0f0f0',
    // 阴影
    boxShadowSecondary: '0 2px 8px rgba(0, 0, 0, 0.06)',
  },
  components: {
    Card: {
      borderRadiusLG: BORDER_RADIUS.lg,
      boxShadowTertiary: '0 2px 8px rgba(0, 0, 0, 0.06)',
    },
    Button: {
      controlHeight: 32,
      controlHeightLG: 40,
      controlHeightSM: 24,
    },
    Table: {
      borderRadiusLG: BORDER_RADIUS.lg,
    },
    Modal: {
      borderRadiusLG: BORDER_RADIUS.lg,
    },
    Drawer: {
      borderRadiusLG: BORDER_RADIUS.lg,
    },
  },
};

/**
 * 深色主题配置
 */
export const darkTheme: ThemeConfig = {
  token: {
    ...baseToken,
    // 深色背景
    colorBgContainer: '#141414',
    colorBgLayout: '#000000',
    colorBgElevated: '#1f1f1f',
    // 文字颜色
    colorText: 'rgba(255, 255, 255, 0.85)',
    colorTextSecondary: 'rgba(255, 255, 255, 0.65)',
    colorTextTertiary: 'rgba(255, 255, 255, 0.45)',
    colorTextQuaternary: 'rgba(255, 255, 255, 0.25)',
    // 边框
    colorBorder: '#424242',
    colorBorderSecondary: '#303030',
    // 阴影
    boxShadowSecondary: '0 2px 8px rgba(0, 0, 0, 0.3)',
  },
  components: {
    Card: {
      borderRadiusLG: BORDER_RADIUS.lg,
      colorBgContainer: '#141414',
      boxShadowTertiary: '0 2px 8px rgba(0, 0, 0, 0.3)',
    },
    Button: {
      controlHeight: 32,
      controlHeightLG: 40,
      controlHeightSM: 24,
    },
    Table: {
      borderRadiusLG: BORDER_RADIUS.lg,
      colorBgContainer: '#141414',
      headerBg: '#1f1f1f',
    },
    Modal: {
      borderRadiusLG: BORDER_RADIUS.lg,
      contentBg: '#1f1f1f',
    },
    Drawer: {
      borderRadiusLG: BORDER_RADIUS.lg,
    },
    Menu: {
      darkItemBg: '#141414',
      darkSubMenuItemBg: '#1f1f1f',
    },
  },
};

/**
 * 获取主题配置
 * @param isDark 是否深色模式
 */
export function getThemeConfig(isDark: boolean): ThemeConfig {
  return isDark ? darkTheme : lightTheme;
}
