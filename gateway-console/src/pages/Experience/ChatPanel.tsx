import { useRef, useEffect, useMemo } from 'react';
import { List, Avatar, Typography, Empty, Spin } from 'antd';
import { UserOutlined, RobotOutlined, LoadingOutlined } from '@ant-design/icons';
import type { ChatMessage } from '@/types/experience';

const { Text, Paragraph } = Typography;

interface ChatPanelProps {
  messages: ChatMessage[];
  isLoading: boolean;
}

/**
 * 聊天面板组件
 *
 * 显示对话消息列表。
 */
export function ChatPanel({ messages, isLoading }: ChatPanelProps) {
  const listRef = useRef<HTMLDivElement>(null);

  // 自动滚动到底部
  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [messages]);

  if (messages.length === 0) {
    return (
      <div
        style={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 24,
        }}
      >
        <Empty description="开始对话以体验模型效果" />
      </div>
    );
  }

  return (
    <div
      ref={listRef}
      style={{
        flex: 1,
        overflow: 'auto',
        padding: 16,
      }}
    >
      <List
        dataSource={messages}
        renderItem={(msg, index) => (
          <List.Item
            key={index}
            style={{
              border: 'none',
              padding: '8px 0',
            }}
          >
            <div
              style={{
                display: 'flex',
                gap: 12,
                width: '100%',
                flexDirection: msg.role === 'user' ? 'row-reverse' : 'row',
              }}
            >
              <Avatar
                icon={msg.role === 'user' ? <UserOutlined /> : <RobotOutlined />}
                style={{
                  backgroundColor: msg.role === 'user' ? '#1890ff' : '#87d068',
                  flexShrink: 0,
                }}
              />
              <div
                style={{
                  maxWidth: '80%',
                  backgroundColor: msg.role === 'user' ? '#e6f7ff' : '#f6f6f6',
                  padding: '8px 12px',
                  borderRadius: 8,
                  wordBreak: 'break-word',
                }}
              >
                {msg.role === 'assistant' && index === messages.length - 1 && isLoading ? (
                  <div>
                    {msg.content ? (
                      <MessageContent content={msg.content} />
                    ) : (
                      <Spin indicator={<LoadingOutlined spin />} size="small" />
                    )}
                    <Text type="secondary" style={{ fontSize: 12, marginLeft: 4 }}>
                      ▌
                    </Text>
                  </div>
                ) : (
                  <MessageContent content={msg.content} />
                )}
              </div>
            </div>
          </List.Item>
        )}
      />
    </div>
  );
}

/** 消息内容组件（支持代码块渲染） */
function MessageContent({ content }: { content: string }) {
  // 转义 HTML 特殊字符，防止 XSS
  const escapeHtml = (text: string): string => {
    const htmlEntities: Record<string, string> = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
    };
    return text.replace(/[&<>"']/g, (char) => htmlEntities[char] || char);
  };

  // 使用 useMemo 优化渲染
  const renderedContent = useMemo(() => {
    const parts: React.ReactNode[] = [];
    const codeBlockRegex = /```(\w*)\n([\s\S]*?)```/g;
    let lastIndex = 0;
    let match;
    let keyIndex = 0;

    while ((match = codeBlockRegex.exec(content)) !== null) {
      // 添加代码块前的文本（已转义）
      if (match.index > lastIndex) {
        parts.push(
          <span key={`text-${keyIndex++}`}>{content.slice(lastIndex, match.index)}</span>
        );
      }

      // 添加代码块（代码内容已转义）
      const lang = match[1] || 'text';
      const code = escapeHtml(match[2]);
      parts.push(
        <pre
          key={`code-${keyIndex++}`}
          style={{
            backgroundColor: '#1e1e1e',
            color: '#d4d4d4',
            padding: 12,
            borderRadius: 6,
            overflow: 'auto',
            fontSize: 13,
            margin: '8px 0',
          }}
        >
          <div style={{ color: '#6a9955', marginBottom: 8 }}>{lang}</div>
          <code dangerouslySetInnerHTML={{ __html: code }} />
        </pre>
      );

      lastIndex = match.index + match[0].length;
    }

    // 添加剩余文本
    if (lastIndex < content.length) {
      parts.push(<span key={`text-${keyIndex++}`}>{content.slice(lastIndex)}</span>);
    }

    return parts.length > 0 ? parts : content;
  }, [content]);

  return (
    <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
      {renderedContent}
    </Paragraph>
  );
}
