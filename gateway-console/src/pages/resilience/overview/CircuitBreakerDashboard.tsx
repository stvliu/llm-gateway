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
/**
 * 端点熔断状态大盘组件
 *
 * <p>容灾总览页「选而非填」范式：拉取全部渠道端点，逐端点展示熔断器当前状态与应急操作。
 * 复用既有 CircuitBreakerButton（含 useCircuitBreakerState 查询 + force-open/force-close），
 * 避免应急操作逻辑重复。</p>
 *
 * <p>表格列：渠道名 / 端点 URL / 协议 / 熔断状态 + 应急按钮。
 * 渠道 × 端点展开为一行一端点。</p>
 */
import { Table, Empty, Spin } from 'antd';
import { useAllChannels } from '@/services/query/useChannels';
import { CircuitBreakerButton } from '@/pages/Channels/CircuitBreakerButton';
import type { Channel } from '@/types/channel';

/** 单行数据：渠道 + 端点组合 */
interface EndpointRow {
  key: string;
  channelId: number;
  channelName: string;
  endpointId: number;
  endpointUrl: string;
  protocol: string;
}

interface CircuitBreakerDashboardProps {
  /** i18n 翻译函数（由总览页注入，命名空间 resilience） */
  t: (key: string) => string;
}

/**
 * 将渠道列表展开为「一行一端点」数据
 *
 * @param channels 渠道列表
 * @returns 端点行数组
 */
function flattenEndpoints(channels: Channel[] | undefined): EndpointRow[] {
  if (!channels) return [];
  return channels.flatMap((ch) =>
    ch.endpoints.map((ep) => ({
      key: `${ch.id}-${ep.id}`,
      channelId: ch.id,
      channelName: ch.name,
      endpointId: ep.id,
      endpointUrl: ep.endpointUrl,
      protocol: ep.protocol,
    })),
  );
}

/**
 * 端点熔断状态大盘
 *
 * <p>拉取全部渠道端点，逐端点渲染熔断状态 Tag + 应急操作按钮（复用 CircuitBreakerButton）。
 * loading 显示 Spin；空列表显示 Empty。</p>
 */
export function CircuitBreakerDashboard({ t }: CircuitBreakerDashboardProps) {
  const channelsQuery = useAllChannels();
  const rows = flattenEndpoints(channelsQuery.data);

  if (channelsQuery.isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 48 }}>
        <Spin />
      </div>
    );
  }

  if (rows.length === 0) {
    return <Empty description={t('overview.noEndpoints')} />;
  }

  return (
    <Table<EndpointRow>
      size="small"
      rowKey="key"
      dataSource={rows}
      pagination={false}
      columns={[
        {
          title: t('overview.circuitBreakerColumns.channel'),
          dataIndex: 'channelName',
          width: 180,
        },
        {
          title: t('overview.circuitBreakerColumns.endpoint'),
          dataIndex: 'endpointUrl',
          render: (url: string) => (
            <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{url}</span>
          ),
        },
        {
          title: t('overview.circuitBreakerColumns.protocol'),
          dataIndex: 'protocol',
          width: 100,
        },
        {
          title: t('overview.circuitBreakerColumns.operation'),
          key: 'operation',
          width: 200,
          render: (_, row) => (
            <CircuitBreakerButton
              channelId={row.channelId}
              endpointId={row.endpointId}
            />
          ),
        },
      ]}
    />
  );
}
