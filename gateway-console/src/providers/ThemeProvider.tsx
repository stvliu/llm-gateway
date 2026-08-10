/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { ConfigProvider, theme } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { useThemeStore } from '@/stores/themeStore';
import { getThemeConfig } from '@/styles/theme';

interface ThemeProviderProps {
  children: React.ReactNode;
}

/**
 * 主题提供者组件
 */
export function ThemeProvider({ children }: ThemeProviderProps) {
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  const themeConfig = getThemeConfig(isDark);

  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        ...themeConfig,
        algorithm: isDark ? theme.darkAlgorithm : theme.defaultAlgorithm,
      }}
      modal={{
        styles: {
          mask: {
            backdropFilter: 'blur(4px)',
          },
        },
      }}
      drawer={{
        styles: {
          mask: {
            backdropFilter: 'blur(4px)',
          },
        },
      }}
    >
      {children}
    </ConfigProvider>
  );
}
