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
import { Typography, Card } from 'antd';
import { useTranslation } from 'react-i18next';
import { useModels } from '@/services/query/useModels';
import RequestConfigBar from './RequestConfigBar';
import CodeSnippet from './CodeSnippet';
import type { Protocol } from './CodeSnippet';
import KeyGenerateModal from './KeyGenerateModal';
import PlaygroundPanel from './PlaygroundPanel';

const { Title, Paragraph } = Typography;

export default function Quickstart() {
  const { t } = useTranslation('quickstart');
  const { data: models, isLoading } = useModels({ limit: 1000 });
  const [keyModalOpen, setKeyModalOpen] = useState(false);

  // 页面级联动状态：Key/模型/协议统一由页面持有，代码示例与试玩共享
  const [currentKey, setCurrentKey] = useState<string>();
  const [currentKeyId, setCurrentKeyId] = useState<number>();
  const [selectedModel, setSelectedModel] = useState<string>('');
  const [protocol, setProtocol] = useState<Protocol>('openai');

  // 可用模型（仅活跃）
  const activeModels = models?.items?.filter((m) => m.state === 'ACTIVE') ?? [];
  const modelNames = activeModels.map((m) => m.modelName);

  // 自动选择第一个模型
  useEffect(() => {
    if (!selectedModel && modelNames.length > 0) {
      setSelectedModel(modelNames[0]);
    }
  }, [modelNames, selectedModel]);

  // 如果当前选中的模型不在列表中，自动切换
  useEffect(() => {
    if (selectedModel && modelNames.length > 0 && !modelNames.includes(selectedModel)) {
      setSelectedModel(modelNames[0]);
    }
  }, [modelNames, selectedModel]);

  const handleKeyChange = (keyPlain: string, keyId: number) => {
    setCurrentKey(keyPlain);
    setCurrentKeyId(keyId);
  };

  const handleKeyCreated = (key: string) => {
    setCurrentKey(key);
    setKeyModalOpen(false);
  };

  return (
    <div>
      <Card>
        <div style={{ marginBottom: 24 }}>
          <Title level={4} style={{ margin: 0 }}>{t('title')}</Title>
          <Paragraph type="secondary" style={{ margin: '4px 0 0' }}>
            {t('subtitle')}
          </Paragraph>
        </div>

        {/* 区块 1: 统一请求配置条（API Key / 模型 / 协议） */}
        <div style={{ marginBottom: 24 }}>
          <RequestConfigBar
            currentKey={currentKey}
            currentKeyId={currentKeyId}
            onKeyChange={handleKeyChange}
            onCreateKeyClick={() => setKeyModalOpen(true)}
            models={activeModels}
            loading={isLoading}
            model={selectedModel}
            onModelChange={setSelectedModel}
            protocol={protocol}
            onProtocolChange={setProtocol}
          />
        </div>

        {/* 区块 2: 代码示例（共享上方配置，语言切换在头部） */}
        <div style={{ marginBottom: 24 }}>
          <CodeSnippet
            apiKey={currentKey}
            model={selectedModel}
            protocol={protocol}
          />
        </div>

        {/* 区块 3: 在线试玩（共享上方配置） */}
        <PlaygroundPanel
          apiKey={currentKey}
          model={selectedModel}
          protocol={protocol}
        />
      </Card>

      <KeyGenerateModal
        open={keyModalOpen}
        onClose={() => setKeyModalOpen(false)}
        onKeyCreated={handleKeyCreated}
      />
    </div>
  );
}
