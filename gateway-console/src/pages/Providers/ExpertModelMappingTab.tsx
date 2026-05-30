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
  Empty,
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
import { useChannels } from '@/services/query/useChannels';
import {
  useChannelModels,
  useCreateChannelModel,
  useDeleteChannelModel,
  useSetEnabledChannelModel,
  useUpdateUpstreamModelName,
} from '@/services/query/useChannelModels';
import { useModelCatalogs } from '@/services/query/useCatalog';
import type { Provider } from '@/types/provider';
import type { ChannelModel } from '@/types/channelModel';

interface Props {
  provider: Provider | null;
}

/** 状态 Tag 颜色映射 */
function stateColor(state: string): 'success' | 'default' {
  return state === 'ACTIVE' ? 'success' : 'default';
}

/**
 * 专家模式 - 模型映射标签页
 * 管理供应商下各渠道的模型关联，支持渠道切换、添加/删除/启停/编辑上游模型名
 */
export default function ExpertModelMappingTab({ provider }: Props) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();

  // ---- 渠道选择 ----
  const { data: channels, isLoading: channelsLoading } = useChannels(provider?.id ?? 0);
  const [selectedChannelId, setSelectedChannelId] = useState<number | undefined>(undefined);

  // 当渠道列表加载完成且尚未选择时，自动选中第一个
  const effectiveChannelId = useMemo(() => {
    if (selectedChannelId) return selectedChannelId;
    if (channels && channels.length > 0) return channels[0].id;
    return undefined;
  }, [selectedChannelId, channels]);

  // ---- 渠道模型数据 ----
  const { data: channelModels, isLoading: modelsLoading } = useChannelModels(effectiveChannelId ?? 0);

  // ---- 模型目录（用于添加模型弹窗的自动补全） ----
  const [catalogKeyword, setCatalogKeyword] = useState('');
  const { data: modelCatalogs } = useModelCatalogs({ keyword: catalogKeyword || undefined });

  // ---- 变更操作 ----
  const createMutation = useCreateChannelModel();
  const deleteMutation = useDeleteChannelModel();
  const setEnabledMutation = useSetEnabledChannelModel();
  const updateUpstreamMutation = useUpdateUpstreamModelName();

  // ---- 添加模型弹窗状态 ----
  const [modalOpen, setModalOpen] = useState(false);
  const [addChannelId, setAddChannelId] = useState<number | undefined>(undefined);
  const [selectedModelName, setSelectedModelName] = useState<string | undefined>(undefined);
  const [upstreamModelNameInput, setUpstreamModelNameInput] = useState('');

  // ---- 行内编辑上游模型名 ----
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingValue, setEditingValue] = useState('');

  // ---- 开关加载状态 ----
  const [pendingToggleId, setPendingToggleId] = useState<number | null>(null);

  // ---- 已关联的 modelName 集合，用于筛选可选模型 ----
  const associatedModelNames = useMemo(
    () => new Set((channelModels ?? []).map((cm) => cm.modelName).filter(Boolean)),
    [channelModels],
  );

  // ---- 可选的模型目录列表（排除已关联的） ----
  const availableCatalogs = useMemo(
    () => (modelCatalogs ?? []).filter((mc) => !associatedModelNames.has(mc.modelName)),
    [modelCatalogs, associatedModelNames],
  );

  // ---- 渠道选项 ----
  const channelOptions = useMemo(
    () => (channels ?? []).map((ch) => ({ value: ch.id, label: ch.name })),
    [channels],
  );

  // ---- 模型目录自动补全选项 ----
  const catalogOptions = useMemo(
    () =>
      availableCatalogs.map((mc) => ({
        value: mc.modelName,
        label: mc.modelName,
      })),
    [availableCatalogs],
  );

  // ==================== 操作回调 ====================

  /** 打开添加模型弹窗 */
  const handleOpenModal = useCallback(() => {
    setAddChannelId(effectiveChannelId);
    setSelectedModelName(undefined);
    setUpstreamModelNameInput('');
    setCatalogKeyword('');
    setModalOpen(true);
  }, [effectiveChannelId]);

  /** 关闭添加模型弹窗 */
  const handleCloseModal = useCallback(() => {
    setModalOpen(false);
    setAddChannelId(undefined);
    setSelectedModelName(undefined);
    setUpstreamModelNameInput('');
    setCatalogKeyword('');
  }, []);

  /** 提交添加模型 */
  const handleCreate = useCallback(async () => {
    if (!addChannelId || !selectedModelName) return;
    // 从目录中查找对应的 modelId（通过 modelName 匹配）
    const catalog = modelCatalogs?.find((mc) => mc.modelName === selectedModelName);
    if (!catalog) {
      message.error(t('modelMapping.catalogNotFound', { defaultValue: '未找到模型目录记录' }));
      return;
    }
    try {
      await createMutation.mutateAsync({
        channelId: addChannelId,
        data: {
          modelId: 0, // modelId 需要从已物化的模型中获取，此处先用 modelName 关联
          upstreamModelName: upstreamModelNameInput.trim() || undefined,
        },
      });
      message.success(t('channelModel.add', { defaultValue: '添加成功' }));
      handleCloseModal();
    } catch {
      message.error(t('channelModel.addFailed', { defaultValue: '添加失败' }));
    }
  }, [addChannelId, selectedModelName, modelCatalogs, upstreamModelNameInput, createMutation, message, t, handleCloseModal]);

  /** 删除模型关联 */
  const handleDelete = useCallback(
    async (record: ChannelModel) => {
      if (!effectiveChannelId) return;
      try {
        await deleteMutation.mutateAsync({ channelId: effectiveChannelId, id: record.id });
        message.success(t('channelModel.deleteSuccess', { defaultValue: '删除成功' }));
      } catch {
        message.error(t('channelModel.deleteFailed', { defaultValue: '删除失败' }));
      }
    },
    [deleteMutation, effectiveChannelId, message, t],
  );

  /** 启停模型关联 */
  const handleToggleEnabled = useCallback(
    async (record: ChannelModel, enabled: boolean) => {
      if (!effectiveChannelId) return;
      setPendingToggleId(record.id);
      try {
        await setEnabledMutation.mutateAsync({ channelId: effectiveChannelId, id: record.id, enabled });
        message.success(
          enabled
            ? t('channelModel.enabled', { defaultValue: '已启用' })
            : t('channelModel.disabled', { defaultValue: '已停用' }),
        );
      } catch {
        message.error(
          enabled
            ? t('channelModel.enabledFailed', { defaultValue: '启用失败' })
            : t('channelModel.disabledFailed', { defaultValue: '停用失败' }),
        );
      } finally {
        setPendingToggleId(null);
      }
    },
    [setEnabledMutation, effectiveChannelId, message, t],
  );

  /** 开始编辑上游模型名 */
  const startEditing = useCallback((record: ChannelModel) => {
    setEditingId(record.id);
    setEditingValue(record.upstreamModelName ?? '');
  }, []);

  /** 取消编辑 */
  const cancelEditing = useCallback(() => {
    setEditingId(null);
    setEditingValue('');
  }, []);

  /** 保存编辑上游模型名 */
  const saveEditing = useCallback(
    async (record: ChannelModel) => {
      if (!effectiveChannelId) return;
      try {
        const newValue = editingValue.trim() || null;
        await updateUpstreamMutation.mutateAsync({
          channelId: effectiveChannelId,
          id: record.id,
          data: { upstreamModelName: newValue },
        });
        setEditingId(null);
        message.success(t('channelModel.upstreamUpdated', { defaultValue: '上游模型名已更新' }));
      } catch {
        message.error(t('channelModel.upstreamUpdateFailed', { defaultValue: '更新失败' }));
      }
    },
    [updateUpstreamMutation, effectiveChannelId, editingValue, message, t],
  );

  // ==================== 列定义 ====================

  const columns = [
    {
      title: t('channelModel.modelName', { defaultValue: '模型名称' }),
      dataIndex: 'modelName',
      key: 'modelName',
      width: 200,
      render: (text: string | undefined) => text || '-',
    },
    {
      title: t('channelModel.upstreamModelName', { defaultValue: '上游模型名' }),
      dataIndex: 'upstreamModelName',
      key: 'upstreamModelName',
      width: 220,
      render: (text: string | undefined | null, record: ChannelModel) => {
        if (editingId === record.id) {
          return (
            <Space size="small">
              <Input
                size="small"
                value={editingValue}
                onChange={(e) => setEditingValue(e.target.value)}
                placeholder={record.modelName}
                style={{ width: 140 }}
              />
              <Tooltip title={t('actions.confirm', { ns: 'common', defaultValue: '确认' })}>
                <Button
                  type="text"
                  size="small"
                  icon={<CheckOutlined />}
                  onClick={() => saveEditing(record)}
                />
              </Tooltip>
              <Tooltip title={t('actions.cancel', { ns: 'common', defaultValue: '取消' })}>
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
            {t('channelModel.upstreamDefault', { defaultValue: '默认' })}
          </Tag>
        );
      },
    },
    {
      title: t('fields.status', { defaultValue: '状态' }),
      dataIndex: 'state',
      key: 'state',
      width: 80,
      render: (state: string) => (
        <Tag color={stateColor(state)}>
          {state === 'ACTIVE'
            ? t('state.active', { defaultValue: '启用' })
            : t('state.inactive', { defaultValue: '停用' })}
        </Tag>
      ),
    },
    {
      title: t('fields.action', { defaultValue: '操作' }),
      key: 'action',
      width: 180,
      render: (_: unknown, record: ChannelModel) => (
        <Space size="small">
          <Tooltip title={t('channelModel.editUpstream', { defaultValue: '编辑上游模型名' })}>
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
                ? t('actions.disable', { ns: 'common', defaultValue: '停用' })
                : t('actions.enable', { ns: 'common', defaultValue: '启用' })
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
            title={t('channelModel.confirmDelete', { defaultValue: '确认删除此模型关联？' })}
            onConfirm={() => handleDelete(record)}
            okText={t('actions.confirm', { ns: 'common', defaultValue: '确认' })}
            cancelText={t('actions.cancel', { ns: 'common', defaultValue: '取消' })}
          >
            <Tooltip title={t('channelModel.delete', { defaultValue: '删除' })}>
              <Button type="text" size="small" danger icon={<DisconnectOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // ==================== 渲染 ====================

  if (!provider) {
    return <Empty description={t('noProviderData', { defaultValue: '暂无供应商数据' })} />;
  }

  // 无渠道时的空状态
  if (!channelsLoading && (!channels || channels.length === 0)) {
    return (
      <div>
        <div style={{ marginBottom: 16, fontWeight: 500, fontSize: 16 }}>
          {t('modelMapping.title', { defaultValue: '模型映射' })}
        </div>
        <Empty
          description={t('modelMapping.noChannels', { defaultValue: '该供应商暂无渠道，请先创建渠道' })}
        />
      </div>
    );
  }

  return (
    <div>
      {/* 标题与渠道选择器 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <div>
          <div style={{ fontWeight: 500, fontSize: 16 }}>
            {t('modelMapping.title', { defaultValue: '模型映射' })}
          </div>
          <div style={{ marginTop: 4, color: '#64748b', fontSize: 13 }}>
            {t('modelMapping.desc', { defaultValue: '配置渠道下的模型关联和上游模型别名。' })}
          </div>
        </div>
        <Space size="middle">
          <Select
            style={{ width: 200 }}
            placeholder={t('modelMapping.selectChannel', { defaultValue: '选择渠道' })}
            value={effectiveChannelId}
            onChange={(value: number) => setSelectedChannelId(value)}
            loading={channelsLoading}
            options={channelOptions}
          />
          <Button
            type="primary"
            icon={<PlusOutlined />}
            disabled={!effectiveChannelId}
            onClick={handleOpenModal}
          >
            {t('modelMapping.addModel', { defaultValue: '添加模型' })}
          </Button>
        </Space>
      </div>

      {/* 模型关联列表 */}
      <Table
        dataSource={channelModels}
        loading={modelsLoading}
        rowKey="id"
        pagination={false}
        size="small"
        columns={columns}
        locale={{
          emptyText: (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={t('modelMapping.noModels', { defaultValue: '该渠道暂无关联模型' })}
            />
          ),
        }}
      />

      {/* 添加模型弹窗 */}
      <Modal
        title={t('modelMapping.addModel', { defaultValue: '添加模型' })}
        open={modalOpen}
        onOk={handleCreate}
        onCancel={handleCloseModal}
        confirmLoading={createMutation.isPending}
        okButtonProps={{ disabled: !addChannelId || !selectedModelName }}
        okText={t('actions.confirm', { ns: 'common', defaultValue: '确认' })}
        cancelText={t('actions.cancel', { ns: 'common', defaultValue: '取消' })}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* 选择渠道 */}
          <div>
            <div style={{ marginBottom: 4, fontWeight: 500, fontSize: 13 }}>
              {t('modelMapping.channel', { defaultValue: '渠道' })}
            </div>
            <Select
              style={{ width: '100%' }}
              placeholder={t('modelMapping.selectChannel', { defaultValue: '选择渠道' })}
              value={addChannelId}
              onChange={setAddChannelId}
              options={channelOptions}
            />
          </div>

          {/* 模型名称（自动补全） */}
          <div>
            <div style={{ marginBottom: 4, fontWeight: 500, fontSize: 13 }}>
              {t('channelModel.modelName', { defaultValue: '模型名称' })}
            </div>
            <Select
              style={{ width: '100%' }}
              placeholder={t('modelMapping.searchModel', { defaultValue: '搜索模型名称' })}
              showSearch
              value={selectedModelName}
              onChange={(value: string) => {
                setSelectedModelName(value);
                // 自动填充上游模型名为模型名称
                if (!upstreamModelNameInput) {
                  setUpstreamModelNameInput(value);
                }
              }}
              onSearch={(value: string) => setCatalogKeyword(value)}
              filterOption={false}
              options={catalogOptions}
              notFoundContent={
                catalogKeyword
                  ? t('modelMapping.noCatalogMatch', { defaultValue: '未找到匹配的模型' })
                  : t('modelMapping.typeToSearch', { defaultValue: '输入关键词搜索模型' })
              }
            />
          </div>

          {/* 上游模型名 */}
          <div>
            <div style={{ marginBottom: 4, fontWeight: 500, fontSize: 13 }}>
              {t('channelModel.upstreamModelName', { defaultValue: '上游模型名' })}
            </div>
            <Input
              placeholder={t('channelModel.upstreamModelNamePlaceholder', { defaultValue: '留空则与模型名称相同' })}
              value={upstreamModelNameInput}
              onChange={(e) => setUpstreamModelNameInput(e.target.value)}
              allowClear
            />
          </div>
        </div>
      </Modal>
    </div>
  );
}
