import { useState } from 'react';
import { Segmented, Button, App } from 'antd';
import { CopyOutlined } from '@ant-design/icons';

const GATEWAY_URL = import.meta.env.VITE_API_BASE_URL || 'https://api.your-gateway.com';

interface Props {
  apiKey?: string;
}

const snippets: Record<string, (key: string) => string> = {
  curl: (key) => `curl ${GATEWAY_URL}/v1/chat/completions \\
  -H "Authorization: Bearer ${key || 'sk-your-api-key'}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "Hello"}]
  }'`,
  python: (key) => `import requests

response = requests.post(
    "${GATEWAY_URL}/v1/chat/completions",
    headers={
        "Authorization": "Bearer ${key || 'sk-your-api-key'}",
        "Content-Type": "application/json"
    },
    json={
        "model": "gpt-4o",
        "messages": [{"role": "user", "content": "Hello"}]
    }
)
print(response.json())`,
  node: (key) => `const response = await fetch("${GATEWAY_URL}/v1/chat/completions", {
  method: "POST",
  headers: {
    "Authorization": "Bearer ${key || 'sk-your-api-key'}",
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    model: "gpt-4o",
    messages: [{ role: "user", content: "Hello" }]
  })
});
const data = await response.json();
console.log(data);`,
  java: (key) => `HttpClient client = HttpClient.newHttpClient();
String body = """
{
  "model": "gpt-4o",
  "messages": [{"role": "user", "content": "Hello"}]
}
""";

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${GATEWAY_URL}/v1/chat/completions"))
    .header("Authorization", "Bearer ${key || 'sk-your-api-key'}")
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(body))
    .build();

HttpResponse<String> response =
    client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());`,
};

type Lang = 'curl' | 'python' | 'node' | 'java';

export default function CodeSnippet({ apiKey }: Props) {
  const { message } = App.useApp();
  const [lang, setLang] = useState<Lang>('curl');
  const code = snippets[lang](apiKey || '');

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(code);
      message.success('已复制到剪贴板');
    } catch {
      message.error('复制失败，请手动选择复制');
    }
  };

  return (
    <div style={{ border: '1px solid #e2e8f0', borderRadius: 8, overflow: 'hidden' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 16px', background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
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
        <Button size="small" icon={<CopyOutlined />} onClick={handleCopy}>
          复制
        </Button>
      </div>
      <pre style={{ margin: 0, padding: 16, background: '#1e293b', color: '#e2e8f0', fontSize: 13, lineHeight: 1.6, overflow: 'auto' }}>
        <code>{code}</code>
      </pre>
    </div>
  );
}