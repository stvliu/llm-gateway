import { useState, useCallback, useMemo } from 'react';
import {
  Button,
  Table,
  Tag,
  Space,
  Popconfirm,
  Tooltip,
  Switch,
  Modal,
  Select,
  Input,
  App,
} from 'antd';
import {
  PlusOutlined,
  DisconnectOutlined,
  EditOutlined,
  CheckOutlined,
  CloseOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import {
  useChannelModels,
  useCreateChannelModel,
  useDeleteChannelModel,
  useSetEnabledChannelModel,
  useUpdateUpstreamModelName,
} from '@/services/query/useChannelModels';
import { useModels } from '@/services/query/useModels';
import type { ChannelModel } from '@/types/channelModel';

interface ChannelModelsPanelProps {
  channelId: number;
  canWrite: boolean;
}

/** 状态 Tag 颜色映射 */
function stateColor(state: string): 'success' | 'default' {
  return state === 'ACTIVE' ? 'success' : 'default';
}

/**
 * 渠道模型关联面板
 * 展示渠道下已关联的模型列表，支持关联新模型、解绑、启用/停用、编辑上游模型名
 */
export default function ChannelModelsPanel({ channelId, canWrite }: ChannelModelsPanelProps) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();

  // 数据查询
  const { data: channelModels, isLoading } = useChannelModels(channelId);
  const { data: allModels } = useModels();

  // 变更操作
  const createMutation = useCreateChannelModel();
  const deleteMutation = useDeleteChannelModel();
  const setEnabledMutation = useSetEnabledChannelModel();
  const updateUpstreamMutation = useUpdateUpstreamModelName();

  // 弹窗状态
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedModelId, setSelectedModelId] = useState<number | undefined>(undefined);
  const [upstreamModelNameInput, setUpstreamModelNameInput] = useState('');

  // 行内编辑状态
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingValue, setEditingValue] = useState('');

  /** 开关加载状态（按记录 ID） */
  const [pendingToggleId, setPendingToggleId] = useState<number | null>(null);

  /** 已关联的 modelId 集合，用于筛选可选模型 */
  const associatedModelIds = useMemo(
    () => new Set((channelModels ?? []).map((cm) => cm.modelId)),
    [channelModels],
  );

  /** 可选的模型列表（排除已关联的） */
  const availableModels = useMemo(
    () => (allModels ?? []).filter((m) => !associatedModelIds.has(m.id)),
    [allModels, associatedModelIds],
  );

  // --- 关联模型弹窗操作 ---

  const handleOpenModal = useCallback(() => {
    setSelectedModelId(undefined);
    setUpstreamModelNameInput('');
    setModalOpen(true);
  }, []);

  const handleCloseModal = useCallback(() => {
    setModalOpen(false);
    setSelectedModelId(undefined);
    setUpstreamModelNameInput('');
  }, []);

  const handleCreate = useCallback(async () => {
    if (!selectedModelId) return;
    try {
      await createMutation.mutateAsync({
        channelId,
        data: {
          modelId: selectedModelId,
          upstreamModelName: upstreamModelNameInput.trim() || undefined,
        },
      });
      message.success(t('channelModel.add'));
      handleCloseModal();
    } catch {
      message.error(t('channelModel.addFailed') || t('channelModel.add'));
    }
  }, [createMutation, channelId, selectedModelId, upstreamModelNameInput, message, t, handleCloseModal]);

  // --- 解绑 ---

  const handleDelete = useCallback(
    async (record: ChannelModel) => {
      try {
        await deleteMutation.mutateAsync({ channelId, id: record.id });
        message.success(t('channelModel.deleteSuccess'));
      } catch {
        message.error(t('channelModel.deleteFailed') || t('channelModel.deleteSuccess'));
      }
    },
    [deleteMutation, channelId, message, t],
  );

  // --- 启停 ---

  const handleToggleEnabled = useCallback(
    async (record: ChannelModel, enabled: boolean) => {
      setPendingToggleId(record.id);
      try {
        await setEnabledMutation.mutateAsync({ channelId, id: record.id, enabled });
        message.success(enabled ? t('channelModel.enabled') : t('channelModel.disabled'));
        setPendingToggleId(null);
      } catch {
        message.error(
          enabled
            ? t('channelModel.enabledFailed') || t('channelModel.enabled')
            : t('channelModel.disabledFailed') || t('channelModel.disabled'),
        );
        setPendingToggleId(null);
      }
    },
    [setEnabledMutation, channelId, message, t],
  );

  // --- 行内编辑上游模型名 ---

  const startEditing = useCallback((record: ChannelModel) => {
    setEditingId(record.id);
    setEditingValue(record.upstreamModelName ?? '');
  }, []);

  const cancelEditing = useCallback(() => {
    setEditingId(null);
    setEditingValue('');
  }, []);

  const saveEditing = useCallback(
    async (record: ChannelModel) => {
      try {
        const newValue = editingValue || null; // 空字符串视为 null
        await updateUpstreamMutation.mutateAsync({
          channelId,
          id: record.id,
          data: { upstreamModelName: newValue },
        });
        setEditingId(null);
        message.success(t('channelModel.upstreamUpdated'));
      } catch {
        message.error(t('channelModel.upstreamUpdateFailed') || t('channelModel.upstreamUpdated'));
      }
    },
    [updateUpstreamMutation, channelId, editingValue, message, t],
  );

  // --- 列定义 ---

  const columns = [
    {
      title: t('channelModel.modelName'),
      dataIndex: 'modelName',
      key: 'modelName',
      width: 200,
    },
    {
      title: t('channelModel.displayName'),
      dataIndex: 'displayName',
      key: 'displayName',
      width: 200,
      render: (text: string | undefined) => text || '-',
    },
    {
      title: t('channelModel.modelFamily'),
      dataIndex: 'modelFamily',
      key: 'modelFamily',
      width: 150,
      render: (text: string | undefined) => text || '-',
    },
    {
      title: t('channelModel.upstreamModelName'),
      dataIndex: 'upstreamModelName',
      key: 'upstreamModelName',
      width: 220,
      render: (text: string | undefined, record: ChannelModel) => {
        if (editingId === record.id && canWrite) {
          return (
            <Space size="small">
              <Input
                size="small"
                value={editingValue}
                onChange={(e) => setEditingValue(e.target.value)}
                placeholder={record.modelName}
                style={{ width: 140 }}
              />
              <Tooltip title={t('actions.confirm', { ns: 'common' })}>
                <Button
                  type="text"
                  size="small"
                  icon={<CheckOutlined />}
                  onClick={() => saveEditing(record)}
                />
              </Tooltip>
              <Tooltip title={t('actions.cancel', { ns: 'common' })}>
                <Button
                  type="text"
                  size="small"
                  icon={<CloseOutlined />}
                  onClick={cancelEditing}
                />
              </Tooltip>
            </Space>
          );
        }
        return text ? (
          <span>{text}</span>
        ) : (
          <Tag style={{ color: '#999', borderColor: '#d9d9d9' }}>
            {t('channelModel.upstreamDefault')}
          </Tag>
        );
      },
    },
    {
      title: t('fields.status'),
      dataIndex: 'state',
      key: 'state',
      width: 80,
      render: (state: string) => (
        <Tag color={stateColor(state)}>
          {state === 'ACTIVE' ? t('state.active') : t('state.inactive')}
        </Tag>
      ),
    },
    ...(canWrite
      ? [
          {
            title: t('fields.action'),
            key: 'action',
            width: 180,
            render: (_: unknown, record: ChannelModel) => (
              <Space size="small">
                <Tooltip title={t('channelModel.editUpstream')}>
                  <Button
                    type="text"
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => startEditing(record)}
                  />
                </Tooltip>
                <Tooltip
                  title={
                    record.state === 'ACTIVE'
                      ? t('actions.disable', { ns: 'common' })
                      : t('actions.enable', { ns: 'common' })
                  }
                >
                  <Switch
                    size="small"
                    checked={record.state === 'ACTIVE'}
                    loading={pendingToggleId === record.id}
                    onChange={(checked) => handleToggleEnabled(record, checked)}
                  />
                </Tooltip>
                <Popconfirm
                  title={t('channelModel.confirmDelete')}
                  onConfirm={() => handleDelete(record)}
                  okText={t('actions.confirm', { ns: 'common' })}
                  cancelText={t('actions.cancel', { ns: 'common' })}
                >
                  <Tooltip title={t('channelModel.delete')}>
                    <Button type="text" size="small" danger icon={<DisconnectOutlined />} />
                  </Tooltip>
                </Popconfirm>
              </Space>
            ),
          },
        ]
      : []),
  ];

  return (
    <div style={{ padding: '8px 0' }}>
      {/* 工具栏 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 8,
        }}
      >
        <span style={{ fontWeight: 500 }}>{t('channelModel.title')}</span>
        {canWrite && (
          <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={handleOpenModal}>
            {t('channelModel.add')}
          </Button>
        )}
      </div>

      {/* 关联模型列表 */}
      <Table
        dataSource={channelModels}
        loading={isLoading}
        rowKey="id"
        pagination={false}
        size="small"
        columns={columns}
        locale={{ emptyText: t('channelModel.noModels') }}
      />

      {/* 关联模型弹窗 */}
      <Modal
        title={t('channelModel.selectModel')}
        open={modalOpen}
        onOk={handleCreate}
        onCancel={handleCloseModal}
        confirmLoading={createMutation.isPending}
        okText={t('actions.confirm', { ns: 'common' })}
        cancelText={t('actions.cancel', { ns: 'common' })}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Select
            style={{ width: '100%' }}
            placeholder={t('channelModel.searchPlaceholder')}
            showSearch
            value={selectedModelId}
            onChange={setSelectedModelId}
            filterOption={(input, option) => {
              const label = option?.label ?? '';
              return label.toString().toLowerCase().includes(input.toLowerCase());
            }}
            options={availableModels.map((m) => ({
              value: m.id,
              label: `${m.modelName}${m.displayName ? ` - ${m.displayName}` : ''}`,
            }))}
          />
          <Input
            placeholder={t('channelModel.upstreamModelNamePlaceholder')}
            value={upstreamModelNameInput}
            onChange={(e) => setUpstreamModelNameInput(e.target.value)}
            allowClear
          />
        </div>
      </Modal>
    </div>
  );
}