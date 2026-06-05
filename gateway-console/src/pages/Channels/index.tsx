import { useState, useMemo } from 'react';
import {
  Typography,
  Input,
  Select,
  Button,
  Space,
  Empty,
  Spin,
  Segmented,
  Card,
  Dropdown,
} from 'antd';
import {
  PlusOutlined,
  AppstoreOutlined,
  UnorderedListOutlined,
  ImportOutlined,
  DownOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAllChannels, useDeleteChannel, useUpdateChannel } from '@/services/query/useChannels';
import { useProviders, useSetEnabledProvider } from '@/services/query/useProviders';
import { useChannelCredentialsBatch } from '@/services/query/useChannels';
import { ChannelGroupedList } from './ChannelGroupedList';
import { ChannelDetailDrawer } from './ChannelDetailDrawer';
import { ChannelCreateWizard } from './ChannelCreateWizard';
import { ProviderEditModal } from './ProviderEditModal';
import { ProviderCreateModal } from './ProviderCreateModal';
import { ChannelTableView } from './ChannelTableView';
import BatchImportModal from './BatchImportModal';
import BatchExportButton from './BatchExportButton';
import type {
  ChannelCard,
  ChannelGroup,
  Channel,
  ChannelCredential,
} from '@/types/channel';
import type { Provider } from '@/types/provider';
import { theme } from 'antd';

const { Title } = Typography;
const { Search } = Input;

type ViewMode = 'grouped' | 'table';

/**
 * 计算渠道的统计信息
 */
const calculateChannelStats = (
  channel: Channel,
  credentials: ChannelCredential[] | undefined
): ChannelCard => {
  return {
    ...channel,
    stats: {
      endpointCount: channel.endpoints?.length || 0,
      credentialCount: credentials?.length || 0,
      modelCount: 0, // TODO: 从模型映射 API 获取
      avgResponseTime: null, // TODO: 从测试结果获取
    },
  };
};

/**
 * 渠道管理页面
 * 支持分组视图/列表视图 + 筛选 + 批量操作 + 搜索增强
 */
