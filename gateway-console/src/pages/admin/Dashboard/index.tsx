import { Row, Col, Card, Statistic, Table, Tag } from 'antd';
import {
  AppstoreOutlined,
  TeamOutlined,
  ApiOutlined,
  DashboardOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useThemeStore } from '@/stores/themeStore';
import { TrendChart, ModelUsageChart } from '@/components/charts';

/**
 * 管理员仪表盘页面
 * 采用官网设计风格：渐变背景、卡片阴影、hover 效果
 */
export default function AdminDashboard() {
  const { t } = useTranslation('dashboard');
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  // 静态数据 - 后续接入真实 API
  const stats = {
    modelCount: 12,
    userCount: 45,
    todayRequests: 1256,
    tokenUsage: '5.6M',
  };

  // 静态数据 - 后续接入真实 API
  // TODO: 接入后端 API，替换 mockData
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
    { title: t('activity.time'), dataIndex: 'time', key: 'time', width: 100 },
    {
      title: t('activity.status'),
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: string) => {
        const colorMap: Record<string, string> = {
          success: 'green',
          warning: 'orange',
          error: 'red',
        };
        const labelMap: Record<string, string> = {
          success: t('status.success'),
          warning: t('status.warning'),
          error: t('status.error'),
        };
        return <Tag color={colorMap[status]}>{labelMap[status]}</Tag>;
      },
    },
  ];

  // 统计卡片配置
  const statCards = [
    {
      key: 'models',
      title: t('stats.modelCount'),
      value: stats.modelCount,
      icon: <AppstoreOutlined />,
      gradient: 'linear-gradient(135deg, #1677ff 0%, #4096ff 100%)',
    },
    {
      key: 'users',
      title: t('stats.userCount'),
      value: stats.userCount,
      icon: <TeamOutlined />,
      gradient: 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)',
    },
    {
      key: 'requests',
      title: t('stats.todayRequests'),
      value: stats.todayRequests,
      icon: <ApiOutlined />,
      gradient: 'linear-gradient(135deg, #722ed1 0%, #9254de 100%)',
    },
    {
      key: 'tokens',
      title: t('stats.tokenUsage'),
      value: stats.tokenUsage,
      icon: <DashboardOutlined />,
      gradient: 'linear-gradient(135deg, #fa8c16 0%, #ffc53d 100%)',
    },
  ];

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* 统计卡片 */}
      <Row gutter={16}>
        {statCards.map((card) => (
          <Col span={6} key={card.key}>
            <Card
              styles={{
                body: {
                  padding: '20px 24px',
                  position: 'relative' as const,
                  overflow: 'hidden',
                },
              }}
              style={{
                height: '100%',
                border: 'none',
                boxShadow: isDark
                  ? '0 2px 8px rgba(0, 0, 0, 0.3)'
                  : '0 2px 8px rgba(0, 0, 0, 0.06)',
                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                cursor: 'pointer',
              }}
              classNames={{
                body: 'stat-card-body',
              }}
              hoverable
            >
              {/* 渐变装饰条 */}
              <div
                style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  right: 0,
                  height: 4,
                  background: card.gradient,
                  borderRadius: '8px 8px 0 0',
                }}
              />
              <Statistic
                title={
                  <span style={{ fontSize: 14, fontWeight: 500 }}>
                    {card.title}
                  </span>
                }
                value={card.value}
                prefix={
                  <span style={{ opacity: 0.8, marginRight: 8 }}>
                    {card.icon}
                  </span>
                }
                valueStyle={{ fontWeight: 600 }}
              />
            </Card>
          </Col>
        ))}
      </Row>

      {/* 趋势图表 */}
      <Row gutter={16} style={{ flex: 1 }}>
        <Col span={16}>
          <Card
            title={t('trend.title')}
            style={{
              height: '100%',
              border: 'none',
              boxShadow: isDark
                ? '0 2px 8px rgba(0, 0, 0, 0.3)'
                : '0 2px 8px rgba(0, 0, 0, 0.06)',
            }}
            styles={{
              body: {
                height: 'calc(100% - 57px)',
                padding: '16px 24px 24px',
              },
            }}
          >
            <TrendChart />
          </Card>
        </Col>
        <Col span={8}>
          <Card
            title={t('modelUsage.title')}
            style={{
              height: '100%',
              border: 'none',
              boxShadow: isDark
                ? '0 2px 8px rgba(0, 0, 0, 0.3)'
                : '0 2px 8px rgba(0, 0, 0, 0.06)',
            }}
            styles={{
              body: {
                height: 'calc(100% - 57px)',
                padding: '16px 24px 24px',
              },
            }}
          >
            <ModelUsageChart />
          </Card>
        </Col>
      </Row>

      {/* 最近活动 */}
      <Card
        title={
          <span>
            <WarningOutlined style={{ marginRight: 8, color: '#faad14' }} />
            {t('activity.title')}
          </span>
        }
        style={{
          border: 'none',
          boxShadow: isDark
            ? '0 2px 8px rgba(0, 0, 0, 0.3)'
            : '0 2px 8px rgba(0, 0, 0, 0.06)',
        }}
      >
        <Table
          dataSource={recentActivities}
          columns={columns}
          pagination={false}
          size="small"
        />
      </Card>
    </div>
  );
}
