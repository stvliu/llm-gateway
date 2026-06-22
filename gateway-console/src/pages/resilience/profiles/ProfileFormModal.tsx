import { useEffect } from 'react';
import { Modal, Form, Input, Select, InputNumber, Switch, Collapse, Tag } from 'antd';
import { useTranslation } from 'react-i18next';
import { App } from 'antd';
import {
  useCreateResilienceProfile,
  useUpdateResilienceProfile,
} from '@/services/query/useResilience';
import { modeToProfileDefaults, modeLabel } from '../mode';
import type { ResilienceProfile, ResilienceProfileRequest, ResilienceMode } from '@/types/resilience';

interface ProfileFormModalProps {
  visible: boolean;
  profile?: ResilienceProfile;
  onClose: () => void;
}

/** 三档位选项 */
const MODE_OPTIONS: { value: ResilienceMode; label: string }[] = [
  { value: 'STANDARD', label: '标准' },
  { value: 'STRICT', label: '严格' },
  { value: 'AGGRESSIVE', label: '激进' },
];

/**
 * 容灾画像创建/编辑表单弹窗
 *
 * <p>「选而非填」范式落地：管理员面对「容灾模式」档位 + 「降级兜底」开关，
 * 专家字段（会话亲和/模型锁定/超时）折叠在「高级配置」里。</p>
 *
 * <p>选档位时自动用 modeToProfileDefaults 预填降级兜底与超时默认值。</p>
 */
export default function ProfileFormModal({ visible, profile, onClose }: ProfileFormModalProps) {
  const { t } = useTranslation('resilience');
  const { message } = App.useApp();
  const [form] = Form.useForm<ResilienceProfileRequest>();
  const isEdit = !!profile;

  const createMutation = useCreateResilienceProfile();
  const updateMutation = useUpdateResilienceProfile();
  const loading = createMutation.isPending || updateMutation.isPending;

  useEffect(() => {
    if (visible) {
      if (profile) {
        form.setFieldsValue({
          code: profile.code,
          name: profile.name,
          mode: profile.mode,
          enableL2ModelDegradation: profile.enableL2ModelDegradation,
          degradationMaxDepth: profile.degradationMaxDepth,
          enableSessionAffinity: profile.enableSessionAffinity,
          sessionAffinityTtlMinutes: profile.sessionAffinityTtlMinutes,
          enablePinnedModel: profile.enablePinnedModel,
          pinnedModelId: profile.pinnedModelId,
          timeout: profile.timeout,
        });
      } else {
        // 新建：用 STANDARD 档位默认值预填（defaults 已含 mode 字段）
        const defaults = modeToProfileDefaults('STANDARD');
        form.setFieldsValue(defaults);
      }
    }
  }, [visible, profile, form]);

  /** 切换档位时用档位默认值覆盖降级兜底与超时 */
  const handleModeChange = (mode: ResilienceMode) => {
    const defaults = modeToProfileDefaults(mode);
    form.setFieldsValue({
      enableL2ModelDegradation: defaults.enableL2ModelDegradation,
      degradationMaxDepth: defaults.degradationMaxDepth,
      timeout: defaults.timeout,
    });
  };

  /** 关闭降级兜底时同步 maxDepth=0 */
  const handleFallbackToggle = (enabled: boolean) => {
    if (!enabled) {
      form.setFieldValue('degradationMaxDepth', 0);
    } else {
      // 重新开启时恢复档位默认深度（若当前为 0）
      const mode = form.getFieldValue('mode') as ResilienceMode;
      const current = form.getFieldValue('degradationMaxDepth') as number;
      if (!current) {
        form.setFieldValue('degradationMaxDepth', modeToProfileDefaults(mode).degradationMaxDepth);
      }
    }
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    try {
      if (isEdit) {
        await updateMutation.mutateAsync({ id: profile!.id, data: values });
        message.success(t('profiles.updateSuccess'));
      } else {
        await createMutation.mutateAsync(values);
        message.success(t('profiles.createSuccess'));
      }
      onClose();
    } catch {
      message.error(isEdit ? t('profiles.updateFailed') : t('profiles.createFailed'));
    }
  };

  return (
    <Modal
      title={isEdit ? t('profiles.edit') : t('profiles.add')}
      open={visible}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={loading}
      destroyOnHidden
      width={560}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="code"
          label={t('profiles.code')}
          rules={[{ required: true, message: t('profiles.codeRequired') }]}
        >
          <Input disabled={isEdit} placeholder="例如：default / strict / aggressive" />
        </Form.Item>
        <Form.Item
          name="name"
          label={t('profiles.name')}
          rules={[{ required: true, message: t('profiles.nameRequired') }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="mode"
          label={t('profiles.mode')}
          rules={[{ required: true }]}
          tooltip={t('mode.description.STANDARD')}
        >
          <Select options={MODE_OPTIONS} onChange={handleModeChange} />
        </Form.Item>

        {/* 「降级兜底」开关 + 深度：面向管理员的两个字段之一 */}
        <Form.Item
          name="enableL2ModelDegradation"
          label={t('fallback.label')}
          valuePropName="checked"
          tooltip={t('mode.description.STRICT')}
        >
          <Switch onChange={handleFallbackToggle} />
        </Form.Item>
        <Form.Item
          name="degradationMaxDepth"
          label={t('fallback.maxDepth')}
          tooltip={t('fallback.maxDepthHelp')}
        >
          <InputNumber min={0} max={5} style={{ width: '100%' }} />
        </Form.Item>

        {/* 专家字段折叠：会话亲和 / 模型锁定 / 超时 */}
        <Collapse
          ghost
          items={[{
            key: 'advanced',
            label: <span>{t('profiles.advanced')} <Tag color="default">{modeLabel(form.getFieldValue('mode') ?? 'STANDARD')}</Tag></span>,
            children: (
              <>
                <Form.Item
                  name="enableSessionAffinity"
                  label={t('profiles.enableSessionAffinity')}
                  valuePropName="checked"
                >
                  <Switch />
                </Form.Item>
                <Form.Item
                  name="sessionAffinityTtlMinutes"
                  label={t('profiles.sessionAffinityTtl')}
                >
                  <InputNumber min={0} style={{ width: '100%' }} />
                </Form.Item>
                <Form.Item
                  name="enablePinnedModel"
                  label={t('profiles.enablePinnedModel')}
                  valuePropName="checked"
                >
                  <Switch />
                </Form.Item>
                <Form.Item
                  name="pinnedModelId"
                  label={t('profiles.pinnedModelId')}
                >
                  <InputNumber style={{ width: '100%' }} placeholder="模型 ID（可空）" />
                </Form.Item>
                <Form.Item
                  name="timeout"
                  label={t('profiles.timeout')}
                >
                  <InputNumber min={0} style={{ width: '100%' }} />
                </Form.Item>
              </>
            ),
          }]}
        />
      </Form>
    </Modal>
  );
}
