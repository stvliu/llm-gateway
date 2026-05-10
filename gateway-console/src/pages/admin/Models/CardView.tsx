import { useMemo } from 'react';
import { Spin, Button } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { EmptyState } from '@/components/common';
import { ProviderCard } from './ProviderCard';
import { useProviders, useModels } from '@/services/query';
import type { Provider } from '@/types/provider';
import type { Model } from '@/types/model';
import type { SearchFilters } from '@/components/common/SearchFilterBar';

interface CardViewProps {
  filters: SearchFilters;
  onAddProvider: () => void;
  onEditProvider: (provider: Provider) => void;
  onDeleteProvider: (provider: Provider) => void;
  onAddModel: (provider: Provider) => void;
  onEditModel: (model: Model) => void;
  onDeleteModel: (model: Model) => void;
  onViewProviderDetail: (provider: Provider) => void;
  onViewModelDetail: (model: Model) => void;
}

/**
 * 卡片视图容器
 * 展示 Provider 卡片列表，支持搜索筛选
 */
export function CardView({
  filters,
  onAddProvider,
  onEditProvider,
  onDeleteProvider,
  onAddModel,
  onEditModel,
  onDeleteModel,
  onViewProviderDetail,
  onViewModelDetail,
}: CardViewProps) {
  const { t } = useTranslation('models');

  const { data: providersData, isLoading: providersLoading } = useProviders({ size: 100 });
  const { data: modelsData, isLoading: modelsLoading } = useModels({ size: 100 });

  const providers = providersData?.items || [];
  const allModels = modelsData?.items || [];

  // 根据筛选条件过滤
  const filteredProviders = useMemo(() => {
    let result = providers;

    // 关键词搜索
    if (filters.keyword) {
      const keyword = filters.keyword.toLowerCase();
      result = result.filter(
        (p) =>
          p.providerName.toLowerCase().includes(keyword) ||
          p.providerType.toLowerCase().includes(keyword)
      );
    }

    // 类型筛选
    if (filters.providerType) {
      result = result.filter((p) => p.providerType === filters.providerType);
    }

    // 状态筛选
    if (filters.enabled) {
      result = result.filter((p) => p.state === 'ACTIVE');
    }

    return result;
  }, [providers, filters]);

  // 获取 Provider 对应的 Models
  const getProviderModels = (providerId: number): Model[] => {
    let result = allModels.filter((m) => m.providerId === providerId);

    // 关键词搜索 Model
    if (filters.keyword) {
      const keyword = filters.keyword.toLowerCase();
      result = result.filter(
        (m) =>
          (m.displayName?.toLowerCase().includes(keyword)) ||
          m.providerModelId?.toLowerCase().includes(keyword)
      );
    }

    // 模型类型筛选（基于 capabilities）
    if (filters.modelType) {
      result = result.filter((m) => m.capabilities?.[filters.modelType!.toLowerCase()]);
    }

    return result;
  };

  const isLoading = providersLoading || modelsLoading;

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  // 空状态：无 Provider
  if (providers.length === 0) {
    return (
      <EmptyState
        type="provider"
        action={
          <Button type="primary" icon={<PlusOutlined />} onClick={onAddProvider}>
            {t('addProvider')}
          </Button>
        }
      />
    );
  }

  // 空状态：搜索无结果
  if (filteredProviders.length === 0 && filters.keyword) {
    return <EmptyState type="search" />;
  }

  return (
    <div>
      {filteredProviders.map((provider) => (
        <ProviderCard
          key={provider.id}
          provider={provider}
          models={getProviderModels(provider.id)}
          onEdit={onEditProvider}
          onDelete={onDeleteProvider}
          onAddModel={onAddModel}
          onEditModel={onEditModel}
          onDeleteModel={onDeleteModel}
          onViewProviderDetail={onViewProviderDetail}
          onViewModelDetail={onViewModelDetail}
        />
      ))}
    </div>
  );
}