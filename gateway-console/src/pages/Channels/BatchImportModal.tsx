import { useState, useMemo } from 'react';
import { Modal, Input, Upload, Button, Space, Alert, App, Tag, Typography } from 'antd';
import { UploadOutlined, CopyOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useCreateProvider } from '@/services/query/useProviders';
import { useAddChannel } from '@/services/query/useChannels';

const { TextArea } = Input;
const { Text } = Typography;

interface Props {
  open: boolean;
  onClose: () => void;
}

interface ParsedProvider {
  name: string;
  code?: string;
  websiteUrl?: string;
  channels?: { name: string }[];
}

interface ParseResult {
  valid: boolean;
  providers: ParsedProvider[];
  errors: string[];
  warnings: string[];
}

/** 解析并验证 YAML/JSON 输入 */
function parseImportContent(text: string): ParseResult {
  const errors: string[] = [];
  const warnings: string[] = [];
  let data: unknown;

  // 尝试 JSON 解析
  try {
    data = JSON.parse(text);
  } catch {
    // 尝试简易 YAML 解析（不支持完整 YAML 规范，仅支持缩进式）
    try {
      data = parseSimpleYaml(text);
    } catch (e) {
      errors.push(`格式解析失败：${e instanceof Error ? e.message : '无法识别的格式'}，请使用标准 JSON 格式`);
      return { valid: false, providers: [], errors, warnings };
    }
  }

  if (!data || typeof data !== 'object') {
    errors.push('配置内容必须是一个对象');
    return { valid: false, providers: [], errors, warnings };
  }

  const root = data as Record<string, unknown>;
  const providersRaw = root.providers;

  if (!Array.isArray(providersRaw)) {
    errors.push('配置必须包含 providers 数组');
    return { valid: false, providers: [], errors, warnings };
  }

  const providers: ParsedProvider[] = [];
  providersRaw.forEach((item: unknown, idx: number) => {
    if (!item || typeof item !== 'object') {
      errors.push(`providers[${idx}] 不是有效的对象`);
      return;
    }
    const p = item as Record<string, unknown>;
    if (!p.name || typeof p.name !== 'string') {
      errors.push(`providers[${idx}] 缺少 name 字段`);
      return;
    }
    const provider: ParsedProvider = { name: p.name as string };
    if (p.code && typeof p.code === 'string') provider.code = p.code;
    else warnings.push(`providers[${idx}] "${p.name}" 缺少 code 字段，将自动生成`);

    if (p.websiteUrl && typeof p.websiteUrl === 'string') provider.websiteUrl = p.websiteUrl;

    if (Array.isArray(p.channels)) {
      provider.channels = p.channels
        .filter((c: unknown) => c && typeof c === 'object' && (c as Record<string, unknown>).name)
        .map((c: Record<string, unknown>) => ({ name: c.name as string }));
    }

    providers.push(provider);
  });

  if (providers.length === 0) {
    errors.push('未找到有效的供应商配置');
  }

  return { valid: errors.length === 0, providers, errors, warnings };
}

