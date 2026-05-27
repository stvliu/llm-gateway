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
  App,
} from 'antd';
import { PlusOutlined, DisconnectOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useChannelModels, useCreateChannelModel, useDeleteChannelModel, useSetEnabledChannelModel } from '@/services/query/useChannelModels';
import { useModelSpecs } from '@/services/query/useModelSpecs';
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
 * 展示渠道下已关联的模型列表，支持关联新模型、解绑、启用/停用
 */
export default function ChannelModelsPanel({ channelId, canWrite }: ChannelModelsPanelProps) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();

  // 数据查询
  const { data: channelModels, isLoading } = useChannelModels(channelId);
  const { data: allModelSpecs } = useModelSpecs();

  // 变更操作
  const createMutation = useCreateChannelModel();
  const deleteMutation = useDeleteChannelModel();
  const setEnabledMutation = useSetEnabledChannelModel();

  // 弹窗状态
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedModelSpecId, setSelectedModelSpecId] = useState<number | undefined>(undefined);

  /** 已关联的 modelSpecId 集合，用于筛选可选模型 */
  const associatedModelSpecIds = useMemo(
    () => new Set((channelModels ?? []).map((cm) => cm.modelSpecId)),
    [channelModels],
  );

  /** 可选的模型列表（排除已关联的） */
  const availableModelSpecs = useMemo(
    () => (allModelSpecs ?? []).filter((ms) => !associatedModelSpecIds.has(ms.id)),
    [allModelSpecs, associatedModelSpecIds],
  );

  // --- 操作处理 ---

  const handleOpenModal = useCallback(() => {
    setSelectedModelSpecId(undefined);
    setModalOpen(true);
  }, []);

  const handleCloseModal = useCallback(() => {
    setModalOpen(false);
    setSelectedModelSpecId(undefined);
  }, []);

  const handleCreate = useCallback(async () => {
    if (!selectedModelSpecId) return;
    try {
      await createMutation.mutateAsync({ channelId, data: { modelSpecId: selectedModelSpecId } });
      message.success(t('channelModel.add'));
      handleCloseModal();
    } catch {
      message.error(t('channelModel.add'));
    }
  }, [createMutation, channelId, selectedModelSpecId, message, t, handleCloseModal]);

  const handleDelete = useCallback(
    async (record: ChannelModel) => {
      try {
        await deleteMutation.mutateAsync({ channelId, id: record.id });
        message.success(t('channelModel.deleteSuccess'));
      } catch {
        message.error(t('channelModel.deleteSuccess'));
      }
    },
    [deleteMutation, channelId, message, t],
  );

  const handleToggleEnabled = useCallback(
    async (record: ChannelModel, enabled: boolean) => {
      try {
        await setEnabledMutation.mutateAsync({ channelId, id: record.id, enabled });
        message.success(enabled ? t('channelModel.enabled') : t('channelModel.disabled'));
      } catch {
        message.error(enabled ? t('channelModel.enabled') : t('channelModel.disabled'));
      }
    },
    [setEnabledMutation, channelId, message, t],
  );

  // --- 列定义 ---

  const columns = [
    {
      title: t('channelModel.providerModelId'),
      dataIndex: 'providerModelId',
      key: 'providerModelId',
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
            width: 140,
            render: (_: unknown, record: ChannelModel) => (
              <Space size="small">
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
                    loading={setEnabledMutation.isPending}
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
        <Select
          style={{ width: '100%' }}
          placeholder={t('channelModel.searchPlaceholder')}
          showSearch
          value={selectedModelSpecId}
          onChange={setSelectedModelSpecId}
          filterOption={(input, option) => {
            const label = option?.label ?? '';
            return label.toString().toLowerCase().includes(input.toLowerCase());
          }}
          options={availableModelSpecs.map((ms) => ({
            value: ms.id,
            label: `${ms.providerModelId}${ms.displayName ? ` - ${ms.displayName}` : ''}`,
          }))}
        />
      </Modal>
    </div>
  );
}