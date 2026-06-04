import { Row, Col, Card, Tag, Typography, Button, Timeline } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  QuestionCircleOutlined,
  RightOutlined,
} from '@ant-design/icons';
import type { Channel, ChannelEndpointResponse, ChannelCredential, ChannelModel } from '@/types/channel';
import type { FC } from 'react';

const { Text, Title: AntTitle } = Typography;

interface ChannelOverviewTabProps {
  channel: Channel;
  credentials: ChannelCredential[];
  channelModels: ChannelModel[];
  onTabChange: (tab: string) => void;
}

/** 连通状态卡片 */
const ConnectivityCard: FC<{ channel: Channel }> = ({ channel }) => {
  // 预留：从本地缓存或测试结果获取连通状态
  const status: 'ok' | 'fail' | 'unknown' = 'unknown' as const;

  return (
    <Card size="small" title="连通状态" style={{ height: '100%' }}>
      {status === 'ok' && (
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          <CheckCircleOutlined style={{ fontSize: 28, color: '#52c41a' }} />
          <div style={{ marginTop: 8 }}>
            <Text strong style={{ color: '#52c41a' }}>连通正常</Text>
          </div>
        </div>
      )}
      {status === 'fail' && (
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          <CloseCircleOutlined style={{ fontSize: 28, color: '#ff4d4f' }} />
          <div style={{ marginTop: 8 }}>
            <Text strong style={{ color: '#ff4d4f' }}>连通异常</Text>
          </div>
        </div>
      )}
      {status === 'unknown' && (
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          <QuestionCircleOutlined style={{ fontSize: 28, color: '#d9d9d9' }} />
          <div style={{ marginTop: 8 }}>
            <Text type="secondary">未测试</Text>
          </div>
          <Button type="link" size="small">
            立即测试
          </Button>
        </div>
      )}
    </Card>
  );
};

/** Token 用量卡片（预留） */
const TokenUsageCard: FC = () => (
  <Card size="small" title="今日 Token" style={{ height: '100%' }}>
    <div style={{ textAlign: 'center', padding: '16px 0' }}>
      <div style={{ fontSize: 24, fontWeight: 700, color: '#1677ff' }}>--</div>
      <Text type="secondary" style={{ fontSize: 12 }}>输入/输出</Text>
    </div>
  </Card>
);

/** 成本卡片（预留） */
const CostCard: FC = () => (
  <Card size="small" title="今日成本" style={{ height: '100%' }}>
    <div style={{ textAlign: 'center', padding: '16px 0' }}>
      <div style={{ fontSize: 24, fontWeight: 700, color: '#722ed1' }}>--</div>
      <Text type="secondary" style={{ fontSize: 12 }}>本月累计: --</Text>
    </div>
  </Card>
);

/** 端点摘要卡片 */
const EndpointSummaryCard: FC<{
  endpoints: ChannelEndpointResponse[];
  onViewDetail: () => void;
}> = ({ endpoints, onViewDetail }) => (
  <Card size="small" title="端点" extra={<Button type="link" size="small" onClick={onViewDetail}>查看详情 <RightOutlined /></Button>}>
    <div style={{ marginBottom: 8 }}>
      <Text strong style={{ fontSize: 20 }}>{endpoints.length}</Text>
      <Text type="secondary" style={{ marginLeft: 8 }}>个端点</Text>
    </div>
    {endpoints.slice(0, 2).map((ep) => (
      <div key={ep.id} style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
        <Tag color={ep.state === 'ACTIVE' ? 'green' : 'default'} style={{ fontSize: 10 }}>
          {ep.state === 'ACTIVE' ? '正常' : '停用'}
        </Tag>
        <Text style={{ fontSize: 12 }} ellipsis>{ep.endpointUrl}</Text>
      </div>
    ))}
    {endpoints.length === 0 && <Text type="secondary">暂无端点</Text>}
  </Card>
);

/** Key 摘要卡片 */
const CredentialSummaryCard: FC<{
  credentials: ChannelCredential[];
  onViewDetail: () => void;
}> = ({ credentials, onViewDetail }) => (
  <Card size="small" title="API Key" extra={<Button type="link" size="small" onClick={onViewDetail}>查看详情 <RightOutlined /></Button>}>
    <div style={{ marginBottom: 8 }}>
      <Text strong style={{ fontSize: 20 }}>{credentials.length}</Text>
      <Text type="secondary" style={{ marginLeft: 8 }}>个 Key</Text>
    </div>
    {credentials.slice(0, 3).map((cred) => (
      <div key={cred.id} style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
        <Tag color={cred.state === 'ACTIVE' ? 'green' : 'default'} style={{ fontSize: 10 }}>
          {cred.apiKeyPrefix}...
        </Tag>
        <Text type="secondary" style={{ fontSize: 11 }}>权重: {cred.weight}</Text>
      </div>
    ))}
    {credentials.length === 0 && (
      <Text type="warning" style={{ fontSize: 12 }}>暂无 Key，请添加</Text>
    )}
  </Card>
);

