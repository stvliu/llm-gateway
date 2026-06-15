import { useState, useMemo } from 'react';
import {
  Input,
  Select,
  Button,
  Space,
  Empty,
  Spin,
  Segmented,
  Card,
  message,
  Modal,
} from 'antd';
import {
  PlusOutlined,
  AppstoreOutlined,
  UnorderedListOutlined,
  ImportOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAllChannels, useDeleteChannel, useTransitionChannelState, useChannelModelsBatch, useTestChannelCredential } from '@/services/query/useChannels';
import { useProviders } from '@/services/query/useProviders';
import { useChannelCredentialsBatch } from '@/services/query/useChannels';
import { extractErrorMessageI18n } from '@/utils/errorMessage';
import { ChannelGroupedList } from './ChannelGroupedList';
import { ChannelDetailDrawer } from './ChannelDetailDrawer';
import { ChannelCreateWizard } from './ChannelCreateWizard';
import { ProviderEditModal } from './ProviderEditModal';
import { ChannelTableView } from './ChannelTableView';
import { ConnectivityTestPanel } from './ConnectivityTestPanel';
import BatchImportModal from './BatchImportModal';
import { BatchExportButton } from './BatchExportButton';
import type {
  ChannelCard,
  ChannelGroup,
  Channel,
  ChannelCredential,
  ChannelModel,
} from '@/types/channel';
import type { Provider } from '@/types/provider';

const { Search } = Input;

type ViewMode = 'grouped' | 'table';

/**
 * 计算渠道的统计信息
 */
const calculateChannelStats = (
  channel: Channel,
  credentials: ChannelCredential[] | undefined,
  models: ChannelModel[] | undefined
): ChannelCard => {
  return {
    ...channel,
    stats: {
      endpointCount: channel.endpoints?.length || 0,
      credentialCount: credentials?.length || 0,
      modelCount: models?.length || 0,
      avgResponseTime: null,
    },
  };
};

/**
 * 渠道管理页面
 * 支持分组视图/列表视图 + 筛选 + 批量操作 + 搜索增强
 */
