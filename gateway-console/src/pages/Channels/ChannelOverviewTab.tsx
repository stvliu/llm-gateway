/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useState } from 'react';
import { Row, Col, Card, Tag, Typography, Button, Timeline, message, theme, Tooltip } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  QuestionCircleOutlined,
  ThunderboltOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { Channel, ChannelEndpointResponse, ChannelCredential, ChannelModel } from '@/types/channel';
import { useTestChannelCredential } from '@/services/query/useChannels';
import type { FC } from 'react';

const { Text } = Typography;

interface ChannelOverviewTabProps {
  channel: Channel;
  credentials: ChannelCredential[];
  channelModels: ChannelModel[];
  onTabChange: (tab: string) => void;
}

/** 连通状态 */
type ConnectivityStatus = 'ok' | 'fail' | 'unknown';

/** 连通状态卡片 */
const ConnectivityCard: FC<{
  channel: Channel;
  credentials: ChannelCredential[];
}> = ({ channel, credentials }) => {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const [status, setStatus] = useState<ConnectivityStatus>('unknown');
  const [testing, setTesting] = useState(false);
  const testCredential = useTestChannelCredential();

  const handleTest = async () => {
    if (credentials.length === 0) {
      message.warning(t('drawer.noCredentials'));
      return;
    }
    setTesting(true);
    try {
      // 测试第一个凭证来判断连通性
      const result = await testCredential.mutateAsync({ channelId: channel.id, id: credentials[0].id });
      setStatus(result.success ? 'ok' : 'fail');
    } catch {
      setStatus('fail');
    } finally {
      setTesting(false);
    }
  };

  return (
    <Card size="small" title={t('overview.connectivity')} style={{ height: '100%' }}>
      {status === 'ok' && (
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          <CheckCircleOutlined style={{ fontSize: 28, color: token.colorSuccess }} />
          <div style={{ marginTop: 8 }}>
            <Text strong style={{ color: token.colorSuccess }}>{t('overview.connected')}</Text>
          </div>
        </div>
      )}
      {status === 'fail' && (
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          <CloseCircleOutlined style={{ fontSize: 28, color: token.colorError }} />
          <div style={{ marginTop: 8 }}>
            <Text strong style={{ color: token.colorError }}>{t('overview.disconnected')}</Text>
          </div>
        </div>
      )}
      {status === 'unknown' && (
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          <QuestionCircleOutlined style={{ fontSize: 28, color: token.colorTextDisabled }} />
          <div style={{ marginTop: 8 }}>
            <Text type="secondary">{t('overview.notTested')}</Text>
          </div>
          <Tooltip title={t('overview.testNow')}>
            <Button type="text" size="small" icon={<ThunderboltOutlined />} onClick={handleTest} loading={testing} />
          </Tooltip>
        </div>
      )}
    </Card>
  );
};

/** Token 用量卡片（预留） */
const TokenUsageCard: FC = () => {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  return (
    <Card size="small" title={t('overview.todayToken')} style={{ height: '100%' }}>
      <div style={{ textAlign: 'center', padding: '16px 0' }}>
        <div style={{ fontSize: 24, fontWeight: 700, color: token.colorPrimary }}>--</div>
        <Text type="secondary" style={{ fontSize: 12 }}>{t('overview.inputOutput')}</Text>
      </div>
    </Card>
  );
};

/** 成本卡片（预留） */
const CostCard: FC = () => {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  return (
    <Card size="small" title={t('overview.todayCost')} style={{ height: '100%' }}>
      <div style={{ textAlign: 'center', padding: '16px 0' }}>
        <div style={{ fontSize: 24, fontWeight: 700, color: token.colorLink }}>--</div>
        <Text type="secondary" style={{ fontSize: 12 }}>{t('overview.monthlyTotal')}</Text>
      </div>
    </Card>
  );
};

/** 端点摘要卡片 */
const EndpointSummaryCard: FC<{
  endpoints: ChannelEndpointResponse[];
  onViewDetail: () => void;
}> = ({ endpoints, onViewDetail }) => {
  const { t } = useTranslation('channels');
  return (
  <Card size="small" title={t('overview.endpoints')} extra={<Tooltip title={t('overview.viewDetail')}><Button type="text" size="small" icon={<EyeOutlined />} onClick={onViewDetail} /></Tooltip>}>
    <div style={{ marginBottom: 8 }}>
      <Text strong style={{ fontSize: 20 }}>{endpoints.length}</Text>
      <Text type="secondary" style={{ marginLeft: 8 }}>{t('overview.endpointCount', { count: endpoints.length })}</Text>
    </div>
    {endpoints.slice(0, 2).map((ep) => (
      <div key={ep.id} style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
        <Tag color="blue" style={{ fontSize: 10 }}>
          {ep.protocol?.toUpperCase()}
        </Tag>
        <Text style={{ fontSize: 12 }} ellipsis>{ep.endpointUrl}</Text>
      </div>
    ))}
    {endpoints.length === 0 && <Text type="secondary">{t('overview.noEndpoints')}</Text>}
  </Card>
  );
};

