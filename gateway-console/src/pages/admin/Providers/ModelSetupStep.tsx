import { useState, useMemo, useCallback, useEffect } from 'react';
import { Card, Button, Space, Tag, Checkbox, Empty, Spin, Input, Typography } from 'antd';
import { AppstoreOutlined, CheckOutlined, SearchOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useTemplates } from '@/services/query';
import { useThemeStore } from '@/stores/themeStore';
import type { NestedModelRequest } from '@/types/provider';
import type { ModelConfig, ProviderTemplate } from '@/types/template';

const { Text } = Typography;

interface ModelSetupStepProps {
  providerType: string;
  selectedModels: NestedModelRequest[];
  onChange: (models: NestedModelRequest[]) => void;
  selectedTemplate?: ProviderTemplate | null;
}

/**
 * 根据能力推断模型类型
 */
function inferModelType(capabilities?: Record<string, boolean>): string {
  if (!capabilities) return 'CHAT';
  if (capabilities.chat || capabilities.vision) return 'CHAT';
  if (capabilities.embedding) return 'EMBEDDING';
  if (capabilities.image) return 'IMAGE';
  if (capabilities.audio) return 'AUDIO';
  return 'CHAT';
}

/**
 * 获取模型类型颜色
 */
function getModelTypeColor(type: string): string {
  const colorMap: Record<string, string> = {
    CHAT: 'blue',
    EMBEDDING: 'purple',
    IMAGE: 'orange',
    AUDIO: 'green',
  };
  return colorMap[type] || 'default';
}

/**
 * 格式化上下文窗口
 */
function formatContextWindow(contextWindow?: number): string {
  if (!contextWindow) return '';
  if (contextWindow >= 1000000) return `${(contextWindow / 1000000).toFixed(1)}M`;
  if (contextWindow >= 1000) return `${Math.round(contextWindow / 1000)}K`;
  return contextWindow.toString();
}

/**
 * 判断是否为热门模型
 */
function isPopularModel(modelId: string): boolean {
  const popularPatterns = [
    /gpt-4o$/i,
    /gpt-4o-mini$/i,
    /claude-sonnet/i,
    /claude-3-5-haiku/i,
    /gemini-2/i,
    /gemini-1-5-pro/i,
    /deepseek-chat/i,
    /qwen-max/i,
    /glm-4/i,
  ];
  return popularPatterns.some(p => p.test(modelId));
}

/**
 * 模型配置步骤组件
 * 用于创建向导中配置模型，支持从模板预选和批量勾选
 */
