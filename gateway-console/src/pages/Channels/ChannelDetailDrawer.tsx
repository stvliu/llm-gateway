import { useState } from 'react';
import {
  Drawer,
  Typography,
  Tag,
  Button,
  Space,
  Divider,
  Badge,
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
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ChannelCard } from '@/types/channel';
import {
  useChannel,
  useChannelCredentials,
  useUpdateChannel,
  useTestChannelCredential,
  useDeleteChannel,
} from '@/services/query/useChannels';
import { EndpointSection } from './EndpointSection';
import { CredentialSection } from './CredentialSection';
import { ModelMappingSection } from './ModelMappingSection';
import { QuotaSettingsSection } from './QuotaSettingsSection';

const { Text } = Typography;

interface ChannelDetailDrawerProps {
  channel: ChannelCard | null;
  open: boolean;
  onClose: () => void;
}

/**
 * 渠道详情抽屉
 * 顶部信息栏 + Tabs 标签页（端点 / API Key / 模型映射 / 配额与设置）
 */
export function ChannelDetailDrawer({
  channel,
  open,
  onClose,
}: ChannelDetailDrawerProps) {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('endpoints');
  const updateChannel = useUpdateChannel();
  const deleteChannel = useDeleteChannel();

  const { data: channelDetail, isLoading: detailLoading } = useChannel(channel?.id || 0);
  const { data: credentials = [], isLoading: credentialsLoading } = useChannelCredentials(
    channel?.id || 0
  );
  const testCredential = useTestChannelCredential();

  if (!channel) return null;

  const isLoading = detailLoading || credentialsLoading;

  const getBillingModeLabel = (mode: string) => {
    const labels: Record<string, string> = {
      pay_as_you_go: '按量付费',
      subscription: '订阅',
      package: '套餐',
    };
    return labels[mode] || mode;
  };

  /** 测试所有凭证 */
  const handleTest = async () => {
    if (credentials.length === 0) {
      message.warning('暂无凭证，请先添加 API Key');
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
        message.success(`测试完成：全部 ${successCount} 个凭证可用`);
      } else {
        message.warning(`测试完成：${successCount} 个可用，${failCount} 个不可用`);
      }
    } catch {
      message.error('测试失败');
    }
  };

  /** 切换渠道状态 */
  const handleToggleState = async () => {
    const newState = channel.state === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await updateChannel.mutateAsync({ id: channel.id, data: { state: newState } });
      message.success(newState === 'ACTIVE' ? '渠道已启用' : '渠道已停用');
    } catch {
      message.error('状态切换失败');
    }
  };

  /** 删除渠道 */
  const handleDelete = async () => {
    try {
      await deleteChannel.mutateAsync({ id: channel.id, providerId: channel.providerId });
      message.success('渠道已删除');
      onClose();
    } catch {
      message.error('删除失败');
    }
  };

  const tabItems = [
    {
      key: 'endpoints',
      label: (
        <Space>
          <GlobalOutlined />
          <span>端点</span>
          {channelDetail?.endpoints ? (
            <Badge count={channelDetail.endpoints.length} size="small" />
          ) : null}
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
          <span>API Key</span>
          <Badge count={credentials.length} size="small" />
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
          <span>模型映射</span>
        </Space>
      ),
      children: <ModelMappingSection channelId={channel.id} />,
    },
    {
      key: 'settings',
      label: (
        <Space>
          <SettingOutlined />
          <span>配额与设置</span>
        </Space>
      ),
      children: <QuotaSettingsSection channel={channelDetail || channel} />,
    },
  ];

  return (
    <Drawer
      title={channel.name}
      placement="right"
      width={800}
      open={open}
      onClose={onClose}
      destroyOnClose
      extra={
        <Space>
          <Button onClick={handleTest} loading={testCredential.isPending}>
            测试
          </Button>
          <Popconfirm
            title={channel.state === 'ACTIVE' ? '确定停用此渠道吗？' : '确定启用此渠道吗？'}
            onConfirm={handleToggleState}
            okText="确定"
            cancelText="取消"
          >
            <Button
              danger={channel.state === 'ACTIVE'}
              loading={updateChannel.isPending}
            >
              {channel.state === 'ACTIVE' ? '停用' : '启用'}
            </Button>
          </Popconfirm>
          <Popconfirm
            title="确认删除"
            description={`确定要删除渠道「${channel.name}」吗？此操作不可撤销。`}
            onConfirm={handleDelete}
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
          >
            <Button danger loading={deleteChannel.isPending}>
              删除
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
          {/* 摘要信息 */}
          <div style={{ marginBottom: 16 }}>
            <Space split={<Divider type="vertical" />} size="small">
              <Text type="secondary">
                供应商:{' '}
                <Typography.Link
                  onClick={() => navigate(`/providers?id=${channel.providerId}`)}
                >
                  {channel.providerName}
                </Typography.Link>
              </Text>
              <Text type="secondary">
                计费模式: <Text strong>{getBillingModeLabel(channel.billingMode)}</Text>
              </Text>
              <Text type="secondary">
                优先级: <Text strong>P{channel.priority}</Text>
              </Text>
              <Text type="secondary">
                权重: <Text strong>W{channel.weight}</Text>
              </Text>
              <Tag color={channel.state === 'ACTIVE' ? 'green' : 'default'}>
                {channel.state === 'ACTIVE' ? '已启用' : '已停用'}
              </Tag>
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
  );
}