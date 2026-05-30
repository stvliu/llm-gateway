import { Modal, Typography, Tag, Space } from 'antd';
import { useTranslation } from 'react-i18next';
import { App } from 'antd';
import {
  useMaterializeProvider,
  useMaterializePlan,
  useMaterializeModel,
} from '@/services/query/useCatalog';
import type { MaterializeType } from '@/types/catalog';

const { Text } = Typography;

interface MaterializeModalProps {
  /** 弹窗是否打开 */
  open: boolean;
  /** 物化类型 */
  type: MaterializeType;
  /** 物化目标编码 */
  code: string;
  /** 物化目标名称 */
  name: string;
  /** 关闭弹窗 */
  onClose: () => void;
}

/** 物化类型标签配置 */
const MATERIALIZE_TYPE_CONFIG: Record<MaterializeType, { color: string; labelKey: string }> = {
  PROVIDER: { color: 'blue', labelKey: 'materialize.provider' },
  PLAN: { color: 'purple', labelKey: 'materialize.plan' },
  MODEL: { color: 'cyan', labelKey: 'materialize.model' },
};

/** 物化确认弹窗 */
export default function MaterializeModal({ open, type, code, name, onClose }: MaterializeModalProps) {
  const { t } = useTranslation('catalog');
  const { message } = App.useApp();

  // 物化 mutation hooks
  const materializeProviderMutation = useMaterializeProvider();
  const materializePlanMutation = useMaterializePlan();
  const materializeModelMutation = useMaterializeModel();

  /** 当前物化类型对应的 mutation */
  const activeMutation = () => {
    switch (type) {
      case 'PROVIDER': return materializeProviderMutation;
      case 'PLAN': return materializePlanMutation;
      case 'MODEL': return materializeModelMutation;
    }
  };

  /** 确认物化 */
  const handleConfirm = async () => {
    const mutation = activeMutation();
    try {
      await mutation.mutateAsync(code);
      message.success(t('message.materializeSuccess'));
      onClose();
    } catch {
      message.error(t('message.materializeFailed'));
    }
  };

  const typeConfig = MATERIALIZE_TYPE_CONFIG[type];
  const isPending = activeMutation().isPending;

  return (
    <Modal
      title={t('materialize.confirmTitle')}
      open={open}
      onCancel={onClose}
      onOk={handleConfirm}
      confirmLoading={isPending}
      okText={t('materialize.confirmTitle')}
    >
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        {/* 物化类型标签 */}
        <div>
          <Tag color={typeConfig.color}>{t(typeConfig.labelKey)}</Tag>
        </div>

        {/* 物化目标信息 */}
        <div>
          <Text type="secondary">{t('materialize.confirmContent', { name })}</Text>
        </div>

        {/* 编码信息 */}
        <div style={{ padding: '8px 12px', background: 'var(--ant-color-fill-quaternary)', borderRadius: 6 }}>
          <Space>
            <Text type="secondary" style={{ fontSize: 12 }}>Code:</Text>
            <Text code style={{ fontSize: 12 }}>{code}</Text>
          </Space>
        </div>
      </Space>
    </Modal>
  );
}
