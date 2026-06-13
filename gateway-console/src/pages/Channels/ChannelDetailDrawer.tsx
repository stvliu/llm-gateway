import { useState, useEffect } from 'react';
import {
  Drawer,
  Typography,
  Button,
  Space,
  Divider,
  message,
  Spin,
  Tabs,
  Dropdown,
  Alert,
  Modal,
} from 'antd';
import {
  GlobalOutlined,
  KeyOutlined,
  RobotOutlined,
  SettingOutlined,
  BarChartOutlined,
  ApiOutlined,
  DeleteOutlined,
  DownOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import ChannelStateTag from '@/components/common/ChannelStateTag';
import { getAvailableTransitions, getTransitionActionLabel } from '@/utils/stateTransitions';
import { useDangerConfirm } from '@/components/common/useDangerConfirm';
import type { ChannelCard, ChannelState } from '@/types/channel';
import {
  useChannel,
  useChannelCredentials,
  useChannelModels,
  useTransitionChannelState,
  useTransitionChannelModelState,
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
  initialTab?: string;
}

/**
 * 渠道详情抽屉
 * 头部：供应商Logo+渠道名+状态Tag+快捷操作条
 */
export function ChannelDetailDrawer({
  channel,
  open,
  onClose,
  initialTab,
}: ChannelDetailDrawerProps) {
  const { t } = useTranslation('channels');
  const [activeTab, setActiveTab] = useState('overview');
  const [editProviderOpen, setEditProviderOpen] = useState(false);
  // 删除整个渠道（任务 8.7）：与其他危险操作统一为 useDangerConfirm
  const { confirm: confirmDeleteChannel, contextHolder: dangerContextHolder } =
    useDangerConfirm();

  useEffect(() => {
    if (open && initialTab) {
      setActiveTab(initialTab);
    }
  }, [open, initialTab]);

  const transitionChannelState = useTransitionChannelState();
  const transitionModelState = useTransitionChannelModelState();
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
  const currentState = (channelDetail?.state ?? channel.state) as ChannelState;
  const availableTransitions = getAvailableTransitions(currentState);
  const isDeprecated = currentState === 'DEPRECATED';

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

  /** 状态转换 */
  const handleTransition = (targetState: ChannelState) => {
    const actionLabel = getTransitionActionLabel(currentState, targetState);

    if (targetState === 'DEPRECATED' || targetState === 'RETIRED') {
      let title = actionLabel;
      let content = t('drawer.confirmDeprecate', '确定要将此渠道标记为下线？');
      if (targetState === 'RETIRED') {
        title = t('channel.action.retire.confirmTitle', '停用渠道？');
        content = t(
          'channel.action.retire.confirmDescription',
          '停用后该渠道不再参与任何流量分配，且无法恢复，已建立的指标历史保留'
        );
      }
      Modal.confirm({
        title,
        content,
        okType: 'danger',
        onOk: () => transitionChannelState.mutateAsync({ id: channel.id, targetState }),
      });
      return;
    }

    // 暂停操作（→ SUSPENDED）：轻量二次确认（非红色）
    if (targetState === 'SUSPENDED') {
      Modal.confirm({
        title: t('channel.action.suspend.confirmTitle', '暂停渠道？'),
        content: t(
          'channel.action.suspend.confirmDescription',
          '暂停后该渠道不再分配流量，但保留配置'
        ),
        okType: 'default',
        onOk: () => transitionChannelState.mutateAsync({ id: channel.id, targetState }),
      });
      return;
    }

    transitionChannelState.mutate({ id: channel.id, targetState });
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
      children: <CredentialSection channelId={channel.id} credentials={credentials} />,
    },
    {
      key: 'models',
      label: (
        <Space>
          <RobotOutlined />
          <span>{t('drawer.tabModels', { count: modelCount })}</span>
        </Space>
      ),
      children: (
        <ModelMappingSection
          channelId={channel.id}
          channelModels={channelModels}
          onStateTransition={(modelId, targetState) =>
            transitionModelState.mutate({ channelId: channel.id, modelId, targetState })
          }
        />
      ),
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
      {/* useDangerConfirm 的 contextHolder 必须挂载到组件树，否则 modal 不出现 */}
      {dangerContextHolder}
      <Drawer
        placement="right"
        width={720}
        open={open}
        onClose={onClose}
        destroyOnClose
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Text strong style={{ fontSize: 16 }}>{channel.name}</Text>
            <ChannelStateTag state={currentState} />
          </div>
        }
        extra={
          <Space size={8}>
            <Button
              icon={<ApiOutlined />}
              onClick={handleTest}
              loading={testCredential.isPending}
            >
              {t('drawer.connectivityTest')}
            </Button>

            {/* 状态转换操作菜单 */}
            {availableTransitions.length > 0 && (
              <Dropdown
                menu={{
                  items: availableTransitions.map((target) => ({
                    key: target,
                    label: getTransitionActionLabel(currentState, target),
                    danger: target === 'DEPRECATED' || target === 'RETIRED',
                  })),
                  onClick: ({ key }) => handleTransition(key as ChannelState),
                }}
              >
                <Button loading={transitionChannelState.isPending}>
                  <Space>
                    {t('drawer.changeState')}
                    <DownOutlined />
                  </Space>
                </Button>
              </Dropdown>
            )}

            <Button
              danger
              icon={<DeleteOutlined />}
              loading={deleteChannel.isPending}
              onClick={() =>
                confirmDeleteChannel({
                  titleKey: 'channel.deleteDangerTitle',
                  descriptionKey: 'channel.deleteDangerDescription',
                  descriptionParams: { name: channel.name },
                  onOk: handleDelete,
                })
              }
            >
              {t('card.delete')}
            </Button>
          </Space>
        }
      >
        {isLoading ? (
          <div style={{ textAlign: 'center', padding: '48px' }}>
            <Spin size="large" />
          </div>
        ) : (
          <div>
            {/* DEPRECATED 状态警告 */}
            {isDeprecated && (
              <Alert
                message={t('drawer.deprecatedWarning', '此渠道已标记下线，将不再被路由选择。')}
                type="warning"
                showIcon
                style={{ marginBottom: 12 }}
              />
            )}

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

      <ProviderEditModal
        open={editProviderOpen}
        provider={provider || null}
        onClose={() => setEditProviderOpen(false)}
      />
    </>
  );
}
