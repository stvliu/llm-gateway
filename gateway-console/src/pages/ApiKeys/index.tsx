import { Tabs } from 'antd';
import { useTranslation } from 'react-i18next';
import UpstreamKeysTable from './UpstreamKeysTable';
import DownstreamKeysTable from './DownstreamKeysTable';

export default function ApiKeys() {
  const { t } = useTranslation('apiKeys');

  return (
    <Tabs
      defaultActiveKey="upstream"
      items={[
        {
          key: 'upstream',
          label: <span>🔼 {t('upstream', { defaultValue: '上游 Key（供应商凭证）' })}</span>,
          children: <UpstreamKeysTable />,
        },
        {
          key: 'downstream',
          label: <span>🔽 {t('downstream', { defaultValue: '下游 Key（用户密钥）' })}</span>,
          children: <DownstreamKeysTable />,
        },
      ]}
    />
  );
}