/** 简易 YAML 解析器（支持基本缩进格式） */
function parseSimpleYaml(text: string): unknown {
  const lines = text.split('\n').map((l) => l.replace(/#.*$/, '').trimEnd()).filter((l) => l.trim());
  const result: Record<string, unknown> = {};
  const stack: { obj: Record<string, unknown>; indent: number }[] = [{ obj: result, indent: -1 }];

  for (const line of lines) {
    const indent = line.search(/\S/);
    const content = line.trim();

    // 数组项
    if (content.startsWith('- ')) {
      const parent = stack[stack.length - 1];
      const key = Object.keys(parent.obj).pop();
      if (!key) continue;
      const arr = parent.obj[key];
      if (Array.isArray(arr)) {
        const value = content.slice(2).trim();
        if (value.includes(': ')) {
          const item: Record<string, unknown> = {};
          value.split(', ').forEach((pair) => {
            const [k, v] = pair.split(': ');
            if (k && v) item[k.trim()] = parseYamlValue(v.trim());
          });
          arr.push(item);
        } else {
          arr.push(parseYamlValue(value));
        }
      }
      continue;
    }

    // 键值对
    const colonIdx = content.indexOf(':');
    if (colonIdx === -1) continue;

    const key = content.slice(0, colonIdx).trim();
    let value = content.slice(colonIdx + 1).trim();

    // 弹出栈中缩进 >= 当前的层级
    while (stack.length > 1 && stack[stack.length - 1].indent >= indent) {
      stack.pop();
    }

    const current = stack[stack.length - 1].obj;

    if (!value) {
      // 下一层级是数组或对象
      const nextLine = lines[lines.indexOf(line) + 1];
      if (nextLine && nextLine.trim().startsWith('- ')) {
        current[key] = [];
      } else {
        current[key] = {};
        stack.push({ obj: current[key] as Record<string, unknown>, indent });
      }
    } else {
      // 去除引号
      if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
        value = value.slice(1, -1);
      }
      current[key] = parseYamlValue(value);
    }
  }

  return result;
}

function parseYamlValue(v: string): unknown {
  if (v === 'true') return true;
  if (v === 'false') return false;
  if (v === 'null' || v === '~') return null;
  if (/^-?\d+$/.test(v)) return parseInt(v, 10);
  if (/^-?\d+\.\d+$/.test(v)) return parseFloat(v);
  return v;
}

export default function BatchImportModal({ open, onClose }: Props) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();
  const [importText, setImportText] = useState('');
  const [step, setStep] = useState<'input' | 'preview'>('input');
  const [importing, setImporting] = useState(false);

  const createProviderMutation = useCreateProvider();
  const addChannelMutation = useAddChannel();

  const parseResult = useMemo(() => {
    if (!importText.trim()) return null;
    return parseImportContent(importText);
  }, [importText]);

  const handlePasteFromClipboard = async () => {
    try {
      const text = await navigator.clipboard.readText();
      setImportText(text);
      message.success(t('batch.pasted', { defaultValue: '已从剪贴板粘贴' }));
    } catch {
      message.error(t('batch.pasteFailed', { defaultValue: '剪贴板读取失败' }));
    }
  };

  const handleParse = () => {
    if (!importText.trim()) {
      message.warning(t('batch.emptyInput', { defaultValue: '请输入配置内容' }));
      return;
    }
    if (parseResult && !parseResult.valid) {
      message.error(t('batch.parseError', { defaultValue: '配置格式有误，请修正后重试' }));
      return;
    }
    setStep('preview');
  };

  const handleImport = async () => {
    if (!parseResult || !parseResult.valid) return;
    setImporting(true);
    try {
      for (const provider of parseResult.providers) {
        const code = provider.code || provider.name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
        const created = await createProviderMutation.mutateAsync({
          code,
          providerName: provider.name,
          websiteUrl: provider.websiteUrl,
        });
        // 创建默认通道
        if (provider.channels?.length) {
          for (const ch of provider.channels) {
            await addChannelMutation.mutateAsync({
              providerId: created.id,
              data: { name: ch.name, providerId: created.id, billingMode: 'pay_as_you_go' },
            });
          }
        } else {
          await addChannelMutation.mutateAsync({
            providerId: created.id,
            data: { name: `${created.providerName} 默认通道`, providerId: created.id, billingMode: 'pay_as_you_go' },
          });
        }
      }
      message.success(t('batch.importSuccess', { defaultValue: `成功导入 ${parseResult.providers.length} 个供应商` }));
      handleClose();
    } catch (err) {
      const errMsg = err instanceof Error ? err.message : '';
      message.error(errMsg || t('batch.importFailed', { defaultValue: '导入失败' }));
    } finally {
      setImporting(false);
    }
  };

  const handleClose = () => {
    setImportText('');
    setStep('input');
    onClose();
  };

  return (
    <Modal
      title={t('batch.import', { defaultValue: '批量导入配置' })}
      open={open}
      onCancel={handleClose}
      footer={
        step === 'input' ? (
          <Space>
            <Button onClick={handleClose}>{t('batch.cancel', { defaultValue: '取消' })}</Button>
            <Button type="primary" onClick={handleParse} disabled={!parseResult?.valid}>
              {t('batch.parse', { defaultValue: '解析预览' })}
            </Button>
          </Space>
        ) : (
          <Space>
            <Button onClick={() => setStep('input')}>{t('batch.back', { defaultValue: '返回' })}</Button>
            <Button type="primary" onClick={handleImport} loading={importing}>
              {t('batch.confirmImport', { defaultValue: '确认导入' })}
            </Button>
          </Space>
        )
      }
      width={640}
      destroyOnHidden
    >
      {step === 'input' ? (
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Alert
            type="info"
            message={t('batch.formatHint', { defaultValue: '支持 YAML / JSON 格式，导入不包含 API Key 明文' })}
          />
          {parseResult && !parseResult.valid && (
            <Alert
              type="error"
              message={t('batch.validationError', { defaultValue: '格式验证失败' })}
              description={
                <ul style={{ margin: 0, paddingLeft: 16 }}>
                  {parseResult.errors.map((e, i) => <li key={i}>{e}</li>)}
                </ul>
              }
            />
          )}
          {parseResult && parseResult.valid && parseResult.warnings.length > 0 && (
            <Alert
              type="warning"
              message={t('batch.hasWarnings', { defaultValue: '存在警告' })}
              description={
                <ul style={{ margin: 0, paddingLeft: 16 }}>
                  {parseResult.warnings.map((w, i) => <li key={i}>{w}</li>)}
                </ul>
              }
            />
          )}
          <TextArea
            value={importText}
            onChange={(e) => setImportText(e.target.value)}
            placeholder={`version: "1.0"\nproviders:\n  - name: OpenAI\n    code: openai\n    channels:\n      - name: 主通道`}
            rows={12}
            style={{ fontFamily: 'monospace', fontSize: 13 }}
          />
          <div style={{ display: 'flex', gap: 8 }}>
            <Button icon={<CopyOutlined />} onClick={handlePasteFromClipboard}>
              {t('batch.fromClipboard', { defaultValue: '从剪贴板粘贴' })}
            </Button>
            <Upload accept=".yaml,.yml,.json" showUploadList={false} beforeUpload={(file) => {
              const reader = new FileReader();
              reader.onload = (e) => setImportText(e.target?.result as string || '');
              reader.readAsText(file);
              return false;
            }}>
              <Button icon={<UploadOutlined />}>{t('batch.uploadFile', { defaultValue: '上传文件' })}</Button>
            </Upload>
            {parseResult?.valid && (
              <Tag icon={<CheckCircleOutlined />} color="success" style={{ marginLeft: 'auto', alignSelf: 'center' }}>
                {t('batch.validConfig', { defaultValue: `检测到 ${parseResult.providers.length} 个供应商` })}
              </Tag>
            )}
          </div>
        </Space>
      ) : (
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Alert type="success" message={t('batch.parseSuccess', { defaultValue: '解析成功，请确认以下配置' })} />
          {parseResult?.providers.map((p, i) => (
            <div key={i} style={{ background: '#f8fafc', padding: 12, borderRadius: 8, border: '1px solid #e2e8f0' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                <Text strong>{p.name}</Text>
                {p.code && <Tag>{p.code}</Tag>}
                {p.channels?.length && <Tag color="blue">{p.channels.length} 个通道</Tag>}
              </div>
              {p.channels?.map((ch, j) => (
                <Text key={j} type="secondary" style={{ fontSize: 12, marginLeft: 16 }}>- {ch.name}</Text>
              ))}
            </div>
          ))}
        </Space>
      )}
    </Modal>
  );
}