export function ModelSetupStep({
  providerType,
  selectedModels,
  onChange,
  selectedTemplate,
}: ModelSetupStepProps) {
  const { t } = useTranslation('providers');
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  const [searchText, setSearchText] = useState('');

  // 查询模板
  const { data: templatesData, isLoading } = useTemplates({
    providerType,
    type: 'OFFICIAL',
    limit: 100,
  });

  // 提取模型配置
  const availableModels = useMemo(() => {
    if (!templatesData?.items) return [];

    const models = templatesData.items.flatMap(item =>
      (item.modelsConfig || []).map((m: ModelConfig) => ({
        id: m.provider_model_id,
        displayName: m.display_name,
        contextWindow: m.context_window,
        inputPrice: m.input_price,
        outputPrice: m.output_price,
        capabilities: m.capabilities,
      }))
    );

    // 去重
    const uniqueMap = new Map<string, typeof models[0]>();
    models.forEach(m => {
      if (!uniqueMap.has(m.id)) {
        uniqueMap.set(m.id, m);
      }
    });

    // 排序：热门优先
    return Array.from(uniqueMap.values()).sort((a, b) => {
      const aPopular = isPopularModel(a.id);
      const bPopular = isPopularModel(b.id);
      if (aPopular && !bPopular) return -1;
      if (!aPopular && bPopular) return 1;
      return a.displayName.localeCompare(b.displayName);
    });
  }, [templatesData]);

  // 从模板预选模型
  useEffect(() => {
    if (selectedTemplate?.modelsConfig && selectedModels.length === 0) {
      const models: NestedModelRequest[] = selectedTemplate.modelsConfig.map(m => ({
        providerModelId: m.provider_model_id,
        displayName: m.display_name,
        contextWindow: m.context_window,
        inputPrice: m.input_price,
        outputPrice: m.output_price,
        capabilities: m.capabilities,
      }));
      onChange(models);
    }
  }, [selectedTemplate, selectedModels.length, onChange]);

  // 过滤模型
  const filteredModels = useMemo(() => {
    if (!searchText) return availableModels;
    const lower = searchText.toLowerCase();
    return availableModels.filter(m =>
      m.id.toLowerCase().includes(lower) ||
      m.displayName.toLowerCase().includes(lower)
    );
  }, [availableModels, searchText]);

  // 热门模型
  const popularModels = useMemo(
    () => availableModels.filter(m => isPopularModel(m.id)).slice(0, 6),
    [availableModels]
  );

  // 检查是否选中
  const isSelected = useCallback((modelId: string) => {
    return selectedModels.some(m => m.providerModelId === modelId);
  }, [selectedModels]);

  // 切换选中
  const handleToggle = useCallback((model: typeof availableModels[0]) => {
    if (isSelected(model.id)) {
      onChange(selectedModels.filter(m => m.providerModelId !== model.id));
    } else {
      onChange([
        ...selectedModels,
        {
          providerModelId: model.id,
          displayName: model.displayName,
          contextWindow: model.contextWindow,
          inputPrice: model.inputPrice,
          outputPrice: model.outputPrice,
          capabilities: model.capabilities,
        },
      ]);
    }
  }, [selectedModels, isSelected, onChange]);

  // 全选
  const handleSelectAll = useCallback(() => {
    onChange(filteredModels.map(m => ({
      providerModelId: m.id,
      displayName: m.displayName,
      contextWindow: m.contextWindow,
      inputPrice: m.inputPrice,
      outputPrice: m.outputPrice,
      capabilities: m.capabilities,
    })));
  }, [filteredModels, onChange]);

  // 清空
  const handleSelectNone = useCallback(() => {
    onChange([]);
  }, [onChange]);

  // 选择热门
  const handleSelectPopular = useCallback(() => {
    onChange(popularModels.map(m => ({
      providerModelId: m.id,
      displayName: m.displayName,
      contextWindow: m.contextWindow,
      inputPrice: m.inputPrice,
      outputPrice: m.outputPrice,
      capabilities: m.capabilities,
    })));
  }, [popularModels, onChange]);

  // 加载状态
  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 40 }}>
        <Spin />
      </div>
    );
  }

  // 空状态
  if (availableModels.length === 0) {
    return (
      <Empty
        description={t('template.noModels', { defaultValue: '该供应商暂无预配置模型' })}
      />
    );
  }

  return (
    <div>
      {/* 头部 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: 16,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <AppstoreOutlined />
          <span style={{ fontWeight: 600, fontSize: 15 }}>
            {t('provider.models')}
          </span>
          <Tag color="blue">{selectedModels.length} / {availableModels.length}</Tag>
        </div>
        <Space>
          <Button size="small" onClick={handleSelectAll}>
            {t('template.selectAll', { defaultValue: '全选' })}
          </Button>
          <Button size="small" onClick={handleSelectPopular}>
            {t('template.selectPopular', { defaultValue: '选择热门' })}
          </Button>
          <Button size="small" onClick={handleSelectNone}>
            {t('template.selectNone', { defaultValue: '清空' })}
          </Button>
        </Space>
      </div>

      {/* 搜索框 */}
      <Input
        placeholder={t('template.searchModel', { defaultValue: '搜索模型' })}
        prefix={<SearchOutlined />}
        value={searchText}
        onChange={(e) => setSearchText(e.target.value)}
        style={{ marginBottom: 16 }}
        allowClear
      />

      {/* 热门模型快捷选择 */}
      {popularModels.length > 0 && !searchText && (
        <div style={{ marginBottom: 16 }}>
          <Text type="secondary" style={{ fontSize: 13, display: 'block', marginBottom: 8 }}>
            {t('template.quickAdd', { defaultValue: '快速添加' })}
          </Text>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {popularModels.map(model => (
              <Tag
                key={model.id}
                color={isSelected(model.id) ? getModelTypeColor(inferModelType(model.capabilities)) : 'default'}
                style={{ cursor: 'pointer', padding: '4px 12px' }}
                onClick={() => handleToggle(model)}
              >
                {isSelected(model.id) && <CheckOutlined style={{ marginRight: 4 }} />}
                {model.displayName}
              </Tag>
            ))}
          </div>
        </div>
      )}

      {/* 模型列表 */}
      <Card
        size="small"
        style={{
          background: isDark ? '#1f1f1f' : '#fafafa',
          maxHeight: 300,
          overflow: 'auto',
        }}
        styles={{ body: { padding: 8 } }}
      >
        {filteredModels.map(model => {
          const modelType = inferModelType(model.capabilities);
          const selected = isSelected(model.id);

          return (
            <div
              key={model.id}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '8px 12px',
                borderRadius: 6,
                marginBottom: 4,
                background: selected ? (isDark ? '#2a2a2a' : '#e6f7ff') : 'transparent',
                cursor: 'pointer',
              }}
              onClick={() => handleToggle(model)}
            >
              <Space>
                <Checkbox checked={selected} />
                <Tag color={getModelTypeColor(modelType)} style={{ margin: 0 }}>
                  {model.displayName}
                </Tag>
                {model.contextWindow && (
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {formatContextWindow(model.contextWindow)}
                  </Text>
                )}
              </Space>
              {model.inputPrice !== undefined && (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  ${model.inputPrice}/{model.outputPrice || 0}/M
                </Text>
              )}
            </div>
          );
        })}
      </Card>

      {/* 已选模型数量提示 */}
      {selectedModels.length > 0 && (
        <div style={{ marginTop: 12 }}>
          <Tag color="green">
            {t('template.selectedModels', {
              defaultValue: '已选择 {{count}} 个模型',
              count: selectedModels.length,
            })}
          </Tag>
        </div>
      )}
    </div>
  );
}

export type { ModelSetupStepProps };