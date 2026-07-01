import { useState, useEffect, useRef } from 'react';
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
  Table,
  Tag,
  Tooltip,
} from 'antd';
import {
  GlobalOutlined,
  KeyOutlined,
  RobotOutlined,
  SettingOutlined,
  BarChartOutlined,
  ApiOutlined,
  MoreOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  CheckCircleFilled,
  CloseCircleFilled,
} from '@ant-design/icons';
import axios from 'axios';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';
import ChannelStateTag from '@/components/common/ChannelStateTag';
import { getTransitionActionLabel } from '@/utils/stateTransitions';
import { getActionBarConfig } from '@/utils/channelActions';
import { useDangerConfirm } from '@/components/common/useDangerConfirm';
import { extractErrorMessage } from '@/utils/errorMessage';
import type {
  ChannelCard,
  ChannelState,
  ChannelHealthMatrixRow,
} from '@/types/channel';
import {
  useChannel,
  useChannelCredentials,
  useChannelModels,
  useTransitionChannelState,
  useTransitionChannelModelState,
  useDeleteChannel,
  channelKeys,
} from '@/services/query/useChannels';
import { channelApi } from '@/services/api/channel';
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
  /** 任务 9.1：闪电图标进入时为 true，用于让 Credentials Tab 内"测试全部"按钮短暂高亮 */
  highlightTestAll?: boolean;
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
  highlightTestAll = false,
}: ChannelDetailDrawerProps) {
  const { t } = useTranslation('channels');
  const [activeTab, setActiveTab] = useState('overview');
  const [editProviderOpen, setEditProviderOpen] = useState(false);
  // 任务 9.1：高亮"测试全部"按钮 800ms 后自清除
  const [testAllHighlight, setTestAllHighlight] = useState(false);
  // 任务 9.5/9.6：矩阵 Table 状态 + AbortController 引用
  const [matrix, setMatrix] = useState<ChannelHealthMatrixRow[] | null>(null);
  const [matrixLoading, setMatrixLoading] = useState(false);
  /**
   * AbortController 引用：每次 triggerTest 创建新的，便于关闭抽屉 / 卸载时调用 abort()
   * 取消进行中的 axios 请求，避免越权 setState 警告。
   */
  const acRef = useRef<AbortController | null>(null);
  const queryClient = useQueryClient();
  // 删除整个渠道（任务 8.7）：与其他危险操作统一为 useDangerConfirm
  const { confirm: confirmDeleteChannel, contextHolder: dangerContextHolder } =
    useDangerConfirm();

  useEffect(() => {
    if (open && initialTab) {
      setActiveTab(initialTab);
    }
  }, [open, initialTab]);

  // 任务 9.1：抽屉打开且 highlightTestAll=true 时，对"测试全部"按钮做 800ms 高亮
  useEffect(() => {
    if (open && highlightTestAll) {
      setTestAllHighlight(true);
      const tid = window.setTimeout(() => setTestAllHighlight(false), 800);
      return () => window.clearTimeout(tid);
    }
  }, [open, highlightTestAll]);

  /**
   * 任务 9.5/9.6：抽屉关闭 / 卸载 / 当前 channel 切换时，中止正在进行的 healthCheck。
   * 防止进入"组件已卸载但 setState 仍触发"的越权场景。
   */
  useEffect(() => {
    if (!open) {
      acRef.current?.abort();
      acRef.current = null;
      // 关闭抽屉清空旧矩阵，避免下次打开时残留
      setMatrix(null);
      setMatrixLoading(false);
    }
  }, [open]);
  useEffect(() => {
    return () => {
      acRef.current?.abort();
      acRef.current = null;
    };
  }, []);

  const transitionChannelState = useTransitionChannelState();
  const transitionModelState = useTransitionChannelModelState();
  const deleteChannel = useDeleteChannel();

  const { data: channelDetail, isLoading: detailLoading } = useChannel(channel?.id || 0);
  const { data: credentials = [], isLoading: credentialsLoading } = useChannelCredentials(
    channel?.id || 0
  );
  const { data: channelModels = [] } = useChannelModels(channel?.id || 0);
  const { data: provider } = useProvider(channel?.providerId || 0);

  if (!channel) return null;

  const isLoading = detailLoading || credentialsLoading;
  const currentState = (channelDetail?.state ?? channel.state) as ChannelState;
  const { primaryAction, dropdownTransitions, deleteDisabled } = getActionBarConfig(currentState);
  const isDeprecated = currentState === 'DEPRECATED';

  const getBillingModeLabel = (mode: string) => {
    const labels: Record<string, string> = {
      pay_as_you_go: t('billing.payAsYouGo'),
      subscription: t('billing.subscription'),
      package: t('billing.package'),
    };
    return labels[mode] || t('billing.default', { mode });
  };

  /**
   * 任务 9.5/9.6：触发健康检查（详情抽屉是唯一执行入口）。
   * - 通过 channelApi.healthCheck 调用 POST /channels/{id}/health-check，source='DRAWER'
   * - 每次调用前 abort 上一次（防止并发态污染）
   * - axios timeout 35s（覆盖后端 30s 超时再加 5s 缓冲）
   * - 成功后 setMatrix + invalidate channels 列表（刷新 lastHealthStatus 等字段）
   */
  const triggerTest = async () => {
    if (!channel) return;
    if (credentials.length === 0) {
      message.warning(t('drawer.noCredentials'));
      return;
    }
    // 取消上一轮（如果有）
    acRef.current?.abort();
    acRef.current = new AbortController();
    setMatrixLoading(true);
    try {
      const res = await channelApi.healthCheck(channel.id, 'DRAWER', {
        signal: acRef.current.signal,
        timeout: 35000,
      });
      // 防御：如果在 await 期间被 abort（如关闭抽屉），不再 setState
      if (acRef.current?.signal.aborted) return;
      setMatrix(res.matrix);
      // 刷新列表：让卡片 HealthDot 重渲（lastHealthStatus / lastHealthCheckAt 已被后端持久化）
      queryClient.invalidateQueries({ queryKey: channelKeys.allChannels() });
      queryClient.invalidateQueries({ queryKey: channelKeys.lists() });
    } catch (err) {
      // axios.isCancel 兼容 AbortError（axios v1+）
      if (axios.isCancel(err) || (err as Error)?.name === 'CanceledError' || (err as Error)?.name === 'AbortError') {
        return;
      }
      message.error(extractErrorMessage(err) || t('drawer.testFailed'));
    } finally {
      // 仅当当前 controller 还是这一次的，才清 loading（避免被新一轮 reset）
      if (!acRef.current?.signal.aborted) {
        setMatrixLoading(false);
      } else {
        setMatrixLoading(false);
      }
    }
  };

  /** 测试所有凭证（保留兼容兜底；当前 extra 按钮已切换到 triggerTest） */
  const handleTest = triggerTest;

  /** 状态转换 */
  const handleTransition = (targetState: ChannelState) => {
    const actionLabel = t(getTransitionActionLabel(currentState, targetState));

    if (targetState === 'RETIRED') {
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

    // DEPRECATED：警告色确认（非危险）
    if (targetState === 'DEPRECATED') {
      Modal.confirm({
        title: actionLabel,
        content: t('drawer.confirmDeprecate', '确定要将此渠道标记为下线？'),
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

  /** 构建抽屉 Dropdown 菜单项 */
  function buildDrawerMenuItems(
    state: ChannelState,
    transitions: ChannelState[],
    delDisabled: boolean,
    tr: (key: string) => string,
  ) {
    const items: any[] = transitions.map(target => ({
      key: target,
      label: tr(getTransitionActionLabel(state, target)),
      danger: target === 'RETIRED',
    }));

    items.push({ type: 'divider' as const });

    items.push({
      key: 'delete',
      label: delDisabled
        ? <Tooltip title={tr('channel.action.deleteDisabledWhenActive')}>
            <span style={{ color: 'rgba(0,0,0,0.25)', cursor: 'not-allowed' }}>{tr('card.delete')}</span>
          </Tooltip>
        : tr('card.delete'),
      danger: true,
    });

    return items;
  }

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
            {/* 连通性测试 — 图标 + Tooltip */}
            <Tooltip title={t('drawer.connectivityTest')}>
              <Button
                icon={<ApiOutlined />}
                onClick={handleTest}
                loading={matrixLoading}
                type={testAllHighlight ? 'primary' : 'default'}
                data-testid="drawer-connectivity-test-btn"
              />
            </Tooltip>

            {/* Primary 按钮 — 图标 + Tooltip */}
            {primaryAction && (
              <Tooltip title={t(getTransitionActionLabel(currentState, primaryAction))}>
                <Button
                  type="text"
                  loading={transitionChannelState.isPending}
                  icon={primaryAction === 'SUSPENDED' ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                  onClick={() => handleTransition(primaryAction)}
                />
              </Tooltip>
            )}

            {/* Dropdown — 剩余转换 + 删除 */}
            <Dropdown
              menu={{
                items: buildDrawerMenuItems(currentState, dropdownTransitions, deleteDisabled, t),
                onClick: ({ key }) => {
                  if (key === 'delete') {
                    if (!deleteDisabled) {
                      confirmDeleteChannel({
                        titleKey: 'channel.deleteDangerTitle',
                        descriptionKey: 'channel.deleteDangerDescription',
                        descriptionParams: { name: channel.name },
                        onOk: handleDelete,
                      });
                    }
                  } else handleTransition(key as ChannelState);
                },
              }}
            >
              <Tooltip title={t('drawer.changeState')}>
                <Button loading={transitionChannelState.isPending} icon={<MoreOutlined />} />
              </Tooltip>
            </Dropdown>
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

            {/* 任务 9.5/9.6：连通性测试矩阵 Table —— 抽屉是唯一执行入口 */}
            {(matrix !== null || matrixLoading) && (
              <div style={{ marginBottom: 16 }} data-testid="health-matrix-section">
                <div style={{ marginBottom: 8 }}>
                  <Text strong>{t('drawer.healthMatrix.title', '连通性测试结果')}</Text>
                </div>
                <Table<ChannelHealthMatrixRow>
                  size="small"
                  rowKey="credentialId"
                  loading={matrixLoading}
                  dataSource={matrix ?? []}
                  pagination={false}
                  columns={[
                    {
                      title: t('drawer.healthMatrix.colKey', '脱敏 Key'),
                      dataIndex: 'keyMasked',
                      key: 'keyMasked',
                    },
                    {
                      title: t('drawer.healthMatrix.colAuth', '认证'),
                      dataIndex: 'auth',
                      key: 'auth',
                      render: (auth: 'PASS' | 'FAIL', row) =>
                        auth === 'PASS' ? (
                          <Tag icon={<CheckCircleFilled />} color="success">
                            {t('drawer.healthMatrix.authPass', '通过')}
                          </Tag>
                        ) : (
                          <Tooltip title={row.authError ?? ''}>
                            <Tag icon={<CloseCircleFilled />} color="error">
                              {row.authError ?? t('drawer.healthMatrix.authFail', '失败')}
                            </Tag>
                          </Tooltip>
                        ),
                    },
                    {
                      title: t('drawer.healthMatrix.colModels', '可用模型'),
                      dataIndex: 'availableModels',
                      key: 'availableModels',
                      render: (models?: string[] | null) => {
                        const list = models ?? [];
                        if (list.length === 0) return <Text type="secondary">-</Text>;
                        return (
                          <Tooltip title={list.join(', ')}>
                            <span>{t('drawer.healthMatrix.modelCount', { count: list.length })}</span>
                          </Tooltip>
                        );
                      },
                    },
                    {
                      title: t('drawer.healthMatrix.colLatency', '延迟'),
                      dataIndex: 'latencyMs',
                      key: 'latencyMs',
                      render: (ms?: number | null) => (ms == null ? <Text type="secondary">-</Text> : <span>{ms}ms</span>),
                    },
                  ]}
                />
              </div>
            )}

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
