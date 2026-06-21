import { Card, Row, Col, Tag, Empty, Spin, Alert, Typography, Tooltip } from 'antd';
import {
  CheckCircleFilled,
  ExclamationCircleFilled,
  CloseCircleFilled,
  InfoCircleOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useClusters } from '@/services/query/useResilience';
import type { Cluster, ClusterHealthStatus } from '@/types/resilience';

const { Text } = Typography;

/**
 * 健康状态 → 颜色/图标/文案 映射
 */
function healthMeta(status: ClusterHealthStatus, t: (k: string) => string) {
  switch (status) {
    case 'HEALTHY':
      return {
        color: 'green' as const,
        icon: <CheckCircleFilled style={{ color: '#52c41a' }} />,
        label: t('overview.healthStatus.HEALTHY'),
      };
    case 'DEGRADED':
      return {
        color: 'orange' as const,
        icon: <ExclamationCircleFilled style={{ color: '#faad14' }} />,
        label: t('overview.healthStatus.DEGRADED'),
      };
    case 'DOWN':
      return {
        color: 'red' as const,
        icon: <CloseCircleFilled style={{ color: '#ff4d4f' }} />,
        label: t('overview.healthStatus.DOWN'),
      };
    default:
      return {
        color: 'default' as const,
        icon: <InfoCircleOutlined />,
        label: status,
      };
  }
}

/**
 * 故障域拓扑卡片
 *
 * <p>展示单个 Cluster 的健康灯与基本信息。成员渠道映射待后端 ChannelResponse
 * 透传 clusterId 后接入（当前 ChannelResponse 不含 clusterId）。</p>
 */
function ClusterCard({ cluster, t }: { cluster: Cluster; t: (k: string) => string }) {
  const meta = healthMeta(cluster.healthStatus, t);
  return (
    <Card
      size="small"
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {meta.icon}
          <span style={{ fontFamily: 'monospace' }}>{cluster.code}</span>
        </div>
      }
      extra={<Tag color={meta.color}>{meta.label}</Tag>}
    >
      <div style={{ marginBottom: 4 }}>
        <Text strong>{cluster.name}</Text>
      </div>
      <div style={{ fontSize: 12, color: 'rgba(0,0,0,0.45)', lineHeight: 1.8 }}>
        <div>
          {t('cluster.region')}: {cluster.region || '-'}
          {t('cluster.priority')}: P{cluster.priority}
        </div>
        <div>
          {t('cluster.providerId')}: {cluster.providerId}
        </div>
      </div>
      {/* 成员渠道：待后端 ChannelResponse 透传 clusterId 后接入 */}
      <div style={{ marginTop: 8, fontSize: 12, color: 'rgba(0,0,0,0.35)' }}>
        <Tooltip title={t('overview.clusterTopologyHelp')}>
          {t('overview.members')}: —
        </Tooltip>
      </div>
    </Card>
  );
}

/**
 * 容灾总览页
 *
 * <p>「选而非填」范式屏1：回答「现在稳不稳」，只读不配置。
 * <ul>
 *   <li>故障域拓扑：Cluster 卡片视图，每卡显示健康灯（绿/黄/红）</li>
 *   <li>耗尽告警：healthStatus=DOWN 的域高亮提示</li>
 *   <li>转移事件流：占位，待 Task 4.11c 接入实时事件流</li>
 * </ul>
 * </p>
 */
export default function OverviewPage() {
  const { t } = useTranslation('resilience');
  const { data: clusters, isLoading } = useClusters();

  // 耗尽告警：healthStatus=DOWN 的故障域
  const downClusters = (clusters ?? []).filter((c) => c.healthStatus === 'DOWN');

  return (
    <div>
      <Card title={t('overview.title')} style={{ marginBottom: 16 }}>
        <div style={{ color: 'rgba(0,0,0,0.45)', fontSize: 13 }}>
          {t('overview.subtitle')}
        </div>
      </Card>

      {/* 耗尽告警 */}
      <Card
        title={
          <span>
            <ExclamationCircleFilled style={{ color: downClusters.length > 0 ? '#ff4d4f' : '#52c41a' }} />{' '}
            {t('overview.exhaustionAlert')}
          </span>
        }
        style={{ marginBottom: 16 }}
      >
        <div style={{ marginBottom: 8, color: 'rgba(0,0,0,0.45)', fontSize: 13 }}>
          {t('overview.exhaustionAlertHelp')}
        </div>
        {downClusters.length === 0 ? (
          <Alert type="success" message={t('overview.noExhaustion')} showIcon />
        ) : (
          <Alert
            type="error"
            showIcon
            message={`${downClusters.length} 个故障域处于 DOWN 状态`}
            description={
              <ul style={{ margin: 0, paddingLeft: 20 }}>
                {downClusters.map((c) => (
                  <li key={c.id}>
                    <Text code>{c.code}</Text> ({c.name})
                  </li>
                ))}
              </ul>
            }
          />
        )}
      </Card>

      {/* 故障域拓扑 */}
      <Card title={t('overview.clusterTopology')} style={{ marginBottom: 16 }}>
        <div style={{ marginBottom: 12, color: 'rgba(0,0,0,0.45)', fontSize: 13 }}>
          {t('overview.clusterTopologyHelp')}
        </div>
        {isLoading ? (
          <div style={{ textAlign: 'center', padding: 48 }}>
            <Spin />
          </div>
        ) : !clusters || clusters.length === 0 ? (
          <Empty description={t('overview.noClusters')} />
        ) : (
          <Row gutter={[16, 16]}>
            {clusters.map((cluster) => (
              <Col key={cluster.id} xs={24} sm={12} md={8} lg={6}>
                <ClusterCard cluster={cluster} t={t} />
              </Col>
            ))}
          </Row>
        )}
      </Card>

      {/* 转移事件流：占位，待 Task 4.11c */}
      <Card title={t('overview.eventStream')}>
        <Alert
          type="info"
          showIcon
          message={t('overview.eventStreamPlaceholder')}
        />
      </Card>
    </div>
  );
}
