import { useState, useCallback } from 'react';
import {
  Button,
  Table,
  Tag,
  Space,
  Popconfirm,
  Segmented,
  Tooltip,
  Typography,
  App,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  StopOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import {
  useChannels,
  useDeleteChannel,
  useChannelCredentials,
  useDeleteChannelCredential,
  useTestChannelCredential,
  useRemoveChannelEndpoint,
  useEnableChannelEndpoint,
  useDisableChannelEndpoint,
  useUpdateChannelCredential,
} from '@/services/query/useChannels';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import ChannelFormModal from './ChannelFormModal';
import ChannelEndpointFormModal from './ChannelEndpointFormModal';
import CredentialFormModal from './CredentialFormModal';
import ChannelModelsPanel from './ChannelModelsPanel';
import type { Channel, ChannelEndpointResponse, ChannelCredential } from '@/types/channel';
import { MaskedKeyDisplay } from '@/components/MaskedKeyDisplay';

interface ProviderChannelTabProps {
  providerId: number;
  editing?: boolean;
}

/** 状态 Tag 颜色映射 */
function stateColor(state: string): 'success' | 'default' {
  return state === 'ACTIVE' ? 'success' : 'default';
}

/** 协议 Tag 颜色映射 */
function protocolColor(protocol: string): string {
  if (protocol === 'openai') return 'green';
  if (protocol === 'anthropic') return 'purple';
  return 'blue';
}

/**
 * 供应商渠道标签页
 * 展示供应商下的渠道列表，支持展开查看端点和凭证
 * 权限控制：无 PROVIDER_WRITE 时隐藏所有写操作按钮
 */
export default function ProviderChannelTab({ providerId }: ProviderChannelTabProps) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.PROVIDER_WRITE);

  // 数据查询与变更
  const { data: channels, isLoading } = useChannels(providerId);
  const deleteChannelMutation = useDeleteChannel();
  const removeEndpointMutation = useRemoveChannelEndpoint();
  const enableEndpointMutation = useEnableChannelEndpoint();
  const disableEndpointMutation = useDisableChannelEndpoint();
  const deleteCredentialMutation = useDeleteChannelCredential();
  const updateCredentialMutation = useUpdateChannelCredential();
  const testCredentialMutation = useTestChannelCredential();

  // 弹窗状态
  const [channelFormOpen, setChannelFormOpen] = useState(false);
  const [editingChannel, setEditingChannel] = useState<Channel | null>(null);
  const [endpointFormOpen, setEndpointFormOpen] = useState(false);
  const [endpointChannelId, setEndpointChannelId] = useState<number>(0);
  const [credentialFormOpen, setCredentialFormOpen] = useState(false);
  const [credentialChannelId, setCredentialChannelId] = useState<number>(0);
  const [testingCredentialId, setTestingCredentialId] = useState<number | null>(null);

  // --- 渠道操作 ---

  const handleCreateChannel = useCallback(() => {
    setEditingChannel(null);
    setChannelFormOpen(true);
  }, []);

  const handleEditChannel = useCallback((channel: Channel) => {
    setEditingChannel(channel);
    setChannelFormOpen(true);
  }, []);

  const handleDeleteChannel = useCallback(async (channel: Channel) => {
    await deleteChannelMutation.mutateAsync({ id: channel.id, providerId });
    message.success(t('channel.deleteSuccess'));
  }, [deleteChannelMutation, providerId, message, t]);

  // --- 端点操作 ---

  const handleAddEndpoint = useCallback((channelId: number) => {
    setEndpointChannelId(channelId);
    setEndpointFormOpen(true);
  }, []);

  const handleDeleteEndpoint = useCallback(async (channelId: number, endpointId: number) => {
    await removeEndpointMutation.mutateAsync({ channelId, endpointId });
    message.success(t('channel.endpointDeleteSuccess'));
  }, [removeEndpointMutation, message, t]);

  /** 启用/停用端点 */
  const handleToggleEndpoint = useCallback(async (channelId: number, endpoint: ChannelEndpointResponse) => {
    if (endpoint.state === 'ACTIVE') {
      await disableEndpointMutation.mutateAsync({ channelId, endpointId: endpoint.id });
      message.success(t('channel.endpointDisabled'));
    } else {
      await enableEndpointMutation.mutateAsync({ channelId, endpointId: endpoint.id });
      message.success(t('channel.endpointEnabled'));
    }
  }, [enableEndpointMutation, disableEndpointMutation, message, t]);

  // --- 凭证操作 ---

  const handleAddCredential = useCallback((channelId: number) => {
    setCredentialChannelId(channelId);
    setCredentialFormOpen(true);
  }, []);

  const handleDeleteCredential = useCallback(async (channelId: number, credentialId: number) => {
    await deleteCredentialMutation.mutateAsync({ channelId, id: credentialId });
    message.success(t('credential.deleteSuccess'));
  }, [deleteCredentialMutation, message, t]);

  /** 启用/停用凭证 */
  const handleToggleCredential = useCallback(async (channelId: number, credential: ChannelCredential) => {
    const newState = credential.state === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    await updateCredentialMutation.mutateAsync({ channelId, id: credential.id, data: { state: newState } });
    message.success(newState === 'ACTIVE' ? t('credential.enabled') : t('credential.disabled'));
  }, [updateCredentialMutation, message, t]);

  /** 测试凭证连通性 */
  const handleTestCredential = useCallback(async (channelId: number, credentialId: number) => {
    setTestingCredentialId(credentialId);
    try {
      const result = await testCredentialMutation.mutateAsync({ channelId, id: credentialId }) as { success: boolean; error?: { message?: string } };
      if (result.success) {
        message.success(t('credential.testSuccess'));
      } else {
        message.error(result.error?.message || t('credential.testFailed'));
      }
    } catch {
      message.error(t('credential.testFailed'));
    } finally {
      setTestingCredentialId(null);
    }
  }, [testCredentialMutation, message, t]);

  // --- 展开行渲染 ---
  const expandedRowRender = useCallback(
    (channel: Channel) => (
      <ChannelExpandedRow
        channel={channel}
        canWrite={canWrite}
        onAddEndpoint={handleAddEndpoint}
        onDeleteEndpoint={handleDeleteEndpoint}
        onToggleEndpoint={handleToggleEndpoint}
        onAddCredential={handleAddCredential}
        onDeleteCredential={handleDeleteCredential}
        onToggleCredential={handleToggleCredential}
        onTestCredential={handleTestCredential}
        testingCredentialId={testingCredentialId}
      />
    ),
    [canWrite, handleAddEndpoint, handleDeleteEndpoint, handleToggleEndpoint,
      handleAddCredential, handleDeleteCredential, handleToggleCredential,
      handleTestCredential, testingCredentialId],
  );

  // 渠道列表列定义
  const columns = [
    {
      title: t('channel.name'),
      dataIndex: 'name',
      key: 'name',
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
    {
      title: t('channel.endpoints'),
      key: 'endpointCount',
      width: 80,
      render: (_: unknown, record: Channel) => record.endpoints?.length ?? 0,
    },
    {
      title: t('channel.billingMode'),
      dataIndex: 'billingMode',
      key: 'billingMode',
      width: 100,
      render: (mode: string) =>
        mode === 'pay_per_call' ? t('channel.billingPayPerCall') : t('channel.billingSubscription'),
    },
    {
      title: t('channel.priority'),
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
    },
    {
      title: t('channel.weight'),
      dataIndex: 'weight',
      key: 'weight',
      width: 80,
    },
    ...(canWrite
      ? [
          {
            title: t('fields.action'),
            key: 'action',
            width: 160,
            render: (_: unknown, record: Channel) => (
              <Space size="small">
                <Tooltip title={t('actions.edit')}>
                  <Button
                    type="text"
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => handleEditChannel(record)}
                  />
                </Tooltip>
                <Popconfirm
                  title={t('channel.confirmDelete')}
                  onConfirm={() => handleDeleteChannel(record)}
                  okText={t('actions.confirm', { ns: 'common' })}
                  cancelText={t('actions.cancel', { ns: 'common' })}
                >
                  <Tooltip title={t('actions.delete', { ns: 'common' })}>
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
      {canWrite && (
        <Button
          type="primary"
          icon={<PlusOutlined />}
          style={{ marginBottom: 16 }}
          onClick={handleCreateChannel}
        >
          {t('channel.create')}
        </Button>
      )}

      <Table
        dataSource={channels}
        loading={isLoading}
        rowKey="id"
        pagination={false}
        columns={columns}
        expandable={{ expandedRowRender, defaultExpandAllRows: false }}
      />

      {/* 渠道创建/编辑弹窗 */}
      <ChannelFormModal
        open={channelFormOpen}
        providerId={providerId}
        channel={editingChannel}
        onClose={() => {
          setChannelFormOpen(false);
          setEditingChannel(null);
        }}
      />

      {/* 端点创建弹窗 */}
      <ChannelEndpointFormModal
        open={endpointFormOpen}
        channelId={endpointChannelId}
        onClose={() => setEndpointFormOpen(false)}
      />

      {/* 凭证创建弹窗 */}
      <CredentialFormModal
        open={credentialFormOpen}
        channelId={credentialChannelId}
        onClose={() => setCredentialFormOpen(false)}
      />
    </div>
  );
}

// ==================== 展开行子组件 ====================

interface ChannelExpandedRowProps {
  channel: Channel;
  canWrite: boolean;
  onAddEndpoint: (channelId: number) => void;
  onDeleteEndpoint: (channelId: number, endpointId: number) => Promise<void>;
  onToggleEndpoint: (channelId: number, endpoint: ChannelEndpointResponse) => Promise<void>;
  onAddCredential: (channelId: number) => void;
  onDeleteCredential: (channelId: number, credentialId: number) => Promise<void>;
  onToggleCredential: (channelId: number, credential: ChannelCredential) => Promise<void>;
  onTestCredential: (channelId: number, credentialId: number) => Promise<void>;
  testingCredentialId: number | null;
}

/** 渠道展开行：上半部分端点列表，下半部分凭证列表 */
function ChannelExpandedRow({
  channel,
  canWrite,
  onAddEndpoint,
  onDeleteEndpoint,
  onToggleEndpoint,
  onAddCredential,
  onDeleteCredential,
  onToggleCredential,
  onTestCredential,
  testingCredentialId,
}: ChannelExpandedRowProps) {
  const { t } = useTranslation('providers');
  const { data: credentials, isLoading: credentialsLoading } = useChannelCredentials(channel.id);
  const endpoints = channel.endpoints ?? [];
  const [activeTab, setActiveTab] = useState<string>('endpoints');

  // 端点列定义
  const endpointColumns = [
    {
      title: t('channel.endpointUrl'),
      dataIndex: 'endpointUrl',
      key: 'endpointUrl',
      render: (url: string) => (
        <Typography.Text copyable={{ text: url }} style={{ maxWidth: 300 }} ellipsis>
          {url}
        </Typography.Text>
      ),
    },
    {
      title: t('channel.protocol'),
      dataIndex: 'protocol',
      key: 'protocol',
      width: 100,
      render: (protocol: string) => (
        <Tag color={protocolColor(protocol)}>{protocol.toUpperCase()}</Tag>
      ),
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
            width: 120,
            render: (_: unknown, record: ChannelEndpointResponse) => (
              <Space size="small">
                <Tooltip
                  title={
                    record.state === 'ACTIVE'
                      ? t('actions.disable', { ns: 'common' })
                      : t('actions.enable', { ns: 'common' })
                  }
                >
                  <Button
                    type="text"
                    size="small"
                    icon={record.state === 'ACTIVE' ? <StopOutlined /> : <CheckCircleOutlined />}
                    onClick={() => onToggleEndpoint(channel.id, record)}
                  />
                </Tooltip>
                <Popconfirm
                  title={t('channel.confirmDeleteEndpoint')}
                  onConfirm={() => onDeleteEndpoint(channel.id, record.id)}
                  okText={t('actions.confirm', { ns: 'common' })}
                  cancelText={t('actions.cancel', { ns: 'common' })}
                >
                  <Tooltip title={t('actions.delete', { ns: 'common' })}>
                    <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                  </Tooltip>
                </Popconfirm>
              </Space>
            ),
          },
        ]
      : []),
  ];

  // 凭证列定义
  const credentialColumns = [
    {
      title: t('credential.apiKey'),
      key: 'apiKeyPrefix',
      render: (_: unknown, record: ChannelCredential) => (
        <MaskedKeyDisplay keyPlain={record.apiKeyPlain} mode="readonly" size="small" />
      ),
    },
    {
      title: t('credential.name'),
      dataIndex: 'name',
      key: 'name',
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
    {
      title: t('channel.priority'),
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
    },
    {
      title: t('channel.weight'),
      dataIndex: 'weight',
      key: 'weight',
      width: 80,
    },
    ...(canWrite
      ? [
          {
            title: t('fields.action'),
            key: 'action',
            width: 160,
            render: (_: unknown, record: ChannelCredential) => (
              <Space size="small">
                <Tooltip title={t('credential.test')}>
                  <Button
                    type="text"
                    size="small"
                    icon={<ThunderboltOutlined />}
                    loading={testingCredentialId === record.id}
                    onClick={() => onTestCredential(channel.id, record.id)}
                  />
                </Tooltip>
                <Tooltip
                  title={
                    record.state === 'ACTIVE'
                      ? t('actions.disable', { ns: 'common' })
                      : t('actions.enable', { ns: 'common' })
                  }
                >
                  <Button
                    type="text"
                    size="small"
                    icon={record.state === 'ACTIVE' ? <StopOutlined /> : <CheckCircleOutlined />}
                    onClick={() => onToggleCredential(channel.id, record)}
                  />
                </Tooltip>
                <Popconfirm
                  title={t('credential.confirmDelete')}
                  onConfirm={() => onDeleteCredential(channel.id, record.id)}
                  okText={t('actions.confirm', { ns: 'common' })}
                  cancelText={t('actions.cancel', { ns: 'common' })}
                >
                  <Tooltip title={t('actions.delete', { ns: 'common' })}>
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
    <div style={{ padding: '8px 0' }}>
      <Segmented
        value={activeTab}
        onChange={(value) => setActiveTab(value as string)}
        options={[
          { label: t('channel.endpoints'), value: 'endpoints' },
          { label: t('credential.plural'), value: 'credentials' },
          { label: t('channelModel.title'), value: 'models' },
        ]}
        style={{ marginBottom: 16 }}
      />

      {activeTab === 'endpoints' && (
        <>
          {/* 端点列表 */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: 8,
            }}
          >
            <span style={{ fontWeight: 500 }}>{t('channel.endpoints')}</span>
            {canWrite && (
              <Button
                size="small"
                type="dashed"
                icon={<PlusOutlined />}
                onClick={() => onAddEndpoint(channel.id)}
              >
                {t('channel.addEndpoint')}
              </Button>
            )}
          </div>
          <Table
            dataSource={endpoints}
            rowKey="id"
            pagination={false}
            size="small"
            columns={endpointColumns}
            locale={{ emptyText: t('channel.noEndpoints') }}
          />
        </>
      )}

      {activeTab === 'credentials' && (
        <>
          {/* 凭证列表 */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: 8,
            }}
          >
            <span style={{ fontWeight: 500 }}>{t('credential.plural')}</span>
            {canWrite && (
              <Button
                size="small"
                type="dashed"
                icon={<PlusOutlined />}
                onClick={() => onAddCredential(channel.id)}
              >
                {t('credential.add')}
              </Button>
            )}
          </div>
          <Table
            dataSource={credentials}
            loading={credentialsLoading}
            rowKey="id"
            pagination={false}
            size="small"
            columns={credentialColumns}
            locale={{ emptyText: t('credential.noCredentials') }}
          />
        </>
      )}

      {activeTab === 'models' && (
        <ChannelModelsPanel channelId={channel.id} canWrite={canWrite} />
      )}
    </div>
  );
}