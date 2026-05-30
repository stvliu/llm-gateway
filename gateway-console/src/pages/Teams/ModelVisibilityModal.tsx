import { useState, useEffect } from 'react';
import { Modal, Table, Checkbox, Space, Tag, Typography, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useModels } from '@/services/query/useModels';
import { teamApi } from '@/services/api/team';
import type { Team } from '@/types/team';
import type { Model } from '@/types/model';

const { Text } = Typography;

interface ModelVisibilityModalProps {
  open: boolean;
  team: Team | null;
  onClose: () => void;
}

/**
 * 团队模型可见性配置模态框
 * 允许管理员控制团队可见的模型列表
 */
export default function ModelVisibilityModal({
  open,
  team,
  onClose,
}: ModelVisibilityModalProps) {
  const { t } = useTranslation('teams');
  const { message } = App.useApp();
  const { data: models, isLoading } = useModels();

  const [visibility, setVisibility] = useState<Record<number, boolean>>({});
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(false);

  // 当模态框打开时，从 API 获取当前可见性配置
  useEffect(() => {
    if (open && team) {
      setLoading(true);
      teamApi.listAllowedModels(team.id)
        .then((allowedIds) => {
          const map: Record<number, boolean> = {};
          const allowedSet = new Set(allowedIds);
          // 如果 API 返回空列表，默认全部可见
          const allVisible = allowedIds.length === 0;
          models?.forEach((model: Model) => {
            map[model.id] = allVisible || allowedSet.has(model.id);
          });
          setVisibility(map);
        })
        .catch(() => {
          // 获取失败时默认全部可见
          const map: Record<number, boolean> = {};
          models?.forEach((model: Model) => {
            map[model.id] = true;
          });
          setVisibility(map);
        })
        .finally(() => setLoading(false));
    }
  }, [open, team, models]);

  const handleToggle = (modelId: number, checked: boolean) => {
    setVisibility((prev) => ({ ...prev, [modelId]: checked }));
  };

  const handleSelectAll = (checked: boolean) => {
    if (!models) return;
    const newVisibility: Record<number, boolean> = {};
    models.forEach((model: Model) => {
      newVisibility[model.id] = checked;
    });
    setVisibility(newVisibility);
  };

  const handleSave = async () => {
    if (!team) return;
    setSaving(true);
    try {
      const allowedIds = Object.entries(visibility)
        .filter(([, v]) => v)
        .map(([id]) => Number(id));
      await teamApi.updateAllowedModels(team.id, allowedIds);
      message.success(t('modelVisibility.saveSuccess', { defaultValue: '模型可见性配置已保存' }));
      onClose();
    } catch (err) {
      const errMsg = err instanceof Error ? err.message : '';
      message.error(errMsg || t('modelVisibility.saveError', { defaultValue: '保存失败' }));
    } finally {
      setSaving(false);
    }
  };

  const columns = [
    {
      title: t('modelVisibility.modelName', { defaultValue: '模型名称' }),
      dataIndex: 'modelName',
      key: 'modelName',
      render: (_text: string, record: Model) => (
        <Space>
          <Text strong>{record.displayName || record.modelName}</Text>
          {record.modelFamily && (
            <Tag color="blue">{record.modelFamily}</Tag>
          )}
        </Space>
      ),
    },
    {
      title: t('modelVisibility.state', { defaultValue: '状态' }),
      dataIndex: 'state',
      key: 'state',
      width: 100,
      render: (state: string) => (
        <Tag color={state === 'ACTIVE' ? 'green' : 'default'}>
          {state === 'ACTIVE' ? t('modelVisibility.active', { defaultValue: '启用' }) : t('modelVisibility.inactive', { defaultValue: '禁用' })}
        </Tag>
      ),
    },
    {
      title: (
        <Space>
          <Checkbox
            checked={models && models.length > 0 && models.every((m: Model) => visibility[m.id])}
            indeterminate={
              models &&
              models.some((m: Model) => visibility[m.id]) &&
              !models.every((m: Model) => visibility[m.id])
            }
            onChange={(e) => handleSelectAll(e.target.checked)}
          />
          {t('modelVisibility.visible', { defaultValue: '可见' })}
        </Space>
      ),
      key: 'visible',
      width: 100,
      render: (_: unknown, record: Model) => (
        <Checkbox
          checked={visibility[record.id] ?? true}
          onChange={(e) => handleToggle(record.id, e.target.checked)}
        />
      ),
    },
  ];

  const visibleCount = Object.values(visibility).filter(Boolean).length;
  const totalCount = models?.length || 0;

  return (
    <Modal
      title={`${team?.name ?? ''} - ${t('modelVisibility.title', { defaultValue: '模型可见性' })}`}
      open={open}
      onCancel={onClose}
      onOk={handleSave}
      confirmLoading={saving}
      width={700}
      destroyOnHidden
      okText={t('modelVisibility.save', { defaultValue: '保存' })}
      cancelText={t('modelVisibility.cancel', { defaultValue: '取消' })}
    >
      <div style={{ marginBottom: 16 }}>
        <Text type="secondary">
          {t('modelVisibility.description', { defaultValue: '配置该团队可访问的模型列表。取消勾选的模型将不会在该团队的模型列表中显示。' })}
        </Text>
        <div style={{ marginTop: 8 }}>
          <Text>
            {t('modelVisibility.selectedCount', {
              defaultValue: `已选择 ${visibleCount}/${totalCount} 个模型`,
              visibleCount,
              totalCount,
            })}
          </Text>
        </div>
      </div>

      <Table
        dataSource={models || []}
        columns={columns}
        loading={isLoading || loading}
        rowKey="id"
        pagination={false}
        size="small"
        scroll={{ y: 400 }}
      />
    </Modal>
  );
}
