import { Card, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { PageHeader } from '@/components/ui/PageHeader';

export default function Models() {
  const { t } = useTranslation('models');
  return (
    <div>
      <PageHeader
        title={t('title', { defaultValue: '模型目录' })}
        subtitle={t('subtitle', { defaultValue: '浏览和管理全局模型' })}
      />
      <Card>
        <Typography.Text type="secondary">
          {t('placeholder', { defaultValue: '模型目录页面开发中...' })}
        </Typography.Text>
      </Card>
    </div>
  );
}