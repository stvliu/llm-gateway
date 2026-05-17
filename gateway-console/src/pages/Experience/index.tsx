import { useExperienceChat } from '@/hooks/useExperienceChat';
import { ConfigPanel } from './ConfigPanel';
import { ChatPanel } from './ChatPanel';
import { StatsPanel } from './StatsPanel';
import { MessageInput } from './MessageInput';
import { Card, Row, Col, Typography, Button, Space } from 'antd';
import { ClearOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Title } = Typography;

/**
 * 模型体验页面
 *
 * 提供流式聊天体验，让用户验证配置和体验模型效果。
 */
export default function ModelExperiencePage() {
  const { t } = useTranslation('experience');
  const { state, setConfig, sendMessage, clearMessages, stopGeneration } = useExperienceChat();

  return (
    <div style={{ padding: 24, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Row gutter={[16, 16]} style={{ flex: 1, minHeight: 0 }}>
        {/* 左侧：配置面板 */}
        <Col xs={24} lg={8} style={{ display: 'flex', flexDirection: 'column' }}>
          <Card
            title={t('config.title')}
            size="small"
            style={{ flex: 1, display: 'flex', flexDirection: 'column' }}
            styles={{ body: { flex: 1, overflow: 'auto' } }}
          >
            <ConfigPanel
              providerType={state.providerType}
              apiKey={state.apiKey}
              baseUrl={state.baseUrl}
              model={state.model}
              onConfigChange={setConfig}
              disabled={state.isLoading}
            />
          </Card>
        </Col>

        {/* 右侧：对话区域 */}
        <Col xs={24} lg={16} style={{ display: 'flex', flexDirection: 'column' }}>
          <Card
            title={
              <Space>
                <Title level={5} style={{ margin: 0 }}>
                  {t('chat.title')}
                </Title>
                <Button
                  type="text"
                  icon={<ClearOutlined />}
                  size="small"
                  onClick={clearMessages}
                  disabled={state.messages.length === 0 || state.isLoading}
                >
                  {t('chat.clear')}
                </Button>
              </Space>
            }
            size="small"
            style={{ flex: 1, display: 'flex', flexDirection: 'column' }}
            styles={{ body: { flex: 1, display: 'flex', flexDirection: 'column', padding: 0 } }}
          >
            {/* 消息列表 */}
            <ChatPanel messages={state.messages} isLoading={state.isLoading} />

            {/* 统计信息 */}
            {state.usage.promptTokens > 0 && (
              <div style={{ padding: '8px 16px', borderTop: '1px solid #f0f0f0' }}>
                <StatsPanel
                  promptTokens={state.usage.promptTokens}
                  completionTokens={state.usage.completionTokens}
                />
              </div>
            )}

            {/* 输入区域 */}
            <div style={{ padding: 16, borderTop: '1px solid #f0f0f0' }}>
              <MessageInput
                onSend={sendMessage}
                onStop={stopGeneration}
                isLoading={state.isLoading}
                disabled={!state.providerType || !state.apiKey || !state.model}
                error={state.error}
              />
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
