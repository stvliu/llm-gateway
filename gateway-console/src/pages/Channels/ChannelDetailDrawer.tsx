import { useState } from 'react';
import {
  Drawer,
  Typography,
  Tag,
  Button,
  Space,
  Divider,
  message,
  Spin,
  Tabs,
  Popconfirm,
} from 'antd';
import {
  GlobalOutlined,
  KeyOutlined,
  RobotOutlined,
  SettingOutlined,
  BarChartOutlined,
  ApiOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { ChannelCard } from '@/types/channel';
import {
  useChannel,
  useChannelCredentials,
  useChannelModels,
  useUpdateChannel,
  useTestChannelCredential,
  useDeleteChannel,
} from '@/services/query/useChannels';
import { EndpointSection } from './EndpointSection';
import { CredentialSection } from './CredentialSection';
import { ModelMappingSection } from './ModelMappingSection';
import { QuotaSettingsSection } from './QuotaSettingsSection';
import { ChannelOverviewTab } from './ChannelOverviewTab';
import { ProviderEditModal } from './ProviderEditModal';
import { useProvider } from '@/services/query/useProviders';

const { Text } = Typography;

interface ChannelDetailDrawerProps {
  channel: ChannelCard | null;
  open: boolean;
  onClose: () => void;
}

/**
 * 渠道详情抽屉
 * 头部：供应商Logo+渠道名+快捷操作条 → 概览/端点/API Key/模型映射/配额与设置
 */
export function ChannelDetailDrawer({
  channel,
  open,
  onClose,
}: ChannelDetailDrawerProps) {
  const { t } = useTranslation('channels');
  const [activeTab, setActiveTab] = useState('overview');
  const [editProviderOpen, setEditProviderOpen] = useState(false);
  const updateChannel = useUpdateChannel();
  const deleteChannel = useDeleteChannel();

  const { data: channelDetail, isLoading: detailLoading } = useChannel(channel?.id || 0);
  const { data: credentials = [], isLoading: credentialsLoading } = useChannelCredentials(
    channel?.id || 0
  );
  const { data: channelModels = [] } = useChannelModels(channel?.id || 0);
  const { data: provider } = useProvider(channel?.providerId || 0);
  const testCredential = useTestChannelCredential();

  if (!channel) return null;

  const isLoading = detailLoading || credentialsLoading;

  const getBillingModeLabel = (mode: string) => {
    const labels: Record<string, string> = {
      pay_as_you_go: t('billing.payAsYouGo'),
      subscription: t('billing.subscription'),
      package: t('billing.package'),
    };
    return labels[mode] || t('billing.default', { mode });
  };

  /** 测试所有凭证 */
  const handleTest = async () => {
    if (credentials.length === 0) {
      message.warning(t('drawer.noCredentials'));
      return;
    }
    try {
      let successCount = 0;
      let failCount = 0;
      for (const cred of credentials) {
        try {
          await testCredential.mutateAsync({ channelId: channel.id, id: cred.id });
          successCount++;
        } catch {
          failCount++;
        }
      }
      if (failCount === 0) {
        message.success(t('drawer.testAllSuccess', { count: successCount }));
      } else {
        message.warning(t('drawer.testPartialSuccess', { success: successCount, fail: failCount }));
      }
    } catch {
      message.error(t('drawer.testFailed'));
    }
  };

  /** 切换渠道状态 */
  const handleToggleState = async () => {
    const newState = channel.state === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await updateChannel.mutateAsync({ id: channel.id, data: { state: newState } });
      message.success(newState === 'ACTIVE' ? t('drawer.channelEnabled') : t('drawer.channelDisabled'));
    } catch {
      message.error(t('drawer.stateToggleFailed'));
    }
  };

  /** 删除渠道 */
  const handleDelete = async () => {
    try {
      await deleteChannel.mutateAsync({ id: channel.id, providerId: channel.providerId });
      message.success(t('drawer.channelDeleted'));
      onClose();
    } catch {
      message.error(t('drawer.deleteFailed'));
    }
  };

  /** 跳转到指定Tab */
  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
  };

  const endpointCount = channelDetail?.endpoints?.length || 0;
  const credentialCount = credentials.length;
  const modelCount = channelModels.length;

  const tabItems = [
    {
      key: 'overview',
      label: (
        <Space>
          <BarChartOutlined />
          <span>{t('drawer.tabOverview')}</span>
        </Space>
      ),
      children: (
        <ChannelOverviewTab
          channel={channelDetail || channel}
          credentials={credentials}
          channelModels={channelModels}
          onTabChange={handleTabChange}
        />
      ),
    },
    {
      key: 'endpoints',
      label: (
        <Space>
          <GlobalOutlined />
          <span>{t('drawer.tabEndpoints', { count: endpointCount })}</span>
        </Space>
      ),
      children: (
        <EndpointSection
          channelId={channel.id}
          endpoints={channelDetail?.endpoints || []}
        />
      ),
    },
    {
      key: 'credentials',
      label: (
        <Space>
          <KeyOutlined />
          <span>{t('drawer.tabCredentials', { count: credentialCount })}</span>
        </Space>
      ),
      children: (
        <CredentialSection channelId={channel.id} credentials={credentials} />
      ),
    },
    {
      key: 'models',
      label: (
        <Space>
          <RobotOutlined />
          <span>{t('drawer.tabModels', { count: modelCount })}</span>
        </Space>
      ),
      children: <ModelMappingSection channelId={channel.id} />,
    },
    {
      key: 'quota',
      label: (
        <Space>
          <SettingOutlined />
          <span>{t('drawer.tabQuota')}</span>
        </Space>
      ),
      children: <QuotaSettingsSection channel={channelDetail || channel} />,
    },
  ];

  return (
    <>
      <Drawer
        placement="right"
        width={720}
        open={open}
        onClose={onClose}
        destroyOnClose
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Text strong style={{ fontSize: 16 }}>{channel.name}</Text>
            <Tag color={channel.state === 'ACTIVE' ? 'green' : 'default'}>
              {channel.state === 'ACTIVE' ? t('status.running') : t('status.stopped')}
            </Tag>
          </div>
        }
        extra={
          <Space size={8}>
            <Button
              type="primary"
              icon={<ApiOutlined />}
              onClick={handleTest}
              loading={testCredential.isPending}
            >
              {t('drawer.connectivityTest')}
            </Button>
            <Popconfirm
              title={channel.state === 'ACTIVE' ? t('drawer.confirmDisable') : t('drawer.confirmEnable')}
              onConfirm={handleToggleState}
              okText={t('actions.confirm', { ns: 'common' })}
              cancelText={t('actions.cancel', { ns: 'common' })}
            >
              <Button loading={updateChannel.isPending}>
                {channel.state === 'ACTIVE' ? t('drawer.disableChannel') : t('drawer.enableChannel')}
              </Button>
            </Popconfirm>
            <Popconfirm
              title={t('drawer.confirmDelete')}
              description={t('drawer.confirmDeleteDesc', { name: channel.name })}
              onConfirm={handleDelete}
              okText={t('actions.delete', { ns: 'common' })}
              cancelText={t('actions.cancel', { ns: 'common' })}
              okButtonProps={{ danger: true }}
            >
              <Button danger loading={deleteChannel.isPending}>
                {t('card.delete')}
              </Button>
            </Popconfirm>
          </Space>
        }
      >
        {isLoading ? (
          <div style={{ textAlign: 'center', padding: '48px' }}>
            <Spin size="large" />
          </div>
        ) : (
          <div>
            <div style={{ marginBottom: 12 }}>
              <Space split={<Divider type="vertical" />} size="small">
                <Text type="secondary">
                  {t('drawer.provider') + ': '}
                  <Typography.Link onClick={() => setEditProviderOpen(true)}>
                    {channel.providerName}
                  </Typography.Link>
                </Text>
                <Text type="secondary">
                  {t('drawer.billingMode') + ': '} <Text strong>{getBillingModeLabel(channel.billingMode)}</Text>
                </Text>
                <Text type="secondary">
                  {t('drawer.priority') + ': '} <Text strong>P{channel.priority}</Text>
                </Text>
                <Text type="secondary">
                  {t('drawer.weight') + ': '} <Text strong>W{channel.weight}</Text>
                </Text>
              </Space>
            </div>

            <Tabs
              activeKey={activeTab}
              onChange={setActiveTab}
              items={tabItems}
            />
          </div>
        )}
      </Drawer>

      {/* 供应商编辑弹窗 */}
      <ProviderEditModal
        open={editProviderOpen}
        provider={provider || null}
        onClose={() => setEditProviderOpen(false)}
      />
    </>
  );
}
