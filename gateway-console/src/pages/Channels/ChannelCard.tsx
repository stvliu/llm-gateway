import { Card, Tag, Typography, Tooltip } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons';
import type { ChannelCard as ChannelCardType } from '@/types/channel';
import type { FC } from 'react';

const { Text } = Typography;

export interface ChannelCardProps {
  /** 渠道数据（含统计） */
  channel: ChannelCardType;
  /** 点击回调 */
  onClick: (channel: ChannelCardType) => void;
}

/**
 * 根据响应时间返回颜色
 * ≤500ms 绿, ≤2s 黄, >2s 红, 未测试 灰
 */
const getResponseTimeColor = (responseTime: number | null | undefined): string => {
  if (responseTime === null || responseTime === undefined) return '#999';
  if (responseTime <= 500) return '#52c41a';
  if (responseTime <= 2000) return '#faad14';
  return '#ff4d4f';
};

/**
 * 根据响应时间返回文本
 */
const getResponseTimeText = (responseTime: number | null | undefined): string => {
  if (responseTime === null || responseTime === undefined) return '未测试';
  if (responseTime < 1000) return `${responseTime}ms`;
  return `${(responseTime / 1000).toFixed(1)}s`;
};

/**
 * 渠道卡片组件
 * 一行一个，横向铺满
 */
export const ChannelCard: FC<ChannelCardProps> = ({ channel, onClick }) => {
  const isActive = channel.state === 'ACTIVE';

  // 检查是否缺少关键配置（凭证为 0 表示配置中）
  const isIncomplete = channel.stats.credentialCount === 0;

  return (
    <Card
      hoverable
      onClick={() => onClick(channel)}
      style={{
        marginBottom: '8px',
        opacity: isActive ? 1 : 0.6,
        border: isIncomplete ? '2px solid #faad14' : undefined,
        transition: 'all 0.2s',
      }}
      styles={{ body: { padding: '16px' } }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        {/* 左侧：状态点 + 渠道名 */}
        <div style={{ flex: '0 0 280px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div
            style={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              backgroundColor: isActive ? '#52c41a' : '#d9d9d9',
            }}
          />
          <Text strong style={{ fontSize: '15px' }}>
            {channel.name}
          </Text>
          {isIncomplete && (
            <Tag color="warning" style={{ marginLeft: '4px' }}>
              配置中
            </Tag>
          )}
        </div>

        {/* 状态标签 */}
        <div style={{ flex: '0 0 80px' }}>
          <Tag color={isActive ? 'success' : 'default'}>
            {isActive ? '已启用' : '已停用'}
          </Tag>
        </div>

        {/* 资源统计 */}
        <div style={{ flex: '0 0 280px', display: 'flex', gap: '16px' }}>
          <Tooltip title="端点数量">
            <Text type="secondary" style={{ fontSize: '13px' }}>
              端点: {channel.stats.endpointCount}
            </Text>
          </Tooltip>
          <Tooltip title="凭证数量">
            <Text
              type={isIncomplete ? 'warning' : 'secondary'}
              style={{ fontSize: '13px' }}
            >
              Key: {channel.stats.credentialCount}
              {isIncomplete && ' ⚠'}
            </Text>
          </Tooltip>
          <Tooltip title="模型映射数量">
            <Text type="secondary" style={{ fontSize: '13px' }}>
              模型: {channel.stats.modelCount}
            </Text>
          </Tooltip>
        </div>

        {/* 计费信息 */}
        <div style={{ flex: '0 0 100px' }}>
          <Text type="secondary" style={{ fontSize: '13px' }}>
            {channel.billingMode === 'PAY_AS_YOU_GO'
              ? '按量付费'
              : channel.billingMode === 'SUBSCRIPTION'
              ? '订阅'
              : channel.billingMode}
          </Text>
        </div>

        {/* 右侧：响应时间 + 详情入口 */}
        <div
          style={{
            flex: '0 0 120px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            gap: '8px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            {channel.stats.avgResponseTime === null ||
            channel.stats.avgResponseTime === undefined ? (
              <QuestionCircleOutlined style={{ color: '#999' }} />
            ) : channel.stats.avgResponseTime <= 500 ? (
              <CheckCircleOutlined style={{ color: '#52c41a' }} />
            ) : channel.stats.avgResponseTime <= 2000 ? (
              <ClockCircleOutlined style={{ color: '#faad14' }} />
            ) : (
              <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
            )}
            <Text
              style={{
                fontSize: '13px',
                color: getResponseTimeColor(channel.stats.avgResponseTime),
              }}
            >
              {getResponseTimeText(channel.stats.avgResponseTime)}
            </Text>
          </div>
          <Text type="secondary" style={{ fontSize: '12px' }}>
            详情 &gt;
          </Text>
        </div>
      </div>
    </Card>
  );
};