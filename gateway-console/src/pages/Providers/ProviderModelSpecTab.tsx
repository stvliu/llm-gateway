import { useState, useCallback } from 'react';
import { Button, Table, Tag, Space, Popconfirm, Tooltip, App } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  StopOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import {
  useModelSpecs,
  useDeleteModelSpec,
  useSetEnabledModelSpec,
} from '@/services/query/useModelSpecs';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import ModelSpecFormModal from './ModelSpecFormModal';
import type { ModelSpec } from '@/types/modelSpec';

interface ProviderModelSpecTabProps {
  editing?: boolean;
}

/** 状态 Tag 颜色映射 */
function stateColor(state: string): 'success' | 'default' {
  return state === 'ACTIVE' ? 'success' : 'default';
}

/**
 * 供应商模型规格标签页
 * 展示供应商下的模型规格列表，支持创建、编辑、启用/禁用、删除操作
 * 权限控制：无 PROVIDER_WRITE 时隐藏所有写操作按钮
 */
export default function ProviderModelSpecTab({ editing }: ProviderModelSpecTabProps) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.PROVIDER_WRITE);

  // 数据查询与变更
  const { data: modelSpecs, isLoading } = useModelSpecs();
  const deleteMutation = useDeleteModelSpec();
  const setEnabledMutation = useSetEnabledModelSpec();

  // 弹窗状态
  const [formOpen, setFormOpen] = useState(false);
  const [editingSpec, setEditingSpec] = useState<ModelSpec | null>(null);

  // --- 操作回调 ---

  const handleCreate = useCallback(() => {
    setEditingSpec(null);
    setFormOpen(true);
  }, []);

  const handleEdit = useCallback((spec: ModelSpec) => {
    setEditingSpec(spec);
    setFormOpen(true);
  }, []);

  const handleDelete = useCallback(
    async (id: number) => {
      await deleteMutation.mutateAsync(id);
      message.success(t('modelSpec.deleteSuccess', { defaultValue: '模型规格删除成功' }));
    },
    [deleteMutation, message, t],
  );

  const handleToggleEnabled = useCallback(
    async (spec: ModelSpec) => {
      const enabled = spec.state !== 'ACTIVE';
      await setEnabledMutation.mutateAsync({ id: spec.id, enabled });
      message.success(enabled ? t('modelSpec.enabled', { defaultValue: '模型规格已启用' }) : t('modelSpec.disabled', { defaultValue: '模型规格已禁用' }));
    },
    [setEnabledMutation, message, t],
  );

  // --- 渲染辅助 ---

  /** 渲染 capabilities 标签 */
  const renderCapabilities = (capabilities?: Record<string, boolean>) => {
    if (!capabilities) return '-';
    const active = Object.entries(capabilities)
      .filter(([, v]) => v)
      .map(([k]) => k);
    return active.length > 0 ? (
      <Space size={4} wrap>
        {active.map((c) => (
          <Tag key={c}>{c}</Tag>
        ))}
      </Space>
    ) : (
      '-'
    );
  };

  // 列定义
  const columns = [
    {
      title: t('modelSpec.providerModelId'),
      dataIndex: 'providerModelId',
      key: 'providerModelId',
    },
    {
      title: t('modelSpec.displayName'),
      dataIndex: 'displayName',
      key: 'displayName',
      render: (v: string) => v || '-',
    },
    {
      title: t('modelSpec.modelFamily'),
      dataIndex: 'modelFamily',
      key: 'modelFamily',
      render: (v: string) => v || '-',
    },
    {
      title: t('modelSpec.contextWindow'),
      dataIndex: 'contextWindow',
      key: 'contextWindow',
      width: 120,
      render: (v: number) => (v ? v.toLocaleString() : '-'),
    },
    {
      title: t('modelSpec.capabilities'),
      dataIndex: 'capabilities',
      key: 'capabilities',
      render: renderCapabilities,
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
    ...(canWrite && editing
      ? [
          {
            title: t('fields.action'),
            key: 'action',
            width: 160,
            render: (_: unknown, record: ModelSpec) => (
              <Space size="small">
                <Tooltip title={t('actions.edit')}>
                  <Button
                    type="text"
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => handleEdit(record)}
                  />
                </Tooltip>
                <Tooltip
                  title={
                    record.state === 'ACTIVE'
                      ? t('actions.disable')
                      : t('actions.enable')
                  }
                >
                  <Popconfirm
                    title={
                      record.state === 'ACTIVE'
                        ? t('modelSpec.confirmDisable', { defaultValue: '确定要禁用该模型规格吗？' })
                        : t('modelSpec.confirmEnable', { defaultValue: '确定要启用该模型规格吗？' })
                    }
                    onConfirm={() => handleToggleEnabled(record)}
                    okText={t('actions.confirm')}
                    cancelText={t('actions.cancel')}
                  >
                    <Button
                      type="text"
                      size="small"
                      icon={record.state === 'ACTIVE' ? <StopOutlined /> : <CheckCircleOutlined />}
                    />
                  </Popconfirm>
                </Tooltip>
                <Popconfirm
                  title={t('modelSpec.confirmDelete', { defaultValue: '确定要删除该模型规格吗？' })}
                  onConfirm={() => handleDelete(record.id)}
                  okText={t('actions.confirm')}
                  cancelText={t('actions.cancel')}
                >
                  <Tooltip title={t('actions.delete')}>
                    <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                  </Tooltip>
                </Popconfirm>
              </Space>
            ),
          },
        ]
      : []),
  ];

  return (
    <div>
      {canWrite && editing && (
        <Button
          type="primary"
          icon={<PlusOutlined />}
          style={{ marginBottom: 16 }}
          onClick={handleCreate}
        >
          {t('modelSpec.create')}
        </Button>
      )}

      <Table
        dataSource={modelSpecs}
        columns={columns}
        loading={isLoading}
        rowKey="id"
        pagination={false}
        size="small"
      />

      {/* 模型规格创建/编辑弹窗 */}
      <ModelSpecFormModal
        open={formOpen}
        modelSpec={editingSpec}
        onClose={() => {
          setFormOpen(false);
          setEditingSpec(null);
        }}
      />
    </div>
  );
}
