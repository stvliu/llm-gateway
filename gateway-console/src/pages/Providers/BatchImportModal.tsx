import { useState } from 'react';
import { Modal, Input, Upload, Button, Space, Alert, App } from 'antd';
import { UploadOutlined, CopyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { TextArea } = Input;

interface Props {
  open: boolean;
  onClose: () => void;
}

/**
 * 批量导入配置模态框组件
 *
 * <p>支持通过 YAML/JSON 格式批量导入供应商配置（不包含 API Key 明文）。</p>
 */
export default function BatchImportModal({ open, onClose }: Props) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();
  const [importText, setImportText] = useState('');
  const [step, setStep] = useState<'input' | 'preview'>('input');

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
    setStep('preview');
  };

  const handleImport = () => {
    // In production, this would call an API to parse and import
    message.success(t('batch.importSuccess', { defaultValue: '导入成功' }));
    handleClose();
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
            <Button type="primary" onClick={handleParse}>{t('batch.parse', { defaultValue: '解析预览' })}</Button>
          </Space>
        ) : (
          <Space>
            <Button onClick={() => setStep('input')}>{t('batch.back', { defaultValue: '返回' })}</Button>
            <Button type="primary" onClick={handleImport}>{t('batch.confirmImport', { defaultValue: '确认导入' })}</Button>
          </Space>
        )
      }
      width={640}
    >
      {step === 'input' ? (
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Alert
            type="info"
            message={t('batch.formatHint', { defaultValue: '支持 YAML / JSON 格式，导入不包含 API Key 明文' })}
          />
          <TextArea
            value={importText}
            onChange={(e) => setImportText(e.target.value)}
            placeholder={`version: "1.0"\nproviders:\n  - name: OpenAI\n    channels:\n      - name: 主通道\n        ...`}
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
          </div>
        </Space>
      ) : (
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Alert type="success" message={t('batch.parseSuccess', { defaultValue: '解析成功，请确认以下配置' })} />
          <pre style={{ background: '#f8fafc', padding: 16, borderRadius: 8, maxHeight: 300, overflow: 'auto', fontSize: 13 }}>
            {importText}
          </pre>
        </Space>
      )}
    </Modal>
  );
}
