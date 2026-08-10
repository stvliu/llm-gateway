/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useState, useCallback } from 'react';
import { Modal, Input, Button, Space, Steps, Alert, App, Upload, Tag, Typography, theme } from 'antd';
import { CopyOutlined, UploadOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useCreateProvider } from '@/services/query/useProviders';
import { useAddChannel } from '@/services/query/useChannels';

const { Text } = Typography;

interface ParsedProvider {
  code: string;
  name: string;
  description?: string;
  website_url?: string;
  api_doc_url?: string;
  channels: {
    name: string;
  }[];
}

interface ParseResult {
  valid: boolean;
  data?: ParsedProvider[];
  error?: string;
  warnings?: string[];
}

/** 解析并验证 YAML/JSON 输入 */
function parseImportContent(content: string, t: (key: string, params?: Record<string, unknown>) => string): ParseResult {
  let data: unknown;
  try {
    data = JSON.parse(content);
  } catch {
    try {
      // 简易 YAML 解析尝试
      const lines = content.split('\n').filter((l) => l.trim());
      const result: Record<string, unknown> = {};
      for (const line of lines) {
        const trimmed = line.trim();
        if (trimmed.startsWith('#') || !trimmed) continue;
        const colonIdx = trimmed.indexOf(':');
        if (colonIdx === -1) continue;
        const key = trimmed.slice(0, colonIdx).trim();
        let value: unknown = trimmed.slice(colonIdx + 1).trim();
        if (!value) continue;
        if ((value as string).startsWith('"') && (value as string).endsWith('"')) {
          value = (value as string).slice(1, -1);
        }
        (result as Record<string, unknown>)[key] = value;
      }
      data = result;
    } catch (e) {
      return { valid: false, error: t('batch.parseFailMsg', { msg: e instanceof Error ? e.message : '' }) };
    }
  }

  if (!data || typeof data !== 'object' || Array.isArray(data)) {
    return { valid: false, error: t('batch.configMustBeObject') };
  }

  const root = data as Record<string, unknown>;
  if (!Array.isArray(root.providers)) {
    return { valid: false, error: t('batch.mustContainProviders') };
  }

  const providers: ParsedProvider[] = [];
  const warnings: string[] = [];

  for (let i = 0; i < root.providers.length; i++) {
    const p = root.providers[i];
    if (!p || typeof p !== 'object' || Array.isArray(p)) {
      return { valid: false, error: t('batch.notValidObject', { idx: i }) };
    }
    const provider = p as Record<string, unknown>;
    if (!provider.name) {
      return { valid: false, error: t('batch.missingName', { idx: i }) };
    }
    if (!provider.code) {
      warnings.push(t('batch.missingCode', { idx: i, name: provider.name }));
    }
    providers.push({
      code: (provider.code as string) || (provider.name as string).toLowerCase().replace(/\s+/g, '-'),
      name: provider.name as string,
      description: provider.description as string | undefined,
      website_url: provider.website_url as string | undefined,
      api_doc_url: provider.api_doc_url as string | undefined,
      channels: Array.isArray(provider.channels)
        ? provider.channels
            .filter((c: unknown) => c && typeof c === 'object')
            .map((c: Record<string, unknown>) => ({ name: (c.name as string) || '' }))
        : [],
    });
  }

  if (providers.length === 0) {
    return { valid: false, error: t('batch.noValidProvider') };
  }

  return { valid: true, data: providers, warnings };
}