export default function Channels() {
  const { t } = useTranslation('channels');
  // 状态管理
  const [searchText, setSearchText] = useState('');
  const [providerFilter, setProviderFilter] = useState<number | undefined>();
  const [statusFilter, setStatusFilter] = useState<string[]>([]);
  const [selectedChannel, setSelectedChannel] = useState<ChannelCard | null>(null);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [wizardVisible, setWizardVisible] = useState(false);
  const [editProviderModalOpen, setEditProviderModalOpen] = useState(false);
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);
  const [batchImportOpen, setBatchImportOpen] = useState(false);
  const [connectivityProviderId, setConnectivityProviderId] = useState<number | null>(null);
  const [drawerInitialTab, setDrawerInitialTab] = useState<string | undefined>(undefined);
  // 任务 9.1：闪电图标点击携带 highlightTestAll=true，由 ChannelDetailDrawer 接收并 800ms 自清除
  const [drawerHighlightTestAll, setDrawerHighlightTestAll] = useState(false);

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
  const transitionChannelState = useTransitionChannelState();
  const testCredential = useTestChannelCredential();

  // 获取所有渠道的凭证（批量）
  const channelIds = useMemo(() => channels?.map((c) => c.id) || [], [channels]);
  const credentialsQueries = useChannelCredentialsBatch(channelIds);
  const modelsQueries = useChannelModelsBatch(channelIds);

  // 提取凭证数据为稳定引用
  const credentialsData = useMemo(
    () => credentialsQueries.map((q) => q.data),
    [credentialsQueries]
  );

  // 构建渠道卡片数据（含统计）
  const channelsWithStats: ChannelCard[] = useMemo(() => {
    if (!channels) return [];

    return channels.map((channel, index) => {
      const credentials = credentialsData[index];
      const models = modelsQueries[index]?.data;
      return calculateChannelStats(channel, credentials, models);
    });
  }, [channels, credentialsData, modelsQueries]);

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

    // 按状态筛选（多选）
    if (statusFilter.length > 0) {
      result = result.map((group) => ({
        ...group,
        channels: group.channels.filter((ch) => statusFilter.includes(ch.state)),
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

  /** 从卡片发起连通性测试（任务 9.1：闪电图标改为打开抽屉到 Credentials Tab，并高亮"测试全部"） */
  const handleTestChannel = async (
    channel: ChannelCard,
    intent?: { tab: 'credentials'; highlightTestAll: boolean },
  ) => {
    // 闪电图标新行为：透传 intent 时打开详情抽屉到指定 Tab + 高亮触发
    if (intent) {
      setSelectedChannel(channel);
      setDrawerInitialTab(intent.tab);
      setDrawerHighlightTestAll(intent.highlightTestAll);
      setDrawerVisible(true);
      return;
    }
    // 兼容兜底：保留旧的就地测试调用路径（其他来源仍可用 testCredential 直测）
    if (channel.state !== 'ACTIVE') return;
    const idx = channels?.findIndex(c => c.id === channel.id) ?? -1;
    const creds = credentialsData[idx];

    if (!creds || creds.length === 0) {
      message.warning(t('card.testNoCredential'));
      return;
    }

    try {
      const result = await testCredential.mutateAsync({ channelId: channel.id, id: creds[0].id });
      if (result.success) {
        message.success(t('card.testSuccess', { latency: result.latency ?? 0 }));
      } else {
        message.error(t('card.testFail', { msg: result.error?.message || t('credential.unknownError') }));
      }
    } catch (err) {
      message.error(t('card.testFail', { msg: err instanceof Error ? err.message : t('credential.unknownError') }));
    }
  };

  return (
    <div>
      <Card title={t('title')}>
        {/* 视图切换 */}
        <div style={{ marginBottom: '16px' }}>
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
            mode="multiple"
            style={{ width: 200 }}
            value={statusFilter}
            onChange={(values: string[]) => setStatusFilter(values ?? [])}
            options={[
              { label: '待激活', value: 'PENDING' },
              { label: '运行中', value: 'ACTIVE' },
              { label: '已暂停', value: 'SUSPENDED' },
              { label: '已下线', value: 'DEPRECATED' },
              { label: '已废弃', value: 'RETIRED' },
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
            onChannelToggleState={async (id, enabled) => {
              try {
                await transitionChannelState.mutateAsync({ id, targetState: enabled ? 'ACTIVE' : 'SUSPENDED' });
                message.success(enabled ? t('statusToggle.enabled') : t('statusToggle.disabled'));
              } catch {
                message.error(t('statusToggle.failed'));
              }
            }}
            onStateTransition={async (id, targetState, reason) => {
              try {
                await transitionChannelState.mutateAsync({ id, targetState, reason });
                message.success(t('statusToggle.enabled'));
              } catch (err) {
                const msg = extractErrorMessageI18n(err, t as (key: string) => string);
                message.error(msg);
              }
            }}
            onTestChannel={handleTestChannel}
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
                transitionChannelState.mutate({ id, targetState: 'SUSPENDED' });
              }
            }}
            onTestProviderConnectivity={(providerCode) => {
              setConnectivityProviderId(providerCode);
            }}
            onExportProvider={() => {
              message.info(t('batch.exportHint'));
            }}
          />
        </>
      ) : (
        <ChannelTableView
          channels={filteredChannels}
          providers={providersData?.items || []}
          onChannelClick={(id) => {
            const ch = channelsWithStats.find((c) => c.id === id);
            if (ch) handleChannelClick(ch);
          }}
          onStateTransition={async (id, targetState, reason) => {
            try {
              await transitionChannelState.mutateAsync({ id, targetState, reason });
              message.success(t('statusToggle.enabled'));
            } catch {
              message.error(t('statusToggle.failed'));
            }
          }}
          onDelete={(id) => handleDelete(id)}
          onTest={(channel) => handleTestChannel(channel)}
        />
      )}

        </Card>

      {/* 渠道详情抽屉 */}
      <ChannelDetailDrawer
        channel={selectedChannel}
        open={drawerVisible}
        onClose={() => {
          setDrawerVisible(false);
          setDrawerInitialTab(undefined);
          setDrawerHighlightTestAll(false);
        }}
        initialTab={drawerInitialTab}
        highlightTestAll={drawerHighlightTestAll}
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

      {/* 供应商连通性测试弹窗 */}
      {connectivityProviderId && (() => {
        const provider = providersData?.items?.find(p => p.id === connectivityProviderId);
        return (
          <Modal
            title={t('group.connectivityTest')}
            open={!!connectivityProviderId}
            onCancel={() => setConnectivityProviderId(null)}
            footer={null}
            width={560}
            destroyOnClose
          >
            {provider && (
              <ConnectivityTestPanel
                providerCode={provider.providerId?.toLowerCase() ?? ''}
                defaultBaseUrl={provider.websiteUrl}
              />
            )}
          </Modal>
        );
      })()}
    </div>
  );
}
