import { useMemo } from 'react';
import { Card, Tag, Typography } from 'antd';
import { GlobalOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useTemplates } from '@/services/query';
import type { ProviderTemplate } from '@/types/template';

const { Text, Paragraph } = Typography;

interface ProviderTemplateSelectorProps {
  onSelect: (template: ProviderTemplate) => void;
}

/**
 * 供应商模板选择器
 * 展示官方模板列表，支持选择模板自动填充供应商表单
 */
export function ProviderTemplateSelector({
  onSelect,
}: ProviderTemplateSelectorProps) {
  const { t } = useTranslation('models');

  const { data: templatesData, isLoading } = useTemplates({
    type: 'OFFICIAL',
    limit: 100,
  });

  // 过滤并排序模板
  const templates = useMemo(() => {
    if (!templatesData?.items) return [];

    // 按 providerType 去重（每个供应商类型只保留一个模板）
    const uniqueMap = new Map<string, ProviderTemplate>();
    templatesData.items.forEach(item => {
      if (!uniqueMap.has(item.providerType)) {
        uniqueMap.set(item.providerType, item);
      }
    });

    // 按热门程度排序
    const popularTypes = ['OPENAI', 'ANTHROPIC', 'DEEPSEEK', 'QWEN', 'GOOGLE'];
    return Array.from(uniqueMap.values()).sort((a, b) => {
      const aIndex = popularTypes.indexOf(a.providerType);
      const bIndex = popularTypes.indexOf(b.providerType);
      if (aIndex === -1 && bIndex === -1) return a.templateName.localeCompare(b.templateName);
      if (aIndex === -1) return 1;
      if (bIndex === -1) return -1;
      return aIndex - bIndex;
    });
  }, [templatesData]);

  if (isLoading) {
    return null;
  }

  if (templates.length === 0) {
    return null;
  }

  return (
    <div>
      {/* 模板卡片网格 */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
          gap: 12,
        }}
      >
        {templates.map(template => (
          <Card
            key={template.id}
            hoverable
            size="small"
            style={{
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
            styles={{
              body: { padding: 12 },
            }}
            onClick={() => onSelect(template)}
          >
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
              {/* 图标 */}
              <div
                style={{
                  width: 36,
                  height: 36,
                  borderRadius: 8,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                <GlobalOutlined style={{ fontSize: 18 }} />
              </div>

              {/* 信息 */}
              <div style={{ flex: 1, minWidth: 0 }}>
                <Text strong style={{ fontSize: 14, display: 'block' }}>
                  {template.templateName}
                </Text>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {template.modelCount || template.modelsConfig?.length || 0} {t('provider.models', { defaultValue: '个模型' })}
                </Text>
              </div>
            </div>

            {/* 描述 */}
            {template.description && (
              <Paragraph
                type="secondary"
                style={{
                  fontSize: 12,
                  marginBottom: 8,
                  marginTop: 8,
                  lineHeight: 1.4,
                }}
                ellipsis={{ rows: 2, tooltip: template.description }}
              >
                {template.description}
              </Paragraph>
            )}

            {/* 标签 */}
            {template.tags && template.tags.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                {template.tags.slice(0, 3).map(tag => (
                  <Tag key={tag} style={{ fontSize: 11, margin: 0, padding: '0 4px' }}>
                    {tag}
                  </Tag>
                ))}
              </div>
            )}
          </Card>
        ))}
      </div>
    </div>
  );
}

export type { ProviderTemplateSelectorProps };