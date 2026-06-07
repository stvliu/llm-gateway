import { useState, useEffect } from 'react';
import { Layout, FloatButton, theme } from 'antd';
import { CommentOutlined } from '@ant-design/icons';
import { Outlet } from 'react-router-dom';
import { LeftHeader, RightHeader } from './Header';
import { Sidebar } from './Sidebar';
import { ChatPanel } from '@/components/chat';
import { useChatStore } from '@/stores/chatStore';

const { Sider, Content } = Layout;

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const { isOpen, setOpen } = useChatStore();
  const { token } = theme.useToken();

  // 同步 body 背景色到主题，防止滚动时底部露出白色
  useEffect(() => {
    document.body.style.backgroundColor = token.colorBgLayout;
    return () => { document.body.style.backgroundColor = ''; };
  }, [token.colorBgLayout]);

  return (
    <Layout style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <Sider
        width={200}
        collapsedWidth={80}
        collapsed={collapsed}
        style={{ background: token.colorBgContainer }}
      >
        <LeftHeader collapsed={collapsed} />
        <Sidebar collapsed={collapsed} />
      </Sider>
      <Layout style={{ minHeight: '100vh', background: token.colorBgLayout }}>
        <RightHeader collapsed={collapsed} onToggle={() => setCollapsed(!collapsed)} />
        <Content style={{ padding: 16, overflow: 'auto', minHeight: 'calc(100vh - 48px)', background: token.colorBgLayout }}>
          <Outlet />
        </Content>
      </Layout>

      <FloatButton
        icon={<CommentOutlined />}
        type="primary"
        onClick={() => setOpen(true)}
        style={{
          right: isOpen ? 400 : 24,
          transition: 'right 0.3s',
        }}
      />

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
    </Layout>
  );
}