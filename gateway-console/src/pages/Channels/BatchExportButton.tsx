import { Button, App } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviders } from '@/services/query';

/**
 * 批量导出配置按钮组件
 *
 * <p>导出所有供应商配置为 YAML 格式（不包含 API Key 明文）。</p>
 */
export default function BatchExportButton() {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();
  const { data: providersData } = useProviders();

  const handleExport = () => {
    const providers = providersData?.items ?? [];
    if (!providers || providers.length === 0) {
      message.warning(t('batch.noData', { defaultValue: '暂无可导出的配置' }));
      return;
    }

    const exportData = {
      version: '1.0',
      providers: providers.map((p) => ({
        name: p.providerName,
        code: p.providerId || p.providerName.toLowerCase(),
        websiteUrl: p.websiteUrl || undefined,
        apiDocUrl: p.apiDocUrl || undefined,
        state: p.state || undefined,
        description: p.description || undefined,
      })),
    };

    const yaml = `version: "${exportData.version}"\nproviders:\n${exportData.providers
      .map((p) => {
        const fields = [`  - name: "${p.name}"`, `    code: "${p.code}"`];
        if (p.websiteUrl) fields.push(`    websiteUrl: "${p.websiteUrl}"`);
        if (p.apiDocUrl) fields.push(`    apiDocUrl: "${p.apiDocUrl}"`);
        if (p.state) fields.push(`    state: ${p.state}`);
        if (p.description) fields.push(`    description: "${p.description}"`);
        return fields.join('\n');
      })
      .join('\n')}`;

    const blob = new Blob([yaml], { type: 'text/yaml' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'gateway-config.yaml';
    a.click();
    URL.revokeObjectURL(url);
    message.success(t('batch.exportSuccess', { defaultValue: '导出成功（不含 API Key 明文）' }));
  };

  return (
    <Button icon={<DownloadOutlined />} onClick={handleExport}>
      {t('batch.export', { defaultValue: '导出全部配置' })}
    </Button>
  );
}
