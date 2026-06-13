import { useEffect, useState } from 'react';
import { Modal } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateProvider } from '@/services/query/useProviders';
import { ProviderForm, type ProviderFormValue } from './ProviderForm';
import type { FC } from 'react';

interface ProviderCreateModalProps {
  open: boolean;
  onClose: () => void;
}

const EMPTY_VALUE: ProviderFormValue = {
  code: '',
  name: '',
};

/**
 * 供应商创建弹窗
 *
 * <p>任务 10.1 重构后：仅作为 ProviderForm 的 Modal 包装，
 * 用于批量导入等仍需独立创建供应商的入口；表单逻辑全部下沉到 ProviderForm。</p>
 */
export const ProviderCreateModal: FC<ProviderCreateModalProps> = ({ open, onClose }) => {
  const { t } = useTranslation('channels');
  const createProvider = useCreateProvider();
  const [value, setValue] = useState<ProviderFormValue>(EMPTY_VALUE);

  // Modal 打开时重置表单值
  useEffect(() => {
    if (open) {
      setValue(EMPTY_VALUE);
    }
  }, [open]);

  /** 触发提交：将 ProviderFormValue 映射回 useCreateProvider 所需的 CreateProviderRequest */
  const handleCreate = async () => {
    if (!value.code || !value.name) {
      // 简单兜底：必填项缺失（详细错误由表单 rules 渲染）
      return;
    }
    await createProvider.mutateAsync({
      code: value.code,
      providerName: value.name,
      description: value.description,
      websiteUrl: value.websiteUrl,
      apiDocUrl: value.apiDocUrl,
    });
    onClose();
  };

  return (
    <Modal
      title={t('providerCreate.title')}
      open={open}
      onCancel={onClose}
      onOk={handleCreate}
      confirmLoading={createProvider.isPending}
      width={480}
      destroyOnClose
    >
      <ProviderForm value={value} onChange={setValue} />
    </Modal>
  );
};
