import { Space, Tag, Tooltip, Typography, theme } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined, MinusCircleOutlined } from '@ant-design/icons';
import type { ConnectivityTestResult } from '@/services/api/provider';

const { Text } = Typography;

interface ConnectivityTestResultProps {
  result: ConnectivityTestResult;
  compact?: boolean;
}

/**
 * 连通性测试结果展示组件
 *
 * 支持 compact 模式（行内）和详细模式
 */
export function ConnectivityTestResultDisplay({ result, compact = false }: ConnectivityTestResultProps) {
  const { token } = theme.useToken();

  const getStatusIcon = (success: boolean | undefined, loading?: boolean) => {
    if (loading) {
      return <LoadingOutlined style={{ color: token.colorPrimary }} />;
    }
    if (success === undefined) {
      return <MinusCircleOutlined style={{ color: token.colorTextDisabled }} />;
    }
    return success
      ? <CheckCircleOutlined style={{ color: token.colorSuccess }} />
      : <CloseCircleOutlined style={{ color: token.colorError }} />;
  };

  const getStatusColor = (success: boolean | undefined): 'secondary' | 'success' | 'danger' => {
    if (success === undefined) return 'secondary';
    return success ? 'success' : 'danger';
  };

  const formatLatency = (ms: number | undefined) => {
    if (ms === undefined) return '';
    return `${ms}ms`;
  };

  // 紧凑模式：只显示整体状态
  if (compact) {
    return (
      <Tooltip title={result.message}>
        <Space size={4}>
          {getStatusIcon(result.success)}
          <Text type={result.success ? 'success' : 'danger'} style={{ fontSize: 12 }}>
            {result.success ? '有效' : '无效'}
          </Text>
        </Space>
      </Tooltip>
    );
  }

  // 详细模式：显示分层结果
  const level1 = result.level1;
  const level2 = result.level2;

  return (
    <div style={{ fontSize: 12 }}>
      {/* 整体状态 */}
      <Space size={4} style={{ marginBottom: 4 }}>
        {getStatusIcon(result.success)}
        <Text strong>{result.success ? '验证成功' : '验证失败'}</Text>
        {result.totalLatencyMs && (
          <Text type="secondary">({result.totalLatencyMs}ms)</Text>
        )}
      </Space>

      {/* Level 1 结果 */}
      {level1 && (
        <div style={{ marginLeft: 16, marginBottom: 4 }}>
          <Space size={4}>
            {getStatusIcon(level1.success)}
            <Text type="secondary">
              认证:
              <Text
                type={getStatusColor(level1.success)}
                style={{ marginLeft: 4 }}
              >
                {level1.success ? '成功' : '失败'}
              </Text>
            </Text>
            {level1.latencyMs && (
              <Text type="secondary">{formatLatency(level1.latencyMs)}</Text>
            )}
            {level1.models && level1.models.length > 0 && (
              <Tag style={{ marginLeft: 4, fontSize: 11 }}>
                {level1.models.length} 个模型
              </Tag>
            )}
            {level1.errorType && !level1.success && (
              <Text type="danger">({level1.errorType})</Text>
            )}
          </Space>
        </div>
      )}

      {/* Level 2 结果 */}
      {level2 !== undefined && (
        <div style={{ marginLeft: 16 }}>
          <Space size={4}>
            {getStatusIcon(level2?.success, false)}
            <Text type="secondary">
              模型可用:
              <Text
                type={getStatusColor(level2?.success)}
                style={{ marginLeft: 4 }}
              >
                {level2?.success === undefined ? '跳过' : level2.success ? '成功' : '失败'}
              </Text>
            </Text>
            {level2?.latencyMs && (
              <Text type="secondary">{formatLatency(level2.latencyMs)}</Text>
            )}
            {level2?.errorType && !level2.success && (
              <Text type="danger">({level2.errorType})</Text>
            )}
          </Space>
        </div>
      )}

      {/* 错误消息 */}
      {result.message && !result.success && (
        <div style={{ marginTop: 8 }}>
          <Text type="danger">{result.message}</Text>
        </div>
      )}
    </div>
  );
}

export type { ConnectivityTestResultProps };
