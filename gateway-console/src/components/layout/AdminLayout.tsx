import { useState } from 'react';
import { Layout, FloatButton, theme } from 'antd';
import { CommentOutlined } from '@ant-design/icons';
import { Outlet } from 'react-router-dom';
import { LeftHeader, RightHeader } from './Header';
import { Sidebar } from './Sidebar';
import { ChatPanel } from '@/components/chat';
import { useChatStore } from '@/stores/chatStore';

const { Sider, Content } = Layout;

/**
 * 管理后台布局组件
 * 侧边栏颜色跟随全局主题
 */
export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const { isOpen, setOpen } = useChatStore();
  const { token } = theme.useToken();

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        width={200}
        collapsedWidth={80}
        collapsed={collapsed}
        style={{ background: token.colorBgContainer }}
      >
        <LeftHeader collapsed={collapsed} />
        <Sidebar collapsed={collapsed} role="ADMIN" />
      </Sider>
      <Layout style={{ background: token.colorBgLayout }}>
        <RightHeader collapsed={collapsed} onToggle={() => setCollapsed(!collapsed)} />
        <Content style={{ padding: 16, overflow: 'auto', background: token.colorBgLayout }}>
          <Outlet />
        </Content>
      </Layout>

      {/* AI 聊天悬浮按钮 */}
      <FloatButton
        icon={<CommentOutlined />}
        type="primary"
        onClick={() => setOpen(true)}
        style={{
          right: isOpen ? 400 : 24,
          transition: 'right 0.3s',
        }}
      />

      {/* AI 聊天面板 */}
      {isOpen && (
        <div
          style={{
            position: 'fixed',
            right: 24,
            bottom: 24,
            zIndex: 1000,
            animation: 'fadeInUp 0.3s ease-out',
          }}
        >
          <ChatPanel />
        </div>
      )}

      {/* 动画样式 */}
      <style>{`
        @keyframes fadeInUp {
          from {
            opacity: 0;
            transform: translateY(20px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
      `}</style>
    </Layout>
  );
}
