import { useState } from 'react';
import { Table, Checkbox, Tag, Typography, Input } from 'antd';
import { useTranslation } from 'react-i18next';
import { useModelMetadataByProvider } from '@/services/query/useMetadata';
import type { ModelMetadata } from '@/types/metadata';

const { Text } = Typography;

interface ModelSetupStepProps {
  providerId: string;
  selectedModels: string[];
  onSelectedModelsChange: (models: string[]) => void;
}

/**
 * 模型配置步骤
 * 从 ModelMetadata 获取某供应商的模型列表，用户勾选需要的模型
 */
export function ModelSetupStep({
  providerId,
  selectedModels,
  onSelectedModelsChange,
}: ModelSetupStepProps) {
  const { t } = useTranslation('providers');
  const [searchText, setSearchText] = useState('');

  const { data: models, isLoading } = useModelMetadataByProvider(providerId);

  const filteredModels = (models ?? []).filter((m: ModelMetadata) =>
    !searchText ||
    m.displayName?.toLowerCase().includes(searchText.toLowerCase()) ||
    m.providerModelId?.toLowerCase().includes(searchText.toLowerCase())
  );

  const handleSelectAll = (checked: boolean) => {
    onSelectedModelsChange(checked ? filteredModels.map((m: ModelMetadata) => m.providerModelId) : []);
  };

  const handleSelectOne = (modelId: string, checked: boolean) => {
    if (checked) {
      onSelectedModelsChange([...selectedModels, modelId]);
    } else {
      onSelectedModelsChange(selectedModels.filter(id => id !== modelId));
    }
  };

  const formatPrice = (price?: number) => {
    if (price == null) return '-';
    return `$${price}`;
  };

  const formatContext = (ctx?: number) => {
    if (ctx == null) return '-';
    if (ctx >= 1_000_000) return `${(ctx / 1_000_000).toFixed(1)}M`;
    if (ctx >= 1_000) return `${(ctx / 1_000).toFixed(0)}K`;
    return `${ctx}`;
  };

  const columns = [
    {
      title: (
        <Checkbox
          checked={filteredModels.length > 0 && selectedModels.length === filteredModels.length}
          indeterminate={selectedModels.length > 0 && selectedModels.length < filteredModels.length}
          onChange={e => handleSelectAll(e.target.checked)}
        />
      ),
      dataIndex: 'providerModelId',
      width: 48,
      render: (modelId: string) => (
        <Checkbox
          checked={selectedModels.includes(modelId)}
          onChange={e => handleSelectOne(modelId, e.target.checked)}
        />
      ),
    },
    {
      title: t('template.modelName', { defaultValue: '模型名称' }),
      dataIndex: 'displayName',
      render: (name: string, record: ModelMetadata) => (
        <div>
          <Text strong>{name}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 11 }}>{record.providerModelId}</Text>
        </div>
      ),
    },
    {
      title: t('template.contextWindow', { defaultValue: '上下文窗口' }),
      dataIndex: 'contextWindow',
      width: 100,
      render: (ctx: number) => <Text>{formatContext(ctx)}</Text>,
    },
    {
      title: t('template.inputPrice', { defaultValue: '输入价格' }),
      dataIndex: 'inputPrice',
      width: 90,
      render: (price: number) => <Text>{formatPrice(price)}</Text>,
    },
    {
      title: t('template.outputPrice', { defaultValue: '输出价格' }),
      dataIndex: 'outputPrice',
      width: 90,
      render: (price: number) => <Text>{formatPrice(price)}</Text>,
    },
    {
      title: t('template.capabilities', { defaultValue: '能力' }),
      dataIndex: 'capabilities',
      width: 160,
      render: (caps: Record<string, boolean>) => {
        if (!caps) return '-';
        const tags: { label: string; color: string }[] = [];
        if (caps.vision) tags.push({ label: 'Vision', color: 'blue' });
        if (caps.function_calling) tags.push({ label: 'FC', color: 'green' });
        if (caps.streaming) tags.push({ label: 'Stream', color: 'orange' });
        return tags.map(tag => <Tag key={tag.label} color={tag.color} style={{ fontSize: 10, margin: 1 }}>{tag.label}</Tag>);
      },
    },
    {
      title: t('template.source', { defaultValue: '来源' }),
      dataIndex: 'source',
      width: 80,
      render: (source: string) => {
        const colorMap: Record<string, string> = {
          BUILTIN: 'default',
          MODELS_DEV: 'processing',
          MANUAL: 'warning',
          OVERRIDE: 'error',
        };
        return <Tag color={colorMap[source] || 'default'} style={{ fontSize: 10 }}>{source}</Tag>;
      },
    },
  ];

  return (
    <div>
      <Input.Search
        placeholder={t('template.searchModel', { defaultValue: '搜索模型...' })}
        value={searchText}
        onChange={e => setSearchText(e.target.value)}
        style={{ marginBottom: 12 }}
        allowClear
      />

      <Table
        dataSource={filteredModels}
        columns={columns}
        rowKey="providerModelId"
        size="small"
        loading={isLoading}
        pagination={false}
        scroll={{ y: 400 }}
      />

      <div style={{ marginTop: 8 }}>
        <Text type="secondary">
          {t('template.selectedCount', { defaultValue: '已选择' })} {selectedModels.length} / {filteredModels.length} {t('template.models', { defaultValue: '个模型' })}
        </Text>
      </div>
    </div>
  );
}