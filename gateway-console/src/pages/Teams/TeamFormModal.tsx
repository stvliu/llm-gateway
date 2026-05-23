import { useEffect } from 'react';
import { Modal, Form, Input } from 'antd';
import { useTranslation } from 'react-i18next';
import { App } from 'antd';
import { useCreateTeam, useUpdateTeam } from '@/services/query/useTeams';
import type { Team } from '@/types/team';

interface TeamFormModalProps {
  visible: boolean;
  team?: Team;
  onClose: () => void;
}

export default function TeamFormModal({ visible, team, onClose }: TeamFormModalProps) {
  const { t } = useTranslation('teams');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const isEdit = !!team;

  const createMutation = useCreateTeam();
  const updateMutation = useUpdateTeam();
  const loading = createMutation.isPending || updateMutation.isPending;

  useEffect(() => {
    if (visible) {
      if (team) {
        form.setFieldsValue({ name: team.name, description: team.description });
      } else {
        form.resetFields();
      }
    }
  }, [visible, team, form]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    try {
      if (isEdit) {
        await updateMutation.mutateAsync({ id: team!.id, data: values });
        message.success(t('team.editTeam'));
      } else {
        await createMutation.mutateAsync(values);
        message.success(t('team.addTeam'));
      }
      onClose();
    } catch {
      message.error(isEdit ? t('team.editTeam') : t('team.addTeam'));
    }
  };

  return (
    <Modal
      title={isEdit ? t('team.editTeam') : t('team.addTeam')}
      open={visible}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={loading}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item name="name" label={t('team.name')} rules={[{ required: true, message: t('team.nameRequired') }]}>
          <Input />
        </Form.Item>
        <Form.Item name="description" label={t('team.description')}>
          <Input.TextArea rows={3} />
        </Form.Item>
      </Form>
    </Modal>
  );
}