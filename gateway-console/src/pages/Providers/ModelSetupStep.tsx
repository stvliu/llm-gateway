import { useState } from 'react';
import { Table, Checkbox, Typography, Input } from 'antd';
import { useTranslation } from 'react-i18next';
import { useModelCatalogs } from '@/services/query/useCatalog';
import type { ModelCatalog } from '@/types/catalog';

const { Text } = Typography;

interface ModelSetupStepProps {
  providerCode: string;
  selectedModels: string[];
  onSelectedModelsChange: (models: string[]) => void;
}

/** 格式化上下文窗口大小 */
function formatContext(tokens: number | undefined): string {
  if (!tokens) return '-';
  if (tokens >= 1_000_000) return `${(tokens / 1_000_000).toFixed(1)}M`;
  if (tokens >= 1_000) return `${(tokens / 1_000).toFixed(0)}K`;
  return `${tokens}`;
}

/**
 * 模型配置步骤
 * 从 ModelCatalog 获取某供应商的模型列表，用户勾选需要的模型
 */
export function ModelSetupStep({
  providerCode,
  selectedModels,
  onSelectedModelsChange,
}: ModelSetupStepProps) {
  const { t } = useTranslation('providers');
  const [searchText, setSearchText] = useState('');

  const { data: models, isLoading } = useModelCatalogs({ keyword: providerCode });

  const filteredModels = (models ?? []).filter((m: ModelCatalog) =>
    m.providerCode === providerCode &&
    (!searchText ||
    m.modelName?.toLowerCase().includes(searchText.toLowerCase()))
  );

  const columns = [
    {
      title: t('model.modelName', { defaultValue: '模型名称' }),
      dataIndex: 'modelName',
      key: 'modelName',
      render: (name: string) => (
        <div>
          <Text strong>{name}</Text>
        </div>
      ),
    },
    {
      title: t('model.contextWindow', { defaultValue: '上下文窗口' }),
      dataIndex: 'maxContextTokens',
      key: 'maxContextTokens',
      width: 100,
      render: (ctx: number) => <Text>{formatContext(ctx)}</Text>,
    },
  ];

  return (
    <div>
      <Input.Search
        placeholder={t('template.searchModel', { defaultValue: '搜索模型' })}
        allowClear
        onChange={(e) => setSearchText(e.target.value)}
        style={{ marginBottom: 12 }}
      />

      <div style={{ marginBottom: 8 }}>
        <Checkbox
          checked={filteredModels.length > 0 && selectedModels.length === filteredModels.length}
          indeterminate={selectedModels.length > 0 && selectedModels.length < filteredModels.length}
          onChange={(e) => {
            if (e.target.checked) {
              onSelectedModelsChange(filteredModels.map((m: ModelCatalog) => m.modelName));
            } else {
              onSelectedModelsChange([]);
            }
          }}
        >
          <Text type="secondary">
            {t('template.selectAll', { defaultValue: '全选' })} ({selectedModels.length}/{filteredModels.length})
          </Text>
        </Checkbox>
      </div>

      <Table
        dataSource={filteredModels}
        columns={columns}
        rowKey="modelName"
        size="small"
        loading={isLoading}
        pagination={false}
        scroll={{ y: 300 }}
        rowSelection={{
          selectedRowKeys: selectedModels,
          onChange: (keys) => onSelectedModelsChange(keys as string[]),
        }}
      />
    </div>
  );
}

export type { ModelSetupStepProps };