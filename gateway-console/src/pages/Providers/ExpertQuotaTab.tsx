import { Typography, Empty } from 'antd';
import { useTranslation } from 'react-i18next';
import type { Provider } from '@/types/provider';

const { Text } = Typography;

interface Props {
  provider: Provider | null;
}

export default function ExpertQuotaTab({ provider }: Props) {
  const { t } = useTranslation('providers');
  if (!provider) return <Empty description={t('noProviderData', { defaultValue: '暂无供应商数据' })} />;

  return (
    <div>
      <Text strong style={{ fontSize: 16 }}>{t('quota.title', { defaultValue: '限流与配额' })}</Text>
      <div style={{ marginTop: 16, color: '#64748b', fontSize: 13 }}>
        {t('quota.desc', { defaultValue: '配置 RPM/TPM 限流和 Token 配额。' })}
      </div>
    </div>
  );
}