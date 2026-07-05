import { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { App } from 'antd';
import { useCreateApplication, useUpdateApplication } from '@/services/query/useApplications';
import type { Application } from '@/types/application';

interface ApplicationFormModalProps {
  visible: boolean;
  application?: Application;
  onClose: () => void;
}

/**
 * 应用创建/编辑表单弹窗
 *
 * <p>承载应用聚合根可编辑字段：code（全局唯一）、name、description、timeout、failureStrategy。
 * timeout=0 表示用渠道默认（承接原 ResilienceProfile.timeout，Task 8）。
 * failureStrategy 控制渠道故障时转移行为，默认 FAIL_RETRY（Task 10）。</p>
 */
export default function ApplicationFormModal({ visible, application, onClose }: ApplicationFormModalProps) {
  const { t } = useTranslation('applications');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const isEdit = !!application;

  const createMutation = useCreateApplication();
  const updateMutation = useUpdateApplication();
  const loading = createMutation.isPending || updateMutation.isPending;

  useEffect(() => {
    if (visible) {
      if (application) {
        form.setFieldsValue({
          code: application.code,
          name: application.name,
          description: application.description,
          timeout: application.timeout ?? 0,
          failureStrategy: application.failureStrategy,
        });
      } else {
        form.resetFields();
        // 新建默认 timeout=0（用渠道默认），failureStrategy=FAIL_RETRY
        form.setFieldsValue({ timeout: 0, failureStrategy: 'FAIL_RETRY' });
      }
    }
  }, [visible, application, form]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    try {
      if (isEdit) {
        await updateMutation.mutateAsync({ id: application!.id, data: values });
        message.success(t('application.editApplication'));
      } else {
        await createMutation.mutateAsync(values);
        message.success(t('application.addApplication'));
      }
      onClose();
    } catch {
      message.error(isEdit ? t('application.editApplication') : t('application.addApplication'));
    }
  };

  return (
    <Modal
      title={isEdit ? t('application.editApplication') : t('application.addApplication')}
      open={visible}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={loading}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="code"
          label={t('application.code')}
          rules={[{ required: true, message: t('application.codeRequired') }]}
        >
          <Input disabled={isEdit} placeholder="例如：APP-001" />
        </Form.Item>
        <Form.Item
          name="name"
          label={t('application.name')}
          rules={[{ required: true, message: t('application.nameRequired') }]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="description" label={t('application.description')}>
          <Input.TextArea rows={3} />
        </Form.Item>
        <Form.Item
          name="timeout"
          label={t('application.timeout')}
          tooltip={t('application.timeoutHelp')}
        >
          <InputNumber min={0} style={{ width: '100%' }} placeholder="0" />
        </Form.Item>
        <Form.Item
          name="failureStrategy"
          label={t('application.failureStrategy')}
          tooltip={t('application.failureStrategyHelp')}
        >
          <Select placeholder={t('application.failureStrategyPlaceholder')}>
            <Select.Option value="FAIL_FAST">
              {t('application.failureStrategyFailFast')}
            </Select.Option>
            <Select.Option value="FAIL_RETRY">
              {t('application.failureStrategyFailRetry')}
            </Select.Option>
            <Select.Option value="FAIL_OVER">
              {t('application.failureStrategyFailOver')}
            </Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
}
