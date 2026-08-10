/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import {
  Card,
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
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import {
  useFailoverEvents,
  useExhaustedEvents,
} from '@/services/query/useResilience';
import {
  errorTypeMeta,
  formatRoute,
  decisionMeta,
  type TagColor,
} from './eventDisplay';
import { CircuitBreakerDashboard } from './CircuitBreakerDashboard';
import type { FailoverEvent } from '@/types/resilience';

const { Text } = Typography;

/**
 * 容灾总览页
 *
 * <p>「选而非填」范式屏1：回答「现在稳不稳」，只读不配置。
 * <ul>
 *   <li>耗尽告警：最近 exhausted=true 转移事件高亮（事件级告警）</li>
 *   <li>端点熔断状态：各端点熔断器当前状态与应急操作（复用 CircuitBreakerButton）</li>
 *   <li>转移事件流：10s 轮询渲染最近转移事件，exhausted 行红色高亮</li>
 * </ul>
 * </p>
 *
 * <p>Task 9：故障域（Cluster）随应用级失败策略删除而退场，拓扑区块移除；
 * 共因跳过标记随 Cluster 退场移除。</p>
 * <p>Task 12：新增端点熔断状态大盘，总览页 = 耗尽告警 + 端点熔断状态 + 转移事件流。</p>
 */
export default function OverviewPage() {
  const { t } = useTranslation('resilience');

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

      {/* 端点熔断状态大盘 */}
      <Card
        title={t('overview.circuitBreakerDashboard')}
        style={{ marginBottom: 16 }}
      >
        <div style={{ marginBottom: 8, color: 'rgba(0,0,0,0.45)', fontSize: 13 }}>
          {t('overview.circuitBreakerDashboardHelp')}
        </div>
        <CircuitBreakerDashboard t={t} />
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
 * Trace。exhausted 行红色背景高亮。</p>
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
