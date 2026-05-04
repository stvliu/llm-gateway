import { useState } from 'react';
import { Layout } from 'antd';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { TabBar } from './TabBar';

const { Sider, Content } = Layout;

export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <Layout style={{ height: '100vh' }}>
      <Header collapsed={collapsed} onToggle={() => setCollapsed(!collapsed)} />
      <Layout>
        <Sider
          width={200}
          collapsedWidth={64}
          collapsed={collapsed}
          style={{ background: '#fff' }}
        >
          <Sidebar collapsed={collapsed} role="ADMIN" />
        </Sider>
        <Content style={{ background: '#f5f5f5' }}>
          <TabBar role="ADMIN" />
        </Content>
      </Layout>
    </Layout>
  );
}
