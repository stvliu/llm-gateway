import { Card, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { PageHeader } from '@/components/ui/PageHeader';

export default function Developer() {
  const { t } = useTranslation('developer');
  return (
    <div>
      <PageHeader
        title={t('title', { defaultValue: '开发者门户' })}
        subtitle={t('subtitle', { defaultValue: '浏览可用模型，创建 API Key 快速开始' })}
      />
      <Card>
        <Typography.Text type="secondary">
          {t('placeholder', { defaultValue: '开发者门户页面开发中...' })}
        </Typography.Text>
      </Card>
    </div>
  );
}