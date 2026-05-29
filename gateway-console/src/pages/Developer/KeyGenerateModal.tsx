import { useState } from 'react';
import { Modal, App, Result, Button, Typography } from 'antd';
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
        name: `开发者 Key - ${new Date().toLocaleDateString()}`,
      });
      setResult(res);
      onKeyCreated?.(res.keyPlain);
      message.success(t('keyCreated', { defaultValue: 'API Key 创建成功' }));
    } catch {
      message.error(t('keyCreateFailed', { defaultValue: '创建失败' }));
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setResult(null);
    onClose();
  };

  return (
    <Modal
      title={t('createKey', { defaultValue: '创建 API Key' })}
      open={open}
      onCancel={handleClose}
      footer={result ? (
        <Button type="primary" onClick={handleClose}>
          {t('done', { defaultValue: '完成' })}
        </Button>
      ) : (
        <Button type="primary" loading={loading} onClick={handleCreate}>
          {t('generate', { defaultValue: '一键生成' })}
        </Button>
      )}
      width={560}
    >
      {result ? (
        <Result
          status="success"
          title={t('keyCreated', { defaultValue: 'API Key 已创建' })}
          subTitle={t('keySaveHint', { defaultValue: '请立即复制，关闭后不再显示' })}
          extra={[
            <div key="key" style={{ background: '#1e293b', color: '#e2e8f0', padding: '12px 16px', borderRadius: 8, fontFamily: 'monospace', fontSize: 13, wordBreak: 'break-all' }}>
              {result.keyPlain}
            </div>,
          ]}
        />
      ) : (
        <Paragraph type="secondary">
          {t('createKeyHint', { defaultValue: '点击下方按钮创建 API Key，创建后自动关联到当前用户，可调用团队已开通的所有模型。' })}
        </Paragraph>
      )}
    </Modal>
  );
}