export default function BatchImportModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { t } = useTranslation('channels');
  const { message } = App.useApp();
  const { token } = theme.useToken();
  const createProviderMutation = useCreateProvider();
  const addChannelMutation = useAddChannel();

  const [step, setStep] = useState(0);
  const [inputValue, setInputValue] = useState('');
  const [parseResult, setParseResult] = useState<ParseResult | null>(null);
  const [importing, setImporting] = useState(false);

  const handlePasteFromClipboard = useCallback(async () => {
    try {
      const text = await navigator.clipboard.readText();
      setInputValue(text);
      message.success(t('batch.pasted'));
    } catch {
      message.error(t('batch.pasteFailed'));
    }
  }, [message, t]);

  const handleParse = useCallback(() => {
    if (!inputValue.trim()) {
      message.warning(t('batch.emptyInput'));
      return;
    }
    const result = parseImportContent(inputValue, t);
    setParseResult(result);
    if (!result.valid) {
      message.error(t('batch.parseError'));
    } else {
      setStep(1);
    }
  }, [inputValue, message, t]);

  const handleImport = useCallback(async () => {
    if (!parseResult?.data) return;
    setImporting(true);
    try {
      for (const provider of parseResult.data) {
        const created = await createProviderMutation.mutateAsync({
          code: provider.code,
          providerName: provider.name,
          websiteUrl: provider.website_url,
        });
        if (provider.channels.length > 0) {
          for (const ch of provider.channels) {
            await addChannelMutation.mutateAsync({
              providerId: created.id,
              data: { name: ch.name, providerId: created.id, billingMode: 'pay_as_you_go' },
            });
          }
        } else {
          await addChannelMutation.mutateAsync({
            providerId: created.id,
            data: { name: t('batch.defaultChannel', { name: created.providerName }), providerId: created.id, billingMode: 'pay_as_you_go' },
          });
        }
      }
      message.success(t('batch.importSuccess', { count: parseResult.data.length }));
      handleClose();
    } catch (err) {
      const errMsg = err instanceof Error ? err.message : '';
      message.error(errMsg || t('batch.importFailed'));
    } finally {
      setImporting(false);
    }
  }, [parseResult, createProviderMutation, addChannelMutation, message, t]);

  const handleClose = () => {
    setStep(0);
    setInputValue('');
    setParseResult(null);
    onClose();
  };

  return (
    <Modal
      title={t('batch.importTitle')}
      open={open}
      onCancel={handleClose}
      width={720}
      destroyOnHidden
      footer={null}
    >
      <Steps
        current={step}
        items={[
          { title: t('batch.parse') },
          { title: t('batch.confirmImport') },
        ]}
        style={{ marginBottom: 24 }}
      />

      {step === 0 && (
        <>
          <Alert
            message={t('batch.formatHint')}
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
          />
          {parseResult && !parseResult.valid && (
            <Alert
              message={t('batch.validationError')}
              description={parseResult.error}
              type="error"
              showIcon
              style={{ marginBottom: 12 }}
            />
          )}
          {parseResult?.warnings && parseResult.warnings.length > 0 && (
            <Alert
              message={t('batch.hasWarnings')}
              description={<ul style={{ margin: 0, paddingLeft: 16 }}>{parseResult.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul>}
              type="warning"
              showIcon
              style={{ marginBottom: 12 }}
            />
          )}
          <Input.TextArea
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            rows={12}
            style={{ fontFamily: 'monospace', fontSize: 13 }}
            placeholder={`version: "1.0"\nproviders:\n  - name: OpenAI\n    code: openai\n    channels:\n      - name: default`}
          />
          <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
            <Button icon={<CopyOutlined />} onClick={handlePasteFromClipboard}>
              {t('batch.fromClipboard')}
            </Button>
            <Upload accept=".yaml,.yml,.json" showUploadList={false} beforeUpload={(file) => {
              const reader = new FileReader();
              reader.onload = (e) => setInputValue(e.target?.result as string || '');
              reader.readAsText(file);
              return false;
            }}>
              <Button icon={<UploadOutlined />}>{t('batch.uploadFile')}</Button>
            </Upload>
            {parseResult?.valid && parseResult.data && (
              <Tag icon={<CheckCircleOutlined />} color="success" style={{ marginLeft: 'auto', alignSelf: 'center' }}>
                {t('batch.validConfig', { count: parseResult.data.length })}
              </Tag>
            )}
          </div>
          <div style={{ marginTop: 16, textAlign: 'right' }}>
            <Space>
              <Button onClick={handleClose}>{t('batch.cancel')}</Button>
              <Button type="primary" onClick={handleParse} disabled={!parseResult?.valid}>
                {t('batch.parse')}
              </Button>
            </Space>
          </div>
        </>
      )}

      {step === 1 && parseResult?.data && (
        <>
          <Alert
            message={t('batch.parseSuccess')}
            type="success"
            showIcon
            style={{ marginBottom: 16 }}
          />
          <div style={{ maxHeight: 320, overflow: 'auto' }}>
            {parseResult.data.map((provider) => (
              <div key={provider.code} style={{ marginBottom: 12, padding: 12, background: token.colorFillQuaternary, borderRadius: token.borderRadiusSM }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                  <Text strong>{provider.name}</Text>
                  <Tag>{provider.code}</Tag>
                  {provider.channels.length > 0 && (
                    <Tag color="blue">{t('batch.channelCountLabel', { count: provider.channels.length })}</Tag>
                  )}
                </div>
                {provider.channels.map((ch, j) => (
                  <Text key={j} type="secondary" style={{ fontSize: 12, marginLeft: 16 }}>- {ch.name}</Text>
                ))}
              </div>
            ))}
          </div>
          <div style={{ marginTop: 16, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setStep(0)}>{t('batch.back')}</Button>
              <Button type="primary" onClick={handleImport} loading={importing}>
                {t('batch.confirmImport')}
              </Button>
            </Space>
          </div>
        </>
      )}
    </Modal>
  );
}