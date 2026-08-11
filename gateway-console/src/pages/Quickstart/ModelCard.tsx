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
import { Card, Tag, Typography, theme } from 'antd';
import { CheckCircleFilled } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { Model } from '@/types/model';

const { Text } = Typography;

interface Props {
  model: Model;
  selected?: boolean;
  onSelect?: (model: Model) => void;
}

export default function ModelCard({ model, selected, onSelect }: Props) {
  const { t } = useTranslation('quickstart');
  const { token } = theme.useToken();

  const capabilityKeys: Record<string, string> = {
    vision: 'capability.vision',
    function_calling: 'capability.functionCalling',
    streaming: 'capability.streaming',
  };

  const caps = model.capabilities
    ? Object.entries(model.capabilities)
        .filter(([, v]) => v)
        .map(([k]) => t(capabilityKeys[k] || k))
    : [];

  return (
    <Card
      hoverable
      size="small"
      onClick={() => onSelect?.(model)}
      styles={{
        body: { padding: 16 },
      }}
      style={selected ? {
        borderColor: token.colorPrimary,
        background: `${token.colorPrimary}08`,
        borderWidth: 2,
      } : undefined}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <Text strong style={{ fontSize: 15 }}>{model.displayName || model.modelName}</Text>
          <div>
            <Text type="secondary" style={{ fontSize: 12 }}>{model.modelName}</Text>
          </div>
        </div>
        {selected ? (
          <CheckCircleFilled style={{ color: token.colorPrimary, fontSize: 18 }} />
        ) : (
          <CheckCircleFilled style={{ color: token.colorTextQuaternary, fontSize: 18 }} />
        )}
      </div>
      {caps.length > 0 && (
        <div style={{ marginTop: 8, display: 'flex', gap: 4, flexWrap: 'wrap' }}>
          {caps.map((c) => (
            <Tag key={c} color={selected ? 'blue' : 'default'} style={{ fontSize: 11 }}>{c}</Tag>
          ))}
        </div>
      )}
      <div style={{ marginTop: 8, fontSize: 12, color: token.colorTextSecondary }}>
        {t('model.contextWindow')} {model.contextWindow ? `${(model.contextWindow / 1000).toFixed(0)}K` : '-'}
      </div>
    </Card>
  );
}
