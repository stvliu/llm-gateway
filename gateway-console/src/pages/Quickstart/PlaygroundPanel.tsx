/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useState, useRef, useCallback } from 'react';
import { Button, Input, Switch, Typography, Alert, theme, Space } from 'antd';
import { PlayCircleOutlined, StopOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { Protocol } from './CodeSnippet';

const { Text } = Typography;

interface Props {
  apiKey?: string;
  model: string;
  protocol: Protocol;
}

type RequestState = 'idle' | 'loading' | 'streaming' | 'done' | 'error';

export default function PlaygroundPanel({ apiKey, model, protocol }: Props) {
  const { t } = useTranslation('quickstart');
  const { token } = theme.useToken();

  const [input, setInput] = useState('你好，请用一句话介绍你自己');
  const [streamEnabled, setStreamEnabled] = useState(true);
  const [state, setState] = useState<RequestState>('idle');
  const [response, setResponse] = useState('');
  const [error, setError] = useState('');
  const [inputTokens, setInputTokens] = useState<number | null>(null);
  const [outputTokens, setOutputTokens] = useState<number | null>(null);
  const [duration, setDuration] = useState<number | null>(null);

  const abortRef = useRef<AbortController | null>(null);

  const gatewayUrl = import.meta.env.VITE_API_BASE_URL || window.location.origin;

  const buildRequest = (): { url: string; headers: Record<string, string>; body: string } => {
    if (protocol === 'openai') {
      return {
        url: `${gatewayUrl}/v1/chat/completions`,
        headers: {
          'Authorization': `Bearer ${apiKey}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          model,
          messages: [{ role: 'user', content: input }],
          stream: streamEnabled,
        }),
      };
    }
    return {
      url: `${gatewayUrl}/anthropic/v1/messages`,
      headers: {
        'x-api-key': apiKey || '',
        'anthropic-version': '2023-06-01',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model,
        max_tokens: 1024,
        messages: [{ role: 'user', content: input }],
        stream: streamEnabled,
      }),
    };
  };

  const handleSend = useCallback(async () => {
    if (!apiKey || !model) return;

    const abort = new AbortController();
    abortRef.current = abort;
    setState('loading');
    setResponse('');
    setError('');
    setInputTokens(null);
    setOutputTokens(null);
    setDuration(null);

    const startTime = Date.now();
    const { url, headers, body } = buildRequest();

    try {
      const res = await fetch(url, {
        method: 'POST',
        headers,
        body,
        signal: abort.signal,
      });

      if (!res.ok) {
        const errText = await res.text();
        throw new Error(`${res.status} ${res.statusText}: ${errText}`);
      }

      if (streamEnabled && res.body) {
        setState('streaming');
        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let accumulated = '';
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          // 保留最后一个不完整的行
          buffer = lines.pop() || '';

          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.slice(6).trim();
              if (data === '[DONE]') continue;

              try {
                const parsed = JSON.parse(data);
                // OpenAI SSE
                if (protocol === 'openai') {
                  const delta = parsed.choices?.[0]?.delta?.content;
                  if (delta) {
                    accumulated += delta;
                  }
                  if (parsed.usage) {
                    setInputTokens(parsed.usage.prompt_tokens);
                    setOutputTokens(parsed.usage.completion_tokens);
                  }
                }
                // Anthropic SSE
                if (protocol === 'anthropic') {
                  if (parsed.type === 'content_block_delta' && parsed.delta?.text) {
                    accumulated += parsed.delta.text;
                  }
                  if (parsed.type === 'message_start' && parsed.message?.usage) {
                    setInputTokens(parsed.message.usage.input_tokens);
                  }
                  if (parsed.type === 'message_delta' && parsed.usage) {
                    setOutputTokens(parsed.usage.output_tokens);
                  }
                }
              } catch {
                // 非 JSON 行，跳过
              }
            }
          }
          setResponse(accumulated);
        }
      } else {
        const data = await res.json();
        if (protocol === 'openai') {
          setResponse(data.choices?.[0]?.message?.content || JSON.stringify(data, null, 2));
          setInputTokens(data.usage?.prompt_tokens ?? null);
          setOutputTokens(data.usage?.completion_tokens ?? null);
        } else {
          setResponse(data.content?.[0]?.text || JSON.stringify(data, null, 2));
          setInputTokens(data.usage?.input_tokens ?? null);
          setOutputTokens(data.usage?.output_tokens ?? null);
        }
      }

      setDuration((Date.now() - startTime) / 1000);
      setState('done');
    } catch (err: unknown) {
      if (err instanceof DOMException && err.name === 'AbortError') {
        setState('idle');
        return;
      }
      setError(err instanceof Error ? err.message : String(err));
      setState('error');
    }
  }, [apiKey, model, protocol, input, streamEnabled, gatewayUrl]);

  const handleStop = () => {
    abortRef.current?.abort();
    abortRef.current = null;
  };

  const isRequesting = state === 'loading' || state === 'streaming';

  return (
    <div style={{
      background: token.colorBgContainer,
      border: `1px solid ${token.colorBorder}`,
      borderRadius: token.borderRadiusLG,
      padding: 16,
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <Text strong style={{ fontSize: 14 }}>{t('playground.title')}</Text>
        <Space size={4} style={{ fontSize: 11, color: token.colorTextSecondary }}>
          <span>{model}</span>
          <span>·</span>
          <span>{protocol === 'openai' ? t('protocol.openai') : t('protocol.anthropic')}</span>
        </Space>
      </div>

      <Input.TextArea
        value={input}
        onChange={(e) => setInput(e.target.value)}
        placeholder={t('playground.placeholder')}
        autoSize={{ minRows: 2, maxRows: 4 }}
        style={{ marginBottom: 12 }}
      />

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <Space>
          {isRequesting ? (
            <Button
              danger
              icon={<StopOutlined />}
              onClick={handleStop}
            >
              {t('playground.stop')}
            </Button>
          ) : (
            <Button
              type="primary"
              icon={<PlayCircleOutlined />}
              onClick={handleSend}
              disabled={!apiKey}
            >
              {t('playground.send')}
            </Button>
          )}
          <Switch
            size="small"
            checked={streamEnabled}
            onChange={setStreamEnabled}
          />
          <Text type="secondary" style={{ fontSize: 11 }}>{t('playground.stream')}</Text>
        </Space>
        {!apiKey && (
          <Text type="secondary" style={{ fontSize: 11 }}>{t('playground.noKey')}</Text>
        )}
      </div>

      {state === 'streaming' && (
        <div style={{ fontSize: 11, color: token.colorSuccess, marginBottom: 8 }}>
          {t('playground.streaming')}
        </div>
      )}

      {state === 'error' && (
        <Alert
          type="error"
          message={t('playground.failed')}
          description={error}
          showIcon
          style={{ marginBottom: 12 }}
        />
      )}

      {response && (
        <pre style={{
          margin: 0,
          padding: 12,
          background: token.colorBgElevated,
          borderRadius: token.borderRadius,
          fontSize: 13,
          lineHeight: 1.6,
          overflow: 'auto',
          maxHeight: 240,
          fontFamily: 'Consolas, Monaco, "Courier New", monospace',
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
        }}>
          {response}
        </pre>
      )}

      {(state === 'done' || state === 'streaming') && (inputTokens !== null || outputTokens !== null || duration !== null) && (
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          marginTop: 8,
          fontSize: 11,
          color: token.colorTextSecondary,
        }}>
          <Space size={12}>
            {inputTokens !== null && (
              <span>{t('playground.inputTokens')}: {inputTokens} {t('playground.tokens')}</span>
            )}
            {outputTokens !== null && (
              <span>{t('playground.outputTokens')}: {outputTokens} {t('playground.tokens')}</span>
            )}
          </Space>
          {duration !== null && <span>{t('playground.duration')}: {duration.toFixed(1)}s</span>}
        </div>
      )}
    </div>
  );
}
