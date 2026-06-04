import { Button, Card, Col, Popconfirm, Row, Tag, Tooltip, Typography, theme } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  DeleteOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  QuestionCircleOutlined,
  RightOutlined,
} from '@ant-design/icons';
import type { ChannelCard as ChannelCardType } from '@/types/channel';
import type { FC } from 'react';

const { Text } = Typography;

export interface ChannelCardProps {
  channel: ChannelCardType;
  onClick: (channel: ChannelCardType) => void;
  onDelete: (id: number) => void;
  onToggleState: (id: number, enabled: boolean) => void;
}

/**
 * 渠道卡片组件
 * 一行一个，横向铺满，展示端点/Key/模型统计和用量
 */
export const ChannelCard: FC<ChannelCardProps> = ({ channel, onClick, onDelete, onToggleState }) => {
  const { token } = theme.useToken();
  const isActive = channel.state === 'ACTIVE';

  // 检查是否缺少关键配置（凭证为 0 表示配置中）
  const isIncomplete = channel.stats.credentialCount === 0;

  const { endpointCount, credentialCount, modelCount } = channel.stats;

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

  /** 统计数字颜色 */
  const statNumberColor = (type: 'endpoint' | 'credential' | 'model') => {
    if (!isActive) return token.colorTextDisabled;
    if (type === 'endpoint') return token.colorPrimary;
    if (type === 'credential') return credentialCount === 0 ? token.colorWarning : token.colorLink;
    return token.colorInfo;
  };

  /** 统计区域背景色 */
  const statBgColor = token.colorFillQuaternary;

  return (
    <Card
      hoverable
      onClick={() => onClick(channel)}
      style={{
        marginBottom: '8px',
        opacity: isActive ? 1 : 0.5,
        border: isIncomplete ? `2px solid ${token.colorWarning}` : undefined,
        transition: 'all 0.2s',
      }}
      styles={{ body: { padding: '16px' } }}
    >
      {/* 第一行：名称 + 状态 + 计费/优先级 + 操作 */}
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
              ⚠ 配置中
            </Tag>
          )}
          {!isActive && (
            <Tag color="default" style={{ marginLeft: '4px' }}>
              已停用
            </Tag>
          )}
        </div>

        {/* 状态标签 */}
        <div style={{ flex: '0 0 80px' }}>
          <Tag color={isActive ? 'success' : 'default'}>
            {isActive ? '运行中' : '已停用'}
          </Tag>
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

        {/* 右侧：响应时间 + 详情入口 + 启用/停用 + 删除 */}
        <div
          style={{
            flex: '0 0 200px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            gap: '4px',
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
          <Tooltip title="查看详情">
            <RightOutlined style={{ fontSize: 12, color: token.colorTextSecondary }} />
          </Tooltip>
          <Popconfirm
            title={isActive ? '确定停用此渠道吗？' : '确定启用此渠道吗？'}
            onConfirm={(e) => {
              e?.stopPropagation();
              onToggleState(channel.id, !isActive);
            }}
            onCancel={(e) => e?.stopPropagation()}
            okText="确定"
            cancelText="取消"
          >
            <Tooltip title={isActive ? '停用' : '启用'}>
              <Button
                type="text"
                size="small"
                icon={isActive ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                onClick={(e) => e.stopPropagation()}
              />
            </Tooltip>
          </Popconfirm>
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
            <Tooltip title="删除">
              <Button
                type="text"
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={(e) => e.stopPropagation()}
              />
            </Tooltip>
          </Popconfirm>
        </div>
      </div>

      {/* 第二行：端点/Key/模型三列统计 + 今日用量 */}
      <Row gutter={8} style={{ marginTop: 10, marginBottom: 4 }}>
        <Col span={6}>
          <div style={{ textAlign: 'center', padding: '6px', background: statBgColor, borderRadius: token.borderRadiusSM }}>
            <div style={{ fontSize: 16, fontWeight: 700, color: statNumberColor('endpoint') }}>
              {endpointCount}
            </div>
            <div style={{ fontSize: 10, color: token.colorTextSecondary }}>端点</div>
          </div>
        </Col>
        <Col span={6}>
          <div style={{ textAlign: 'center', padding: '6px', background: statBgColor, borderRadius: token.borderRadiusSM }}>
            <div style={{ fontSize: 16, fontWeight: 700, color: statNumberColor('credential') }}>
              {credentialCount}
            </div>
            <div style={{ fontSize: 10, color: token.colorTextSecondary }}>Key</div>
          </div>
        </Col>
        <Col span={6}>
          <div style={{ textAlign: 'center', padding: '6px', background: statBgColor, borderRadius: token.borderRadiusSM }}>
            <div style={{ fontSize: 16, fontWeight: 700, color: statNumberColor('model') }}>
              {modelCount}
            </div>
            <div style={{ fontSize: 10, color: token.colorTextSecondary }}>模型</div>
          </div>
        </Col>
        <Col span={6}>
          <div style={{ textAlign: 'center', padding: '6px', background: statBgColor, borderRadius: token.borderRadiusSM }}>
            <div style={{ fontSize: 14, fontWeight: 600, color: isActive ? token.colorPrimary : token.colorTextDisabled }}>
              --
            </div>
            <div style={{ fontSize: 10, color: token.colorTextSecondary }}>今日Token</div>
          </div>
        </Col>
      </Row>

      {/* 停用渠道显示最后活跃时间 */}
      {!isActive && channel.updatedAt && (
        <div style={{ marginTop: 4 }}>
          <Text type="secondary" style={{ fontSize: 11 }}>
            最后活跃: {new Date(channel.updatedAt).toLocaleDateString()}
          </Text>
        </div>
      )}
    </Card>
  );
};
