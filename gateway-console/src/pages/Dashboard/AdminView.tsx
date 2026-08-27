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
import { useMemo } from 'react';
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
import dayjs from 'dayjs';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { TrendChart, ModelUsageChart } from '@/components/charts';
import { useStats, useStatsTrend, useStatsModelUsage } from '@/services/query';
import { useAuditLogs } from '@/services/query/useAuditLogs';
import { useUsers } from '@/services/query/useUsers';

export default function AdminView() {
  const { t } = useTranslation('dashboard');
  const { token } = theme.useToken();
  const { hasPermission } = useAuthStore();
  const { data: statsData, isLoading: statsLoading } = useStats();
  // 趋势与模型用量：真实统计端点（替代早期模拟数据）
  const { data: trendData, isLoading: trendLoading } = useStatsTrend(7);
  const { data: modelUsageData, isLoading: modelUsageLoading } = useStatsModelUsage(5);
  // 最近活动：真实管理操作审计（最近 5 条），替代早期硬编码假数据
  const { data: auditData, isLoading: auditLoading } = useAuditLogs({ page: 1, limit: 5 });
  const { data: usersData } = useUsers({ page: 1, size: 200 });

  const stats = {
    providerCount: statsData?.providerCount ?? 0,
    channelCount: statsData?.channelCount ?? 0,
    modelCount: statsData?.modelCount ?? 0,
    userCount: statsData?.userCount ?? 0,
    todayRequests: statsData?.todayRequests ?? 0,
    tokenUsage: statsData?.tokenUsage ?? '0',
  };

  // 操作人映射：审计只存 userId，用用户列表展示用户名
  const userMap = useMemo(() => {
    const map = new Map<number, { username: string }>();
    usersData?.items?.forEach((u: { id: number; username: string }) => map.set(u.id, u));
    return map;
  }, [usersData]);

  const activityColumns = [
    {
      title: t('activity.columnTime'),
      dataIndex: 'createdAt',
      key: 'time',
      width: 170,
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: t('activity.user'),
      dataIndex: 'userId',
      key: 'user',
      width: 120,
      render: (userId: number) => userMap.get(userId)?.username ?? (userId === 0 ? '-' : `用户 ${userId}`),
    },
    { title: t('activity.action'), dataIndex: 'action', key: 'action', width: 200 },
    { title: t('activity.target'), dataIndex: 'resource', key: 'target', ellipsis: true },
    {
      title: t('activity.state'),
      dataIndex: 'result',
      key: 'state',
      width: 80,
      render: (result: string) =>
        result === 'SUCCESS' ? (
          <Tag color="green">{t('state.success')}</Tag>
        ) : (
          <Tag color="red">{t('state.failure')}</Tag>
        ),
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
            <TrendChart data={trendData} loading={trendLoading} />
          </Card>
        </Col>
        <Col span={8}>
          <Card title={t('modelUsage.title')} style={{ height: '100%', border: 'none', boxShadow: token.boxShadow }} styles={{ body: { height: 'calc(100% - 57px)', padding: '16px 24px 24px' } }}>
            <ModelUsageChart data={modelUsageData} loading={modelUsageLoading} />
          </Card>
        </Col>
      </Row>

      {hasPermission(P.USER_READ) && (
        <Card
          title={<span><HistoryOutlined style={{ marginRight: 8, color: token.colorPrimary }} />{t('activity.title')}</span>}
          style={{ border: 'none', boxShadow: token.boxShadow }}
        >
          <Table
            dataSource={auditData?.items ?? []}
            columns={activityColumns}
            rowKey="id"
            loading={auditLoading}
            pagination={false}
            size="small"
            locale={{ emptyText: t('activity.noActivity', { defaultValue: '暂无活动' }) }}
          />
        </Card>
      )}
    </div>
  );
}