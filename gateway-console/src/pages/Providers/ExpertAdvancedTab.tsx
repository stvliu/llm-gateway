import { Typography, Empty } from 'antd';
import { useTranslation } from 'react-i18next';
import type { Provider } from '@/types/provider';

const { Text } = Typography;

interface Props {
  provider: Provider | null;
}

export default function ExpertAdvancedTab({ provider }: Props) {
  const { t } = useTranslation('providers');
  if (!provider) return <Empty description={t('noProviderData', { defaultValue: '暂无供应商数据' })} />;

  return (
    <div>
      <Text strong style={{ fontSize: 16 }}>{t('advanced.title', { defaultValue: '高级设置' })}</Text>
      <div style={{ marginTop: 16, color: '#64748b', fontSize: 13 }}>
        {t('advanced.desc', { defaultValue: '配置超时、重试策略、断路器和自定义 Header。' })}
      </div>
    </div>
  );
}