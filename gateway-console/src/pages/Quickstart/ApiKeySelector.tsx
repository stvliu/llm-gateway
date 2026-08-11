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
import { Button, Dropdown, App, Typography, theme, Space } from 'antd';
import { CopyOutlined, PlusOutlined, SwapOutlined, KeyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { useUserApiKeys } from '@/services/query/useUserApiKeys';
import { userApiKeyApi } from '@/services/api/userApiKey';
import { maskApiKey } from '@/utils/maskApiKey';

const { Text } = Typography;

interface Props {
  currentKey: string | undefined;
  currentKeyId: number | undefined;
  onKeyChange: (keyPlain: string, keyId: number) => void;
  onCreateClick: () => void;
}

export default function ApiKeySelector({ currentKey, onKeyChange, onCreateClick }: Props) {
  const { t } = useTranslation('quickstart');
  const { token } = theme.useToken();
  const { message } = App.useApp();
  const currentUser = useAuthStore((s) => s.user);
  const userId = currentUser?.id ?? 0;
  const { data: keys } = useUserApiKeys(userId);
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

  const dropdownItems = keys?.map((k) => ({
    key: String(k.id),
    label: (
      <Space>
        <Text code style={{ fontSize: 12 }}>{maskApiKey(k.keyPlain)}</Text>
        <Text type="secondary" style={{ fontSize: 11 }}>{k.name}</Text>
      </Space>
    ),
    onClick: () => loadKeyDetail(k.id),
  })) ?? [];

  const hasKey = !!currentKey;

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      padding: '12px 16px',
      background: token.colorBgContainer,
      border: `1px solid ${hasKey ? token.colorBorder : token.colorBorderSecondary}`,
      borderRadius: token.borderRadiusLG,
      borderStyle: hasKey ? 'solid' : 'dashed',
    }}>
      <div style={{
        width: 32, height: 32,
        background: hasKey ? `${token.colorPrimary}10` : token.colorFillQuaternary,
        borderRadius: 6,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: hasKey ? token.colorPrimary : token.colorTextQuaternary,
        fontSize: 16,
      }}>
        <KeyOutlined />
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 12, color: token.colorTextSecondary, marginBottom: 2 }}>
          {t('apiKey.current')}
        </div>
        {hasKey ? (
          <Text code style={{ fontSize: 14 }}>{maskApiKey(currentKey!)}</Text>
        ) : (
          <Text type="secondary" style={{ fontSize: 13 }}>{t('apiKey.placeholder')}</Text>
        )}
      </div>
      <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
        {hasKey && (
          <>
            <Button size="small" icon={<CopyOutlined />} onClick={handleCopy} loading={loadingKeyId !== null}>
              {t('apiKey.copy')}
            </Button>
            <Dropdown menu={{ items: dropdownItems }} trigger={['click']}>
              <Button size="small" icon={<SwapOutlined />}>
                {t('apiKey.switch')}
              </Button>
            </Dropdown>
          </>
        )}
        <Button size="small" type="primary" icon={<PlusOutlined />} onClick={onCreateClick}>
          {t('apiKey.create')}
        </Button>
      </div>
    </div>
  );
}
