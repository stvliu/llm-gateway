import { ConfigProvider, theme } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { useThemeStore } from '@/stores/themeStore';
import { getThemeConfig, PRIMARY_COLOR } from '@/styles/theme';

interface ThemeProviderProps {
  children: React.ReactNode;
}

/**
 * 主题提供者组件
 * 集成深色模式切换和 Ant Design 主题配置
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
      // Modal 和 Drawer 遮罩模糊效果（v6 新特性）
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

// 导出品牌色供其他组件使用
export { PRIMARY_COLOR };
