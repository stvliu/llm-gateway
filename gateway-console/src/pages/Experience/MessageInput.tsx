import { useState, useCallback } from 'react';
import { Input, Button, Space, Alert } from 'antd';
import { SendOutlined, StopOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { TextArea } = Input;

interface MessageInputProps {
  onSend: (content: string) => void;
  onStop: () => void;
  isLoading: boolean;
  disabled: boolean;
  error: string | null;
}

/**
 * 消息输入组件
 *
 * 用于输入和发送聊天消息。
 */
export function MessageInput({
  onSend,
  onStop,
  isLoading,
  disabled,
  error,
}: MessageInputProps) {
  const { t } = useTranslation('experience');
  const [input, setInput] = useState('');

  const handleSend = useCallback(() => {
    const trimmed = input.trim();
    if (trimmed && !isLoading && !disabled) {
      onSend(trimmed);
      setInput('');
    }
  }, [input, isLoading, disabled, onSend]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSend();
      }
    },
    [handleSend]
  );

  return (
    <div>
      {error && (
        <Alert
          message={error}
          type="error"
          showIcon
          closable
          style={{ marginBottom: 12 }}
        />
      )}

      <Space.Compact style={{ width: '100%' }}>
        <TextArea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={t('input.placeholder')}
          disabled={disabled || isLoading}
          autoSize={{ minRows: 1, maxRows: 4 }}
          style={{ resize: 'none' }}
        />
        {isLoading ? (
          <Button
            type="primary"
            danger
            icon={<StopOutlined />}
            onClick={onStop}
          >
            {t('input.stop')}
          </Button>
        ) : (
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={handleSend}
            disabled={disabled || !input.trim()}
          >
            {t('input.send')}
          </Button>
        )}
      </Space.Compact>

      <div style={{ marginTop: 8, color: '#999', fontSize: 12 }}>
        {t('input.hint')}
      </div>
    </div>
  );
}
