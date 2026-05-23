import { useEffect, useCallback, useRef } from 'react';
import { Typography, Input } from 'antd';
import { useTranslation } from 'react-i18next';
import { useProviderMetadataList } from '@/services/query/useMetadata';
import type { CreateProviderRequest } from '@/types/provider';
import type { ProviderMetadata } from '@/types/metadata';

const { Text } = Typography;

interface BasicInfoStepProps {
  basicInfo: CreateProviderRequest | null;
  onChange: (info: CreateProviderRequest | null) => void;
  onMetadataLoad: (metadata: ProviderMetadata | null) => void;
  onSelectedModelIdsChange: (modelIds: string[]) => void;
}

/**
 * 基本信息步骤组件
 * 所有字段均可手动编辑
 */
export function BasicInfoStep({
  basicInfo,
  onChange,
  onMetadataLoad: _onMetadataLoad,
  onSelectedModelIdsChange: _onSelectedModelIdsChange,
}: BasicInfoStepProps) {
  const { t } = useTranslation('providers');

  // 使用 ref 保持最新的 basicInfo 引用，避免 handleFieldChange 因 basicInfo 变化而重建
  const basicInfoRef = useRef(basicInfo);
  basicInfoRef.current = basicInfo;

  // 查询供应商元数据列表
  const { data: _metadataList } = useProviderMetadataList();

  // 初始化基本信息
  useEffect(() => {
    if (!basicInfo) {
      onChange({
        providerName: '',
        websiteUrl: '',
        apiDocUrl: '',
      });
    }
  }, []); // 仅在挂载时初始化

  // 处理字段变更（使用 ref 保持回调稳定，避免 Input 因回调重建而失去焦点）
  const handleFieldChange = useCallback((field: keyof CreateProviderRequest, value: string) => {
    const current = basicInfoRef.current;
    if (current) {
      onChange({ ...current, [field]: value });
    }
  }, [onChange]);

  return (
    <div>
      <div style={{ display: 'grid', gap: 16 }}>
        {/* 供应商名称 */}
        <div>
          <Text type="secondary" style={{ fontSize: 12, marginBottom: 4, display: 'block' }}>
            {t('provider.name')} <Text type="danger">*</Text>
          </Text>
          <Input
            value={basicInfo?.providerName || ''}
            onChange={(e) => handleFieldChange('providerName', e.target.value)}
            placeholder={t('template.providerNamePlaceholder', { defaultValue: '例如：OpenAI 官方' })}
          />
        </div>

        {/* 官网地址 */}
        <div>
          <Text type="secondary" style={{ fontSize: 12, marginBottom: 4, display: 'block' }}>
            {t('provider.websiteUrl', { defaultValue: '官网地址' })}
          </Text>
          <Input
            value={basicInfo?.websiteUrl || ''}
            onChange={(e) => handleFieldChange('websiteUrl', e.target.value)}
            placeholder="https://example.com"
          />
        </div>

        {/* API 文档 */}
        <div>
          <Text type="secondary" style={{ fontSize: 12, marginBottom: 4, display: 'block' }}>
            {t('provider.apiDocUrl', { defaultValue: 'API 文档' })}
          </Text>
          <Input
            value={basicInfo?.apiDocUrl || ''}
            onChange={(e) => handleFieldChange('apiDocUrl', e.target.value)}
            placeholder="https://docs.example.com"
          />
        </div>
      </div>
    </div>
  );
}

export type { BasicInfoStepProps };