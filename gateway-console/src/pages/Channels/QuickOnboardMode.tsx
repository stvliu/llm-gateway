import { useState, useEffect, useMemo } from 'react';
import { Steps, Select, Input, Checkbox, Button, Space, Typography, App, Spin, Tag, Divider, theme } from 'antd';
import { PlusOutlined, DeleteOutlined, CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { planCatalogApi, provisionApi } from '@/services/api/catalog';
import type { ProvisionRequest } from '@/types/catalog';
import { ProviderForm, type ProviderFormValue } from './ProviderForm';

const { Text, Title } = Typography;
const { TextArea } = Input;

interface QuickOnboardModeProps {
  onComplete: () => void;
  onCancel: () => void;
  initialPlanCode?: string;
  initialPlanName?: string;
}

interface EditableEndpoint {
  id: string;
  protocol: string;
  url: string;
  checked: boolean;
}

/** 内联创建供应商的初始空值 */
const EMPTY_INLINE_PROVIDER: ProviderFormValue = {
  code: '',
  name: '',
};

/**
 * 快速接入模式——四步完成渠道创建
 *
 * <p>任务 10.2 / 10.3-10.5：状态扁平化 + Step 0.5 内联创建供应商。
 * 不变量：</p>
 * <ul>
 *   <li>selectedProviderCode != null ⇔ inlineProviderExpanded == false && inlineProvider == null</li>
 *   <li>inlineProviderExpanded == true ⇔ selectedProviderCode == null</li>
 *   <li>切换分支时 clear 对方</li>
 * </ul>
 */
export function QuickOnboardMode({ onComplete, initialPlanCode, initialPlanName }: QuickOnboardModeProps) {
  const { t } = useTranslation('channels');
  const { message } = App.useApp();
  const { token } = theme.useToken();
  const queryClient = useQueryClient();
  const [step, setStep] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  // Step 0：选已有 provider 路径
  const [selectedProviderCode, setSelectedProviderCode] = useState<string | null>(null);
  const [selectedPlanCode, setSelectedPlanCode] = useState<string>();
  const [selectedPlanName, setSelectedPlanName] = useState<string>();
  // Step 0.5：内联创建供应商路径（与 selectedProviderCode 互斥）
  const [inlineProviderExpanded, setInlineProviderExpanded] = useState(false);
  const [inlineProvider, setInlineProvider] = useState<ProviderFormValue | null>(null);

  const [endpoints, setEndpoints] = useState<EditableEndpoint[]>([]);
  const [selectedModels, setSelectedModels] = useState<string[]>([]);
  const [customModelInput, setCustomModelInput] = useState('');
  const [apiKeyInput, setApiKeyInput] = useState('');

  const { data: providers, isLoading: providersLoading } = useQuery({
    queryKey: ['provider-catalog', 'list'],
    queryFn: () => planCatalogApi.listProviders(),
  });

  const { data: plans, isLoading: plansLoading } = useQuery({
    queryKey: ['plan-catalog', 'list', selectedProviderCode],
    queryFn: () => planCatalogApi.list({ providerCode: selectedProviderCode ?? undefined }),
    enabled: !!selectedProviderCode,
  });

  const { data: planDetail, isLoading: planDetailLoading } = useQuery({
    queryKey: ['plan-catalog', 'detail', selectedPlanCode],
    queryFn: () => planCatalogApi.getDetail(selectedPlanCode!),
    enabled: !!selectedPlanCode,
  });

  useEffect(() => {
    if (initialPlanCode) {
      setSelectedPlanCode(initialPlanCode);
      if (initialPlanName) setSelectedPlanName(initialPlanName);
    }
  }, [initialPlanCode, initialPlanName]);

  useEffect(() => {
    if (planDetail) {
      const initialEndpoints: EditableEndpoint[] = planDetail.endpoints.map((ep, index) => ({
        id: `ep-${index}`,
        protocol: ep.protocol || 'openai',
        url: ep.url,
        checked: true,
      }));
      setEndpoints(initialEndpoints);
      setSelectedModels(planDetail.pricing.map((p) => p.modelName));
      setSelectedPlanName((prev) => prev || planDetail.planName);
      if (planDetail.providerCode) setSelectedProviderCode(planDetail.providerCode);
    }
  }, [planDetail]);

  const parsedApiKeys = useMemo(() => {
    if (!apiKeyInput.trim()) return [];
    const keys = apiKeyInput.split(/[;,\n]+/).map((k) => k.trim()).filter((k) => k.length > 0);
    return keys.map((raw, index) => ({
      id: `key-${index}`,
      raw,
      valid: raw.length >= 8,
      warning: raw.length < 8 ? t('onboard.keyTooShort') : undefined,
    }));
  }, [apiKeyInput, t]);

  /**
   * 内联表单是否通过基本校验：必须有 code（非空字符串）。
   * 注：详细 pattern / 一致性校验由 ProviderForm 的 antd rules 渲染错误。
   */
  const inlineProviderValid = useMemo(() => {
    if (!inlineProvider) return false;
    return Boolean(inlineProvider.code?.trim()) && Boolean(inlineProvider.name?.trim());
  }, [inlineProvider]);

  /** Step 0 是否已通过：要么选了已有 provider 且选了 plan，要么走内联创建且内联表单有效 */
  const step0Valid = useMemo(() => {
    if (selectedProviderCode && selectedPlanCode) return true;
    if (inlineProviderExpanded && inlineProviderValid) return true;
    return false;
  }, [selectedProviderCode, selectedPlanCode, inlineProviderExpanded, inlineProviderValid]);

  /** 切换到"使用已有"分支：清空内联表单字段（互斥不变量） */
  const switchToExisting = (providerCode: string) => {
    setSelectedProviderCode(providerCode);
    setSelectedPlanCode(undefined);
    setSelectedPlanName(undefined);
    setEndpoints([]);
    setSelectedModels([]);
    // 清空 inline 分支
    setInlineProviderExpanded(false);
    setInlineProvider(null);
  };

  /** 切换到"内联新建"分支：清空 selectedProviderCode（互斥不变量） */
  const expandInlineProvider = () => {
    setInlineProviderExpanded(true);
    setInlineProvider({ ...EMPTY_INLINE_PROVIDER });
    // 清空 existing 分支
    setSelectedProviderCode(null);
    setSelectedPlanCode(undefined);
    setSelectedPlanName(undefined);
    setEndpoints([]);
    setSelectedModels([]);
  };

  const handleAddEndpoint = () => {
    setEndpoints([...endpoints, { id: `ep-new-${Date.now()}`, protocol: 'openai', url: '', checked: true }]);
  };

  const handleRemoveEndpoint = (id: string) => {
    setEndpoints(endpoints.filter((ep) => ep.id !== id));
  };

  const handleEndpointChange = (id: string, field: 'protocol' | 'url', value: string) => {
    setEndpoints(endpoints.map((ep) => ep.id === id ? { ...ep, [field]: value } : ep));
  };

  const handleEndpointCheck = (id: string, checked: boolean) => {
    setEndpoints(endpoints.map((ep) => ep.id === id ? { ...ep, checked } : ep));
  };

  const handleAddCustomModel = () => {
    const modelName = customModelInput.trim();
    if (modelName && !selectedModels.includes(modelName)) {
      setSelectedModels([...selectedModels, modelName]);
      setCustomModelInput('');
    }
  };

  const handleNext = () => {
    if (step === 0) {
      if (!step0Valid) {
        message.warning(t('onboard.step0.eitherRequired'));
        return;
      }
    }
    if (step === 1) {
      if (endpoints.filter((ep) => ep.checked && ep.url.trim()).length === 0) {
        message.warning(t('onboard.endpointRequired'));
        return;
      }
      if (selectedModels.length === 0) {
        message.warning(t('onboard.modelRequired'));
        return;
      }
    }
    setStep(step + 1);
  };

  const handleFinish = async () => {
    // 走"已有 provider"路径需要套餐已加载
    if (!selectedPlanCode || !planDetail) {
      message.error(t('onboard.selectPlanWarning'));
      return;
    }
    if (parsedApiKeys.some((k) => !k.valid)) {
      message.warning(t('onboard.invalidKeyWarning'));
      return;
    }
    try {
      setSubmitting(true);
      // 构造 ProvisionRequest：仅当走内联路径时携带 inlineProvider
      const requestData: ProvisionRequest = {
        apiKeys: parsedApiKeys.map((k) => k.raw),
      };
      if (inlineProviderExpanded && inlineProvider) {
        requestData.inlineProvider = {
          code: inlineProvider.code,
          name: inlineProvider.name,
          description: inlineProvider.description,
          websiteUrl: inlineProvider.websiteUrl,
          apiDocUrl: inlineProvider.apiDocUrl,
        };
      }
      await provisionApi.fromPlan(selectedPlanCode, requestData);
      message.success(t('onboard.createSuccess'));
      queryClient.invalidateQueries({ queryKey: ['channels'] });
      queryClient.invalidateQueries({ queryKey: ['providers'] });
      queryClient.invalidateQueries({ queryKey: ['plan-catalog'] });
      onComplete();
    } catch (error) {
      message.error(t('onboard.createFailed'));
    } finally {
      setSubmitting(false);
    }
  };

  const handlePlanChange = (planCode: string) => {
    const plan = plans?.find((p) => p.planCode === planCode);
    setSelectedPlanCode(planCode);
    setSelectedPlanName(plan?.planName);
    setEndpoints([]);
    setSelectedModels([]);
  };

  const renderStep0 = () => (
    <div>
      <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
        {t('onboard.selectPlanHint')}
      </Text>
      <div style={{ marginBottom: 16 }}>
        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
          <Text strong>{t('onboard.providerLabel')}</Text>
          {/* 任务 10.3：Step 0 旁加"+ 新建供应商"链接 */}
          <Button
            type="link"
            size="small"
            icon={<PlusOutlined />}
            onClick={expandInlineProvider}
          >
            {t('quickOnboard.inlineProvider.linkText')}
          </Button>
        </Space>
        <Select
          placeholder={t('onboard.providerRequired')}
          style={{ width: '100%', marginTop: 4 }}
          value={selectedProviderCode ?? undefined}
          onChange={(value) => switchToExisting(value)}
          loading={providersLoading}
          showSearch
          optionFilterProp="label"
          options={providers?.map((p: { code: string; name: string }) => ({ label: p.name, value: p.code }))}
        />
      </div>

      {/* 任务 10.3：Step 0.5 同 Drawer 内展开的内联创建供应商表单 */}
      {inlineProviderExpanded && (
        <div
          style={{
            marginBottom: 16,
            padding: 12,
            background: token.colorBgLayout,
            borderRadius: 8,
            border: `1px dashed ${token.colorBorder}`,
          }}
        >
          <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 8 }}>
            <Text strong>{t('quickOnboard.inlineProvider.expandedTitle')}</Text>
            <Button
              type="text"
              size="small"
              onClick={() => {
                setInlineProviderExpanded(false);
                setInlineProvider(null);
              }}
            >
              {t('onboard.btnPrev') /* 复用"上一步"——简化文案 */}
            </Button>
          </Space>
          <ProviderForm
            value={inlineProvider ?? EMPTY_INLINE_PROVIDER}
            onChange={(next) => setInlineProvider(next)}
            // 期望 code = 选中套餐的 providerCode；选套餐前不强制
            expectedProviderCode={planDetail?.providerCode}
          />
        </div>
      )}

      {!inlineProviderExpanded && (
        <div style={{ marginBottom: 16 }}>
          <Text strong>{t('onboard.planLabel')}</Text>
          <Select
            placeholder={selectedProviderCode ? t('onboard.planPlaceholder') : t('onboard.planPlaceholderNoProvider')}
            style={{ width: '100%', marginTop: 4 }}
            value={selectedPlanCode}
            onChange={handlePlanChange}
            loading={plansLoading}
            disabled={!selectedProviderCode}
            showSearch
            optionFilterProp="label"
            options={plans?.map((p: { planCode: string; planName: string }) => ({ label: p.planName, value: p.planCode }))}
          />
        </div>
      )}

      {planDetail && !inlineProviderExpanded && (
        <div style={{ marginTop: 16, padding: 12, background: token.colorBgLayout, borderRadius: 8 }}>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>{t('onboard.planPreview')}</Text>
          <Space direction="vertical" size="small">
            <Text><Text type="secondary">{t('onboard.planName')}：</Text>{planDetail.planName}</Text>
            <Text><Text type="secondary">{t('onboard.billingLabel')}：</Text><Tag color={planDetail.billingMode === 'pay_as_you_go' ? 'green' : 'blue'}>{planDetail.billingMode}</Tag></Text>
            <Text><Text type="secondary">{t('onboard.endpointCount')}：</Text>{t('onboard.unit', { count: planDetail.endpoints.length })}</Text>
            <Text><Text type="secondary">{t('onboard.modelCount')}：</Text>{t('onboard.unit', { count: planDetail.pricing.length })}</Text>
          </Space>
        </div>
      )}
      {planDetailLoading && <div style={{ textAlign: 'center', padding: 24 }}><Spin /></div>}
    </div>
  );

  const renderStep1 = () => (
    <div>
      <Title level={5}>{t('onboard.configureEndpoints')}</Title>
      <div style={{ marginBottom: 8 }}><Text type="secondary">{t('onboard.configureEndpointsHint')}</Text></div>
      <div style={{ marginBottom: 16 }}>
        {endpoints.map((ep) => (
          <div key={ep.id} style={{ display: 'flex', alignItems: 'center', marginBottom: 8, gap: 8 }}>
            <Checkbox checked={ep.checked} onChange={(e) => handleEndpointCheck(ep.id, e.target.checked)} />
            <Select value={ep.protocol} onChange={(value) => handleEndpointChange(ep.id, 'protocol', value)} style={{ width: 120 }}
              options={[{ label: 'OpenAI', value: 'openai' }, { label: 'Anthropic', value: 'anthropic' }]} />
            <Input placeholder={t('onboard.endpointPlaceholder')} value={ep.url} onChange={(e) => handleEndpointChange(ep.id, 'url', e.target.value)} style={{ flex: 1 }} />
            <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleRemoveEndpoint(ep.id)} />
          </div>
        ))}
        <Button type="dashed" icon={<PlusOutlined />} onClick={handleAddEndpoint}>{t('onboard.addEndpoint')}</Button>
      </div>
      <Divider />
      <Title level={5}>{t('onboard.configureModels')}</Title>
      <div style={{ marginBottom: 8 }}><Text type="secondary">{t('onboard.configureModelsHint', { count: selectedModels.length })}</Text></div>
      <div style={{ maxHeight: 300, overflow: 'auto', marginBottom: 16 }}>
        <Checkbox.Group value={selectedModels} onChange={(values: (string | number | boolean)[]) => setSelectedModels(values.filter((v): v is string => typeof v === 'string'))} style={{ width: '100%' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {(planDetail?.pricing ?? []).map((p) => (
              <Checkbox key={p.modelName} value={p.modelName}>
                <span style={{ fontWeight: 500 }}>{p.modelName}</span>
                {p.inputPrice != null && <span style={{ color: token.colorTextSecondary, marginLeft: 8, fontSize: 12 }}>{t('onboard.inputPrice')} ${p.inputPrice}/1M</span>}
                {p.outputPrice != null && <span style={{ color: token.colorTextSecondary, marginLeft: 8, fontSize: 12 }}>{t('onboard.outputPrice')} ${p.outputPrice}/1M</span>}
              </Checkbox>
            ))}
          </div>
        </Checkbox.Group>
      </div>
      <div style={{ display: 'flex', gap: 8 }}>
        <Input placeholder={t('onboard.customModelPlaceholder')} value={customModelInput} onChange={(e) => setCustomModelInput(e.target.value)} onPressEnter={handleAddCustomModel} style={{ width: 240 }} />
        <Button onClick={handleAddCustomModel}>{t('onboard.addModel')}</Button>
      </div>
    </div>
  );

  const renderStep2 = () => (
    <div>
      <Title level={5}>{t('onboard.credentialConfig')}</Title>
      <div style={{ marginBottom: 8 }}><Text type="secondary">{t('onboard.credentialHint')}</Text></div>
      <TextArea placeholder="sk-xxxxxxxxxxxxx&#10;sk-yyyyyyyyyyyyy" value={apiKeyInput} onChange={(e) => setApiKeyInput(e.target.value)} rows={6} style={{ marginBottom: 16 }} />
      {parsedApiKeys.length > 0 && (
        <div style={{ marginTop: 16 }}>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>{t('onboard.keyParsePreview', { count: parsedApiKeys.length })}</Text>
          <div style={{ maxHeight: 200, overflow: 'auto' }}>
            {parsedApiKeys.map((key) => (
              <div key={key.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 12px', marginBottom: 4, background: key.valid ? token.colorSuccessBg : token.colorErrorBg, borderRadius: 4, border: `1px solid ${key.valid ? token.colorSuccessBorder : token.colorErrorBorder}` }}>
                <Space>
                  {key.valid ? <CheckCircleOutlined style={{ color: token.colorSuccess }} /> : <ExclamationCircleOutlined style={{ color: token.colorError }} />}
                  <Text code>{key.raw}</Text>
                </Space>
                {key.warning && <Text type="danger" style={{ fontSize: 12 }}>{key.warning}</Text>}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );

  const renderStep3 = () => {
    const checkedEndpoints = endpoints.filter((ep) => ep.checked && ep.url.trim());
    const validKeys = parsedApiKeys.filter((k) => k.valid);
    return (
      <div>
        <Title level={5}>{t('onboard.confirmCreate')}</Title>
        <div style={{ marginBottom: 8 }}><Text type="secondary">{t('onboard.confirmHint')}</Text></div>
        <div style={{ marginTop: 16, padding: 16, background: token.colorBgLayout, borderRadius: 8 }}>
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <div><Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>{t('onboard.planName')}</Text><Text strong>{selectedPlanName || planDetail?.planName || '-'}</Text></div>
            <div><Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>{t('onboard.endpointCount')}</Text><Space><Tag color="blue">{t('onboard.unit', { count: checkedEndpoints.length })}</Tag>{checkedEndpoints.length > 0 && <Text type="secondary" style={{ fontSize: 12 }}>{checkedEndpoints.map((ep) => ep.url).join(', ')}</Text>}</Space></div>
            <div><Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>{t('onboard.modelCount')}</Text><Tag color="green">{t('onboard.unit', { count: selectedModels.length })}</Tag></div>
            <div><Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>{t('onboard.credentialLabel')}</Text><Space><Tag color={validKeys.length > 0 ? 'green' : 'red'}>{t('onboard.unit', { count: parsedApiKeys.length })}</Tag>{parsedApiKeys.length > 0 && <Text type="secondary" style={{ fontSize: 12 }}>{parsedApiKeys.map((k) => k.raw).join(', ')}</Text>}</Space></div>
          </Space>
        </div>
        {parsedApiKeys.length === 0 && (
          <div style={{ marginTop: 12, padding: 12, background: token.colorWarningBg, borderRadius: 8 }}>
            <Text type="warning">{t('onboard.noKeyWarning')}</Text>
          </div>
        )}
      </div>
    );
  };

  const stepItems = [
    { title: t('onboard.step1') },
    { title: t('onboard.step2') },
    { title: t('onboard.step3') },
    { title: t('onboard.step4') },
  ];

  return (
    <div>
      <Steps current={step} size="small" style={{ marginBottom: 24 }} items={stepItems} />
      {step === 0 && (
        <div>{renderStep0()}<div style={{ marginTop: 24, textAlign: 'right' }}><Button disabled={!step0Valid} type="primary" onClick={handleNext}>{t('onboard.btnNext')}</Button></div></div>
      )}
      {step === 1 && (
        <div>{renderStep1()}<div style={{ marginTop: 24, textAlign: 'right' }}><Space><Button onClick={() => setStep(0)}>{t('onboard.btnPrev')}</Button><Button type="primary" onClick={handleNext}>{t('onboard.btnNext')}</Button></Space></div></div>
      )}
      {step === 2 && (
        <div>{renderStep2()}<div style={{ marginTop: 24, textAlign: 'right' }}><Space><Button onClick={() => setStep(1)}>{t('onboard.btnPrev')}</Button><Button type="primary" onClick={handleNext}>{t('onboard.btnNext')}</Button></Space></div></div>
      )}
      {step === 3 && (
        <div>{renderStep3()}<div style={{ marginTop: 24, textAlign: 'right' }}><Space><Button onClick={() => setStep(2)}>{t('onboard.btnPrev')}</Button><Button type="primary" onClick={handleFinish} loading={submitting}>{t('onboard.btnSubmit')}</Button></Space></div></div>
      )}
    </div>
  );
}
