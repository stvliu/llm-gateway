import { Card, Modal, Typography, Button, Space, Tag, Spin } from 'antd';
import { useTranslation } from 'react-i18next';
import { useProviderCatalogs } from '@/services/query/useCatalog';
import { ProviderIcon } from '@/components/ui';

const { Text, Paragraph } = Typography;

interface Props {
  open: boolean;
  onClose: () => void;
  onSelectTemplate: (code: string) => void;
}

/**
 * 供应商模板库组件
 *
 * <p>展示预配置的供应商模板，用户可快速选择并创建供应商配置。</p>
 */
export default function TemplateLibrary({ open, onClose, onSelectTemplate }: Props) {
  const { t } = useTranslation('providers');
  const { data: catalogs, isLoading } = useProviderCatalogs();

  return (
    <Modal
      title={t('template.title', { defaultValue: '供应商模板库' })}
      open={open}
      onCancel={onClose}
      footer={null}
      width={720}
    >
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        {t('template.desc', { defaultValue: '选择模板可快速创建供应商和模型配置' })}
      </Paragraph>
      {isLoading ? (
        <div style={{ textAlign: 'center', padding: 32 }}>
          <Spin />
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 12 }}>
          {catalogs?.map((catalog) => (
            <Card
              key={catalog.code}
              size="small"
              hoverable
            >
              <Card.Meta
                avatar={<ProviderIcon providerId={catalog.code} size={24} />}
                title={catalog.name}
                description={
                  <Space direction="vertical" size={4}>
                    <Text type="secondary" style={{ fontSize: 12 }}>{catalog.code}</Text>
                    <Tag color={catalog.materialized ? 'green' : 'blue'} style={{ fontSize: 11 }}>
                      {catalog.materialized
                        ? t('template.materialized', { defaultValue: '已物化' })
                        : t('template.notMaterialized', { defaultValue: '未物化' })}
                    </Tag>
                  </Space>
                }
              />
              <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
                <Button
                  type="primary"
                  size="small"
                  onClick={() => { onSelectTemplate(catalog.code); onClose(); }}
                >
                  {t('template.use', { defaultValue: '使用此模板' })}
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}
      {(!catalogs || catalogs.length === 0) && !isLoading && (
        <div style={{ textAlign: 'center', padding: 32 }}>
          <Text type="secondary">{t('template.noTemplate', { defaultValue: '暂无模板' })}</Text>
        </div>
      )}
    </Modal>
  );
}
