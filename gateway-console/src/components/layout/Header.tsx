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
import { useState } from 'react';
import { Space, Dropdown, Button, Select, Avatar, theme, type MenuProps } from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SunOutlined,
  MoonOutlined,
  LaptopOutlined,
  CodeOutlined,
  LogoutOutlined,
  LockOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { useThemeStore } from '@/stores/themeStore';
import { ChangePasswordModal } from '@/components/ChangePasswordModal';
import logoSvg from '@/assets/images/logo-full.svg';

interface LeftHeaderProps {
  collapsed: boolean;
}

interface RightHeaderProps {
  collapsed: boolean;
  onToggle: () => void;
}

/**
 * 左侧头部组件（侧边栏内）
 * 显示 Logo 和应用名称，颜色跟随全局主题
 */
export function LeftHeader({ collapsed }: LeftHeaderProps) {
  const { t } = useTranslation('common');
  const { token } = theme.useToken();

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        padding: '0 16px',
        height: 48,
        background: 'transparent',
      }}
    >
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
        <span
          style={{
            marginLeft: 12,
            fontSize: 16,
            fontWeight: 600,
            color: token.colorText,
            whiteSpace: 'nowrap',
          }}
        >
          {t('app.title')}
        </span>
      )}
    </div>
  );
}

/**
 * 右侧头部组件
 * 显示折叠按钮、主题切换、语言切换、用户菜单
 */
export function RightHeader({ collapsed, onToggle }: RightHeaderProps) {
  const { t, i18n } = useTranslation('common');
  const navigate = useNavigate();
  const { user, logout, hasPermission } = useAuthStore();
  const { setMode, getEffectiveTheme } = useThemeStore();
  const { token } = theme.useToken();
  const isDark = getEffectiveTheme() === 'dark';
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);

  const headerStyle = {
    display: 'flex',
    alignItems: 'center',
    padding: '0 16px',
    height: 48,
    background: token.colorBgContainer,
    borderBottom: `1px solid ${token.colorBorderSecondary}`,
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
      key: 'change-password',
      label: t('menu.changePassword'),
      icon: <LockOutlined />,
      onClick: () => setPasswordModalOpen(true),
    },
    {
      type: 'divider' as const,
    },
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
    <>
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

          {hasPermission('quickstart:access') && (
            <Button
              type="text"
              icon={<CodeOutlined />}
              onClick={() => navigate('/quickstart')}
            >
              {t('header.quickStart')}
            </Button>
          )}

          <Dropdown menu={{ items: userItems }} trigger={['click']}>
            <Space style={{ cursor: 'pointer' }}>
              <Avatar size="small" style={{ backgroundColor: token.colorPrimary }}>
                {user?.username?.charAt(0).toUpperCase()}
              </Avatar>
              <span style={{ color: token.colorText }}>
                {user?.username}
              </span>
            </Space>
          </Dropdown>
        </Space>
      </div>

      <ChangePasswordModal
        open={passwordModalOpen}
        onClose={() => setPasswordModalOpen(false)}
      />
    </>
  );
}
