import { useState, useCallback, useRef, useEffect } from 'react';
import { Modal, Select, Button, Space, Input, Typography, Empty, Alert, Row, Col, theme } from 'antd';
import { SendOutlined, StopOutlined, ClearOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { experienceApi } from '@/services/api/experience';
import { useProviderKeys } from '@/services/query';
import type { Provider } from '@/types/provider';
import type { ChatMessage, ExperienceModelResponse } from '@/types/experience';

const { Text, Paragraph } = Typography;
const { TextArea } = Input;

interface ProviderExperienceModalProps {
  open: boolean;
  provider: Provider | null;
  /** 预选的 API Key ID */
  apiKeyId?: number;
  /** 预选的模型 ID */
  modelId?: string;
  onClose: () => void;
}

/**
 * 供应商模型体验弹窗
 *
 * 使用已保存的供应商配置进行模型体验。
 * 支持自动填充供应商、API Key、模型。
 */
export function ProviderExperienceModal({
  open,
  provider,
  apiKeyId,
  modelId,
  onClose,
}: ProviderExperienceModalProps) {
  const { t } = useTranslation('experience');
  const { t: tp } = useTranslation('providers');
  const { token } = theme.useToken();

  // 数据状态
  const [models, setModels] = useState<ExperienceModelResponse[]>([]);
  const [selectedApiKey, setSelectedApiKey] = useState<number | undefined>();
  const [selectedModel, setSelectedModel] = useState<string>('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [usage, setUsage] = useState({ promptTokens: 0, completionTokens: 0 });
  const [error, setError] = useState<string | null>(null);

  // Refs
  const abortControllerRef = useRef<AbortController | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesRef = useRef<ChatMessage[]>([]);

  // 查询 API Keys
  const { data: keysData, isLoading: keysLoading } = useProviderKeys(provider?.id || 0, {
    enabled: open && !!provider?.id,
  });
  const apiKeys = keysData?.keys || [];

  // 同步 messages 到 ref
  useEffect(() => {
    messagesRef.current = messages;
  }, [messages]);

  // 加载模型列表
  useEffect(() => {
    if (open && provider?.id && provider.id > 0) {
      experienceApi.getProviderModels(provider.id)
        .then((data) => {
          setModels(data);
          // 如果传入了预选模型 ID，自动选中
          if (modelId && data.some(m => m.providerModelId === modelId)) {
            setSelectedModel(modelId);
          } else if (data.length > 0) {
            // 默认选中第一个模型
            setSelectedModel(data[0].providerModelId);
          }
        })
        .catch(() => setModels([]));
    }
  }, [open, provider?.id, modelId]);

  // 自动选择 API Key
  useEffect(() => {
    if (apiKeys.length > 0) {
      if (apiKeyId && apiKeys.some(k => k.id === apiKeyId)) {
        setSelectedApiKey(apiKeyId);
      } else {
        // 优先选择默认 Key
        const defaultKey = apiKeys.find(k => k.isDefault && k.state === 'ACTIVE');
        const activeKey = apiKeys.find(k => k.state === 'ACTIVE');
        setSelectedApiKey(defaultKey?.id || activeKey?.id || apiKeys[0]?.id);
      }
    }
  }, [apiKeys, apiKeyId]);

  // 自动滚动到底部
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // 重置状态
  useEffect(() => {
    if (!open) {
      setMessages([]);
      setUsage({ promptTokens: 0, completionTokens: 0 });
      setError(null);
      setInput('');
      setSelectedModel('');
      setSelectedApiKey(undefined);
      setModels([]);
    }
  }, [open]);

  // 发送消息
  const handleSend = useCallback(async () => {
    if (!input.trim() || isLoading || !provider || !selectedModel) return;

    const userMessage: ChatMessage = { role: 'user', content: input.trim() };
    const currentMessages = messagesRef.current;
    const newMessages = [...currentMessages, userMessage];
    const assistantMessage: ChatMessage = { role: 'assistant', content: '' };

    setMessages([...newMessages, assistantMessage]);
    setInput('');
    setIsLoading(true);
    setError(null);
    setUsage({ promptTokens: 0, completionTokens: 0 });

    abortControllerRef.current = new AbortController();

    let accumulatedContent = '';

    await experienceApi.chatStream(
      {
        providerId: provider.id,
        apiKeyId: selectedApiKey,
        model: selectedModel,
        messages: newMessages,
      },
      {
        onContent: (chunk) => {
          accumulatedContent += chunk;
          setMessages((prev) => {
            const updated = [...prev];
            if (updated.length > 0) {
              updated[updated.length - 1] = {
                ...updated[updated.length - 1],
                content: accumulatedContent,
              };
            }
            return updated;
          });
        },
        onUsage: (promptTokens, completionTokens) => {
          setUsage({ promptTokens, completionTokens });
        },
        onError: (message) => {
          setMessages((prev) => prev.slice(0, -1));
          setError(message);
          setIsLoading(false);
        },
        onDone: () => {
          setIsLoading(false);
        },
      },
      abortControllerRef.current.signal
    );
  }, [input, isLoading, provider, selectedApiKey, selectedModel]);

  // 停止生成
  const handleStop = useCallback(() => {
    abortControllerRef.current?.abort();
    setIsLoading(false);
  }, []);

  // 清空对话
  const handleClear = useCallback(() => {
    setMessages([]);
    setUsage({ promptTokens: 0, completionTokens: 0 });
    setError(null);
  }, []);

  // 键盘事件
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSend();
      }
    },
    [handleSend]
  );

  // 关闭时停止请求
  const handleModalClose = useCallback(() => {
    if (isLoading) {
      abortControllerRef.current?.abort();
      setIsLoading(false);
    }
    onClose();
  }, [isLoading, onClose]);

  // 弹窗标题
  const title = provider
    ? `${tp('provider.experience')} - ${provider.providerName}`
    : tp('provider.experience');

  return (
    <Modal
      title={title}
      open={open}
      onCancel={handleModalClose}
      width={640}
      footer={null}
      destroyOnClose
    >
      {!provider ? (
        <Empty description={t('errors.noProvider')} />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* 配置区：API Key 和模型选择 */}
          <Row gutter={16}>
            <Col span={12}>
              <div style={{ marginBottom: 4 }}>
                <Text type="secondary">{t('config.apiKey')}</Text>
              </div>
              <Select
                style={{ width: '100%' }}
                placeholder={t('config.selectApiKey')}
                value={selectedApiKey}
                onChange={setSelectedApiKey}
                loading={keysLoading}
                options={apiKeys
                  .filter(k => k.state === 'ACTIVE')
                  .map(k => ({
                    value: k.id,
                    label: (
                      <Space>
                        <span>{k.keyName}</span>
                        {k.isDefault && (
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            ({t('config.defaultKey')})
                          </Text>
                        )}
                      </Space>
                    ),
                  }))}
              />
            </Col>
            <Col span={12}>
              <div style={{ marginBottom: 4 }}>
                <Text type="secondary">{t('config.model')}</Text>
              </div>
              <Select
                style={{ width: '100%' }}
                placeholder={t('config.selectModel')}
                value={selectedModel || undefined}
                onChange={setSelectedModel}
                options={models.map((m) => ({
                  value: m.providerModelId,
                  label: m.displayName || m.providerModelId,
                }))}
                showSearch
                filterOption={(input, option) =>
                  (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                }
              />
            </Col>
          </Row>

          {/* 对话区域 */}
          <div
            style={{
              height: 320,
              overflow: 'auto',
              border: `1px solid ${token.colorBorder}`,
              borderRadius: token.borderRadius,
              padding: 12,
            }}
          >
            {messages.length === 0 ? (
              <Empty description={t('chat.empty')} style={{ marginTop: 80 }} />
            ) : (
              <>
                {messages.map((msg, index) => (
                  <div
                    key={index}
                    style={{
                      marginBottom: 12,
                      display: 'flex',
                      justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
                    }}
                  >
                    <div
                      style={{
                        maxWidth: '80%',
                        padding: '8px 12px',
                        borderRadius: token.borderRadius,
                        backgroundColor: msg.role === 'user' ? token.colorPrimaryBg : token.colorBgContainer,
                        border: `1px solid ${msg.role === 'user' ? token.colorPrimaryBorder : token.colorBorder}`,
                      }}
                    >
                      <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                        {msg.content || (isLoading && index === messages.length - 1 ? '...' : '')}
                      </Paragraph>
                    </div>
                  </div>
                ))}
                <div ref={messagesEndRef} />
              </>
            )}
          </div>

          {/* 统计信息 */}
          {(usage.promptTokens > 0 || usage.completionTokens > 0) && (
            <Row gutter={16}>
              <Col>
                <Text type="secondary">
                  {t('stats.promptTokens')}: <Text strong>{usage.promptTokens}</Text>
                </Text>
              </Col>
              <Col>
                <Text type="secondary">
                  {t('stats.completionTokens')}: <Text strong>{usage.completionTokens}</Text>
                </Text>
              </Col>
            </Row>
          )}

          {/* 错误提示 */}
          {error && <Alert message={error} type="error" showIcon closable onClose={() => setError(null)} />}

          {/* 输入区域 */}
          <Space.Compact style={{ width: '100%' }}>
            <TextArea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={t('input.placeholder')}
              autoSize={{ minRows: 1, maxRows: 3 }}
              style={{ resize: 'none' }}
              disabled={!selectedModel || isLoading}
            />
            {isLoading ? (
              <Button type="primary" danger icon={<StopOutlined />} onClick={handleStop}>
                {t('input.stop')}
              </Button>
            ) : (
              <Button
                type="primary"
                icon={<SendOutlined />}
                onClick={handleSend}
                disabled={!selectedModel || !input.trim()}
              >
                {t('input.send')}
              </Button>
            )}
            <Button icon={<ClearOutlined />} onClick={handleClear} disabled={messages.length === 0 || isLoading}>
              {t('chat.clear')}
            </Button>
          </Space.Compact>

          <Text type="secondary" style={{ fontSize: 12 }}>
            {t('input.hint')}
          </Text>
        </div>
      )}
    </Modal>
  );
}

export type { ProviderExperienceModalProps };
