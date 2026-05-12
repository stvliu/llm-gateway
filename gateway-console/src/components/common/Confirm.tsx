import { Modal } from 'antd';
import { ExclamationCircleFilled } from '@ant-design/icons';

/**
 * 删除确认框组件（用于声明式使用）
 */
export interface DeleteConfirmModalProps {
  open: boolean;
  title: string;
  description?: string;
  okText?: string;
  cancelText?: string;
  loading?: boolean;
  onConfirm: () => void | Promise<void>;
  onCancel: () => void;
}

export function DeleteConfirmModal({
  open,
  title,
  description,
  okText = '删除',
  cancelText = '取消',
  loading,
  onConfirm,
  onCancel,
}: DeleteConfirmModalProps) {
  return (
    <Modal
      open={open}
      onCancel={onCancel}
      onOk={onConfirm}
      okText={okText}
      cancelText={cancelText}
      okButtonProps={{ danger: true, loading }}
      centered
      title={
        <span>
          <ExclamationCircleFilled style={{ marginRight: 8 }} />
          {title}
        </span>
      }
    >
      {description && <p style={{ marginBottom: 0 }}>{description}</p>}
    </Modal>
  );
}