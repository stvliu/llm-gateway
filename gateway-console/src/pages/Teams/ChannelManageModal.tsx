'use client';

import { useEffect, useState } from 'react';
import { Modal, Table, Checkbox, Space, Tag, Typography, App, Alert, Spin } from 'antd';
import { useTranslation } from 'react-i18next';
import { channelApi } from '@/services/api/channel';
import { teamApi } from '@/services/api/team';
import type { Channel } from '@/types/channel';

const { Text } = Typography;

interface ChannelManageModalProps {
  open: boolean;
  teamId: number;
  teamName: string;
  onCancel: () => void;
}

/**
 * 团队渠道管理弹窗
 * 配置团队可访问的渠道，团队成员的 API Key 将继承这些渠道权限
 */
export default function ChannelManageModal({
  open,
  teamId,
  teamName,
  onCancel,
}: ChannelManageModalProps) {
  const { t } = useTranslation('teams');
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
        // 并行加载所有渠道和团队已配置的渠道
        const [allChannels, teamChannelIds] = await Promise.all([
          channelApi.list(),
          teamApi.listChannels(teamId),
        ]);
        setChannels(allChannels);
        setSelectedChannelIds(new Set(teamChannelIds));
      } catch (error) {
        console.error('加载渠道数据失败:', error);
        message.error(t('channelManage.loadError', { defaultValue: '加载渠道数据失败' }));
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [open, teamId, message, t]);

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
      await teamApi.updateChannels(teamId, Array.from(selectedChannelIds));
      message.success(t('channelManage.saveSuccess', { defaultValue: '渠道配置已保存' }));
      onCancel();
    } catch (error) {
      console.error('保存渠道配置失败:', error);
      message.error(t('channelManage.saveError', { defaultValue: '保存失败' }));
    } finally {
      setSaving(false);
    }
  };

  const columns = [
    {
      title: t('channelManage.channelName', { defaultValue: '渠道名称' }),
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
      title: t('channelManage.billingMode', { defaultValue: '计费模式' }),
      dataIndex: 'billingMode',
      key: 'billingMode',
      width: 120,
      render: (mode: string) => {
        const modeMap: Record<string, { label: string; color: string }> = {
          PAY_AS_YOU_GO: { label: t('channelManage.payAsYouGo', { defaultValue: '按量付费' }), color: 'green' },
          SUBSCRIPTION: { label: t('channelManage.subscription', { defaultValue: '订阅' }), color: 'blue' },
          PACKAGE: { label: t('channelManage.package', { defaultValue: '套餐' }), color: 'orange' },
        };
        const info = modeMap[mode] || { label: mode, color: 'default' };
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: t('channelManage.state', { defaultValue: '状态' }),
      dataIndex: 'state',
      key: 'state',
      width: 80,
      render: (state: string) => (
        <Tag color={state === 'ACTIVE' ? 'green' : 'default'}>
          {state === 'ACTIVE' ? t('channelManage.active', { defaultValue: '启用' }) : t('channelManage.inactive', { defaultValue: '禁用' })}
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
          {t('channelManage.accessible', { defaultValue: '可访问' })}
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

  const selectedCount = selectedChannelIds.size;
  const totalCount = channels.length;

  return (
    <Modal
      title={`${teamName} - ${t('channelManage.title', { defaultValue: '渠道管理' })}`}
      open={open}
      onCancel={onCancel}
      onOk={handleSave}
      confirmLoading={saving}
      width={800}
      destroyOnHidden
      okText={t('channelManage.save', { defaultValue: '保存' })}
      cancelText={t('channelManage.cancel', { defaultValue: '取消' })}
    >
      <div style={{ marginBottom: 16 }}>
        <Alert
          type="info"
          message={t('channelManage.permissionHint', { defaultValue: '配置该团队可访问的渠道，团队成员的 API Key 将继承这些渠道权限' })}
          style={{ marginBottom: 12 }}
          showIcon
        />
        <div style={{ marginTop: 8 }}>
          <Text>
            {t('channelManage.selectedCount', {
              defaultValue: `已选择 ${selectedCount}/${totalCount} 个渠道`,
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
          dataSource={channels}
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