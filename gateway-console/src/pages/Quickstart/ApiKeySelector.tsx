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
import { useState, useEffect } from 'react';
import { Select, Button, App, Typography, theme } from 'antd';
import { CopyOutlined, PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { useUserApiKeys } from '@/services/query/useUserApiKeys';
import { userApiKeyApi } from '@/services/api/userApiKey';
import { maskApiKey } from '@/utils/maskApiKey';

const { Text } = Typography;

interface Props {
  currentKey?: string;
  currentKeyId?: number;
  onKeyChange: (keyPlain: string, keyId: number) => void;
  onCreateClick: () => void;
}

/** API Key 紧凑选择：下拉切换 + 复制 + 新建（无 Key 时警示态作为唯一阻塞项） */
export default function ApiKeySelector({ currentKey, currentKeyId, onKeyChange, onCreateClick }: Props) {
  const { t } = useTranslation('quickstart');
  const { token } = theme.useToken();
  const { message } = App.useApp();
  const currentUser = useAuthStore((s) => s.user);
  const userId = currentUser?.id ?? 0;
  const { data: keys, isLoading } = useUserApiKeys(userId);
  const [loadingKeyId, setLoadingKeyId] = useState<number | null>(null);

  // 自动选择第一个可用 Key
  useEffect(() => {
    if (currentKey || !keys || keys.length === 0) return;
    loadKeyDetail(keys[0].id);
  }, [keys, currentKey]);

  const loadKeyDetail = async (keyId: number) => {
    setLoadingKeyId(keyId);
    try {
      const detail = await userApiKeyApi.getDetail(keyId);
      onKeyChange(detail.keyPlain, detail.id);
    } catch {
      // 获取失败忽略
    } finally {
      setLoadingKeyId(null);
    }
  };

  const handleCopy = async () => {
    if (!currentKey) return;
    try {
      await navigator.clipboard.writeText(currentKey);
      message.success(t('keyCopied'));
    } catch {
      message.error(t('keyCopyFailed'));
    }
  };

  const hasKey = !!currentKey;

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <Text type="secondary" style={{ fontSize: 12, whiteSpace: 'nowrap' }}>
        {t('apiKey.label')}
      </Text>
      <Select
        size="small"
        data-testid="key-select"
        style={{ minWidth: 170 }}
        value={currentKeyId}
        placeholder={t('apiKey.placeholder')}
        status={hasKey ? undefined : 'warning'}
        loading={isLoading}
        disabled={!keys || keys.length === 0}
        options={(keys ?? []).map((k) => ({
          value: k.id,
          label: `${maskApiKey(k.keyPlain)} · ${k.name}`,
        }))}
        onChange={(id: number) => loadKeyDetail(id)}
      />
      <Button
        size="small"
        aria-label={t('apiKey.copy')}
        icon={<CopyOutlined />}
        onClick={handleCopy}
        disabled={!hasKey || loadingKeyId !== null}
        style={{ color: token.colorTextSecondary }}
      />
      <Button size="small" type="primary" icon={<PlusOutlined />} onClick={onCreateClick}>
        {t('apiKey.create')}
      </Button>
    </div>
  );
}
