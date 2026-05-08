import { Space, Dropdown, Button, Select, Avatar, type MenuProps } from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SunOutlined,
  MoonOutlined,
  LaptopOutlined,
  LogoutOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { useThemeStore } from '@/stores/themeStore';
import logoSvg from '@/assets/images/logo-full.svg';

interface LeftHeaderProps {
  collapsed: boolean;
}

interface RightHeaderProps {
  collapsed: boolean;
  onToggle: () => void;
}

export function LeftHeader({ collapsed }: LeftHeaderProps) {
  const { t } = useTranslation('common');
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  const headerStyle = {
    display: 'flex',
    alignItems: 'center',
    padding: '0 16px',
    height: 48,
    background: isDark ? '#141414' : '#fff',
    borderBottom: isDark ? '1px solid #303030' : '1px solid #f0f0f0',
  };

  return (
    <div style={headerStyle}>
      <img
        src={logoSvg}
        alt={t('app.title')}
        style={{
          height: 28,
          width: 'auto',
          opacity: collapsed ? 0 : 1,
          transition: 'opacity 0.2s',
        }}
      />
      {!collapsed && (
        <span style={{
          marginLeft: 12,
          fontSize: 16,
          fontWeight: 600,
          color: isDark ? 'rgba(255, 255, 255, 0.85)' : '#1f1f1f',
          whiteSpace: 'nowrap',
        }}>
          {t('app.title')}
        </span>
      )}
    </div>
  );
}

export function RightHeader({ collapsed, onToggle }: RightHeaderProps) {
  const { t, i18n } = useTranslation('common');
  const { user, logout } = useAuthStore();
  const { setMode, getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  const headerStyle = {
    display: 'flex',
    alignItems: 'center',
    padding: '0 16px',
    height: 48,
    background: isDark ? '#141414' : '#fff',
    borderBottom: isDark ? '1px solid #303030' : '1px solid #f0f0f0',
  };

  const themeItems: MenuProps['items'] = [
    {
      key: 'system',
      label: t('theme.system'),
      icon: <LaptopOutlined />,
      onClick: () => setMode('system'),
    },
    {
      key: 'light',
      label: t('theme.light'),
      icon: <SunOutlined />,
      onClick: () => setMode('light'),
    },
    {
      key: 'dark',
      label: t('theme.dark'),
      icon: <MoonOutlined />,
      onClick: () => setMode('dark'),
    },
  ];

  const userItems: MenuProps['items'] = [
    {
      key: 'logout',
      label: t('user.logout'),
      icon: <LogoutOutlined />,
      onClick: logout,
    },
  ];

  const handleLanguageChange = (lng: string) => {
    i18n.changeLanguage(lng);
  };

  const ThemeIcon = isDark ? MoonOutlined : SunOutlined;

  return (
    <div style={{ ...headerStyle, justifyContent: 'space-between' }}>
      <Button
        type="text"
        icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        onClick={onToggle}
      />

      <Space>
        <Dropdown menu={{ items: themeItems }} trigger={['click']}>
          <Button type="text" icon={<ThemeIcon />} />
        </Dropdown>

        <Select
          value={i18n.language || 'zh-CN'}
          onChange={handleLanguageChange}
          variant="borderless"
          options={[
            { value: 'zh-CN', label: t('language.zhCN') },
            { value: 'en-US', label: t('language.enUS') },
          ]}
        />

        <Dropdown menu={{ items: userItems }} trigger={['click']}>
          <Space style={{ cursor: 'pointer' }}>
            <Avatar size="small" icon={<UserOutlined />} />
            <span style={{ color: isDark ? 'rgba(255, 255, 255, 0.85)' : undefined }}>
              {user?.username}
            </span>
          </Space>
        </Dropdown>
      </Space>
    </div>
  );
}
