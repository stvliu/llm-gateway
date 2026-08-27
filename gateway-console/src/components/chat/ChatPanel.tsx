/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useRef, useEffect, useState } from 'react';
import {
  App,
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
import type { TextAreaRef } from 'antd/es/input/TextArea';
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
import { useModels } from '@/services/query/useModels';
import { useMyUserApiKeys } from '@/services/query/useUserApiKeys';
import { userApiKeyApi } from '@/services/api/userApiKey';
import { theme } from 'antd';

const { TextArea } = Input;
const { Text } = Typography;

/**
 * 聊天面板组件
 */
export function ChatPanel() {
  const { t } = useTranslation('chat');
  const { token } = theme.useToken();
  const [inputValue, setInputValue] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<TextAreaRef>(null);

  const {
    messages,
    currentModel,
    availableModels,
    isLoading,
    addMessage,
    updateLastMessage,
    clearMessages,
    setCurrentModel,
    setAvailableModels,
    setLoading,
    setStreaming,
    setOpen,
  } = useChatStore();
  const { message } = App.useApp();

  // 数据面认证用的真实 API Key（登录 token 无法通过网关认证）
  const [chatApiKey, setChatApiKey] = useState<string>();

  // 可用模型：从真实模型接口加载（活跃模型），替代早期硬编码列表
  const { data: modelsData } = useModels({ limit: 1000 });

  useEffect(() => {
    const activeModels = modelsData?.items?.filter((m) => m.state === 'ACTIVE') ?? [];
    if (activeModels.length === 0) return;
    const options = activeModels.map((m) => ({
      id: m.modelName,
      name: m.displayName || m.modelName,
      provider: m.providerName ?? '',
    }));
    setAvailableModels(options);
    // 当前模型不在列表中时自动切换为第一个
    const cur = useChatStore.getState().currentModel;
    if (!options.some((o) => o.id === cur)) {
      setCurrentModel(options[0].id);
    }
  }, [modelsData, setAvailableModels, setCurrentModel]);

  // API Key：自动使用当前用户第一个可用 Key（复用 ApiKeySelector 的取明文模式）
  const { data: myKeys } = useMyUserApiKeys();

  useEffect(() => {
    if (chatApiKey || !myKeys || myKeys.length === 0) return;
    userApiKeyApi
      .getDetail(myKeys[0].id)
      .then((detail) => setChatApiKey(detail.keyPlain))
      .catch(() => {
        // 获取 Key 明文失败时保持无 Key 状态，由发送保护提示
      });
  }, [myKeys, chatApiKey]);

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

    if (!chatApiKey) {
      message.warning(t('noApiKey', { defaultValue: '请先在快速开始页创建 API Key' }));
      return;
    }
    if (!currentModel) {
      message.warning(t('noModel', { defaultValue: '暂无可用的模型' }));
      return;
    }

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
        chatApiKey,
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
    } catch {
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
                  background: msg.role === 'user' ? token.colorPrimary : token.colorBgLayout,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                {msg.role === 'user' ? (
                  <UserOutlined style={{ color: token.colorTextLightSolid, fontSize: 14 }} />
                ) : (
                  <RobotOutlined style={{ color: token.colorPrimary, fontSize: 14 }} />
                )}
              </div>
              <div
                style={{
                  maxWidth: '80%',
                  padding: '8px 12px',
                  borderRadius: 8,
                  background: msg.role === 'user' ? token.colorPrimary : token.colorBgLayout,
                  color: msg.role === 'user' ? token.colorTextLightSolid : token.colorText,
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
