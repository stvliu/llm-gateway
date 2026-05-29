import { Card, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { PageHeader } from '@/components/ui/PageHeader';

export default function ApiKeys() {
  const { t } = useTranslation('apiKeys');
  return (
    <div>
      <PageHeader
        title={t('title', { defaultValue: 'API Key 管理' })}
        subtitle={t('subtitle', { defaultValue: '管理上游供应商凭证和下游用户密钥' })}
      />
      <Card>
        <Typography.Text type="secondary">
          {t('placeholder', { defaultValue: 'API Key 管理页面开发中...' })}
        </Typography.Text>
      </Card>
    </div>
  );
}