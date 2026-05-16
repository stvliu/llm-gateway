import { useState, useEffect, useCallback, useRef } from 'react';
import { Typography, Spin, Input, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { useProviderMetadataList } from '@/services/query/useMetadata';
import { providerApi } from '@/services/api/provider';
import type { CreateProviderRequest, ProviderType } from '@/types/provider';
import type { ProviderMetadata } from '@/types/metadata';
import type { ProviderTypeOption } from '@/types/api';

const { Text } = Typography;

interface BasicInfoStepProps {
  basicInfo: CreateProviderRequest | null;
  onChange: (info: CreateProviderRequest | null) => void;
  onMetadataLoad: (metadata: ProviderMetadata | null) => void;
  onSelectedModelIdsChange: (modelIds: string[]) => void;
}

/**
 * 基本信息步骤组件
 * 供应商类型下拉框在第一行，选择后自动从元数据填充其他字段
 * 所有字段均可手动编辑修改
 */
export function BasicInfoStep({
  basicInfo,
  onChange,
  onMetadataLoad,
  onSelectedModelIdsChange,
}: BasicInfoStepProps) {
  const { t } = useTranslation('providers');
  const [loadingMetadata, setLoadingMetadata] = useState(false);
  const [providerTypes, setProviderTypes] = useState<ProviderTypeOption[]>([]);
  const [loadingTypes, setLoadingTypes] = useState(true);

  // 使用 ref 保持最新的 basicInfo 引用，避免 handleFieldChange 因 basicInfo 变化而重建
  const basicInfoRef = useRef(basicInfo);
  basicInfoRef.current = basicInfo;

  // 追踪是否已完成初始元数据填充
  const initialFillDoneRef = useRef(false);

  // 查询供应商元数据列表
  const { data: metadataList } = useProviderMetadataList();

  // 获取供应商类型列表
  useEffect(() => {
    const fetchProviderTypes = async () => {
      try {
        const types = await providerApi.getProviderTypes();
        setProviderTypes(types);
      } catch {
        setProviderTypes([
          { value: 'OPENAI', label: 'OpenAI' },
          { value: 'ANTHROPIC', label: 'Anthropic' },
          { value: 'GEMINI', label: 'Google Gemini' },
          { value: 'DEEPSEEK', label: 'DeepSeek' },
          { value: 'MOONSHOT', label: 'Moonshot' },
          { value: 'ZHIPU', label: '智谱 GLM' },
          { value: 'BAICHUAN', label: '百川智能' },
          { value: 'MINIMAX', label: 'MiniMax' },
          { value: 'VOLCENGINE', label: '火山引擎' },
          { value: 'QWEN', label: '通义千问' },
          { value: 'WENXIN', label: '文心一言' },
          { value: 'TENCENT', label: '腾讯混元' },
          { value: 'XUNFEI', label: '讯飞星火' },
          { value: 'OTHER', label: '其他' },
        ]);
      } finally {
        setLoadingTypes(false);
      }
    };
    fetchProviderTypes();
  }, []);

  // 初始化基本信息
  useEffect(() => {
    if (!basicInfo) {
      onChange({
        providerName: '',
        providerType: 'OPENAI',
        baseUrl: '',
        websiteUrl: '',
        apiDocUrl: '',
        apiKeys: [],
        models: [],
      });
    }
  }, []); // 仅在挂载时初始化

  // 根据供应商类型自动填充基本信息（定义在 useEffect 之前，避免提升问题）
  const autoFillFromMetadata = useCallback((providerType: ProviderType) => {
    if (!metadataList) {
      onChange({
        providerName: '',
        providerType,
        baseUrl: '',
        websiteUrl: '',
        apiDocUrl: '',
        apiKeys: [],
        models: [],
      });
      return;
    }

    // 查找该供应商类型的官方元数据
    const metadata = metadataList.find(
      (m: ProviderMetadata) => m.providerType === providerType
    );

    if (metadata) {
      onMetadataLoad(metadata);

      // 解析 providerConfig（后端返回的是 JSON 字符串）
      let config: Record<string, unknown> = {};
      if (metadata.providerConfig) {
        if (typeof metadata.providerConfig === 'string') {
          try {
            config = JSON.parse(metadata.providerConfig as string);
          } catch (e) {
            console.error('[BasicInfoStep] Failed to parse providerConfig:', e);
          }
        } else if (typeof metadata.providerConfig === 'object') {
          config = metadata.providerConfig as Record<string, unknown>;
        }
      }

      const newInfo: CreateProviderRequest = {
        providerName: String(config.provider_name || metadata.providerName),
        providerType,
        baseUrl: String(config.base_url || ''),
        websiteUrl: String(config.website_url || ''),
        apiDocUrl: String(config.api_doc_url || ''),
        apiKeys: [],
        models: [],
      };

      onChange(newInfo);
      onSelectedModelIdsChange([]);
      setLoadingMetadata(false);
    } else {
      onMetadataLoad(null);
      onSelectedModelIdsChange([]);
      onChange({
        providerName: '',
        providerType,
        baseUrl: '',
        websiteUrl: '',
        apiDocUrl: '',
        apiKeys: [],
        models: [],
      });
    }
  }, [metadataList, onChange, onMetadataLoad, onSelectedModelIdsChange]);

  // 当元数据列表加载完成后，自动填充当前选中的供应商类型（仅执行一次）
  useEffect(() => {
    if (
      metadataList &&
      metadataList.length > 0 &&
      basicInfo?.providerType &&
      !initialFillDoneRef.current
    ) {
      initialFillDoneRef.current = true;
      autoFillFromMetadata(basicInfo.providerType as ProviderType);
    }
  }, [metadataList, basicInfo?.providerType, autoFillFromMetadata]);

  // 处理供应商类型变更
  const handleProviderTypeChange = useCallback((newType: ProviderType) => {
    autoFillFromMetadata(newType);
  }, [autoFillFromMetadata]);

  // 处理字段变更（使用 ref 保持回调稳定，避免 Input 因回调重建而失去焦点）
  const handleFieldChange = useCallback((field: keyof CreateProviderRequest, value: string) => {
    const current = basicInfoRef.current;
    if (current) {
      onChange({ ...current, [field]: value });
    }
  }, [onChange]);

  return (
    <div>
      {/* 加载提示 */}
      {(loadingMetadata || loadingTypes) && (
        <div style={{ textAlign: 'center', padding: 8, marginBottom: 16 }}>
          <Spin size="small" />
          <Text type="secondary" style={{ marginLeft: 8 }}>
            {loadingTypes
              ? t('template.loadingTypes', { defaultValue: '正在加载供应商类型...' })
              : t('template.loading', { defaultValue: '正在加载元数据...' })}
          </Text>
        </div>
      )}

      <div style={{ display: 'grid', gap: 16 }}>
        {/* 供应商类型（第一行） */}
        <div>
          <Text type="secondary" style={{ fontSize: 12, marginBottom: 4, display: 'block' }}>
            {t('provider.type')} <Text type="danger">*</Text>
          </Text>
          <Select
            value={basicInfo?.providerType || 'OPENAI'}
            onChange={(value) => handleProviderTypeChange(value as ProviderType)}
            style={{ width: '100%' }}
            disabled={loadingMetadata || loadingTypes}
            options={providerTypes.map(opt => ({
              value: opt.value,
              label: opt.label,
            }))}
          />
          <Text type="secondary" style={{ fontSize: 11, marginTop: 4, display: 'block' }}>
            {t('template.typeHint', { defaultValue: '选择供应商类型后将自动填充基本信息' })}
          </Text>
        </div>

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

        {/* API 地址 */}
        <div>
          <Text type="secondary" style={{ fontSize: 12, marginBottom: 4, display: 'block' }}>
            {t('provider.baseUrl')} <Text type="danger">*</Text>
          </Text>
          <Input
            value={basicInfo?.baseUrl || ''}
            onChange={(e) => handleFieldChange('baseUrl', e.target.value)}
            placeholder="https://api.example.com"
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