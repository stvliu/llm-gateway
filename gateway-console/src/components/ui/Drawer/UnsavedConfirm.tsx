import { Modal, Space, Button, theme } from 'antd';
import { WarningOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

export interface UnsavedConfirmProps {
  /** 是否打开 */
  open: boolean;
  /** 继续编辑回调 */
  onContinue: () => void;
  /** 放弃更改回调 */
  onDiscard: () => void;
  /** 取消回调（关闭对话框） */
  onCancel: () => void;
  /** 自定义标题 */
  title?: string;
  /** 自定义消息 */
  message?: string;
}

/**
 * 未保存更改确认对话框
 * 使用 Ant Design token 响应主题切换
 */
export function UnsavedConfirm({
  open,
  onContinue,
  onDiscard,
  onCancel,
  title,
  message,
}: UnsavedConfirmProps) {
  const { t } = useTranslation();
  const { token } = theme.useToken();

  return (
    <Modal
      open={open}
      onCancel={onCancel}
      footer={null}
      closable={false}
      width={400}
      centered
      className="unsaved-confirm-modal"
    >
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '24px 0' }}>
        {/* 警告图标 */}
        <div
          style={{
            width: 48,
            height: 48,
            borderRadius: '50%',
            background: token.colorWarningBg,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: 16,
          }}
        >
          <WarningOutlined style={{ fontSize: 24, color: token.colorWarning }} />
        </div>

        {/* 标题 */}
        <h3 style={{ fontSize: 18, fontWeight: 500, color: token.colorText, marginBottom: 8 }}>
          {title || t('confirm.unsavedTitle')}
        </h3>

        {/* 消息 */}
        <p
          style={{
            fontSize: 14,
            color: token.colorTextSecondary,
            textAlign: 'center',
            marginBottom: 24,
          }}
        >
          {message || t('confirm.unsavedMessage')}
        </p>

        {/* 操作按钮 */}
        <Space size="middle">
          <Button onClick={onContinue}>
            {t('confirm.continueEdit')}
          </Button>
          <Button type="primary" danger onClick={onDiscard}>
            {t('confirm.discard')}
          </Button>
        </Space>
      </div>
    </Modal>
  );
}