/** 模型映射摘要卡片 */
const ModelSummaryCard: FC<{
  channelModels: ChannelModel[];
  onViewDetail: () => void;
}> = ({ channelModels, onViewDetail }) => (
  <Card size="small" title="模型映射" extra={<Button type="link" size="small" onClick={onViewDetail}>
    {channelModels.length > 0 ? `查看全部 ${channelModels.length} 个` : '查看详情'} <RightOutlined />
  </Button>}>
    <div style={{ marginBottom: 8 }}>
      <Text strong style={{ fontSize: 20 }}>{channelModels.length}</Text>
      <Text type="secondary" style={{ marginLeft: 8 }}>个映射</Text>
    </div>
    {channelModels.slice(0, 2).map((cm) => (
      <div key={cm.id} style={{ marginBottom: 4, fontSize: 12 }}>
        <Text>{cm.modelName}</Text>
        <Text type="secondary"> → </Text>
        <Text type="secondary">{cm.upstreamModelName}</Text>
      </div>
    ))}
    {channelModels.length === 0 && <Text type="secondary">暂无模型映射，点击添加</Text>}
  </Card>
);

/** 配额摘要卡片 */
const QuotaSummaryCard: FC<{
  channel: Channel;
  onViewDetail: () => void;
}> = ({ channel, onViewDetail }) => (
  <Card size="small" title="配额与设置" extra={<Button type="link" size="small" onClick={onViewDetail}>查看详情 <RightOutlined /></Button>}>
    <div style={{ marginBottom: 4 }}>
      <Text type="secondary" style={{ fontSize: 12 }}>配额上限: </Text>
      <Text style={{ fontSize: 12 }}>{channel.quotaLimit ?? '无限制'}</Text>
    </div>
    <div style={{ marginBottom: 4 }}>
      <Text type="secondary" style={{ fontSize: 12 }}>超时: </Text>
      <Text style={{ fontSize: 12 }}>{channel.timeout ? `${channel.timeout}ms` : '默认'}</Text>
    </div>
    <div>
      <Text type="secondary" style={{ fontSize: 12 }}>重试次数: </Text>
      <Text style={{ fontSize: 12 }}>{channel.maxRetries ?? '默认'}</Text>
    </div>
  </Card>
);

/**
 * 渠道概览Tab
 * 包含连通状态、Token/成本统计、资源摘要4卡片、活动时间线
 */
export const ChannelOverviewTab: FC<ChannelOverviewTabProps> = ({
  channel,
  credentials,
  channelModels,
  onTabChange,
}) => {
  return (
    <div style={{ padding: '0 4px' }}>
      {/* 连通状态 + Token/成本 统计卡 */}
      <Row gutter={12} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <ConnectivityCard channel={channel} />
        </Col>
        <Col span={8}>
          <TokenUsageCard />
        </Col>
        <Col span={8}>
          <CostCard />
        </Col>
      </Row>

      {/* 资源摘要 4卡片 */}
      <Row gutter={12} style={{ marginBottom: 16 }}>
        <Col span={12} style={{ marginBottom: 12 }}>
          <EndpointSummaryCard
            endpoints={channel.endpoints || []}
            onViewDetail={() => onTabChange('endpoints')}
          />
        </Col>
        <Col span={12} style={{ marginBottom: 12 }}>
          <CredentialSummaryCard
            credentials={credentials}
            onViewDetail={() => onTabChange('credentials')}
          />
        </Col>
        <Col span={12}>
          <ModelSummaryCard
            channelModels={channelModels}
            onViewDetail={() => onTabChange('models')}
          />
        </Col>
        <Col span={12}>
          <QuotaSummaryCard
            channel={channel}
            onViewDetail={() => onTabChange('quota')}
          />
        </Col>
      </Row>

      {/* 最近活动 */}
      <Card size="small" title="最近活动">
        <Timeline
          items={[{ children: <Text type="secondary">暂无活动记录</Text> }]}
        />
      </Card>
    </div>
  );
};
