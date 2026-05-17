import { useState, useCallback, useRef, useEffect } from 'react';
import { Select, Button, Space, Input, Typography, Empty, Alert, Row, Col } from 'antd';
import { SendOutlined, StopOutlined, ClearOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { experienceApi } from '@/services/api/experience';
import type { Provider } from '@/types/provider';
import type { ChatMessage, ExperienceModelResponse } from '@/types/experience';

const { Text, Paragraph } = Typography;
const { TextArea } = Input;

interface ProviderExperienceTabProps {
  provider: Provider | null;
}

/**
 * 供应商体验标签页
 *
 * 使用已保存的供应商配置进行模型体验。
 */
export function ProviderExperienceTab({ provider }: ProviderExperienceTabProps) {
  const { t } = useTranslation('experience');
  const [models, setModels] = useState<ExperienceModelResponse[]>([]);
  const [selectedModel, setSelectedModel] = useState<string>('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [usage, setUsage] = useState({ promptTokens: 0, completionTokens: 0 });
  const [error, setError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  // 使用 ref 存储消息列表，避免 useCallback 依赖 messages 导致频繁重建
  const messagesRef = useRef<ChatMessage[]>([]);

  // 同步 messages 到 ref
  useEffect(() => {
    messagesRef.current = messages;
  }, [messages]);

  // 加载模型列表
  useEffect(() => {
    if (provider?.id && provider.id > 0) {
      experienceApi.getProviderModels(provider.id)
        .then(setModels)
        .catch(() => setModels([]));
    }
  }, [provider?.id]);

  // 自动滚动到底部
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

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
  }, [input, isLoading, provider, selectedModel]);

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

  if (!provider) {
    return <Empty description={t('errors.noProvider')} />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, height: '100%' }}>
      {/* 模型选择 */}
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

      {/* 对话区域 */}
      <div
        style={{
          flex: 1,
          overflow: 'auto',
          border: '1px solid #f0f0f0',
          borderRadius: 8,
          padding: 12,
          minHeight: 200,
          maxHeight: 400,
          backgroundColor: '#fafafa',
        }}
      >
        {messages.length === 0 ? (
          <Empty description={t('chat.empty')} style={{ marginTop: 60 }} />
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
                    borderRadius: 8,
                    backgroundColor: msg.role === 'user' ? '#e6f7ff' : '#fff',
                    border: msg.role === 'assistant' ? '1px solid #f0f0f0' : 'none',
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
  );
}
