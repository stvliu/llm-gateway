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

interface HeaderProps {
  collapsed: boolean;
  onToggle: () => void;
}

export function Header({ collapsed, onToggle }: HeaderProps) {
  const { t } = useTranslation('common');
  const { user, logout } = useAuthStore();
  const { setMode, getEffectiveTheme } = useThemeStore();

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
    localStorage.setItem('i18nextLng', lng);
    window.location.reload();
  };

  const ThemeIcon = getEffectiveTheme() === 'dark' ? MoonOutlined : SunOutlined;

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 16px',
        height: 48,
        background: '#fff',
        borderBottom: '1px solid #f0f0f0',
      }}
    >
      <Space>
        <Button
          type="text"
          icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          onClick={onToggle}
        />
        <img src={logoSvg} alt="LLM Gateway" style={{ height: 28, width: 'auto' }} />
      </Space>

      <Space>
        <Dropdown menu={{ items: themeItems }} trigger={['click']}>
          <Button type="text" icon={<ThemeIcon />} />
        </Dropdown>

        <Select
          value={localStorage.getItem('i18nextLng') || 'zh-CN'}
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
            <span>{user?.username}</span>
          </Space>
        </Dropdown>
      </Space>
    </div>
  );
}
