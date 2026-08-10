/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
'use client';

import { useEffect, useMemo, useState } from 'react';
import { Modal, Table, Checkbox, Space, Tag, Typography, App, Alert, Spin, InputNumber } from 'antd';
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
 * 配置应用可访问的渠道及其应用级转移优先级，应用下的 API Key 将继承这些渠道权限。
 *
 * Task gap2：priority 由渠道级只读展示改为应用级可编辑，保存到 ApplicationChannel.priority。
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
  // 已选中的渠道：key=channelId，value=应用级转移优先级（null 表示未配置，回退默认 100）
  const [selected, setSelected] = useState<Map<number, number | null>>(new Map());
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

        const appChannels = await applicationApi.listChannels(applicationId).catch(() => {
          // 非管理员可能无权查询应用渠道，此时默认为空
          return [] as { channelId: number; priority: number | null }[];
        });
        // 用应用级 priority 初始化选中渠道
        const next = new Map<number, number | null>();
        for (const item of appChannels) {
          next.set(item.channelId, item.priority);
        }
        setSelected(next);
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
   *
   * 勾选时 priority 默认 null（回退 100）；取消勾选时从 map 移除。
   */
  const handleToggle = (channelId: number, checked: boolean) => {
    setSelected((prev) => {
      const next = new Map(prev);
      if (checked) {
        next.set(channelId, prev.has(channelId) ? prev.get(channelId)! : null);
      } else {
        next.delete(channelId);
      }
      return next;
    });
  };

  /**
   * 修改某渠道的应用级 priority
   */
  const handlePriorityChange = (channelId: number, value: number | null) => {
    setSelected((prev) => {
      // 仅当选中时才记录 priority
      if (!prev.has(channelId)) return prev;
      const next = new Map(prev);
      // InputNumber 清空返回 null；非负整数校验由控件 min/step 保证
      next.set(channelId, value == null ? null : Math.floor(value));
      return next;
    });
  };

  /**
   * 全选/取消全选
   */
  const handleSelectAll = (checked: boolean) => {
    setSelected((prev) => {
      if (checked) {
        const next = new Map<number, number | null>();
        for (const c of channels) {
          next.set(c.id, prev.get(c.id) ?? null);
        }
        return next;
      } else {
        return new Map();
      }
    });
  };

  /**
   * 保存配置
   *
   * 将选中的渠道及其应用级 priority 提交后端，null 表示未配置（后端回退默认值 100）。
   */
  const handleSave = async () => {
    setSaving(true);
    try {
      const items = Array.from(selected.entries()).map(([channelId, priority]) => ({
        channelId,
        priority,
      }));
      await applicationApi.updateChannels(applicationId, items);
      message.success(t('channelAuthorization.saveSuccess'));
      onCancel();
    } catch {
      message.error(t('channelAuthorization.saveError'));
    } finally {
      setSaving(false);
    }
  };

  /**
   * 渲染应用级 priority 编辑控件
   *
   * 未选中渠道时禁用编辑；选中时允许输入非负整数，留空表示默认（100）。
   */
  const renderPriority = (record: Channel) => {
    const isSelected = selected.has(record.id);
    const priority = selected.get(record.id) ?? null;
    return (
      <InputNumber
        size="small"
        min={0}
        step={1}
        value={priority}
        disabled={!isSelected}
        placeholder={t('channelAuthorization.priorityPlaceholder')}
        style={{ width: 80 }}
        onChange={(value) => handlePriorityChange(record.id, value)}
      />
    );
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
      // 应用级转移优先级（数值越小越优先），可编辑；留空表示默认（100）
      title: (
        <Space direction="vertical" size={0}>
          <span>{t('channelAuthorization.priority')}</span>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {t('channelAuthorization.priorityHint')}
          </Text>
        </Space>
      ),
      key: 'priority',
      width: 110,
      render: (_: unknown, record: Channel) => renderPriority(record),
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
            checked={channels.length > 0 && channels.every((c) => selected.has(c.id))}
            indeterminate={
              channels.length > 0 &&
              selected.size > 0 &&
              selected.size < channels.length
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
          checked={selected.has(record.id)}
          onChange={(e) => handleToggle(record.id, e.target.checked)}
        />
      ),
    },
  ];

  // 按应用级 priority 升序排序展示（数值越小越优先，null 视为默认值 100）
  const sortedChannels = useMemo(() => {
    const priorityOf = (channelId: number): number => {
      const p = selected.get(channelId);
      return p == null ? 100 : p;
    };
    return [...channels].sort((a, b) => priorityOf(a.id) - priorityOf(b.id));
  }, [channels, selected]);

  const selectedCount = selected.size;
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
