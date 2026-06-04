import { Button, Card, Popconfirm, Tag, Typography, Tooltip, theme } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  DeleteOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons';
import type { ChannelCard as ChannelCardType } from '@/types/channel';
import type { FC } from 'react';

const { Text } = Typography;

export interface ChannelCardProps {
  channel: ChannelCardType;
  onClick: (channel: ChannelCardType) => void;
  onDelete: (id: number) => void;
}

/**
 * 渠道卡片组件
 * 一行一个，横向铺满
 */
export const ChannelCard: FC<ChannelCardProps> = ({ channel, onClick, onDelete }) => {
  const { token } = theme.useToken();
  const isActive = channel.state === 'ACTIVE';

  // 检查是否缺少关键配置（凭证为 0 表示配置中）
  const isIncomplete = channel.stats.credentialCount === 0;

  /**
   * 根据响应时间返回颜色 token
   */
  const getResponseTimeColor = (responseTime: number | null | undefined): string => {
    if (responseTime === null || responseTime === undefined) return token.colorTextSecondary;
    if (responseTime <= 500) return token.colorSuccess;
    if (responseTime <= 2000) return token.colorWarning;
    return token.colorError;
  };

  /**
   * 根据响应时间返回文本
   */
  const getResponseTimeText = (responseTime: number | null | undefined): string => {
    if (responseTime === null || responseTime === undefined) return '未测试';
    if (responseTime < 1000) return `${responseTime}ms`;
    return `${(responseTime / 1000).toFixed(1)}s`;
  };

  return (
    <Card
      hoverable
      onClick={() => onClick(channel)}
      style={{
        marginBottom: '8px',
        opacity: isActive ? 1 : 0.6,
        border: isIncomplete ? `2px solid ${token.colorWarning}` : undefined,
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
              backgroundColor: isActive ? token.colorSuccess : token.colorBorder,
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
            {channel.billingMode === 'pay_as_you_go'
              ? '按量付费'
              : channel.billingMode === 'subscription'
              ? '订阅'
              : channel.billingMode}
          </Text>
        </div>

        {/* 右侧：响应时间 + 详情入口 + 删除 */}
        <div
          style={{
            flex: '0 0 160px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            gap: '8px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            {channel.stats.avgResponseTime === null ||
            channel.stats.avgResponseTime === undefined ? (
              <QuestionCircleOutlined style={{ color: token.colorTextSecondary }} />
            ) : channel.stats.avgResponseTime <= 500 ? (
              <CheckCircleOutlined style={{ color: token.colorSuccess }} />
            ) : channel.stats.avgResponseTime <= 2000 ? (
              <ClockCircleOutlined style={{ color: token.colorWarning }} />
            ) : (
              <CloseCircleOutlined style={{ color: token.colorError }} />
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
          <Popconfirm
            title="确认删除"
            description={`确定要删除渠道「${channel.name}」吗？此操作不可撤销。`}
            onConfirm={(e) => {
              e?.stopPropagation();
              onDelete(channel.id);
            }}
            onCancel={(e) => e?.stopPropagation()}
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
          >
            <Button
              type="text"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={(e) => e.stopPropagation()}
            />
          </Popconfirm>
        </div>
      </div>
    </Card>
  );
};