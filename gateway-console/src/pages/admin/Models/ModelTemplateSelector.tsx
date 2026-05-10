import { useMemo } from 'react';
import { Tag, Space, Typography, Tooltip, theme } from 'antd';
import {
  MessageOutlined,
  ApiOutlined,
  PictureOutlined,
  AudioOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useTemplates } from '@/services/query';
import type { ModelConfig } from '@/types/template';

const { Text } = Typography;
const { useToken } = theme;

interface ModelTemplate {
  id: string;
  displayName: string;
  contextWindow?: number;
  inputPrice?: number;
  outputPrice?: number;
  capabilities?: Record<string, boolean>;
}

interface ModelTemplateSelectorProps {
  providerType: string;
  onSelect: (template: ModelTemplate) => void;
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
 * 获取模型类型图标
 */
function getModelTypeIcon(type: string) {
  const iconMap: Record<string, React.ReactNode> = {
    CHAT: <MessageOutlined />,
    EMBEDDING: <ApiOutlined />,
    IMAGE: <PictureOutlined />,
    AUDIO: <AudioOutlined />,
  };
  return iconMap[type] || <ApiOutlined />;
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
  if (contextWindow >= 1000000) {
    return `${(contextWindow / 1000000).toFixed(1)}M`;
  }
  if (contextWindow >= 1000) {
    return `${Math.round(contextWindow / 1000)}K`;
  }
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
 * 模型模板选择器
 * 展示 Provider 对应的模型模板，支持快速添加
 */
export function ModelTemplateSelector({
  providerType,
  onSelect,
}: ModelTemplateSelectorProps) {
  const { t } = useTranslation('models');
  const { token } = useToken();

  const { data: templatesData, isLoading } = useTemplates({
    providerType,
    type: 'OFFICIAL',
    limit: 100,
  });

  // 提取并转换模型配置
  const modelTemplates = useMemo(() => {
    if (!templatesData?.items) return [];

    const templates = templatesData.items.flatMap(item =>
      (item.modelsConfig || []).map((m: ModelConfig) => ({
        id: m.provider_model_id,
        displayName: m.display_name,
        contextWindow: m.context_window,
        inputPrice: m.input_price,
        outputPrice: m.output_price,
        capabilities: m.capabilities,
      }))
    );

    // 去重（按 id）
    const uniqueMap = new Map<string, ModelTemplate>();
    templates.forEach(t => {
      if (!uniqueMap.has(t.id)) {
        uniqueMap.set(t.id, t);
      }
    });

    // 排序：热门模型优先
    return Array.from(uniqueMap.values()).sort((a, b) => {
      const aPopular = isPopularModel(a.id);
      const bPopular = isPopularModel(b.id);
      if (aPopular && !bPopular) return -1;
      if (!aPopular && bPopular) return 1;
      return a.displayName.localeCompare(b.displayName);
    });
  }, [templatesData]);

  // 热门模型（用于快速添加区）
  const popularModels = useMemo(
    () => modelTemplates.filter(m => isPopularModel(m.id)).slice(0, 6),
    [modelTemplates]
  );

  // 加载状态
  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 24, color: '#999' }}>
        {t('template.loading', { defaultValue: '加载模型模板...' })}
      </div>
    );
  }

  if (modelTemplates.length === 0) {
    return null;
  }

  return (
    <div style={{ marginBottom: 24 }}>
      {/* 快速添加区 */}
      <div style={{ marginBottom: 16 }}>
        <Text type="secondary" style={{ fontSize: 13 }}>
          {t('template.quickAdd', { defaultValue: '快速添加' })}
        </Text>
        <div
          style={{
            marginTop: 8,
            display: 'flex',
            flexWrap: 'wrap',
            gap: 8,
          }}
        >
          {popularModels.map(template => {
            const modelType = inferModelType(template.capabilities);
            return (
              <Tooltip
                key={template.id}
                title={
                  <Space direction="vertical" size={2}>
                    <Text style={{ color: '#fff' }}>{template.id}</Text>
                    {template.contextWindow && (
                      <Text style={{ color: '#fff', fontSize: 12 }}>
                        {t('detail.contextWindow')}: {formatContextWindow(template.contextWindow)}
                      </Text>
                    )}
                    {template.inputPrice !== undefined && (
                      <Text style={{ color: '#fff', fontSize: 12 }}>
                        ${template.inputPrice}/${t('template.perMillion', { defaultValue: '百万 tokens' })}
                      </Text>
                    )}
                  </Space>
                }
              >
                <Tag
                  color={getModelTypeColor(modelType)}
                  icon={getModelTypeIcon(modelType)}
                  style={{
                    cursor: 'pointer',
                    padding: '4px 12px',
                    margin: 0,
                    fontSize: 13,
                  }}
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
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 16,
          marginBottom: 16,
        }}
      >
        <div
          style={{
            flex: 1,
            height: 1,
            background: token.colorBorder,
          }}
        />
        <Text type="secondary" style={{ fontSize: 12 }}>
          {t('template.orSelect', { defaultValue: '或选择其他模型' })}
        </Text>
        <div
          style={{
            flex: 1,
            height: 1,
            background: token.colorBorder,
          }}
        />
      </div>

      {/* 模型选择下拉 */}
      <div>
        <Text type="secondary" style={{ fontSize: 13, marginBottom: 8, display: 'block' }}>
          {t('template.selectFromList', { defaultValue: '从列表选择' })}
        </Text>
        <div
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            gap: 8,
            maxHeight: 200,
            overflowY: 'auto',
            padding: '8px 0',
          }}
        >
          {modelTemplates.map(template => {
            const modelType = inferModelType(template.capabilities);
            return (
              <Tag
                key={template.id}
                style={{
                  cursor: 'pointer',
                  padding: '4px 10px',
                  margin: 0,
                  fontSize: 12,
                  background: token.colorBgContainer,
                  border: `1px solid ${token.colorBorder}`,
                }}
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

export type { ModelTemplate };
