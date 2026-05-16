import { useMemo } from 'react';
import { Card, Tag, Typography, Empty, Tooltip } from 'antd';
import { GlobalOutlined, CheckOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviderMetadataList } from '@/services/query/useMetadata';
import type { ProviderMetadata } from '@/types/metadata';

const { Text, Paragraph } = Typography;

interface ProviderMetadataSelectorProps {
  onSelect: (metadata: ProviderMetadata) => void;
  selectedId?: number;
}

/**
 * 供应商元数据选择器
 * 展示供应商元数据列表，点击直接选择
 */
export function ProviderMetadataSelector({
  onSelect,
  selectedId,
}: ProviderMetadataSelectorProps) {
  const { t } = useTranslation('providers');

  const { data: metadataList, isLoading } = useProviderMetadataList();

  // 按 providerType 去重并排序
  const metadata = useMemo(() => {
    if (!metadataList) return [];

    const popularTypes = ['OPENAI', 'ANTHROPIC', 'DEEPSEEK', 'QWEN', 'GEMINI'];
    return [...metadataList].sort((a, b) => {
      const aIndex = popularTypes.indexOf(a.providerType);
      const bIndex = popularTypes.indexOf(b.providerType);
      if (aIndex === -1 && bIndex === -1) return a.providerName.localeCompare(b.providerName);
      if (aIndex === -1) return 1;
      if (bIndex === -1) return -1;
      return aIndex - bIndex;
    });
  }, [metadataList]);

  if (isLoading) return null;

  if (metadata.length === 0) {
    return <Empty description={t('template.noTemplate', { defaultValue: '暂无模板' })} />;
  }

  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
        gap: 12,
      }}
    >
      {metadata.map(item => {
        const isSelected = selectedId === item.id;
        const modelCount = item.modelCount || 0;

        return (
          <Card
            key={item.id}
            hoverable
            size="small"
            style={{ cursor: 'pointer', transition: 'all 0.2s' }}
            styles={{ body: { padding: 12 } }}
            onClick={() => onSelect(item)}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <div
                style={{
                  width: 32, height: 32, borderRadius: 8,
                  display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                }}
              >
                <GlobalOutlined style={{ fontSize: 16 }} />
              </div>

              <div style={{ flex: 1, minWidth: 0 }}>
                <Text strong style={{ fontSize: 13, display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {item.providerName}
                </Text>
                <Text type="secondary" style={{ fontSize: 11 }}>
                  {modelCount} {t('template.modelCount', { defaultValue: '个模型' })}
                </Text>
              </div>

              {isSelected && <CheckOutlined style={{ fontSize: 16 }} />}
            </div>

            {item.description && (
              <Tooltip title={item.description}>
                <Paragraph type="secondary" style={{ fontSize: 11, marginBottom: 0, marginTop: 8, lineHeight: 1.4 }} ellipsis={{ rows: 2 }}>
                  {item.description}
                </Paragraph>
              </Tooltip>
            )}

            {item.tags && item.tags.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, marginTop: 8 }}>
                {item.tags.slice(0, 2).map(tag => (
                  <Tag key={tag} style={{ fontSize: 10, margin: 0, padding: '0 4px' }}>{tag}</Tag>
                ))}
              </div>
            )}
          </Card>
        );
      })}
    </div>
  );
}

export type { ProviderMetadataSelectorProps };