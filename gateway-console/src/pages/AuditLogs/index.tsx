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
import { useState, useMemo } from 'react';
import { Card, Table, Select, Button, Space, Tag, DatePicker, Alert, Tooltip, Typography } from 'antd';
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import { useAuditLogs } from '@/services/query/useAuditLogs';
import { useUsers } from '@/services/query/useUsers';
import type { AuditLogItem } from '@/types/audit';
import type { User } from '@/types/user';

const { RangePicker } = DatePicker;
const { Text } = Typography;

/**
 * 审计日志页
 *
 * <p>展示管理操作审计记录（仅 ADMIN 可见）。支持按结果、时间范围筛选与服务端分页。
 * 操作人以 userId 关联用户列表展示用户名。</p>
 */
export default function AuditLogs() {
  const { t } = useTranslation('auditLogs');
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(20);
  const [resultFilter, setResultFilter] = useState<'SUCCESS' | 'FAILURE' | undefined>();
  const [timeRange, setTimeRange] = useState<[Dayjs, Dayjs] | null>(null);

  const params = useMemo(() => {
    const p: {
      page: number;
      limit: number;
      result?: 'SUCCESS' | 'FAILURE';
      startTime?: string;
      endTime?: string;
    } = { page, limit };
    if (resultFilter) p.result = resultFilter;
    if (timeRange) {
      p.startTime = timeRange[0].startOf('day').toISOString();
      p.endTime = timeRange[1].endOf('day').toISOString();
    }
    return p;
  }, [page, limit, resultFilter, timeRange]);

  const { data, isLoading, isError, isFetching, refetch } = useAuditLogs(params);
  // 操作人映射：审计只存 userId，用用户列表展示用户名（参照 DownstreamKeysTable 的 userMap 模式）
  const { data: usersData } = useUsers({ page: 1, size: 200 });

  const userMap = useMemo(() => {
    const map = new Map<number, User>();
    usersData?.items?.forEach((u: User) => map.set(u.id, u));
    return map;
  }, [usersData]);

  const handleResultFilter = (value?: string) => {
    setResultFilter(value as 'SUCCESS' | 'FAILURE' | undefined);
    setPage(1);
  };

  const handleTimeChange = (dates: [Dayjs | null, Dayjs | null] | null) => {
    if (dates && dates[0] && dates[1]) {
      setTimeRange([dates[0], dates[1]]);
    } else {
      setTimeRange(null);
    }
    setPage(1);
  };

  const handleTableChange = (pagination: { current?: number; pageSize?: number }) => {
    if (pagination.current) setPage(pagination.current);
    if (pagination.pageSize) setLimit(pagination.pageSize);
  };

  const columns = [
    {
      title: t('time', { defaultValue: '操作时间' }),
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: t('user', { defaultValue: '操作人' }),
      dataIndex: 'userId',
      key: 'userId',
      width: 120,
      render: (userId: number) => {
        const user = userMap.get(userId);
        if (user) return `${user.username} (${userId})`;
        return userId === 0 ? <Text type="secondary">{t('anonymous', { defaultValue: '未认证' })}</Text> : `用户 ${userId}`;
      },
    },
    {
      title: t('action', { defaultValue: '操作' }),
      dataIndex: 'action',
      key: 'action',
      width: 220,
      render: (val: string) => <Text code style={{ fontSize: 12 }}>{val}</Text>,
    },
    {
      title: t('resource', { defaultValue: '资源' }),
      dataIndex: 'resource',
      key: 'resource',
      ellipsis: true,
      render: (val: string) => val || '-',
    },
    {
      title: t('result', { defaultValue: '结果' }),
      dataIndex: 'result',
      key: 'result',
      width: 100,
      render: (result: string) =>
        result === 'SUCCESS' ? (
          <Tag color="green">{t('success', { defaultValue: '成功' })}</Tag>
        ) : (
          <Tag color="red">{t('failure', { defaultValue: '失败' })}</Tag>
        ),
    },
    {
      title: t('ip', { defaultValue: '客户端 IP' }),
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      width: 140,
      render: (val: string) => val || '-',
    },
  ];

  return (
    <div>
      <Card
        title={t('title', { defaultValue: '审计日志' })}
        extra={
          <Button
            size="small"
            icon={<ReloadOutlined />}
            loading={isFetching}
            onClick={() => refetch()}
          >
            {t('refresh', { defaultValue: '刷新' })}
          </Button>
        }
      >
        <div style={{ marginBottom: 16, display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
          <Space size={12} wrap>
            <Select
              data-testid="result-filter-select"
              placeholder={t('filterByResult', { defaultValue: '筛选结果' })}
              style={{ width: 140 }}
              allowClear
              value={resultFilter}
              onChange={handleResultFilter}
              options={[
                { value: 'SUCCESS', label: t('success', { defaultValue: '成功' }) },
                { value: 'FAILURE', label: t('failure', { defaultValue: '失败' }) },
              ]}
            />
            <RangePicker
              value={timeRange}
              onChange={handleTimeChange}
              allowClear
            />
            <Tooltip title={t('filterHint', { defaultValue: '按时间范围筛选' })}>
              <SearchOutlined style={{ color: '#999' }} />
            </Tooltip>
          </Space>
        </div>

        {isError && (
          <Alert
            type="error"
            showIcon
            style={{ marginBottom: 16 }}
            message={t('loadFailed', { defaultValue: '加载失败' })}
            description={t('loadFailedHint', { defaultValue: '请检查网络或稍后重试' })}
            action={
              <Button size="small" onClick={() => refetch()}>
                {t('retry', { defaultValue: '重试' })}
              </Button>
            }
          />
        )}

        <Table<AuditLogItem>
          dataSource={data?.items ?? []}
          columns={columns}
          rowKey="id"
          size="middle"
          loading={isLoading}
          pagination={{
            current: page,
            pageSize: limit,
            total: data?.pagination?.total ?? 0,
            showSizeChanger: true,
            showTotal: (total) => t('total', { defaultValue: '共 {{count}} 条', count: total }),
            onChange: (current, size) => handleTableChange({ current, pageSize: size }),
          }}
          locale={{
            emptyText: t('noLogs', { defaultValue: '暂无审计日志' }),
          }}
        />
      </Card>
    </div>
  );
}