export default function Channels() {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  // 状态管理
  const [searchText, setSearchText] = useState('');
  const [providerFilter, setProviderFilter] = useState<number | undefined>();
  const [statusFilter, setStatusFilter] = useState<string | undefined>();
  const [selectedChannel, setSelectedChannel] = useState<ChannelCard | null>(null);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [wizardVisible, setWizardVisible] = useState(false);
  const [editProviderModalOpen, setEditProviderModalOpen] = useState(false);
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);
  const [batchImportOpen, setBatchImportOpen] = useState(false);
  const [createProviderOpen, setCreateProviderOpen] = useState(false);

  // 视图切换（持久化到 localStorage）
  const [viewMode, setViewMode] = useState<ViewMode>(() => {
    try {
      return (localStorage.getItem('channel-view-mode') as ViewMode) || 'grouped';
    } catch {
      return 'grouped';
    }
  });

  const handleViewChange = (mode: string | number) => {
    const newMode = mode as ViewMode;
    setViewMode(newMode);
    try {
      localStorage.setItem('channel-view-mode', newMode);
    } catch {
      // localStorage 不可用时静默失败
    }
  };

  // 数据获取
  const { data: providersData, isLoading: providersLoading } = useProviders({ size: 100 });
  const { data: channels, isLoading: channelsLoading } = useAllChannels();
  const deleteChannel = useDeleteChannel();
  const updateChannel = useUpdateChannel();
  const setEnabledProvider = useSetEnabledProvider();

  // 获取所有渠道的凭证（批量）
  const channelIds = useMemo(() => channels?.map((c) => c.id) || [], [channels]);
  const credentialsQueries = useChannelCredentialsBatch(channelIds);

  // 提取凭证数据为稳定引用
  const credentialsData = useMemo(
    () => credentialsQueries.map((q) => q.data),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [channels, credentialsQueries.map((q) => q.data?.length).join(',')]
  );

  // 构建渠道卡片数据（含统计）
  const channelsWithStats: ChannelCard[] = useMemo(() => {
    if (!channels) return [];

    return channels.map((channel, index) => {
      const credentials = credentialsData[index];
      return calculateChannelStats(channel, credentials);
    });
  }, [channels, credentialsData]);

  // 按供应商分组
  const groupedChannels: ChannelGroup[] = useMemo(() => {
    const providers = providersData?.items || [];

    // 构建 providerId -> provider 映射
    const providerMap = new Map(
      providers.map((p) => [p.id, p])
    );

    // 按 providerId 分组
    const groupMap = new Map<number, ChannelCard[]>();
    channelsWithStats.forEach((channel) => {
      const list = groupMap.get(channel.providerId) || [];
      list.push(channel);
      groupMap.set(channel.providerId, list);
    });

    // 转换为 ChannelGroup 数组
    const groups: ChannelGroup[] = [];
    groupMap.forEach((channelList, providerId) => {
      const provider = providerMap.get(providerId);
      if (provider) {
        groups.push({
          provider: {
            id: provider.id,
            providerId: provider.providerId,
            providerName: provider.providerName,
            state: provider.state,
          },
          channels: channelList,
        });
      }
    });

    return groups;
  }, [channelsWithStats, providersData]);

  // 筛选 + 搜索增强（匹配渠道名称和端点URL）
  const filteredGroups = useMemo(() => {
    let result = groupedChannels;

    // 按供应商筛选
    if (providerFilter !== undefined) {
      result = result.filter((g) => g.provider.id === providerFilter);
    }

    // 按状态筛选
    if (statusFilter) {
      result = result.map((group) => ({
        ...group,
        channels: group.channels.filter((ch) => ch.state === statusFilter),
      })).filter((group) => group.channels.length > 0);
    }

    // 搜索增强：匹配渠道名称和端点URL
    if (searchText) {
      const q = searchText.toLowerCase();
      result = result.map((group) => ({
        ...group,
        channels: group.channels.filter((ch) => {
          if (ch.name.toLowerCase().includes(q)) return true;
          if (ch.endpoints?.some((ep) => ep.endpointUrl.toLowerCase().includes(q))) return true;
          return false;
        }),
      })).filter((group) => group.channels.length > 0);
    }

    return result;
  }, [groupedChannels, providerFilter, statusFilter, searchText]);

  // 列表视图的扁平化渠道数据
  const filteredChannels = useMemo(() => {
    return filteredGroups.flatMap((g) => g.channels);
  }, [filteredGroups]);

  // 事件处理
  const handleChannelClick = (channel: ChannelCard) => {
    setSelectedChannel(channel);
    setDrawerVisible(true);
  };

  const handleCreateChannel = () => {
    setWizardVisible(true);
  };

  const isLoading = providersLoading || channelsLoading;

  const handleDelete = (id: number) => {
    const ch = channels?.find(c => c.id === id);
    deleteChannel.mutate({ id, providerId: ch?.providerId ?? 0 });
  };

  return (
    <div style={{ padding: '24px' }}>
      {/* 页面标题 + 视图切换 */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <Title level={4} style={{ margin: 0 }}>
          {t('title')}
        </Title>
        <Segmented
          value={viewMode}
          onChange={handleViewChange}
          options={[
            { label: t('viewMode.grouped'), value: 'grouped', icon: <AppstoreOutlined /> },
            { label: t('viewMode.table'), value: 'table', icon: <UnorderedListOutlined /> },
          ]}
        />
      </div>

      {/* 工具栏 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          marginBottom: '16px',
          flexWrap: 'wrap',
          gap: '8px',
        }}
      >
        {/* 左侧：搜索 + 筛选 */}
        <Space size={12} wrap>
          <Search
            placeholder={t('searchPlaceholder')}
            allowClear
            style={{ width: 240 }}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
          />
          <Select
            placeholder={t('providerFilter')}
            allowClear
            style={{ width: 160 }}
            value={providerFilter}
            onChange={setProviderFilter}
            options={
              providersData?.items?.map((p) => ({
                label: p.providerName,
                value: p.id,
              })) || []
            }
          />
          <Select
            placeholder={t('statusFilter')}
            allowClear
            style={{ width: 120 }}
            value={statusFilter}
            onChange={setStatusFilter}
            options={[
              { label: t('status.active'), value: 'ACTIVE' },
              { label: t('status.inactive'), value: 'INACTIVE' },
            ]}
          />
        </Space>

        {/* 右侧：操作按钮 */}
        <Space size={12} wrap>
          <BatchExportButton />
          <Button icon={<ImportOutlined />} onClick={() => setBatchImportOpen(true)}>
            {t('batchImport')}
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreateChannel}>
            {t('addChannel')}
          </Button>
          <Dropdown menu={{
            items: [
              { key: 'provider', label: t('addProvider'), icon: <AppstoreOutlined /> },
            ],
            onClick: ({ key }) => {
              if (key === 'provider') setCreateProviderOpen(true);
            },
          }}>
            <Button icon={<DownOutlined />} />
          </Dropdown>
        </Space>
      </div>

      {/* 内容区 */}
      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '48px' }}>
          <Spin size="large" />
        </div>
      ) : filteredGroups.length === 0 ? (
        <Empty description={t('empty')} />
      ) : viewMode === 'grouped' ? (
        <>
          <ChannelGroupedList
            groups={filteredGroups}
            onChannelClick={handleChannelClick}
            onChannelDelete={handleDelete}
            onChannelToggleState={(id, enabled) => {
              updateChannel.mutate({ id, data: { state: enabled ? 'ACTIVE' : 'INACTIVE' } });
            }}
            onEditProvider={(id) => {
              const p = providersData?.items?.find((p) => p.id === id);
              if (p) {
                setEditingProvider(p);
                setEditProviderModalOpen(true);
              }
            }}
            onToggleProviderEnabled={(id) => {
              const p = providersData?.items?.find((p) => p.id === id);
              if (p) {
                setEnabledProvider.mutate({ id, enabled: p.state !== 'ACTIVE' });
              }
            }}
          />
          {/* 底部新增渠道虚线卡片 */}
          <Card
            hoverable
            style={{
              border: `2px dashed ${token.colorBorder}`,
              textAlign: 'center',
              cursor: 'pointer',
              marginTop: 16,
            }}
            styles={{ body: { padding: '24px' } }}
            onClick={() => setCreateProviderOpen(true)}
          >
            <PlusOutlined style={{ fontSize: 24, color: token.colorPrimary, marginBottom: 8 }} />
            <div style={{ color: token.colorPrimary, fontSize: 14 }}>{t('addProvider')}</div>
          </Card>
        </>
      ) : (
        <ChannelTableView
          channels={filteredChannels}
          providers={providersData?.items || []}
          onChannelClick={(id) => {
            const ch = channelsWithStats.find((c) => c.id === id);
            if (ch) handleChannelClick(ch);
          }}
        />
      )}

      {/* 渠道详情抽屉 */}
      <ChannelDetailDrawer
        channel={selectedChannel}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
      />

      {/* 创建向导 */}
      <ChannelCreateWizard
        open={wizardVisible}
        onClose={() => setWizardVisible(false)}
      />

      {/* 供应商编辑弹窗 */}
      <ProviderEditModal
        open={editProviderModalOpen}
        provider={editingProvider}
        onClose={() => {
          setEditProviderModalOpen(false);
          setEditingProvider(null);
        }}
      />

      {/* 批量导入弹窗 */}
      <BatchImportModal
        open={batchImportOpen}
        onClose={() => setBatchImportOpen(false)}
      />

      {/* 供应商创建弹窗 */}
      <ProviderCreateModal
        open={createProviderOpen}
        onClose={() => setCreateProviderOpen(false)}
      />
    </div>
  );
}
