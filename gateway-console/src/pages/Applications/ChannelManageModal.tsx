'use client';

import { useEffect, useMemo, useState } from 'react';
import { Modal, Table, Checkbox, Space, Tag, Typography, App, Alert, Spin } from 'antd';
import { useTranslation } from 'react-i18next';
import { channelApi } from '@/services/api/channel';
import { applicationApi } from '@/services/api/application';
import type { Channel } from '@/types/channel';

const { Text } = Typography;

interface ChannelManageModalProps {
  open: boolean;
  applicationId: number;
  applicationName: string;
  onCancel: () => void;
}

/**
 * 应用渠道授权弹窗
 *
 * 配置应用可访问的渠道，应用下的 API Key 将继承这些渠道权限。
 */
export default function ChannelManageModal({
  open,
  applicationId,
  applicationName,
  onCancel,
}: ChannelManageModalProps) {
  const { t } = useTranslation('applications');
  const { message } = App.useApp();

  // 所有渠道列表
  const [channels, setChannels] = useState<Channel[]>([]);
  // 已选中的渠道 ID 集合
  const [selectedChannelIds, setSelectedChannelIds] = useState<Set<number>>(new Set());
  // 加载状态
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  // 加载渠道数据
  useEffect(() => {
    if (!open) return;

    const loadData = async () => {
      setLoading(true);
      try {
        // 独立加载，避免某个 API 失败导致全部数据丢失
        const allChannels = await channelApi.list();
        setChannels(allChannels);

        const appChannelIds = await applicationApi.listChannels(applicationId).catch(() => {
          // 非管理员可能无权查询应用渠道，此时默认为空
          return [] as number[];
        });
        setSelectedChannelIds(new Set(appChannelIds));
      } catch {
        message.error(t('channelAuthorization.loadError'));
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [open, applicationId, message, t]);

  /**
   * 切换渠道选中状态
   */
  const handleToggle = (channelId: number, checked: boolean) => {
    setSelectedChannelIds((prev) => {
      const next = new Set(prev);
      if (checked) {
        next.add(channelId);
      } else {
        next.delete(channelId);
      }
      return next;
    });
  };

  /**
   * 全选/取消全选
   */
  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedChannelIds(new Set(channels.map((c) => c.id)));
    } else {
      setSelectedChannelIds(new Set());
    }
  };

  /**
   * 保存配置
   */
  const handleSave = async () => {
    setSaving(true);
    try {
      await applicationApi.updateChannels(applicationId, Array.from(selectedChannelIds));
      message.success(t('channelAuthorization.saveSuccess'));
      onCancel();
    } catch {
      message.error(t('channelAuthorization.saveError'));
    } finally {
      setSaving(false);
    }
  };

  const columns = [
    {
      title: t('channelAuthorization.channelName'),
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: Channel) => (
        <Space>
          <Text strong>{name}</Text>
          {record.providerName && <Tag color="blue">{record.providerName}</Tag>}
        </Space>
      ),
    },
    {
      // 渠道转移优先级（数值越小越优先），按此列升序展示先后次序
      title: t('channelAuthorization.priority'),
      dataIndex: 'priority',
      key: 'priority',
      width: 90,
      render: (priority: number) => <Tag color="blue">P{priority}</Tag>,
    },
    {
      title: t('channelAuthorization.billingMode'),
      dataIndex: 'billingMode',
      key: 'billingMode',
      width: 120,
      render: (mode: string) => {
        const modeMap: Record<string, { label: string; color: string }> = {
          pay_as_you_go: { label: t('channelAuthorization.payAsYouGo'), color: 'green' },
          subscription: { label: t('channelAuthorization.subscription'), color: 'blue' },
          package: { label: t('channelAuthorization.package'), color: 'orange' },
        };
        const info = modeMap[mode] || { label: mode, color: 'default' };
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: t('channelAuthorization.state'),
      dataIndex: 'state',
      key: 'state',
      width: 80,
      render: (state: string) => (
        <Tag color={state === 'ACTIVE' ? 'green' : 'default'}>
          {state === 'ACTIVE' ? t('channelAuthorization.active') : t('channelAuthorization.inactive')}
        </Tag>
      ),
    },
    {
      title: (
        <Space>
          <Checkbox
            checked={channels.length > 0 && channels.every((c) => selectedChannelIds.has(c.id))}
            indeterminate={
              channels.length > 0 &&
              selectedChannelIds.size > 0 &&
              selectedChannelIds.size < channels.length
            }
            onChange={(e) => handleSelectAll(e.target.checked)}
          />
          {t('channelAuthorization.accessible')}
        </Space>
      ),
      key: 'accessible',
      width: 100,
      render: (_: unknown, record: Channel) => (
        <Checkbox
          checked={selectedChannelIds.has(record.id)}
          onChange={(e) => handleToggle(record.id, e.target.checked)}
        />
      ),
    },
  ];

  // 按 priority 升序排序展示（数值越小越优先，定义 L1 转移先后次序）
  const sortedChannels = useMemo(
    () => [...channels].sort((a, b) => a.priority - b.priority),
    [channels],
  );

  const selectedCount = selectedChannelIds.size;
  const totalCount = channels.length;

  return (
    <Modal
      title={`${applicationName} - ${t('channelAuthorization.title')}`}
      open={open}
      onCancel={onCancel}
      onOk={handleSave}
      confirmLoading={saving}
      width={800}
      destroyOnHidden
      okText={t('channelAuthorization.save')}
      cancelText={t('channelAuthorization.cancel')}
    >
      <div style={{ marginBottom: 16 }}>
        <Alert
          type="info"
          message={t('channelAuthorization.permissionHint')}
          style={{ marginBottom: 12 }}
          showIcon
        />
        <div style={{ marginTop: 8 }}>
          <Text>
            {t('channelAuthorization.selectedCount', {
              selectedCount,
              totalCount,
            })}
          </Text>
        </div>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <Spin />
        </div>
      ) : (
        <Table
          dataSource={sortedChannels}
          columns={columns}
          rowKey="id"
          pagination={false}
          size="small"
          scroll={{ y: 400 }}
        />
      )}
    </Modal>
  );
}
