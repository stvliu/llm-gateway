import { useState } from 'react';
import { Layout, FloatButton } from 'antd';
import { CommentOutlined } from '@ant-design/icons';
import { Outlet } from 'react-router-dom';
import { LeftHeader, RightHeader } from './Header';
import { Sidebar } from './Sidebar';
import { ChatPanel } from '@/components/chat';
import { useChatStore } from '@/stores/chatStore';

const { Sider, Content } = Layout;

export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const { isOpen, setOpen } = useChatStore();

  return (
    <Layout style={{ height: '100vh' }}>
      <Layout>
        <Sider
          width={200}
          collapsedWidth={64}
          collapsed={collapsed}
          style={{ background: '#fff', borderRight: '1px solid #d9d9d9' }}
        >
          <LeftHeader collapsed={collapsed} />
          <Sidebar collapsed={collapsed} role="ADMIN" />
        </Sider>
        <Layout>
          <RightHeader collapsed={collapsed} onToggle={() => setCollapsed(!collapsed)} />
          <Content style={{ background: '#f5f5f5', padding: 16, overflow: 'auto' }}>
            <Outlet />
          </Content>
        </Layout>
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
