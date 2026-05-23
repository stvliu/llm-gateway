import { useState, useEffect } from 'react';
import { Button, Tag, Tooltip } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useTestProductApiKey } from '@/services/query/useProducts';

interface Props {
  productId: number;
  keyId: number;
}

export default function ProductApiKeyTestButton({ productId, keyId }: Props) {
  const { t } = useTranslation('products');
  const [result, setResult] = useState<{ success: boolean; latency?: number; error?: string } | null>(null);
  const testMutation = useTestProductApiKey();

  // 结果 3 秒后自动消失
  useEffect(() => {
    if (result) {
      const timer = setTimeout(() => setResult(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [result]);

  const handleTest = async () => {
    try {
      const response = await testMutation.mutateAsync({ productId, id: keyId });
      setResult({
        success: response.success,
        latency: response.latency ?? undefined,
        error: response.error?.message,
      });
    } catch {
      setResult({ success: false, error: t('product.testFailed') });
    }
  };

  if (result) {
    return result.success ? (
      <Tooltip title={`${t('product.latency')}: ${result.latency}ms`}>
        <Tag color="success" icon={<CheckCircleOutlined />}>
          {t('product.testSuccess')}
        </Tag>
      </Tooltip>
    ) : (
      <Tooltip title={result.error}>
        <Tag color="error" icon={<CloseCircleOutlined />}>
          {t('product.testFailed')}
        </Tag>
      </Tooltip>
    );
  }

  return (
    <Button
      type="text"
      size="small"
      icon={testMutation.isPending ? <LoadingOutlined /> : undefined}
      onClick={handleTest}
      loading={testMutation.isPending}
    >
      {t('product.testKey')}
    </Button>
  );
}
