import { Row, Col, Card, Statistic, Table, Tag, Spin, theme } from 'antd';
import {
  AppstoreOutlined,
  UserOutlined,
  ApiOutlined,
  DashboardOutlined,
  NodeIndexOutlined,
  CloudServerOutlined,
  HistoryOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { TrendChart, ModelUsageChart } from '@/components/charts';
import { useStats } from '@/services/query';

export default function AdminView() {
  const { t } = useTranslation('dashboard');
  const { token } = theme.useToken();
  const { hasPermission } = useAuthStore();
  const { data: statsData, isLoading: statsLoading } = useStats();

  const stats = {
    providerCount: statsData?.providerCount ?? 0,
    channelCount: statsData?.channelCount ?? 0,
    modelCount: statsData?.modelCount ?? 0,
    userCount: statsData?.userCount ?? 0,
    todayRequests: statsData?.todayRequests ?? 0,
    tokenUsage: statsData?.tokenUsage ?? '0',
  };

  const recentActivities = [
    { key: '1', action: t('activity.actions.createUser'), user: 'admin', target: 'user_001', time: t('activity.time.minutesAgo', { count: 2 }), status: 'success' },
    { key: '2', action: t('activity.actions.addModel'), user: 'admin', target: 'gpt-4-turbo', time: t('activity.time.minutesAgo', { count: 15 }), status: 'success' },
    { key: '3', action: t('activity.actions.keyExpired'), user: 'system', target: 'key_xxx', time: t('activity.time.hoursAgo', { count: 1 }), status: 'warning' },
    { key: '4', action: t('activity.actions.userLogin'), user: 'user_001', target: '-', time: t('activity.time.hoursAgo', { count: 2 }), status: 'success' },
    { key: '5', action: t('activity.actions.rateLimit'), user: 'user_002', target: 'api_calls', time: t('activity.time.hoursAgo', { count: 3 }), status: 'error' },
  ];

  const columns = [
    { title: t('activity.action'), dataIndex: 'action', key: 'action', width: 120 },
    { title: t('activity.user'), dataIndex: 'user', key: 'user', width: 100 },
    { title: t('activity.target'), dataIndex: 'target', key: 'target' },
    { title: t('activity.columnTime'), dataIndex: 'time', key: 'time', width: 100 },
    {
      title: t('activity.state'),
      dataIndex: 'status',
      key: 'state',
      width: 80,
      render: (status: string) => {
        const colorMap: Record<string, string> = { success: 'green', warning: 'orange', error: 'red' };
        const labelMap: Record<string, string> = { success: t('state.success'), warning: t('state.warning'), error: t('state.error') };
        return <Tag color={colorMap[status]}>{labelMap[status]}</Tag>;
      },
    },
  ];

  const statCards = [
    { key: 'providers', title: t('stats.providerCount'), value: stats.providerCount, icon: <CloudServerOutlined />, gradient: `linear-gradient(135deg, #13c2c2 0%, #08979c 100%)` },
    { key: 'channels', title: t('stats.channelCount'), value: stats.channelCount, icon: <NodeIndexOutlined />, gradient: `linear-gradient(135deg, ${token.colorError} 0%, ${token.colorErrorActive} 100%)` },
    { key: 'models', title: t('stats.modelCount'), value: stats.modelCount, icon: <AppstoreOutlined />, gradient: `linear-gradient(135deg, ${token.colorPrimary} 0%, ${token.colorPrimaryActive} 100%)` },
    { key: 'users', title: t('stats.userCount'), value: stats.userCount, icon: <UserOutlined />, gradient: `linear-gradient(135deg, ${token.colorSuccess} 0%, ${token.colorSuccessActive} 100%)` },
    { key: 'requests', title: t('stats.todayRequests'), value: stats.todayRequests, icon: <ApiOutlined />, gradient: `linear-gradient(135deg, ${token.colorWarning} 0%, ${token.colorWarningActive} 100%)` },
    { key: 'tokens', title: t('stats.tokenUsage'), value: stats.tokenUsage, icon: <DashboardOutlined />, gradient: `linear-gradient(135deg, #722ed1 0%, #531dab 100%)` },
  ];

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Spin spinning={statsLoading}>
        <Row gutter={16}>
          {statCards.map((card) => (
            <Col span={4} key={card.key}>
              <Card
                styles={{ body: { padding: '20px 24px', position: 'relative' as const, overflow: 'hidden' } }}
                style={{ height: '100%', border: 'none', boxShadow: token.boxShadow, transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)', cursor: 'pointer' }}
                classNames={{ body: 'stat-card-body' }}
                hoverable
              >
                <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 4, background: card.gradient, borderRadius: '8px 8px 0 0' }} />
                <Statistic
                  title={<span style={{ fontSize: 14, fontWeight: 500 }}>{card.title}</span>}
                  value={card.value}
                  prefix={<span style={{ opacity: 0.8, marginRight: 8 }}>{card.icon}</span>}
                  styles={{ content: { fontWeight: 600 } }}
                />
              </Card>
            </Col>
          ))}
        </Row>
      </Spin>

      <Row gutter={16} style={{ flex: 1 }}>
        <Col span={16}>
          <Card title={t('trend.title')} style={{ height: '100%', border: 'none', boxShadow: token.boxShadow }} styles={{ body: { height: 'calc(100% - 57px)', padding: '16px 24px 24px' } }}>
            <TrendChart />
          </Card>
        </Col>
        <Col span={8}>
          <Card title={t('modelUsage.title')} style={{ height: '100%', border: 'none', boxShadow: token.boxShadow }} styles={{ body: { height: 'calc(100% - 57px)', padding: '16px 24px 24px' } }}>
            <ModelUsageChart />
          </Card>
        </Col>
      </Row>

      {hasPermission(P.USER_READ) && (
        <Card
          title={<span><HistoryOutlined style={{ marginRight: 8, color: token.colorPrimary }} />{t('activity.title')}</span>}
          style={{ border: 'none', boxShadow: token.boxShadow }}
        >
          <Table dataSource={recentActivities} columns={columns} pagination={false} size="small" />
        </Card>
      )}
    </div>
  );
}