/** Key 摘要卡片 */
const CredentialSummaryCard: FC<{
  credentials: ChannelCredential[];
  onViewDetail: () => void;
}> = ({ credentials, onViewDetail }) => {
  const { t } = useTranslation('channels');
  return (
  <Card size="small" title={t('overview.credentials')} extra={<Tooltip title={t('overview.viewDetail')}><Button type="text" size="small" icon={<EyeOutlined />} onClick={onViewDetail} /></Tooltip>}>
    <div style={{ marginBottom: 8 }}>
      <Text strong style={{ fontSize: 20 }}>{credentials.length}</Text>
      <Text type="secondary" style={{ marginLeft: 8 }}>{t('overview.credentialCount', { count: credentials.length })}</Text>
    </div>
    {credentials.slice(0, 3).map((cred) => (
      <div key={cred.id} style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
        <Tag color={cred.state === 'ACTIVE' ? 'green' : 'default'} style={{ fontSize: 10 }}>
          {cred.apiKeyPrefix}...
        </Tag>
        <Text type="secondary" style={{ fontSize: 11 }}>{t('overview.weight', { weight: cred.weight })}</Text>
      </div>
    ))}
    {credentials.length === 0 && (
      <Text type="warning" style={{ fontSize: 12 }}>{t('overview.noCredentials')}</Text>
    )}
  </Card>
  );
};

/** 模型映射摘要卡片 */
const ModelSummaryCard: FC<{
  channelModels: ChannelModel[];
  onViewDetail: () => void;
}> = ({ channelModels, onViewDetail }) => {
  const { t } = useTranslation('channels');
  return (
  <Card size="small" title={t('overview.modelMappings')} extra={<Tooltip title={channelModels.length > 0 ? t('overview.viewAll', { count: channelModels.length }) : t('overview.viewDetail')}><Button type="text" size="small" icon={<EyeOutlined />} onClick={onViewDetail} /></Tooltip>}>
    <div style={{ marginBottom: 8 }}>
      <Text strong style={{ fontSize: 20 }}>{channelModels.length}</Text>
      <Text type="secondary" style={{ marginLeft: 8 }}>{t('overview.mappingCount', { count: channelModels.length })}</Text>
    </div>
    {channelModels.slice(0, 2).map((cm) => (
      <div key={cm.id} style={{ marginBottom: 4, fontSize: 12 }}>
        <Text>{cm.modelName}</Text>
        <Text type="secondary"> → </Text>
        <Text type="secondary">{cm.upstreamModelName}</Text>
      </div>
    ))}
    {channelModels.length === 0 && <Text type="secondary">{t('overview.noModels')}</Text>}
  </Card>
  );
};

/** 配额摘要卡片 */
const QuotaSummaryCard: FC<{
  channel: Channel;
  onViewDetail: () => void;
}> = ({ channel, onViewDetail }) => {
  const { t } = useTranslation('channels');
  return (
  <Card size="small" title={t('overview.quotaSettings')} extra={<Tooltip title={t('overview.viewDetail')}><Button type="text" size="small" icon={<EyeOutlined />} onClick={onViewDetail} /></Tooltip>}>
    <div style={{ marginBottom: 4 }}>
      <Text type="secondary" style={{ fontSize: 12 }}>{t('overview.quotaLimit')}</Text>
      <Text style={{ fontSize: 12 }}>{channel.quotaLimit ?? t('overview.noLimit')}</Text>
    </div>
    <div style={{ marginBottom: 4 }}>
      <Text type="secondary" style={{ fontSize: 12 }}>{t('overview.timeout')}</Text>
      <Text style={{ fontSize: 12 }}>{channel.timeout ? `${channel.timeout}ms` : t('overview.timeoutDefault')}</Text>
    </div>
    <div>
      <Text type="secondary" style={{ fontSize: 12 }}>{t('overview.maxRetries')}</Text>
      <Text style={{ fontSize: 12 }}>{channel.maxRetries ?? t('overview.timeoutDefault')}</Text>
    </div>
  </Card>
  );
};

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
  const { t } = useTranslation('channels');
  return (
    <div style={{ padding: '0 4px' }}>
      {/* 连通状态 + Token/成本 统计卡 */}
      <Row gutter={12} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <ConnectivityCard channel={channel} credentials={credentials} />
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
      <Card size="small" title={t('overview.recentActivity')}>
        <Timeline
          items={[{ children: <Text type="secondary">{t('overview.noActivity')}</Text> }]}
        />
      </Card>
    </div>
  );
};
