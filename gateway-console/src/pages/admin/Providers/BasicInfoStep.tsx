import { useState, useEffect, useCallback } from 'react';
import { Typography, Spin, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { useTemplates } from '@/services/query';
import { providerApi } from '@/services/api/provider';
import type { CreateProviderRequest, ProviderType } from '@/types/provider';
import type { ProviderTemplate, ModelConfig } from '@/types/template';
import type { NestedModelRequest } from '@/types/provider';
import type { ProviderTypeOption } from '@/types/api';

const { Text } = Typography;

interface BasicInfoStepProps {
  basicInfo: CreateProviderRequest | null;
  onChange: (info: CreateProviderRequest | null) => void;
  onTemplateLoad: (template: ProviderTemplate | null) => void;
  onModelsChange: (models: NestedModelRequest[]) => void;
}

/**
 * 基本信息步骤组件
 * 供应商类型下拉框在第一行，选择后自动从模板填充其他字段
 */
export function BasicInfoStep({
  basicInfo,
  onChange,
  onTemplateLoad,
  onModelsChange,
}: BasicInfoStepProps) {
  const { t } = useTranslation('providers');
  const [loadingTemplate, setLoadingTemplate] = useState(false);
  const [providerTypes, setProviderTypes] = useState<ProviderTypeOption[]>([]);
  const [loadingTypes, setLoadingTypes] = useState(true);

  // 查询模板数据
  const { data: templatesData } = useTemplates({
    type: 'OFFICIAL',
    limit: 100,
  });

  // 获取供应商类型列表
  useEffect(() => {
    const fetchProviderTypes = async () => {
      try {
        const types = await providerApi.getProviderTypes();
        setProviderTypes(types);
      } catch {
        // 如果 API 失败，使用默认列表
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
  }, [basicInfo, onChange]);

  // 根据供应商类型自动填充基本信息
  const autoFillFromTemplate = useCallback((providerType: ProviderType) => {
    if (!templatesData?.items) {
      // 模板数据未加载，只更新类型
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

    // 查找该供应商类型的官方模板
    const template = templatesData.items.find(
      (t: ProviderTemplate) => t.providerType === providerType
    );

    if (template) {
      setLoadingTemplate(true);
      onTemplateLoad(template);

      const config = template.providerConfig as Record<string, unknown>;

      // 从模板填充基本信息
      const newInfo: CreateProviderRequest = {
        providerName: String(config.provider_name || template.templateName),
        providerType,
        baseUrl: String(config.base_url || ''),
        websiteUrl: String(config.website_url || ''),
        apiDocUrl: String(config.api_doc_url || ''),
        apiKeys: [],
        models: [],
      };

      onChange(newInfo);

      // 从模板提取模型配置
      if (template.modelsConfig && template.modelsConfig.length > 0) {
        const models: NestedModelRequest[] = template.modelsConfig.map((m: ModelConfig) => ({
          providerModelId: m.provider_model_id,
          displayName: m.display_name,
          contextWindow: m.context_window,
          inputPrice: m.input_price,
          outputPrice: m.output_price,
          capabilities: m.capabilities,
        }));
        onModelsChange(models);
      } else {
        onModelsChange([]);
      }

      // 显示提示
      message.success(t('template.autoFilled', { defaultValue: '已从模板自动填充基本信息' }));

      setTimeout(() => setLoadingTemplate(false), 300);
    } else {
      // 没有找到模板，只更新类型
      onTemplateLoad(null);
      onModelsChange([]);
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
  }, [templatesData, onChange, onTemplateLoad, onModelsChange, t]);

  // 处理供应商类型变更
  const handleProviderTypeChange = useCallback((newType: ProviderType) => {
    // 自动填充
    autoFillFromTemplate(newType);
  }, [autoFillFromTemplate]);

  // 处理字段变更
  const handleFieldChange = useCallback((field: keyof CreateProviderRequest, value: string) => {
    if (basicInfo) {
      onChange({ ...basicInfo, [field]: value });
    }
  }, [onChange, basicInfo]);

  // 输入框样式
  const inputStyle = {
    width: '100%',
    padding: '8px 12px',
    border: '1px solid #d9d9d9',
    borderRadius: 6,
    fontSize: 14,
  };

  return (
    <div>
      {/* 加载提示 */}
      {(loadingTemplate || loadingTypes) && (
        <div style={{ textAlign: 'center', padding: 8, marginBottom: 16 }}>
          <Spin size="small" />
          <Text type="secondary" style={{ marginLeft: 8 }}>
            {loadingTypes
              ? t('template.loadingTypes', { defaultValue: '正在加载供应商类型...' })
              : t('template.loading', { defaultValue: '正在加载模板数据...' })}
          </Text>
        </div>
      )}

      <div style={{ display: 'grid', gap: 16 }}>
        {/* 供应商类型（第一行） */}
        <div>
          <Text type="secondary" style={{ fontSize: 12, marginBottom: 4, display: 'block' }}>
            {t('provider.type')} <Text type="danger">*</Text>
          </Text>
          <select
            value={basicInfo?.providerType || 'OPENAI'}
            onChange={(e) => handleProviderTypeChange(e.target.value as ProviderType)}
            style={inputStyle}
            disabled={loadingTemplate || loadingTypes}
          >
            {providerTypes.map(opt => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <Text type="secondary" style={{ fontSize: 11, marginTop: 4, display: 'block' }}>
            {t('template.typeHint', { defaultValue: '选择供应商类型后将自动填充基本信息' })}
          </Text>
        </div>

        {/* 供应商名称 */}
        <div>
          <Text type="secondary" style={{ fontSize: 12, marginBottom: 4, display: 'block' }}>
            {t('provider.name')} <Text type="danger">*</Text>
          </Text>
          <input
            type="text"
            value={basicInfo?.providerName || ''}
            onChange={(e) => handleFieldChange('providerName', e.target.value)}
            placeholder={t('template.providerNamePlaceholder', { defaultValue: '例如：OpenAI 官方' })}
            style={inputStyle}
          />
        </div>

        {/* API 地址 */}
        <div>
          <Text type="secondary" style={{ fontSize: 12, marginBottom: 4, display: 'block' }}>
            {t('provider.baseUrl')} <Text type="danger">*</Text>
          </Text>
          <input
            type="text"
            value={basicInfo?.baseUrl || ''}
            onChange={(e) => handleFieldChange('baseUrl', e.target.value)}
            placeholder="https://api.example.com"
            style={inputStyle}
          />
        </div>

        {/* 官网地址 */}
        <div>
          <Text type="secondary" style={{ fontSize: 12, marginBottom: 4, display: 'block' }}>
            {t('provider.websiteUrl', { defaultValue: '官网地址' })}
          </Text>
          <input
            type="text"
            value={basicInfo?.websiteUrl || ''}
            onChange={(e) => handleFieldChange('websiteUrl', e.target.value)}
            placeholder="https://example.com"
            style={inputStyle}
          />
        </div>

        {/* API 文档 */}
        <div>
          <Text type="secondary" style={{ fontSize: 12, marginBottom: 4, display: 'block' }}>
            {t('provider.apiDocUrl', { defaultValue: 'API 文档' })}
          </Text>
          <input
            type="text"
            value={basicInfo?.apiDocUrl || ''}
            onChange={(e) => handleFieldChange('apiDocUrl', e.target.value)}
            placeholder="https://docs.example.com"
            style={inputStyle}
          />
        </div>
      </div>
    </div>
  );
}

export type { BasicInfoStepProps };