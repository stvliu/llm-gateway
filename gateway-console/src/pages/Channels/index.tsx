import { useState, useMemo } from 'react';
import {
  Typography,
  Input,
  Select,
  Button,
  Space,
  Empty,
  Spin,
} from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useAllChannels, useDeleteChannel } from '@/services/query/useChannels';
import { useProviders, useSetEnabledProvider } from '@/services/query/useProviders';
import { useChannelCredentialsBatch } from '@/services/query/useChannels';
import { ChannelGroupedList } from './ChannelGroupedList';
import { ChannelDetailDrawer } from './ChannelDetailDrawer';
import { ChannelCreateWizard } from './ChannelCreateWizard';
import { ProviderEditModal } from './ProviderEditModal';
import type {
  ChannelCard,
  ChannelGroup,
  Channel,
  ChannelCredential,
} from '@/types/channel';
import type { Provider } from '@/types/provider';
const { Title } = Typography;
const { Search } = Input;

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
 * 按供应商分组的渠道卡片列表 + 渠道详情抽屉
 */
export default function Channels() {
  // 状态管理
  const [searchText, setSearchText] = useState('');
  const [providerFilter, setProviderFilter] = useState<number | undefined>();
  const [statusFilter, setStatusFilter] = useState<string | undefined>();
  const [selectedChannel, setSelectedChannel] = useState<ChannelCard | null>(null);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [wizardVisible, setWizardVisible] = useState(false);
  const [editProviderModalOpen, setEditProviderModalOpen] = useState(false);
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);

  // 数据获取
  const { data: providersData, isLoading: providersLoading } = useProviders({ size: 100 });
  const { data: channels, isLoading: channelsLoading } = useAllChannels();
  const deleteChannel = useDeleteChannel();
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

  // 筛选后的数据
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

    // 按搜索文本筛选
    if (searchText) {
      const lowerSearch = searchText.toLowerCase();
      result = result.map((group) => ({
        ...group,
        channels: group.channels.filter((ch) =>
          ch.name.toLowerCase().includes(lowerSearch)
        ),
      })).filter((group) => group.channels.length > 0);
    }

    return result;
  }, [groupedChannels, providerFilter, statusFilter, searchText]);

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
      {/* 页面标题 */}
      <Title level={4} style={{ marginBottom: '24px' }}>
        渠道管理
      </Title>

      {/* 工具栏 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          marginBottom: '24px',
        }}
      >
        <Space size={12}>
          <Search
            placeholder="搜索渠道名称"
            allowClear
            style={{ width: 240 }}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
          />
          <Select
            placeholder="选择供应商"
            allowClear
            style={{ width: 180 }}
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
            placeholder="选择状态"
            allowClear
            style={{ width: 120 }}
            value={statusFilter}
            onChange={setStatusFilter}
            options={[
              { label: '已启用', value: 'ACTIVE' },
              { label: '已停用', value: 'INACTIVE' },
            ]}
          />
        </Space>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleCreateChannel}
        >
          新建渠道
        </Button>
      </div>

      {/* 内容区 */}
      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '48px' }}>
          <Spin size="large" />
        </div>
      ) : filteredGroups.length === 0 ? (
        <Empty description="暂无渠道数据" />
      ) : (
        <ChannelGroupedList
          groups={filteredGroups}
          onChannelClick={handleChannelClick}
          onChannelDelete={handleDelete}
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
    </div>
  );
}
