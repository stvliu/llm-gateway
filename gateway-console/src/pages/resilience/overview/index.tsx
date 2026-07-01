import { useMemo } from 'react';
import {
  Card,
  Row,
  Col,
  Tag,
  Empty,
  Spin,
  Alert,
  Typography,
  Tooltip,
  Button,
  Table,
  Space,
} from 'antd';
import {
  ExclamationCircleFilled,
  ReloadOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import {
  useClusters,
  useFailoverEvents,
  useExhaustedEvents,
} from '@/services/query/useResilience';
import { useAllChannels } from '@/services/query/useChannels';
import { groupChannelsByCluster } from './grouping';
import {
  errorTypeMeta,
  formatRoute,
  decisionMeta,
  type TagColor,
} from './eventDisplay';
import type { Cluster, FailoverEvent } from '@/types/resilience';
import type { Channel } from '@/types/channel';

const { Text } = Typography;

/**
 * 故障域拓扑卡片
 *
 * <p>展示单个 Cluster 的基本信息（code/name/description/providerId）与成员渠道列表。
 * 成员渠道来自 ChannelResponse 透传的 clusterId 按 cluster 分组映射。</p>
 *
 * <p>Task 10：Cluster 语义改造为跨供应商故障独立性分组，域级健康聚合已删，
 * 不再展示健康灯 / region / priority。</p>
 */
function ClusterCard({
  cluster,
  members,
  t,
}: {
  cluster: Cluster;
  members: Channel[];
  t: (k: string) => string;
}) {
  return (
    <Card
      size="small"
      title={<span style={{ fontFamily: 'monospace' }}>{cluster.code}</span>}
    >
      <div style={{ marginBottom: 4 }}>
        <Text strong>{cluster.name}</Text>
      </div>
      <div style={{ fontSize: 12, color: 'rgba(0,0,0,0.45)', lineHeight: 1.8 }}>
        {cluster.description && <div>{cluster.description}</div>}
        <div>
          {t('cluster.providerId')}: {cluster.providerId}
        </div>
      </div>
      {/* 成员渠道：按 clusterId 分组映射到本 Cluster */}
      <div style={{ marginTop: 8, fontSize: 12 }}>
        <div style={{ marginBottom: 4, color: 'rgba(0,0,0,0.45)' }}>
          {t('overview.members')}（{members.length}）
        </div>
        {members.length === 0 ? (
          <div style={{ color: 'rgba(0,0,0,0.35)' }}>—</div>
        ) : (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
            {members.map((ch) => (
              <Tooltip key={ch.id} title={ch.name}>
                <Tag style={{ margin: 0 }}>
                  <Text code style={{ fontSize: 11 }}>
                    {ch.id}
                  </Text>{' '}
                  {ch.name}
                </Tag>
              </Tooltip>
            ))}
          </div>
        )}
      </div>
    </Card>
  );
}

/**
 * 容灾总览页
 *
 * <p>「选而非填」范式屏1：回答「现在稳不稳」，只读不配置。
 * <ul>
 *   <li>耗尽告警：最近 exhausted=true 转移事件高亮（域级健康聚合已删，仅事件级告警）</li>
 *   <li>故障域拓扑：Cluster 卡片视图，渠道按 clusterId 归域（跨供应商故障独立性分组）</li>
 *   <li>转移事件流：10s 轮询渲染最近转移事件，exhausted 行红色高亮，共因跳过事件标记</li>
 * </ul>
 * </p>
 *
 * <p>Task 10：移除降级/会话亲和/PinnedModel 展示；域级 DOWN 告警随 healthStatus 删除；
 * 转移事件流新增「是否共因跳过」标记。</p>
 */
export default function OverviewPage() {
  const { t } = useTranslation('resilience');
  const { data: clusters, isLoading } = useClusters();
  // 拉取全部渠道，按 clusterId 分组映射到 Cluster 卡片（后端 ChannelResponse 已透传 clusterId）
  const { data: channels } = useAllChannels();
  const channelsByCluster = useMemo(
    () => groupChannelsByCluster(channels ?? []),
    [channels],
  );

  // 转移事件流（10s 轮询，默认 100 条）
  const failoverQuery = useFailoverEvents();
  const exhaustedQuery = useExhaustedEvents();

  // 耗尽告警 - 事件级：最近 exhausted=true 转移事件
  const exhaustedEvents = exhaustedQuery.data ?? [];
  const failoverEvents = failoverQuery.data ?? [];

  return (
    <div>
      <Card title={t('overview.title')} style={{ marginBottom: 16 }}>
        <div style={{ color: 'rgba(0,0,0,0.45)', fontSize: 13 }}>
          {t('overview.subtitle')}
        </div>
      </Card>

      {/* 耗尽告警（事件级 exhausted） */}
      <Card
        title={
          <span>
            <ExclamationCircleFilled
              style={{ color: exhaustedEvents.length > 0 ? '#ff4d4f' : '#52c41a' }}
            />{' '}
            {t('overview.exhaustionAlert')}
          </span>
        }
        style={{ marginBottom: 16 }}
      >
        <div style={{ marginBottom: 8, color: 'rgba(0,0,0,0.45)', fontSize: 13 }}>
          {t('overview.exhaustionAlertHelp')}
        </div>
        {/* 事件级 exhausted 告警 */}
        {exhaustedEvents.length === 0 ? (
          <Alert type="success" message={t('overview.noExhaustion')} showIcon />
        ) : (
          <Alert
            type="error"
            showIcon
            message={t('overview.exhaustedEventsAlert', { count: exhaustedEvents.length })}
            description={
              <ul style={{ margin: 0, paddingLeft: 20 }}>
                {exhaustedEvents.slice(0, 5).map((e) => (
                  <li key={e.id}>
                    <Text code>{formatEventTime(e)}</Text> —{' '}
                    {e.traceId
                      ? t('overview.exhaustedEventItemWithTrace', {
                          channelId: e.fromChannelId ?? '?',
                          trace: e.traceId.slice(0, 8),
                        })
                      : t('overview.exhaustedEventItem', {
                          channelId: e.fromChannelId ?? '?',
                        })}
                  </li>
                ))}
                {exhaustedEvents.length > 5 && (
                  <li style={{ color: 'rgba(0,0,0,0.45)' }}>
                    {t('overview.moreExhaustedEvents', {
                      count: exhaustedEvents.length - 5,
                    })}
                  </li>
                )}
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
                <ClusterCard
                  cluster={cluster}
                  members={channelsByCluster.get(cluster.id) ?? []}
                  t={t}
                />
              </Col>
            ))}
          </Row>
        )}
      </Card>

      {/* 转移事件流（10s 轮询，exhausted 行红色高亮） */}
      <Card
        title={
          <Space>
            <span>{t('overview.eventStream')}</span>
            <Tag color="processing" style={{ fontSize: 11 }}>
              {t('overview.pollingInterval')}
            </Tag>
          </Space>
        }
        extra={
          <Button
            size="small"
            icon={<ReloadOutlined />}
            loading={failoverQuery.isFetching}
            onClick={() => failoverQuery.refetch()}
          >
            {t('overview.refresh')}
          </Button>
        }
      >
        <div style={{ marginBottom: 12, color: 'rgba(0,0,0,0.45)', fontSize: 13 }}>
          {t('overview.eventStreamHelp')}
        </div>
        {failoverQuery.isLoading ? (
          <div style={{ textAlign: 'center', padding: 48 }}>
            <Spin />
          </div>
        ) : failoverEvents.length === 0 ? (
          <Empty description={t('overview.noEvents')} />
        ) : (
          <FailoverEventTable events={failoverEvents} t={t} />
        )}
      </Card>
    </div>
  );
}

