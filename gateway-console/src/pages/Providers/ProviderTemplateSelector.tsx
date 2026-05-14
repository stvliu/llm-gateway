import { useMemo } from 'react';
import { Card, Tag, Typography, Empty, Tooltip } from 'antd';
import { GlobalOutlined, CheckOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useTemplates } from '@/services/query';
import type { ProviderTemplate } from '@/types/template';

const { Text, Paragraph } = Typography;

interface ProviderTemplateSelectorProps {
  onSelect: (template: ProviderTemplate) => void;
  selectedId?: number;
}

/**
 * 供应商模板选择器
 * 展示官方模板列表，点击直接选择（无预览抽屉）
 */
export function ProviderTemplateSelector({
  onSelect,
  selectedId,
}: ProviderTemplateSelectorProps) {
  const { t } = useTranslation('providers');

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

  // 加载状态
  if (isLoading) {
    return null;
  }

  // 空状态
  if (templates.length === 0) {
    return (
      <Empty description={t('template.noTemplate', { defaultValue: '暂无模板' })} />
    );
  }

  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
        gap: 12,
      }}
    >
      {templates.map(template => {
        const isSelected = selectedId === template.id;
        const modelCount = template.modelCount || template.modelsConfig?.length || 0;

        return (
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
            {/* 头部：图标和选中状态 */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 8,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                <GlobalOutlined style={{ fontSize: 16 }} />
              </div>

              <div style={{ flex: 1, minWidth: 0 }}>
                <Text strong style={{ fontSize: 13, display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {template.templateName}
                </Text>
                <Text type="secondary" style={{ fontSize: 11 }}>
                  {modelCount} {t('template.modelCount', { defaultValue: '个模型' })}
                </Text>
              </div>

              {isSelected && (
                <CheckOutlined style={{ fontSize: 16 }} />
              )}
            </div>

            {/* 描述 */}
            {template.description && (
              <Tooltip title={template.description}>
                <Paragraph
                  type="secondary"
                  style={{
                    fontSize: 11,
                    marginBottom: 0,
                    marginTop: 8,
                    lineHeight: 1.4,
                  }}
                  ellipsis={{ rows: 2 }}
                >
                  {template.description}
                </Paragraph>
              </Tooltip>
            )}

            {/* 标签 */}
            {template.tags && template.tags.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, marginTop: 8 }}>
                {template.tags.slice(0, 2).map(tag => (
                  <Tag key={tag} style={{ fontSize: 10, margin: 0, padding: '0 4px' }}>
                    {tag}
                  </Tag>
                ))}
              </div>
            )}
          </Card>
        );
      })}
    </div>
  );
}

export type { ProviderTemplateSelectorProps };
