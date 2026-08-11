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
import { useState } from 'react';
import { Segmented, Button, App, theme, Select } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

export type Protocol = 'openai' | 'anthropic';
type Lang = 'curl' | 'python' | 'node' | 'java';

interface Props {
  apiKey?: string;
  model: string;
  models: string[];
  protocol: Protocol;
  onModelChange: (model: string) => void;
  onProtocolChange: (protocol: Protocol) => void;
}

/** OpenAI 协议代码模板 */
const openaiSnippets: Record<Lang, (url: string, key: string, model: string) => string> = {
  curl: (url, key, model) => `curl ${url}/v1/chat/completions \\
  -H "Authorization: Bearer ${key}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "model": "${model}",
    "messages": [{"role": "user", "content": "Hello"}]
  }'`,
  python: (url, key, model) => `import requests

response = requests.post(
    "${url}/v1/chat/completions",
    headers={
        "Authorization": "Bearer ${key}",
        "Content-Type": "application/json"
    },
    json={
        "model": "${model}",
        "messages": [{"role": "user", "content": "Hello"}]
    }
)
print(response.json())`,
  node: (url, key, model) => `const response = await fetch("${url}/v1/chat/completions", {
  method: "POST",
  headers: {
    "Authorization": "Bearer ${key}",
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    model: "${model}",
    messages: [{ role: "user", content: "Hello" }]
  })
});
const data = await response.json();
console.log(data);`,
  java: (url, key, model) => `HttpClient client = HttpClient.newHttpClient();
String body = """
{
  "model": "${model}",
  "messages": [{"role": "user", "content": "Hello"}]
}
""";

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${url}/v1/chat/completions"))
    .header("Authorization", "Bearer ${key}")
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(body))
    .build();

HttpResponse<String> response =
    client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());`,
};

/** Anthropic 协议代码模板 */
const anthropicSnippets: Record<Lang, (url: string, key: string, model: string) => string> = {
  curl: (url, key, model) => `curl ${url}/anthropic/v1/messages \\
  -H "x-api-key: ${key}" \\
  -H "anthropic-version: 2023-06-01" \\
  -H "Content-Type: application/json" \\
  -d '{
    "model": "${model}",
    "max_tokens": 1024,
    "messages": [{"role": "user", "content": "Hello"}]
  }'`,
  python: (url, key, model) => `import requests

response = requests.post(
    "${url}/anthropic/v1/messages",
    headers={
        "x-api-key": "${key}",
        "anthropic-version": "2023-06-01",
        "Content-Type": "application/json"
    },
    json={
        "model": "${model}",
        "max_tokens": 1024,
        "messages": [{"role": "user", "content": "Hello"}]
    }
)
print(response.json())`,
  node: (url, key, model) => `const response = await fetch("${url}/anthropic/v1/messages", {
  method: "POST",
  headers: {
    "x-api-key": "${key}",
    "anthropic-version": "2023-06-01",
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    model: "${model}",
    max_tokens: 1024,
    messages: [{ role: "user", content: "Hello" }]
  })
});
const data = await response.json();
console.log(data);`,
  java: (url, key, model) => `HttpClient client = HttpClient.newHttpClient();
String body = """
{
  "model": "${model}",
  "max_tokens": 1024,
  "messages": [{"role": "user", "content": "Hello"}]
}
""";

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${url}/anthropic/v1/messages"))
    .header("x-api-key", "${key}")
    .header("anthropic-version", "2023-06-01")
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(body))
    .build();

HttpResponse<String> response =
    client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());`,
};

const allSnippets: Record<Protocol, Record<Lang, (url: string, key: string, model: string) => string>> = {
  openai: openaiSnippets,
  anthropic: anthropicSnippets,
};

export default function CodeSnippet({ apiKey, model, models, protocol, onModelChange, onProtocolChange }: Props) {
  const { t } = useTranslation('quickstart');
  const { token } = theme.useToken();
  const { message } = App.useApp();
  const [lang, setLang] = useState<Lang>('curl');

  const gatewayUrl = import.meta.env.VITE_API_BASE_URL || window.location.origin;
  const displayApiKey = apiKey || 'sk-your-api-key';
  const code = allSnippets[protocol][lang](gatewayUrl, displayApiKey, model);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(code);
      message.success(t('copySuccess'));
    } catch {
      const textArea = document.createElement('textarea');
      textArea.value = code;
      textArea.style.position = 'fixed';
      textArea.style.left = '-9999px';
      document.body.appendChild(textArea);
      textArea.select();
      try {
        document.execCommand('copy');
        message.success(t('copySuccess'));
      } catch {
        message.error(t('copyFailed'));
      }
      document.body.removeChild(textArea);
    }
  };

  return (
    <div style={{ border: `1px solid ${token.colorBorder}`, borderRadius: token.borderRadiusLG, overflow: 'hidden' }}>
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '8px 16px',
        background: token.colorBgLayout,
        borderBottom: `1px solid ${token.colorBorder}`,
        flexWrap: 'wrap',
        gap: 8,
      }}>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
          <Segmented
            size="small"
            value={protocol}
            onChange={(v) => onProtocolChange(v as Protocol)}
            options={[
              { value: 'openai', label: t('protocol.openai') },
              { value: 'anthropic', label: t('protocol.anthropic') },
            ]}
          />
          <Segmented
            size="small"
            value={lang}
            onChange={(v) => setLang(v as Lang)}
            options={[
              { value: 'curl', label: 'cURL' },
              { value: 'python', label: 'Python' },
              { value: 'node', label: 'Node.js' },
              { value: 'java', label: 'Java' },
            ]}
          />
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <Select
            size="small"
            value={model}
            onChange={onModelChange}
            style={{ minWidth: 160 }}
            options={models.map((m) => ({ value: m, label: m }))}
            placeholder={t('model.select')}
          />
          <Button size="small" icon={<CopyOutlined />} onClick={handleCopy}>
            {t('copy')}
          </Button>
        </div>
      </div>
      <pre style={{
        margin: 0,
        padding: 16,
        background: token.colorBgElevated,
        color: token.colorText,
        fontSize: 13,
        lineHeight: 1.6,
        overflow: 'auto',
        fontFamily: 'Consolas, Monaco, "Courier New", monospace',
      }}>
        <code>{code}</code>
      </pre>
    </div>
  );
}