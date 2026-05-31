import {
  Drawer,
  Typography,
  Tag,
  Button,
  Space,
  Divider,
  Badge,
  Row,
  Col,
  message,
  Spin,
} from 'antd';
import {
  GlobalOutlined,
  KeyOutlined,
  RobotOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import type { ChannelCard } from '@/types/channel';
import { useChannel, useChannelCredentials } from '@/services/query/useChannels';
import { EndpointSection } from './EndpointSection';
import { CredentialSection } from './CredentialSection';
import { ModelMappingSection } from './ModelMappingSection';
import { QuotaSettingsSection } from './QuotaSettingsSection';

const { Title, Text, Link } = Typography;

interface ChannelDetailDrawerProps {
  channel: ChannelCard | null;
  open: boolean;
  onClose: () => void;
}

/**
 * 渠道详情抽屉主组件
 * 展示渠道的完整信息，包含四个可编辑区域
 */
export function ChannelDetailDrawer({
  channel,
  open,
  onClose,
}: ChannelDetailDrawerProps) {

  // 获取渠道详情（包含端点）
  const { data: channelDetail, isLoading: detailLoading } = useChannel(channel?.id || 0);

  // 获取凭证列表
  const { data: credentials = [], isLoading: credentialsLoading } = useChannelCredentials(
    channel?.id || 0
  );

  if (!channel) return null;

  const isLoading = detailLoading || credentialsLoading;

  /** 计费模式显示 */
  const getBillingModeLabel = (mode: string) => {
    const labels: Record<string, string> = {
      PAY_AS_YOU_GO: '按量付费',
      SUBSCRIPTION: '订阅',
      PACKAGE: '套餐',
    };
    return labels[mode] || mode;
  };

  /** 状态标签颜色 */
  const getStateColor = (state: string) => {
    return state === 'ACTIVE' ? 'green' : 'default';
  };

  /** 头部区域 */
  const renderHeader = () => (
    <div>
      {/* 供应商 Logo + 渠道名 + 状态标签 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
        <Title level={4} style={{ margin: 0 }}>
          {channel.name}
        </Title>
        <Tag color={getStateColor(channel.state)}>
          {channel.state === 'ACTIVE' ? '已启用' : '已停用'}
        </Tag>
      </div>

      {/* 供应商名称（可点击） */}
      <div style={{ marginBottom: 8 }}>
        <Text type="secondary">供应商: </Text>
        <Link onClick={() => message.info('供应商详情页面将在后续实现')}>
          {channel.providerName}
        </Link>
      </div>

      {/* 计费模式 · 优先级 · 权重 */}
      <Space split={<Divider type="vertical" />} size="small">
        <Text type="secondary">
          计费模式: <Text strong>{getBillingModeLabel(channel.billingMode)}</Text>
        </Text>
        <Text type="secondary">
          优先级: <Text strong>P{channel.priority}</Text>
        </Text>
        <Text type="secondary">
          权重: <Text strong>W{channel.weight}</Text>
        </Text>
      </Space>

      {/* 操作按钮 */}
      <div style={{ marginTop: 16 }}>
        <Space>
          <Button
            type="primary"
            onClick={() => message.info('测试功能将在后续实现')}
          >
            测试
          </Button>
          <Button onClick={() => message.info('编辑功能将在后续实现')}>
            编辑
          </Button>
          <Button
            danger
            onClick={() => message.info('停用功能将在后续实现')}
          >
            停用
          </Button>
        </Space>
      </div>
    </div>
  );

  /** 四宫格区域 */
  const renderGrid = () => {
    if (isLoading) {
      return (
        <div style={{ textAlign: 'center', padding: '48px' }}>
          <Spin size="large" />
        </div>
      );
    }

    return (
      <Row gutter={[16, 16]}>
        {/* 🌐 端点 */}
        <Col span={12}>
          <div
            style={{
              border: '1px solid #d9d9d9',
              borderRadius: 8,
              padding: 16,
              height: '100%',
            }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: 12,
              }}
            >
              <Space>
                <GlobalOutlined style={{ fontSize: 18 }} />
                <Text strong>端点</Text>
                <Badge count={channelDetail?.endpoints?.length || 0} />
              </Space>
            </div>
            <EndpointSection
              channelId={channel.id}
              endpoints={channelDetail?.endpoints || []}
            />
          </div>
        </Col>

        {/* 🔑 API Key */}
        <Col span={12}>
          <div
            style={{
              border: '1px solid #d9d9d9',
              borderRadius: 8,
              padding: 16,
              height: '100%',
            }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: 12,
              }}
            >
              <Space>
                <KeyOutlined style={{ fontSize: 18 }} />
                <Text strong>API Key</Text>
                <Badge count={credentials.length} />
              </Space>
            </div>
            <CredentialSection channelId={channel.id} credentials={credentials} />
          </div>
        </Col>

        {/* 🤖 模型映射 */}
        <Col span={12}>
          <div
            style={{
              border: '1px solid #d9d9d9',
              borderRadius: 8,
              padding: 16,
              height: '100%',
            }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: 12,
              }}
            >
              <Space>
                <RobotOutlined style={{ fontSize: 18 }} />
                <Text strong>模型映射</Text>
                {/* 模型数量将在数据加载后显示 */}
              </Space>
            </div>
            <ModelMappingSection channelId={channel.id} />
          </div>
        </Col>

        {/* ⚙️ 配额与设置 */}
        <Col span={12}>
          <div
            style={{
              border: '1px solid #d9d9d9',
              borderRadius: 8,
              padding: 16,
              height: '100%',
            }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: 12,
              }}
            >
              <Space>
                <SettingOutlined style={{ fontSize: 18 }} />
                <Text strong>配额与设置</Text>
              </Space>
            </div>
            <QuotaSettingsSection channel={channelDetail || channel} />
          </div>
        </Col>
      </Row>
    );
  };

  return (
    <Drawer
      title="渠道详情"
      placement="right"
      width={800}
      open={open}
      onClose={onClose}
      destroyOnClose
    >
      <div>
        {/* 头部区域 */}
        {renderHeader()}

        <Divider />

        {/* 四宫格区域 */}
        {renderGrid()}
      </div>
    </Drawer>
  );
}