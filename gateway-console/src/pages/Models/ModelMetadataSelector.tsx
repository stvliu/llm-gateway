import { useMemo } from 'react';
import { Tag, Space, Typography, Tooltip, theme, Button } from 'antd';
import {
  MessageOutlined,
  ApiOutlined,
  PictureOutlined,
  AudioOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useModelMetadataByProvider } from '@/services/query/useMetadata';
import type { ModelMetadata } from '@/types/metadata';

const { Text } = Typography;
const { useToken } = theme;

interface ModelMetadataItem {
  id: string;
  displayName: string;
  contextWindow?: number;
  inputPrice?: number;
  outputPrice?: number;
  capabilities?: Record<string, boolean>;
}

interface ModelMetadataSelectorProps {
  providerId: string;
  onSelect: (model: ModelMetadataItem) => void;
  onCustomAdd?: () => void;
}

function inferModelType(capabilities?: Record<string, boolean>): string {
  if (!capabilities) return 'CHAT';
  if (capabilities.chat || capabilities.vision) return 'CHAT';
  if (capabilities.embedding) return 'EMBEDDING';
  if (capabilities.image) return 'IMAGE';
  if (capabilities.audio) return 'AUDIO';
  return 'CHAT';
}

function getModelTypeIcon(type: string) {
  const iconMap: Record<string, React.ReactNode> = {
    CHAT: <MessageOutlined />,
    EMBEDDING: <ApiOutlined />,
    IMAGE: <PictureOutlined />,
    AUDIO: <AudioOutlined />,
  };
  return iconMap[type] || <ApiOutlined />;
}

function getModelTypeColor(type: string): string {
  const colorMap: Record<string, string> = {
    CHAT: 'blue',
    EMBEDDING: 'purple',
    IMAGE: 'orange',
    AUDIO: 'green',
  };
  return colorMap[type] || 'default';
}

function formatContextWindow(contextWindow?: number): string {
  if (!contextWindow) return '';
  if (contextWindow >= 1000000) return `${(contextWindow / 1000000).toFixed(1)}M`;
  if (contextWindow >= 1000) return `${Math.round(contextWindow / 1000)}K`;
  return contextWindow.toString();
}

function isPopularModel(modelId: string): boolean {
  const popularPatterns = [
    /gpt-4o$/i, /gpt-4o-mini$/i, /claude-sonnet/i, /claude-3-5-haiku/i,
    /gemini-2/i, /gemini-1-5-pro/i, /deepseek-chat/i, /qwen-max/i, /glm-4/i,
  ];
  return popularPatterns.some(p => p.test(modelId));
}

/**
 * 模型元数据选择器
 * 从 ModelMetadata 获取模型列表，支持快速添加
 */
export function ModelMetadataSelector({
  providerId,
  onSelect,
  onCustomAdd,
}: ModelMetadataSelectorProps) {
  const { t } = useTranslation('models');
  const { token } = useToken();

  const { data: modelMetadataList, isLoading } = useModelMetadataByProvider(providerId);

  const modelItems = useMemo(() => {
    if (!modelMetadataList) return [];

    const items: ModelMetadataItem[] = modelMetadataList.map((m: ModelMetadata) => ({
      id: m.providerModelId,
      displayName: m.displayName,
      contextWindow: m.contextWindow,
      inputPrice: m.inputPrice,
      outputPrice: m.outputPrice,
      capabilities: m.capabilities,
    }));

    // 去重（按 id）
    const uniqueMap = new Map<string, ModelMetadataItem>();
    items.forEach(item => {
      if (!uniqueMap.has(item.id)) uniqueMap.set(item.id, item);
    });

    // 排序：热门模型优先
    return Array.from(uniqueMap.values()).sort((a, b) => {
      const aPopular = isPopularModel(a.id);
      const bPopular = isPopularModel(b.id);
      if (aPopular && !bPopular) return -1;
      if (!aPopular && bPopular) return 1;
      return a.displayName.localeCompare(b.displayName);
    });
  }, [modelMetadataList]);

  const popularModels = useMemo(
    () => modelItems.filter(m => isPopularModel(m.id)).slice(0, 6),
    [modelItems]
  );

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 24, color: token.colorTextSecondary }}>
        {t('template.loading', { defaultValue: '加载模型元数据...' })}
      </div>
    );
  }

  if (modelItems.length === 0) return null;

  return (
    <div style={{ marginBottom: 24 }}>
      {/* 快速添加区 */}
      <div style={{ marginBottom: 16 }}>
        <Text type="secondary" style={{ fontSize: 13 }}>
          {t('template.quickAdd', { defaultValue: '快速添加' })}
        </Text>
        <div style={{ marginTop: 8, display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {popularModels.map(template => {
            const modelType = inferModelType(template.capabilities);
            return (
              <Tooltip
                key={template.id}
                title={
                  <Space direction="vertical" size={2}>
                    <Text style={{ color: token.colorTextLightSolid }}>{template.id}</Text>
                    {template.contextWindow && (
                      <Text style={{ color: token.colorTextLightSolid, fontSize: 12 }}>
                        {t('detail.contextWindow')}: {formatContextWindow(template.contextWindow)}
                      </Text>
                    )}
                    {template.inputPrice !== undefined && (
                      <Text style={{ color: token.colorTextLightSolid, fontSize: 12 }}>
                        ${template.inputPrice}/{t('template.perMillion', { defaultValue: '百万 tokens' })}
                      </Text>
                    )}
                  </Space>
                }
              >
                <Tag
                  color={getModelTypeColor(modelType)}
                  icon={getModelTypeIcon(modelType)}
                  style={{ cursor: 'pointer', padding: '4px 12px', margin: 0, fontSize: 13 }}
                  onClick={() => onSelect(template)}
                >
                  {template.displayName}
                </Tag>
              </Tooltip>
            );
          })}
        </div>
      </div>

      {/* 分隔线 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16 }}>
        <div style={{ flex: 1, height: 1, background: token.colorBorder }} />
        <Text type="secondary" style={{ fontSize: 12 }}>
          {t('template.orSelect', { defaultValue: '或选择其他模型' })}
        </Text>
        <div style={{ flex: 1, height: 1, background: token.colorBorder }} />
      </div>

      {onCustomAdd && (
        <Button type="link" onClick={onCustomAdd} style={{ marginLeft: 8, fontSize: 13 }}>
          {t('template.customAdd', { defaultValue: '自定义添加' })}
        </Button>
      )}

      {/* 模型选择列表 */}
      <div>
        <Text type="secondary" style={{ fontSize: 13, marginBottom: 8, display: 'block' }}>
          {t('template.selectFromList', { defaultValue: '从列表选择' })}
        </Text>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, maxHeight: 200, overflowY: 'auto', padding: '8px 0' }}>
          {modelItems.map(template => {
            const modelType = inferModelType(template.capabilities);
            return (
              <Tag
                key={template.id}
                style={{ cursor: 'pointer', padding: '4px 10px', margin: 0, fontSize: 12, background: token.colorBgContainer, border: `1px solid ${token.colorBorder}` }}
                onClick={() => onSelect(template)}
              >
                <Space size={4}>
                  <span style={{ fontSize: 12 }}>{getModelTypeIcon(modelType)}</span>
                  <span>{template.displayName}</span>
                </Space>
              </Tag>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export type { ModelMetadataSelectorProps, ModelMetadataItem };