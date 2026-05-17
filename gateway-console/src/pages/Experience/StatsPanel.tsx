import { Space, Typography } from 'antd';
import { useTranslation } from 'react-i18next';

const { Text } = Typography;

interface StatsPanelProps {
  promptTokens: number;
  completionTokens: number;
}

/**
 * 统计面板组件
 *
 * 显示 Token 使用统计。
 */
export function StatsPanel({ promptTokens, completionTokens }: StatsPanelProps) {
  const { t } = useTranslation('experience');
  const total = promptTokens + completionTokens;

  return (
    <Space split={<Text type="secondary">|</Text>} size="small">
      <Text type="secondary">
        {t('stats.promptTokens')}: <Text strong>{promptTokens.toLocaleString()}</Text>
      </Text>
      <Text type="secondary">
        {t('stats.completionTokens')}: <Text strong>{completionTokens.toLocaleString()}</Text>
      </Text>
      <Text type="secondary">
        {t('stats.totalTokens')}: <Text strong>{total.toLocaleString()}</Text>
      </Text>
    </Space>
  );
}
