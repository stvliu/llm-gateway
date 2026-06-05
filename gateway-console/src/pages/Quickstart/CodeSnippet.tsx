import { useState, useEffect } from 'react';
import { Segmented, Button, App, theme, Typography, Spin, Empty } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { useUserApiKeys } from '@/services/query/useUserApiKeys';
import { userApiKeyApi } from '@/services/api/userApiKey';

const { Text } = Typography;

interface Props {
  apiKey?: string;
}

const snippets: Record<string, (url: string, key: string) => string> = {
  curl: (url, key) => `curl ${url}/v1/chat/completions \\
  -H "Authorization: Bearer ${key}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "Hello"}]
  }'`,
  python: (url, key) => `import requests

response = requests.post(
    "${url}/v1/chat/completions",
    headers={
        "Authorization": "Bearer ${key}",
        "Content-Type": "application/json"
    },
    json={
        "model": "gpt-4o",
        "messages": [{"role": "user", "content": "Hello"}]
    }
)
print(response.json())`,
  node: (url, key) => `const response = await fetch("${url}/v1/chat/completions", {
  method: "POST",
  headers: {
    "Authorization": "Bearer ${key}",
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    model: "gpt-4o",
    messages: [{ role: "user", content: "Hello" }]
  })
});
const data = await response.json();
console.log(data);`,
  java: (url, key) => `HttpClient client = HttpClient.newHttpClient();
String body = """
{
  "model": "gpt-4o",
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

type Lang = 'curl' | 'python' | 'node' | 'java';

export default function CodeSnippet({ apiKey: propApiKey }: Props) {
  const { t } = useTranslation('developer');
  const { token } = theme.useToken();
  const { message } = App.useApp();
  const [lang, setLang] = useState<Lang>('curl');

  // 获取当前用户
  const currentUser = useAuthStore((s) => s.user);
  const userId = currentUser?.id ?? 0;

  // 查询用户的 API Key 列表
  const { data: userKeys, isLoading: keysLoading } = useUserApiKeys(userId);

  // 自动获取第一个 ACTIVE Key 的明文
  const [autoApiKey, setAutoApiKey] = useState<string>('');
  const [fetchingKey, setFetchingKey] = useState(false);

  useEffect(() => {
    // 如果传入了 apiKey prop，优先使用
    if (propApiKey) {
      setAutoApiKey(propApiKey);
      return;
    }

    // 自动获取用户的第一个 ACTIVE Key
    const fetchFirstActiveKey = async () => {
      if (!userKeys || userKeys.length === 0) {
        setAutoApiKey('');
        return;
      }

      const activeKey = userKeys.find((k) => k.state === 'ACTIVE');
      if (!activeKey) {
        setAutoApiKey('');
        return;
      }

      setFetchingKey(true);
      try {
        const detail = await userApiKeyApi.getDetail(activeKey.id);
        setAutoApiKey(detail.keyPlain);
      } catch {
        setAutoApiKey('');
      } finally {
        setFetchingKey(false);
      }
    };

    fetchFirstActiveKey();
  }, [propApiKey, userKeys]);

  // 计算 Gateway URL：优先使用环境变量，后备使用当前域名
  const gatewayUrl = import.meta.env.VITE_API_BASE_URL || window.location.origin;

  // 最终使用的 API Key
  const displayApiKey = autoApiKey || 'sk-your-api-key';
  const code = snippets[lang](gatewayUrl, displayApiKey);

  const handleCopy = async () => {
    // 优先使用 Clipboard API，失败时降级到 execCommand
    try {
      await navigator.clipboard.writeText(code);
      message.success(t('copySuccess'));
    } catch {
      // Fallback: 使用 execCommand（兼容非 HTTPS 环境）
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

  // 判断是否有可用的 API Key
  const hasApiKey = autoApiKey && autoApiKey !== 'sk-your-api-key';

  return (
    <div style={{ border: `1px solid ${token.colorBorder}`, borderRadius: token.borderRadiusLG, overflow: 'hidden' }}>
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '8px 16px',
        background: token.colorBgLayout,
        borderBottom: `1px solid ${token.colorBorder}`,
      }}>
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
          {t('copy')}
        </Button>
      </div>
      <Spin spinning={keysLoading || fetchingKey}>
        {!hasApiKey && !keysLoading && !fetchingKey ? (
          <div style={{
            padding: 24,
            background: token.colorBgContainer,
            textAlign: 'center',
          }}>
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={
                <Text type="secondary">
                  {userKeys && userKeys.length > 0
                    ? t('noActiveKey')
                    : t('noKey')}
                </Text>
              }
            />
          </div>
        ) : (
          <pre style={{
            margin: 0,
            padding: 16,
            background: token.colorBgContainer,
            color: token.colorText,
            fontSize: 13,
            lineHeight: 1.6,
            overflow: 'auto',
            fontFamily: 'Consolas, Monaco, "Courier New", monospace',
          }}>
            <code>{code}</code>
          </pre>
        )}
      </Spin>
    </div>
  );
}