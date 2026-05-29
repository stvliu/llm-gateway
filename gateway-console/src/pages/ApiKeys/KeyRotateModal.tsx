import { Modal, Result, Button } from 'antd';
import { useTranslation } from 'react-i18next';

interface KeyRotateModalProps {
  open: boolean;
  onClose: () => void;
}

/**
 * Key 轮换模态框
 * 轮换成功后展示新旧 Key 宽限期提示
 */
export default function KeyRotateModal({ open, onClose }: KeyRotateModalProps) {
  const { t } = useTranslation('apiKeys');

  return (
    <Modal
      title={t('rotateKey', { defaultValue: '轮换 Key' })}
      open={open}
      onCancel={onClose}
      width={480}
      footer={<Button type="primary" onClick={onClose}>{t('done', { defaultValue: '完成' })}</Button>}
    >
      <Result
        status="success"
        title={t('rotateSuccess', { defaultValue: 'Key 轮换成功' })}
        subTitle={t('rotateHint', { defaultValue: '新旧 Key 将在 24 小时宽限期内同时有效' })}
      />
    </Modal>
  );
}