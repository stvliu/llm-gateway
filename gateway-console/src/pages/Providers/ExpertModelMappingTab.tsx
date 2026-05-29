import { Typography, Empty } from 'antd';
import { useTranslation } from 'react-i18next';
import type { Provider } from '@/types/provider';

const { Text } = Typography;

interface Props {
  provider: Provider | null;
}

export default function ExpertModelMappingTab({ provider }: Props) {
  const { t } = useTranslation('providers');
  if (!provider) return <Empty description={t('noProviderData', { defaultValue: '暂无供应商数据' })} />;

  return (
    <div>
      <Text strong style={{ fontSize: 16 }}>{t('modelMapping.title', { defaultValue: '模型映射' })}</Text>
      <div style={{ marginTop: 16, color: '#64748b', fontSize: 13 }}>
        {t('modelMapping.desc', { defaultValue: '配置模型别名映射和定价覆盖。' })}
      </div>
    </div>
  );
}