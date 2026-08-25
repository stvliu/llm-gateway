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
import { Select, Skeleton, Typography, theme } from 'antd';
import { useTranslation } from 'react-i18next';
import ApiKeySelector from './ApiKeySelector';
import type { Protocol } from './CodeSnippet';
import type { Model } from '@/types/model';

const { Text } = Typography;

interface Props {
  currentKey?: string;
  currentKeyId?: number;
  onKeyChange: (keyPlain: string, keyId: number) => void;
  onCreateKeyClick: () => void;
  /** 可用模型（仅活跃） */
  models: Model[];
  loading?: boolean;
  model: string;
  onModelChange: (model: string) => void;
  protocol: Protocol;
  onProtocolChange: (protocol: Protocol) => void;
}

/** 统一请求配置条：API Key / 模型 / 协议 / 代码语言 四个条件同容器、同交互风格 */
export default function RequestConfigBar(props: Props) {
  const { t } = useTranslation('quickstart');
  const { token } = theme.useToken();

  const modelOptions = props.models.map((m) => ({
    value: m.modelName,
    label: m.displayName || m.modelName,
  }));

  const protocolOptions = [
    { value: 'openai' as Protocol, label: t('protocol.openai') },
    { value: 'anthropic' as Protocol, label: t('protocol.anthropic') },
  ];

  return (
    <div style={{
      border: `1px solid ${token.colorBorder}`,
      borderRadius: token.borderRadiusLG,
      padding: '12px 16px',
      background: token.colorBgContainer,
    }}>
      <div style={{ fontSize: 12, color: token.colorTextSecondary, marginBottom: 10 }}>
        {t('config.title')}
      </div>
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '12px 24px' }}>
        <ApiKeySelector
          currentKey={props.currentKey}
          currentKeyId={props.currentKeyId}
          onKeyChange={props.onKeyChange}
          onCreateClick={props.onCreateKeyClick}
        />

        {/* 模型 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Text type="secondary" style={{ fontSize: 12, whiteSpace: 'nowrap' }}>
            {t('model.label')}
          </Text>
          {props.loading ? (
            <Skeleton.Input active size="small" style={{ width: 160 }} />
          ) : props.models.length === 0 ? (
            <Text type="secondary" style={{ fontSize: 12 }}>{t('noModels')}</Text>
          ) : (
            <Select
              size="small"
              data-testid="model-select"
              style={{ minWidth: 160 }}
              value={props.model}
              placeholder={t('model.select')}
              showSearch
              filterOption={(input, option) =>
                String(option?.label ?? '').toLowerCase().includes(input.toLowerCase()) ||
                String(option?.value ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={modelOptions}
              onChange={props.onModelChange}
            />
          )}
        </div>

        {/* 协议（端点/格式） */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Text type="secondary" style={{ fontSize: 12, whiteSpace: 'nowrap' }}>
            {t('protocol.label')}
          </Text>
          <Select
            size="small"
            data-testid="protocol-select"
            style={{ minWidth: 150 }}
            value={props.protocol}
            onChange={props.onProtocolChange}
            options={protocolOptions}
            optionRender={(opt) => (
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16 }}>
                <span>{t(`protocol.${opt.value}`)}</span>
                <Text type="secondary" style={{ fontSize: 11 }}>
                  {opt.value === 'openai'
                    ? t('protocol.openaiEndpoint')
                    : t('protocol.anthropicEndpoint')}
                </Text>
              </div>
            )}
          />
        </div>
      </div>
    </div>
  );
}
