import { Typography, Empty } from 'antd';
import { useTranslation } from 'react-i18next';
import type { Provider } from '@/types/provider';

const { Text } = Typography;

interface Props {
  provider: Provider | null;
}

export default function ExpertCredentialTab({ provider }: Props) {
  const { t } = useTranslation('providers');
  if (!provider) return <Empty description={t('noProviderData', { defaultValue: '暂无供应商数据' })} />;

  return (
    <div>
      <Text strong style={{ fontSize: 16 }}>{t('credential.title', { defaultValue: 'API Key 管理' })}</Text>
      <div style={{ marginTop: 16, color: '#64748b', fontSize: 13 }}>
        {t('credential.desc', { defaultValue: '管理供应商 API Key，支持多 Key 负载均衡和轮换操作。' })}
      </div>
    </div>
  );
}