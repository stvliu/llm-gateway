import { useRef, useEffect, useState } from 'react';
import {
  Card,
  Input,
  Button,
  Select,
  Space,
  Typography,
  Empty,
  Spin,
  Tooltip,
} from 'antd';
import {
  SendOutlined,
  ClearOutlined,
  RobotOutlined,
  UserOutlined,
  CloseOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useChatStore } from '@/stores/chatStore';
import { chatService } from '@/services/chatService';

const { TextArea } = Input;
const { Text } = Typography;

/**
 * 聊天面板组件
 */
export function ChatPanel() {
  const { t } = useTranslation('chat');
  const [inputValue, setInputValue] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const {
    messages,
    currentModel,
    availableModels,
    isLoading,
    addMessage,
    updateLastMessage,
    clearMessages,
    setCurrentModel,
    setLoading,
    setStreaming,
    setOpen,
  } = useChatStore();

  // 自动滚动到底部
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 发送消息
  const handleSend = async () => {
    const content = inputValue.trim();
    if (!content || isLoading) return;

    setInputValue('');
    addMessage({ role: 'user', content });

    // 添加空的 assistant 消息占位
    addMessage({ role: 'assistant', content: '', isStreaming: true });
    setLoading(true);

    let fullResponse = '';

    try {
      await chatService.streamChat(
        [...messages, { id: '', role: 'user' as const, content, timestamp: Date.now() }],
        currentModel,
        {
          onToken: (token) => {
            fullResponse += token;
            updateLastMessage(fullResponse);
          },
          onComplete: () => {
            setStreaming(false);
            setLoading(false);
          },
          onError: (error) => {
            updateLastMessage(t('error.apiError', { message: error.message }));
            setStreaming(false);
            setLoading(false);
          },
        }
      );
    } catch (error) {
      updateLastMessage(t('error.unexpected'));
      setLoading(false);
    }
  };

  // 处理键盘事件
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // 清空对话
  const handleClear = () => {
    clearMessages();
  };

  // 关闭面板
  const handleClose = () => {
    setOpen(false);
  };

  return (
    <Card
      title={
        <Space>
          <RobotOutlined />
          <span>{t('title')}</span>
        </Space>
      }
      extra={
        <Space>
          <Select
            value={currentModel}
            onChange={setCurrentModel}
            size="small"
            style={{ width: 140 }}
            options={availableModels.map((m) => ({
              value: m.id,
              label: m.name,
            }))}
          />
          <Tooltip title={t('clear')}>
            <Button
              type="text"
              size="small"
              icon={<ClearOutlined />}
              onClick={handleClear}
              disabled={messages.length === 0}
            />
          </Tooltip>
          <Tooltip title={t('close')}>
            <Button
              type="text"
              size="small"
              icon={<CloseOutlined />}
              onClick={handleClose}
            />
          </Tooltip>
        </Space>
      }
      styles={{
        body: {
          display: 'flex',
          flexDirection: 'column',
          height: 'calc(100% - 57px)',
          padding: '12px',
        },
      }}
      style={{ width: 380, height: 520 }}
    >
      {/* 消息列表 */}
      <div
        style={{
          flex: 1,
          overflowY: 'auto',
          marginBottom: 12,
          paddingRight: 4,
        }}
      >
        {messages.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={t('emptyMessage')}
            style={{ marginTop: 80 }}
          />
        ) : (
          messages.map((msg) => (
            <div
              key={msg.id}
              style={{
                display: 'flex',
                gap: 8,
                marginBottom: 12,
                flexDirection: msg.role === 'user' ? 'row-reverse' : 'row',
              }}
            >
              <div
                style={{
                  width: 28,
                  height: 28,
                  borderRadius: '50%',
                  background: msg.role === 'user' ? '#1890ff' : '#87e8de',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                {msg.role === 'user' ? (
                  <UserOutlined style={{ color: '#fff', fontSize: 14 }} />
                ) : (
                  <RobotOutlined style={{ color: '#006d75', fontSize: 14 }} />
                )}
              </div>
              <div
                style={{
                  maxWidth: '80%',
                  padding: '8px 12px',
                  borderRadius: 8,
                  background: msg.role === 'user' ? '#1890ff' : '#f5f5f5',
                  color: msg.role === 'user' ? '#fff' : '#1f1f1f',
                  wordBreak: 'break-word',
                }}
              >
                {msg.isStreaming && msg.content === '' ? (
                  <Spin size="small" />
                ) : (
                  <Text
                    style={{
                      color: 'inherit',
                      whiteSpace: 'pre-wrap',
                    }}
                  >
                    {msg.content}
                    {msg.isStreaming && (
                      <span className="typing-cursor">▌</span>
                    )}
                  </Text>
                )}
              </div>
            </div>
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* 输入区域 */}
      <Space.Compact style={{ width: '100%' }}>
        <TextArea
          ref={inputRef}
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={t('inputPlaceholder')}
          autoSize={{ minRows: 1, maxRows: 3 }}
          style={{ borderRadius: '6px 0 0 6px' }}
          disabled={isLoading}
        />
        <Button
          type="primary"
          icon={<SendOutlined />}
          onClick={handleSend}
          loading={isLoading}
          disabled={!inputValue.trim()}
          style={{ height: 'auto', borderRadius: '0 6px 6px 0' }}
        />
      </Space.Compact>

      {/* 提示文字 */}
      <Text type="secondary" style={{ fontSize: 11, marginTop: 8, display: 'block' }}>
        {t('inputHint')}
      </Text>

      {/* 打字光标样式 */}
      <style>{`
        .typing-cursor {
          animation: blink 1s infinite;
        }
        @keyframes blink {
          0%, 50% { opacity: 1; }
          51%, 100% { opacity: 0; }
        }
      `}</style>
    </Card>
  );
}
