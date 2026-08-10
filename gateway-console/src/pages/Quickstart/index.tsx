/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useState, useEffect } from 'react';
import { Row, Col, Input, Typography, Empty, Skeleton, Card } from 'antd';
import { useTranslation } from 'react-i18next';
import { useModels } from '@/services/query/useModels';
import ModelCard from './ModelCard';
import ApiKeySelector from './ApiKeySelector';
import CodeSnippet from './CodeSnippet';
import type { Protocol } from './CodeSnippet';
import KeyGenerateModal from './KeyGenerateModal';
import PlaygroundPanel from './PlaygroundPanel';

const { Title, Paragraph, Text } = Typography;

export default function Quickstart() {
  const { t } = useTranslation('quickstart');
  const { data: models, isLoading } = useModels({ limit: 1000 });
  const [search, setSearch] = useState('');
  const [keyModalOpen, setKeyModalOpen] = useState(false);

  // 页面级联动状态
  const [currentKey, setCurrentKey] = useState<string>();
  const [currentKeyId, setCurrentKeyId] = useState<number>();
  const [selectedModel, setSelectedModel] = useState<string>('');
  const [protocol, setProtocol] = useState<Protocol>('openai');

  const filtered = models?.items?.filter((m) =>
    m.state === 'ACTIVE' &&
    (m.displayName || m.modelName).toLowerCase().includes(search.toLowerCase())
  ) ?? [];

  // 模型列表（给 CodeSnippet 的 Select 用）
  const modelNames = filtered.map((m) => m.modelName);

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

  const handleModelSelect = (modelName: string) => {
    setSelectedModel(modelName);
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

      {/* 区块 1: API Key 快捷区 */}
      <div style={{ marginBottom: 24 }}>
        <ApiKeySelector
          currentKey={currentKey}
          currentKeyId={currentKeyId}
          onKeyChange={handleKeyChange}
          onCreateClick={() => setKeyModalOpen(true)}
        />
      </div>

      {/* 区块 2: 模型选择 + 代码示例 */}
      <div style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <Text strong>{t('search').replace('...', '')}</Text>
          <Input.Search
            placeholder={t('search')}
            style={{ width: 280 }}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            allowClear
          />
        </div>

        {isLoading ? (
          <Row gutter={[12, 12]}>
            {Array.from({ length: 6 }).map((_, i) => (
              <Col key={i} xs={24} sm={12} md={8}>
                <Skeleton active paragraph={{ rows: 2 }} />
              </Col>
            ))}
          </Row>
        ) : filtered.length === 0 ? (
          <Empty description={t('noModels')} />
        ) : (
          <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
            {filtered.map((m) => (
              <Col key={m.id} xs={24} sm={12} md={8}>
                <ModelCard
                  model={m}
                  selected={m.modelName === selectedModel}
                  onSelect={(model) => handleModelSelect(model.modelName)}
                />
              </Col>
            ))}
          </Row>
        )}

        <CodeSnippet
          apiKey={currentKey}
          model={selectedModel}
          models={modelNames}
          protocol={protocol}
          onModelChange={setSelectedModel}
          onProtocolChange={setProtocol}
        />
      </div>

      {/* 区块 3: 轻量试玩 */}
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
