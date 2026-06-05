import { useState } from 'react';
import { Modal, App, Result, Button, Typography } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useCreateUserApiKey } from '@/services/query/useUserApiKeys';
import { useAuthStore } from '@/stores/authStore';
import type { CreateUserApiKeyResponse } from '@/types/team';

const { Paragraph } = Typography;

interface Props {
  open: boolean;
  onClose: () => void;
  onKeyCreated?: (key: string) => void;
}

export default function KeyGenerateModal({ open, onClose, onKeyCreated }: Props) {
  const { t } = useTranslation('developer');
  const { message } = App.useApp();
  const { user } = useAuthStore();
  const createMutation = useCreateUserApiKey();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<CreateUserApiKeyResponse | null>(null);

  const handleCreate = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const res = await createMutation.mutateAsync({
        userId: user.id,
        name: t('keyNamePrefix', { date: new Date().toLocaleDateString() }),
      });
      setResult(res);
      onKeyCreated?.(res.keyPlain);
      message.success(t('keyCreated'));
    } catch {
      message.error(t('keyCreateFailed'));
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setResult(null);
    onClose();
  };

  const handleCopyKey = async () => {
    try {
      await navigator.clipboard.writeText(result!.keyPlain);
      message.success(t('keyCopied'));
    } catch {
      message.error(t('keyCopyFailed'));
    }
  };

  return (
    <Modal
      title={t('createKey')}
      open={open}
      onCancel={handleClose}
      footer={result ? (
        <Button type="primary" onClick={handleClose}>
          {t('done')}
        </Button>
      ) : (
        <Button type="primary" loading={loading} onClick={handleCreate}>
          {t('generate')}
        </Button>
      )}
      width={560}
      destroyOnHidden
    >
      {result ? (
        <Result
          status="success"
          title={t('keyCreatedTitle')}
          subTitle={t('keySaveHint')}
          extra={[
            <div key="key" style={{ display: 'flex', alignItems: 'center', gap: 8, background: '#1e293b', color: '#e2e8f0', padding: '12px 16px', borderRadius: 8 }}>
              <span style={{ flex: 1, fontFamily: 'monospace', fontSize: 13, wordBreak: 'break-all' }}>
                {result.keyPlain}
              </span>
              <Button type="text" icon={<CopyOutlined />} style={{ color: '#e2e8f0' }} onClick={handleCopyKey} />
            </div>,
          ]}
        />
      ) : (
        <Paragraph type="secondary">
          {t('createKeyHint')}
        </Paragraph>
      )}
    </Modal>
  );
}