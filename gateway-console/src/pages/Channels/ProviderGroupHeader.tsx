import { Typography, Space, Tooltip } from 'antd';
import { RightOutlined } from '@ant-design/icons';
import { ProviderIcon } from '@/components/ui/ProviderIcon';
import type { FC } from 'react';

const { Text } = Typography;

export interface ProviderGroupHeaderProps {
  /** 供应商 ID */
  providerId?: string;
  /** 供应商名称 */
  providerName: string;
  /** 渠道数量 */
  channelCount: number;
  /** 端点总数 */
  endpointCount: number;
  /** 凭证总数 */
  credentialCount: number;
  /** 模型总数 */
  modelCount: number;
  /** 是否折叠 */
  collapsed: boolean;
  /** 折叠/展开回调 */
  onToggle: () => void;
}

/**
 * 供应商分组头组件
 * 显示供应商 Logo + 名称 + 聚合统计 + 折叠箭头
 */
export const ProviderGroupHeader: FC<ProviderGroupHeaderProps> = ({
  providerId,
  providerName,
  channelCount,
  endpointCount,
  credentialCount,
  modelCount,
  collapsed,
  onToggle,
}) => {
  return (
    <div
      onClick={onToggle}
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '12px 16px',
        background: '#fafafa',
        borderRadius: '8px',
        cursor: 'pointer',
        marginBottom: '8px',
        transition: 'all 0.2s',
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.background = '#f0f0f0';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.background = '#fafafa';
      }}
    >
      {/* 左侧：供应商信息 */}
      <Space size={12}>
        <ProviderIcon providerId={providerId} size={32} />
        <div>
          <Text strong style={{ fontSize: '16px' }}>
            {providerName}
          </Text>
          <br />
          <Text type="secondary" style={{ fontSize: '12px' }}>
            {channelCount} 个渠道
          </Text>
        </div>
      </Space>

      {/* 右侧：统计信息 + 折叠箭头 */}
      <Space size={24}>
        <Space size={16}>
          <Tooltip title="端点数量">
            <Text type="secondary" style={{ fontSize: '13px' }}>
              端点: {endpointCount}
            </Text>
          </Tooltip>
          <Tooltip title="凭证数量">
            <Text type="secondary" style={{ fontSize: '13px' }}>
              Key: {credentialCount}
            </Text>
          </Tooltip>
          <Tooltip title="模型数量">
            <Text type="secondary" style={{ fontSize: '13px' }}>
              模型: {modelCount}
            </Text>
          </Tooltip>
        </Space>
        <RightOutlined
          rotate={collapsed ? 0 : 90}
          style={{ transition: 'transform 0.2s', color: '#999' }}
        />
      </Space>
    </div>
  );
};
