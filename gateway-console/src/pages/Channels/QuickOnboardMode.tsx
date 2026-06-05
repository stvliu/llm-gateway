import { useState, useEffect, useMemo } from 'react';
import { Steps, Select, Input, Checkbox, Button, Space, Typography, App, Spin, Tag, Divider, theme } from 'antd';
import { PlusOutlined, DeleteOutlined, CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { providerCatalogApi, planCatalogApi, catalogMaterializeApi } from '@/services/api/catalog';
import type { MaterializePlanRequest } from '@/types/catalog';

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

/**
 * 快速接入模式——四步完成渠道创建
 */
export function QuickOnboardMode({ onComplete, initialPlanCode, initialPlanName }: QuickOnboardModeProps) {
  const { t } = useTranslation('channels');
  const { message } = App.useApp();
  const { token } = theme.useToken();
  const queryClient = useQueryClient();
  const [step, setStep] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  const [selectedProviderCode, setSelectedProviderCode] = useState<string>();
  const [selectedPlanCode, setSelectedPlanCode] = useState<string>();
  const [selectedPlanName, setSelectedPlanName] = useState<string>();
  const [endpoints, setEndpoints] = useState<EditableEndpoint[]>([]);
  const [selectedModels, setSelectedModels] = useState<string[]>([]);
  const [customModelInput, setCustomModelInput] = useState('');
  const [apiKeyInput, setApiKeyInput] = useState('');

  const { data: providers, isLoading: providersLoading } = useQuery({
    queryKey: ['provider-catalog', 'list'],
    queryFn: () => providerCatalogApi.list(),
  });

  const { data: plans, isLoading: plansLoading } = useQuery({
    queryKey: ['plan-catalog', 'list', selectedProviderCode],
    queryFn: () => planCatalogApi.list({ providerCode: selectedProviderCode }),
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
      setSelectedModels(planDetail.pricing.map((p) => p.providerModelId));
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
    if (step === 0 && !selectedPlanCode) {
      message.warning(t('onboard.selectPlanWarning'));
      return;
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
      const requestData: MaterializePlanRequest = {
        apiKeys: parsedApiKeys.map((k) => k.raw),
        endpoints: endpoints.filter((ep) => ep.checked && ep.url.trim()).map((ep) => ({ protocol: ep.protocol, url: ep.url.trim() })),
        models: selectedModels,
        channelName: selectedPlanName || planDetail.planName,
      };
      await catalogMaterializeApi.materializePlan(selectedPlanCode, requestData);
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
        <Text strong>{t('onboard.providerLabel')}</Text>
        <Select
          placeholder={t('onboard.providerRequired')}
          style={{ width: '100%', marginTop: 4 }}
          value={selectedProviderCode}
          onChange={(value) => {
            setSelectedProviderCode(value);
            setSelectedPlanCode(undefined);
            setSelectedPlanName(undefined);
            setEndpoints([]);
            setSelectedModels([]);
          }}
          loading={providersLoading}
          showSearch
          optionFilterProp="label"
          options={providers?.map((p) => ({ label: p.name, value: p.code }))}
        />
      </div>
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
          options={plans?.map((p) => ({ label: p.planName, value: p.planCode }))}
        />
      </div>
      {planDetail && (
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
              <Checkbox key={p.providerModelId} value={p.providerModelId}>
                <span style={{ fontWeight: 500 }}>{p.providerModelId}</span>
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
        <div>{renderStep0()}<div style={{ marginTop: 24, textAlign: 'right' }}><Button disabled={!selectedPlanCode} type="primary" onClick={handleNext}>{t('onboard.btnNext')}</Button></div></div>
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