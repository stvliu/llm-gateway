import { useState, useEffect, useMemo } from 'react';
import { Steps, Select, Input, Checkbox, Button, Space, Typography, App, Spin, Tag, Divider, theme } from 'antd';
import { PlusOutlined, DeleteOutlined, CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { providerCatalogApi, planCatalogApi, catalogMaterializeApi } from '@/services/api/catalog';
import type { MaterializePlanRequest } from '@/types/catalog';

const { Text, Title } = Typography;
const { TextArea } = Input;

interface QuickOnboardModeProps {
  onComplete: () => void;
  onCancel: () => void;
  /** 预选套餐编码（从目录入口传入） */
  initialPlanCode?: string;
  /** 预选套餐名称（用于显示） */
  initialPlanName?: string;
}

/** 编辑态端点 */
interface EditableEndpoint {
  id: string;
  protocol: string;
  url: string;
  checked: boolean;
}

/**
 * 快速接入模式——四步完成渠道创建
 * 支持从目录入口预选套餐
 */
export function QuickOnboardMode({ onComplete, initialPlanCode, initialPlanName }: QuickOnboardModeProps) {
  const { message } = App.useApp();
  const { token } = theme.useToken();
  const queryClient = useQueryClient();
  const [step, setStep] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  // ===== Step 0: 选择套餐 =====
  const [selectedProviderCode, setSelectedProviderCode] = useState<string>();
  const [selectedPlanCode, setSelectedPlanCode] = useState<string>();
  const [selectedPlanName, setSelectedPlanName] = useState<string>();

  // ===== Step 1: 配置端点与模型 =====
  const [endpoints, setEndpoints] = useState<EditableEndpoint[]>([]);
  const [selectedModels, setSelectedModels] = useState<string[]>([]);
  const [customModelInput, setCustomModelInput] = useState('');

  // ===== Step 2: 配置凭证 =====
  const [apiKeyInput, setApiKeyInput] = useState('');

  // ===== 数据查询 =====

  // 供应商目录列表
  const { data: providers, isLoading: providersLoading } = useQuery({
    queryKey: ['provider-catalog', 'list'],
    queryFn: () => providerCatalogApi.list(),
  });

  // 套餐目录列表（按供应商过滤）
  const { data: plans, isLoading: plansLoading } = useQuery({
    queryKey: ['plan-catalog', 'list', selectedProviderCode],
    queryFn: () => planCatalogApi.list({ providerCode: selectedProviderCode }),
    enabled: !!selectedProviderCode,
  });

  // 套餐详情
  const { data: planDetail, isLoading: planDetailLoading } = useQuery({
    queryKey: ['plan-catalog', 'detail', selectedPlanCode],
    queryFn: () => planCatalogApi.getDetail(selectedPlanCode!),
    enabled: !!selectedPlanCode,
  });

  // ===== 初始化预选套餐 =====
  useEffect(() => {
    if (initialPlanCode) {
      // 如果有预选套餐，直接设置并跳到下一步
      setSelectedPlanCode(initialPlanCode);
      if (initialPlanName) {
        setSelectedPlanName(initialPlanName);
      }
    }
  }, [initialPlanCode, initialPlanName]);

  // 当套餐详情加载后，初始化端点和模型
  useEffect(() => {
    if (planDetail) {
      // 初始化端点
      const initialEndpoints: EditableEndpoint[] = planDetail.endpoints.map((ep, index) => ({
        id: `ep-${index}`,
        protocol: ep.protocol || 'openai',
        url: ep.url,
        checked: true,
      }));
      setEndpoints(initialEndpoints);

      // 初始化模型（从 pricing 中提取，默认全选）
      const modelNames = planDetail.pricing.map((p) => p.modelName);
      setSelectedModels(modelNames);

      // 设置套餐名称
      setSelectedPlanName((prev) => prev || planDetail.planName);

      // 从套餐编码中提取供应商编码
      const providerCode = planDetail.providerCode;
      if (providerCode) {
        setSelectedProviderCode(providerCode);
      }
    }
  }, [planDetail]);

  // ===== 解析 API Keys =====
  const parsedApiKeys = useMemo(() => {
    if (!apiKeyInput.trim()) return [];

    // 支持分号和逗号分隔，以及换行
    const keys = apiKeyInput
      .split(/[;,\n]+/)
      .map((k) => k.trim())
      .filter((k) => k.length > 0);

    return keys.map((raw, index) => {
      const masked = maskApiKey(raw);
      const valid = raw.length >= 8;
      const warning = !valid ? 'API Key 长度过短（<8位）' : undefined;

      return {
        id: `key-${index}`,
        raw,
        masked,
        valid,
        warning,
      };
    });
  }, [apiKeyInput]);

  // ===== API Key 脱敏 =====
  function maskApiKey(key: string): string {
    if (key.length <= 8) {
      return '****';
    }
    const prefix = key.slice(0, 4);
    const suffix = key.slice(-4);
    return `${prefix}****${suffix}`;
  }

  // ===== 端点操作 =====
  const handleAddEndpoint = () => {
    const newEndpoint: EditableEndpoint = {
      id: `ep-new-${Date.now()}`,
      protocol: 'openai',
      url: '',
      checked: true,
    };
    setEndpoints([...endpoints, newEndpoint]);
  };

  const handleRemoveEndpoint = (id: string) => {
    setEndpoints(endpoints.filter((ep) => ep.id !== id));
  };

  const handleEndpointChange = (id: string, field: 'protocol' | 'url', value: string) => {
    setEndpoints(endpoints.map((ep) =>
      ep.id === id ? { ...ep, [field]: value } : ep
    ));
  };

  const handleEndpointCheck = (id: string, checked: boolean) => {
    setEndpoints(endpoints.map((ep) =>
      ep.id === id ? { ...ep, checked } : ep
    ));
  };

  // ===== 模型操作 =====
  const handleAddCustomModel = () => {
    const modelName = customModelInput.trim();
    if (modelName && !selectedModels.includes(modelName)) {
      setSelectedModels([...selectedModels, modelName]);
      setCustomModelInput('');
    }
  };

  const handleRemoveModel = (modelName: string) => {
    setSelectedModels(selectedModels.filter((m) => m !== modelName));
  };

  // ===== 导航 =====
  const handleNext = () => {
    if (step === 0) {
      if (!selectedPlanCode) {
        message.warning('请选择套餐');
        return;
      }
    } else if (step === 1) {
      const checkedEndpoints = endpoints.filter((ep) => ep.checked && ep.url.trim());
      if (checkedEndpoints.length === 0) {
        message.warning('请至少配置一个有效端点');
        return;
      }
      if (selectedModels.length === 0) {
        message.warning('请至少选择一个模型');
        return;
      }
    }
    setStep(step + 1);
  };

  // ===== 创建渠道 =====
  const handleFinish = async () => {
    if (!selectedPlanCode || !planDetail) {
      message.error('未选择套餐');
      return;
    }

    const invalidKeys = parsedApiKeys.filter((k) => !k.valid);
    if (invalidKeys.length > 0) {
      message.warning('存在无效的 API Key，请检查');
      return;
    }

    try {
      setSubmitting(true);

      // 构建物化请求
      const requestData: MaterializePlanRequest = {
        apiKeys: parsedApiKeys.map((k) => k.raw),
        endpoints: endpoints
          .filter((ep) => ep.checked && ep.url.trim())
          .map((ep) => ({
            protocol: ep.protocol,
            url: ep.url.trim(),
          })),
        models: selectedModels,
        channelName: selectedPlanName || planDetail.planName,
      };

      // 调用物化 API
      await catalogMaterializeApi.materializePlan(selectedPlanCode, requestData);

      message.success('渠道创建成功');
      queryClient.invalidateQueries({ queryKey: ['channels'] });
      queryClient.invalidateQueries({ queryKey: ['providers'] });
      queryClient.invalidateQueries({ queryKey: ['plan-catalog'] });
      onComplete();
    } catch (error) {
      message.error(`创建失败：${error instanceof Error ? error.message : '未知错误'}`);
    } finally {
      setSubmitting(false);
    }
  };

  // ===== 更换套餐时重置端点和模型 =====
  const handlePlanChange = (planCode: string) => {
    const plan = plans?.find((p) => p.planCode === planCode);
    setSelectedPlanCode(planCode);
    setSelectedPlanName(plan?.planName);
    setEndpoints([]);
    setSelectedModels([]);
  };

  // ===== 渲染步骤内容 =====

  // Step 0: 选择套餐
  const renderStep0 = () => (
    <div>
      <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
        选择供应商和套餐模板，系统将自动填充端点和模型配置
      </Text>

      <div style={{ marginBottom: 16 }}>
        <Text strong>供应商</Text>
        <Select
          placeholder="选择供应商"
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
          options={providers?.map((p) => ({
            label: p.name,
            value: p.code,
          }))}
        />
      </div>

      <div style={{ marginBottom: 16 }}>
        <Text strong>套餐</Text>
        <Select
          placeholder={selectedProviderCode ? '选择套餐' : '请先选择供应商'}
          style={{ width: '100%', marginTop: 4 }}
          value={selectedPlanCode}
          onChange={handlePlanChange}
          loading={plansLoading}
          disabled={!selectedProviderCode}
          showSearch
          optionFilterProp="label"
          options={plans?.map((p) => ({
            label: p.planName,
            value: p.planCode,
          }))}
        />
      </div>

      {/* 套餐预览 */}
      {planDetail && (
        <div style={{ marginTop: 16, padding: 12, background: token.colorBgLayout, borderRadius: 8 }}>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>套餐预览</Text>
          <Space direction="vertical" size="small">
            <Text>
              <Text type="secondary">套餐名称：</Text>
              {planDetail.planName}
            </Text>
            <Text>
              <Text type="secondary">计费模式：</Text>
              <Tag color={planDetail.billingMode === 'pay_as_you_go' ? 'green' : 'blue'}>
                {planDetail.billingMode}
              </Tag>
            </Text>
            <Text>
              <Text type="secondary">端点数量：</Text>
              {planDetail.endpoints.length} 个
            </Text>
            <Text>
              <Text type="secondary">模型数量：</Text>
              {planDetail.pricing.length} 个
            </Text>
          </Space>
        </div>
      )}

      {planDetailLoading && (
        <div style={{ textAlign: 'center', padding: 24 }}>
          <Spin />
        </div>
      )}
    </div>
  );

  // Step 1: 配置端点与模型
  const renderStep1 = () => (
    <div>
      {/* 端点配置 */}
      <Title level={5}>端点配置</Title>
      <div style={{ marginBottom: 8 }}>
        <Text type="secondary">勾选需要启用的端点，可编辑协议和 URL</Text>
      </div>

      <div style={{ marginBottom: 16 }}>
        {endpoints.map((ep) => (
          <div key={ep.id} style={{ display: 'flex', alignItems: 'center', marginBottom: 8, gap: 8 }}>
            <Checkbox
              checked={ep.checked}
              onChange={(e) => handleEndpointCheck(ep.id, e.target.checked)}
            />
            <Select
              value={ep.protocol}
              onChange={(value) => handleEndpointChange(ep.id, 'protocol', value)}
              style={{ width: 120 }}
              options={[
                { label: 'OpenAI', value: 'openai' },
                { label: 'Anthropic', value: 'anthropic' },
              ]}
            />
            <Input
              placeholder="https://api.example.com/v1"
              value={ep.url}
              onChange={(e) => handleEndpointChange(ep.id, 'url', e.target.value)}
              style={{ flex: 1 }}
            />
            <Button
              type="text"
              danger
              icon={<DeleteOutlined />}
              onClick={() => handleRemoveEndpoint(ep.id)}
            />
          </div>
        ))}
        <Button type="dashed" icon={<PlusOutlined />} onClick={handleAddEndpoint}>
          新增端点
        </Button>
      </div>

      <Divider />

      {/* 模型配置 */}
      <Title level={5}>模型配置</Title>
      <div style={{ marginBottom: 8 }}>
        <Text type="secondary">勾选需要启用的模型（已选 {selectedModels.length} 个）</Text>
      </div>

      <div style={{ maxHeight: 200, overflow: 'auto', marginBottom: 16 }}>
        <Checkbox.Group
          value={selectedModels}
          onChange={(values: (string | number | boolean)[]) => setSelectedModels(values.filter((v): v is string => typeof v === 'string'))}
          style={{ width: '100%' }}
        >
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {selectedModels.map((modelName) => (
              <Tag
                key={modelName}
                closable
                onClose={(e) => {
                  e.preventDefault();
                  handleRemoveModel(modelName);
                }}
                style={{ padding: '4px 8px' }}
              >
                <Checkbox value={modelName} style={{ marginRight: 4 }} />
                {modelName}
              </Tag>
            ))}
          </div>
        </Checkbox.Group>
      </div>

      <div style={{ display: 'flex', gap: 8 }}>
        <Input
          placeholder="输入自定义模型名称"
          value={customModelInput}
          onChange={(e) => setCustomModelInput(e.target.value)}
          onPressEnter={handleAddCustomModel}
          style={{ width: 240 }}
        />
        <Button onClick={handleAddCustomModel}>添加模型</Button>
      </div>
    </div>
  );

  // Step 2: 配置凭证
  const renderStep2 = () => (
    <div>
      <Title level={5}>API Key 配置</Title>
      <div style={{ marginBottom: 8 }}>
        <Text type="secondary">支持分号、逗号或换行分隔多个 API Key</Text>
      </div>

      <TextArea
        placeholder="sk-xxxxxxxxxxxxx&#10;sk-yyyyyyyyyyyyy"
        value={apiKeyInput}
        onChange={(e) => setApiKeyInput(e.target.value)}
        rows={6}
        style={{ marginBottom: 16 }}
      />

      {/* 解析预览 */}
      {parsedApiKeys.length > 0 && (
        <div style={{ marginTop: 16 }}>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>
            解析预览（{parsedApiKeys.length} 个）
          </Text>
          <div style={{ maxHeight: 200, overflow: 'auto' }}>
            {parsedApiKeys.map((key) => (
              <div
                key={key.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '8px 12px',
                  marginBottom: 4,
                  background: key.valid ? token.colorSuccessBg : token.colorErrorBg,
                  borderRadius: 4,
                  border: `1px solid ${key.valid ? token.colorSuccessBorder : token.colorErrorBorder}`,
                }}
              >
                <Space>
                  {key.valid ? (
                    <CheckCircleOutlined style={{ color: token.colorSuccess }} />
                  ) : (
                    <ExclamationCircleOutlined style={{ color: token.colorError }} />
                  )}
                  <Text code>{key.masked}</Text>
                </Space>
                {key.warning && (
                  <Text type="danger" style={{ fontSize: 12 }}>
                    {key.warning}
                  </Text>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );

  // Step 3: 确认创建
  const renderStep3 = () => {
    const checkedEndpoints = endpoints.filter((ep) => ep.checked && ep.url.trim());
    const validKeys = parsedApiKeys.filter((k) => k.valid);

    return (
      <div>
        <Title level={5}>确认创建</Title>
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">请确认以下配置信息，点击"创建渠道"完成创建</Text>
        </div>

        <div style={{ marginTop: 16, padding: 16, background: token.colorBgLayout, borderRadius: 8 }}>
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            {/* 套餐信息 */}
            <div>
              <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>套餐名称</Text>
              <Text strong>{selectedPlanName || planDetail?.planName || '-'}</Text>
            </div>

            {/* 端点统计 */}
            <div>
              <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>端点数量</Text>
              <Space>
                <Tag color="blue">{checkedEndpoints.length} 个</Tag>
                {checkedEndpoints.length > 0 && (
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {checkedEndpoints.map((ep) => ep.url).join(', ')}
                  </Text>
                )}
              </Space>
            </div>

            {/* 模型统计 */}
            <div>
              <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>模型数量</Text>
              <Tag color="green">{selectedModels.length} 个</Tag>
            </div>

            {/* API Key 统计 */}
            <div>
              <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>API Key 数量</Text>
              <Space>
                <Tag color={validKeys.length > 0 ? 'green' : 'red'}>{parsedApiKeys.length} 个</Tag>
                {parsedApiKeys.length > 0 && (
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {parsedApiKeys.map((k) => k.masked).join(', ')}
                  </Text>
                )}
              </Space>
            </div>
          </Space>
        </div>

        {parsedApiKeys.length === 0 && (
          <div style={{ marginTop: 12, padding: 12, background: token.colorWarningBg, borderRadius: 8 }}>
            <Text type="warning">未配置 API Key，创建后渠道将无法正常使用，请在创建后及时配置</Text>
          </div>
        )}
      </div>
    );
  };

  // 渲染步骤指示器
  const stepItems = [
    { title: '选择套餐' },
    { title: '端点与模型' },
    { title: '粘贴 Key' },
    { title: '确认创建' },
  ];

  return (
    <div>
      <Steps current={step} size="small" style={{ marginBottom: 24 }} items={stepItems} />

      {step === 0 && (
        <div>
          {renderStep0()}
          <div style={{ marginTop: 24, textAlign: 'right' }}>
            <Button disabled={!selectedPlanCode} type="primary" onClick={handleNext}>
              下一步
            </Button>
          </div>
        </div>
      )}

      {step === 1 && (
        <div>
          {renderStep1()}
          <div style={{ marginTop: 24, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setStep(0)}>上一步</Button>
              <Button type="primary" onClick={handleNext}>
                下一步
              </Button>
            </Space>
          </div>
        </div>
      )}

      {step === 2 && (
        <div>
          {renderStep2()}
          <div style={{ marginTop: 24, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setStep(1)}>上一步</Button>
              <Button type="primary" onClick={handleNext}>
                下一步
              </Button>
            </Space>
          </div>
        </div>
      )}

      {step === 3 && (
        <div>
          {renderStep3()}
          <div style={{ marginTop: 24, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setStep(2)}>上一步</Button>
              <Button
                type="primary"
                onClick={handleFinish}
                loading={submitting}
              >
                创建渠道
              </Button>
            </Space>
          </div>
        </div>
      )}
    </div>
  );
}
