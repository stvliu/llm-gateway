import { Typography, Empty } from 'antd';
import { useTranslation } from 'react-i18next';
import type { Provider } from '@/types/provider';

const { Text } = Typography;

interface Props {
  provider: Provider | null;
}

export default function ExpertEndpointTab({ provider }: Props) {
  const { t } = useTranslation('providers');
  if (!provider) return <Empty description={t('noProviderData', { defaultValue: '暂无供应商数据' })} />;

  return (
    <div>
      <Text strong style={{ fontSize: 16 }}>{t('endpoint.title', { defaultValue: '接入点管理' })}</Text>
      <div style={{ marginTop: 16, color: '#64748b', fontSize: 13 }}>
        {t('endpoint.desc', { defaultValue: '管理协议类型和 Base URL 等接入点信息。' })}
      </div>
    </div>
  );
}