/**
 * 格式化事件时间为本地可读串
 */
function formatEventTime(ev: FailoverEvent): string {
  if (!ev.occurredAt) return '-';
  // 后端返回 ISO-8601 Instant，直接 new Date 解析
  const d = new Date(ev.occurredAt);
  if (Number.isNaN(d.getTime())) return ev.occurredAt;
  // YYYY-MM-DD HH:mm:ss
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

/**
 * 转移事件流表格
 *
 * <p>列：时间 / 转移路径（from→to 渠道）/ 原因（errorType Tag）/ 决策（L1 Tag）/
 * 是否共因跳过 / Trace。exhausted 行红色背景高亮，共因跳过行橙色 Tag 标记。</p>
 */
function FailoverEventTable({
  events,
  t,
}: {
  events: FailoverEvent[];
  t: (k: string) => string;
}) {
  return (
    <Table<FailoverEvent>
      size="small"
      rowKey="id"
      dataSource={events}
      pagination={{ pageSize: 20, showSizeChanger: false }}
      rowClassName={(ev) => (ev.exhausted ? 'failover-event-exhausted' : '')}
      columns={[
        {
          title: t('overview.eventColumns.time'),
          dataIndex: 'occurredAt',
          width: 170,
          render: (_, ev) => <Text style={{ fontFamily: 'monospace', fontSize: 12 }}>{formatEventTime(ev)}</Text>,
        },
        {
          title: t('overview.eventColumns.route'),
          key: 'route',
          width: 160,
          render: (_, ev) => {
            const route = formatRoute(ev);
            // exhausted 无目标时走 i18n 文案
            const label = route === 'exhaustedNoTarget' ? t('overview.exhaustedNoTarget') : route;
            return (
              <Text
                code
                style={{
                  fontFamily: 'monospace',
                  fontSize: 12,
                  color: ev.exhausted && route === 'exhaustedNoTarget' ? '#ff4d4f' : undefined,
                }}
              >
                {label}
              </Text>
            );
          },
        },
        {
          title: t('overview.eventColumns.reason'),
          dataIndex: 'errorType',
          width: 160,
          render: (errorType?: string | null) => {
            const meta = errorTypeMeta(errorType);
            return (
              <Tag color={meta.color as TagColor} style={{ margin: 0 }}>
                {errorType ?? '-'}
              </Tag>
            );
          },
        },
        {
          title: t('overview.eventColumns.decision'),
          dataIndex: 'decision',
          width: 100,
          render: (decision?: string | null) => {
            const meta = decisionMeta(decision);
            // labelKey 形如 'overview.decisions.L1'，直接走 t；未知决策回退原始值
            const label = meta.labelKey.startsWith('overview.')
              ? t(meta.labelKey)
              : meta.labelKey || '-';
            return (
              <Tag color={meta.color as TagColor} style={{ margin: 0 }}>
                {label}
              </Tag>
            );
          },
        },
        {
          // 是否共因跳过（Task 9 新增）：true 表示同域共因跳过转移
          title: t('overview.eventColumns.commonCauseSkip'),
          dataIndex: 'commonCauseSkip',
          width: 110,
          render: (commonCauseSkip?: boolean) =>
            commonCauseSkip ? (
              <Tooltip title={t('overview.commonCauseSkipHelp')}>
                <Tag color="orange" style={{ margin: 0 }} icon={<WarningOutlined />}>
                  {t('overview.commonCauseSkip')}
                </Tag>
              </Tooltip>
            ) : (
              <Text type="secondary">-</Text>
            ),
        },
        {
          title: t('overview.eventColumns.trace'),
          dataIndex: 'traceId',
          render: (traceId?: string | null) =>
            traceId ? (
              <Tooltip title={traceId}>
                <Text code style={{ fontSize: 11 }}>
                  {traceId.slice(0, 8)}…
                </Text>
              </Tooltip>
            ) : (
              <Text type="secondary">-</Text>
            ),
        },
      ]}
    />
  );
}
