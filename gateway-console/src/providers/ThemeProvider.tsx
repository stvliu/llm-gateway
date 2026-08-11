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
