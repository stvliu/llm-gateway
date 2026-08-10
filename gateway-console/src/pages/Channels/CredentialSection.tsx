/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useState, useEffect, useCallback, useRef } from 'react';
import { Tag, Input, InputNumber, Button, Space, Form, message, theme, Tooltip } from 'antd';
import { ThunderboltOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';
import { InlineEditableList } from './InlineEditableList';
import { MaskedKeyDisplay } from '@/components/MaskedKeyDisplay';
import { ApiKeyEditModal } from './ApiKeyEditModal';
import type { ChannelCredential, CreateChannelCredentialRequest, UpdateChannelCredentialRequest } from '@/types/channel';
import {
  useCreateChannelCredential,
  useUpdateChannelCredential,
  useDeleteChannelCredential,
  useTestChannelCredential,
  channelKeys,
} from '@/services/query/useChannels';
import { extractErrorMessage } from '@/utils/errorMessage';
import type { PulseState } from '@/components/common/useSavePulse';
import { useDangerConfirm } from '@/components/common/useDangerConfirm';
import '@/components/common/SavePulse.css';

interface CredentialSectionProps {
  channelId: number;
  credentials: ChannelCredential[];
}

/**
 * 行级保存反馈状态。
 * 不直接复用 useSavePulse hook，因为本组件 InlineEditableList 在编辑/展示态切换时会
 * 卸载并重建 renderItem 子树，导致 hook 实例丢失。改用 Section 级 Map 管理。
 */
interface RowPulse {
  state: PulseState;
  errorMsg?: string;
}

const ROW_PULSE_IDLE: RowPulse = { state: 'idle' };

/** 由 RowPulse 派生 className，与 useSavePulse 的派生规则一致 */
function pulseClassName(p: RowPulse | undefined): string {
  if (!p) return '';
  if (p.state === 'success') return 'save-pulse-success';
  if (p.state === 'error') return 'save-pulse-error';
  return '';
}

/**
 * 单个凭证行展示组件。
 * 接受 Section 维护的 RowPulse，渲染 className + 行尾 ✓ / ✗ 反馈。
 */
function CredentialRowDisplay({
  credential,
  testingId,
  pulse,
  savedLabel,
  onTest,
  onEdit,
}: {
  credential: ChannelCredential;
  testingId: number | null;
  pulse: RowPulse | undefined;
  savedLabel: string;
  onTest: (credential: ChannelCredential) => Promise<void>;
  onEdit: () => void;
}) {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();

  const className = pulseClassName(pulse);

  return (
    <div
      className={className}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        width: '100%',
        padding: 4,
        borderRadius: 4,
      }}
    >
      <MaskedKeyDisplay
        keyPlain={credential.apiKeyPlain}
        mode="editable"
        size="small"
        onEdit={onEdit}
      />
      <Tag color="blue">P{credential.priority}</Tag>
      <Tag color="purple">W{credential.weight}</Tag>
      <span style={{ color: token.colorTextSecondary, fontSize: 12 }}>
        {t('credential.lastUsed')}: {t('credential.noData')}
      </span>
      <Tag color={credential.state === 'ACTIVE' ? 'green' : 'default'}>
        {credential.state === 'ACTIVE' ? t('status.active') : t('status.inactive')}
      </Tag>
      {pulse?.state === 'success' && (
        <span className="save-tip-ok">✓ {savedLabel}</span>
      )}
      {pulse?.state === 'error' && (
        <span className="save-tip-err">✗ {pulse.errorMsg}</span>
      )}
      <Tooltip title={t('credential.test')}>
        <Button
          type="text"
          size="small"
          icon={<ThunderboltOutlined />}
          loading={testingId === credential.id}
          onClick={() => onTest(credential)}
        />
      </Tooltip>
    </div>
  );
}

/**
 * API Key 区组件
 * 展示渠道的凭证列表，支持行内编辑和测试。
 * 编辑保存采用乐观更新 + 失败回滚 + 行内脉冲反馈策略。
 */
export function CredentialSection({ channelId, credentials }: CredentialSectionProps) {
  const { t } = useTranslation('channels');
  const queryClient = useQueryClient();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [testingId, setTestingId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);

  // 危险删除确认（任务 8.4）：注意必须把 contextHolder 渲染到组件树
  const { confirm: confirmDelete, contextHolder: dangerContextHolder } = useDangerConfirm();

  // Section 级 RowPulse 表（按 credential.id 索引），自动归位定时器引用
  const [pulses, setPulses] = useState<Record<number, RowPulse>>({});
  const successTimers = useRef<Record<number, ReturnType<typeof setTimeout>>>({});

  /** 触发某行成功脉冲：3 秒后归位 idle */
  const triggerRowSuccess = useCallback((id: number) => {
    if (successTimers.current[id]) {
      clearTimeout(successTimers.current[id]);
    }
    setPulses((prev) => ({ ...prev, [id]: { state: 'success' } }));
    successTimers.current[id] = setTimeout(() => {
      setPulses((prev) => ({ ...prev, [id]: ROW_PULSE_IDLE }));
      delete successTimers.current[id];
    }, 3000);
  }, []);

  /** 触发某行错误脉冲：常驻直至下次 trigger */
  const triggerRowError = useCallback((id: number, msg: string) => {
    if (successTimers.current[id]) {
      clearTimeout(successTimers.current[id]);
      delete successTimers.current[id];
    }
    setPulses((prev) => ({ ...prev, [id]: { state: 'error', errorMsg: msg } }));
  }, []);

  // 卸载时清理所有定时器
  useEffect(() => {
    const timers = successTimers.current;
    return () => {
      Object.values(timers).forEach((tid) => clearTimeout(tid));
    };
  }, []);

  // Mutations
  const createCredential = useCreateChannelCredential();
  const updateCredential = useUpdateChannelCredential();
  const deleteCredential = useDeleteChannelCredential();
  const testCredential = useTestChannelCredential();

  /** 编辑时同步表单值 */
  useEffect(() => {
    if (editingId !== null) {
      const credential = credentials.find(c => c.id === editingId);
      if (credential) {
        form.setFieldsValue({
          priority: credential.priority,
          weight: credential.weight,
          description: credential.description || '',
        });
      }
    }
  }, [editingId, credentials, form]);

  /** 测试凭证连通性 */
  const handleTestCredential = useCallback(
    async (credential: ChannelCredential) => {
      setTestingId(credential.id);
      try {
        const result = await testCredential.mutateAsync({
          channelId,
          id: credential.id,
        });
        if (result.success) {
          message.success(t('credential.testSuccess', { latency: result.latency }));
        } else {
          message.error(
            t('credential.testFail', {
              msg: result.error?.message || t('credential.unknownError'),
            })
          );
        }
      } catch {
        message.error(t('credential.testRequestFail'));
      } finally {
        setTestingId(null);
      }
    },
    [channelId, testCredential, t]
  );

  const savedLabel = t('common:message.saved', { defaultValue: '已保存' });

  /** 渲染展示行 */
  const renderItem = useCallback(
    (credential: ChannelCredential) => (
      <CredentialRowDisplay
        credential={credential}
        testingId={testingId}
        pulse={pulses[credential.id]}
        savedLabel={savedLabel}
        onTest={handleTestCredential}
        onEdit={() => setEditingId(credential.id)}
      />
    ),
    [testingId, pulses, savedLabel, handleTestCredential]
  );

  /**
   * 渲染编辑表单。
   * 内嵌 EditFormBody 子组件在 mount 时把 credential 字段写入 form，
   * 保证表单字段注册之后立即 setFieldsValue，避免 validateFields 时拿到 undefined。
   */
  const renderEditForm = (
    credential: ChannelCredential,
    onSave: (updated: ChannelCredential) => void,
    onCancel: () => void
  ) => {
    /** 编辑表单内嵌子组件：mount 时把 credential 字段写入 form */
    const EditFormBody = () => {
      useEffect(() => {
        form.setFieldsValue({
          priority: credential.priority,
          weight: credential.weight,
          description: credential.description || '',
        });
        // 仅在挂载时初始化一次
        // eslint-disable-next-line react-hooks/exhaustive-deps
      }, []);
      return null;
    };

    const handleSave = async () => {
      let values: { priority: number; weight: number; description?: string };
      try {
        setLoading(true);
        values = await form.validateFields();
      } catch (err) {
        const reason = extractErrorMessage(err);
        if (reason) {
          message.error(t('common:message.saveFailed', { reason }));
        }
        setLoading(false);
        return;
      }
      const data: UpdateChannelCredentialRequest = {
        priority: values.priority,
        weight: values.weight,
        description: values.description,
      };

      // 乐观更新：备份并写缓存
      const credKey = channelKeys.credentials(channelId);
      await queryClient.cancelQueries({ queryKey: credKey });
      const prev = queryClient.getQueryData<ChannelCredential[]>(credKey);
      if (prev) {
        queryClient.setQueryData<ChannelCredential[]>(
          credKey,
          prev.map((c) => (c.id === credential.id ? { ...c, ...data } : c))
        );
      }

      try {
        const result = await updateCredential.mutateAsync({
          channelId,
          id: credential.id,
          data,
        });
        triggerRowSuccess(credential.id);
        message.success(t('credential.updateSuccess'));
        onSave(result);
      } catch (err) {
        // 失败：回滚缓存 + 触发 error 脉冲 + 保留 toast
        if (prev !== undefined) {
          queryClient.setQueryData(credKey, prev);
        }
        const reason = extractErrorMessage(err);
        triggerRowError(
          credential.id,
          reason || t('common:message.saveFailed', { reason: '' })
        );
        if (reason) {
          message.error(t('common:message.saveFailed', { reason }));
        }
        // 失败后退出编辑态，让展示态显示回滚后的原值（同时显示红框 + ✗ 错误）
        onCancel();
      } finally {
        setLoading(false);
        queryClient.invalidateQueries({ queryKey: credKey });
      }
    };

    return (
      <Form form={form} layout="inline" style={{ gap: 12 }}>
        <EditFormBody />
        <Form.Item
          name="priority"
          label={t('credential.priority')}
          rules={[{ required: true, message: t('credential.priorityRequired') }]}
        >
          <InputNumber min={1} max={10} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item
          name="weight"
          label={t('credential.weight')}
          rules={[{ required: true, message: t('credential.weightRequired') }]}
        >
          <InputNumber min={1} max={100} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item name="description" label={t('credential.description')}>
          <Input style={{ width: 200 }} placeholder={t('credential.descriptionPlaceholder')} />
        </Form.Item>
        <Space>
          <Button type="primary" size="small" onClick={handleSave} loading={loading}>
            {t('drawer.save')}
          </Button>
          <Button size="small" onClick={onCancel}>
            {t('drawer.cancel')}
          </Button>
        </Space>
      </Form>
    );
  };

  /** 渲染新增表单 */
  const renderAddForm = (
    onSave: (newItem: Partial<ChannelCredential>) => void,
    onCancel: () => void
  ) => {
    const handleSave = async () => {
      try {
        setLoading(true);
        const values = await form.validateFields();
        const data: CreateChannelCredentialRequest = {
          apiKey: values.apiKey,
          priority: values.priority,
          weight: values.weight,
          description: values.description,
        };
        const result = await createCredential.mutateAsync({ channelId, data });
        message.success(t('credential.addSuccess'));
        onSave({
          id: result.id,
          apiKeyPrefix: result.apiKeyPlain.substring(0, Math.min(10, result.apiKeyPlain.length)),
          apiKeyPlain: result.apiKeyPlain,
          name: '',
          description: values.description || null,
          weight: values.weight,
          priority: values.priority,
          state: 'ACTIVE',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          channelId,
        });
      } catch (err) {
        // 校验失败 → AntD 行内已显示，不重复弹 toast
        const reason = extractErrorMessage(err);
        if (reason) {
          message.error(t('common:message.saveFailed', { reason }));
        }
      } finally {
        setLoading(false);
      }
    };

    return (
      <Form form={form} layout="inline" style={{ gap: 12 }}>
        <Form.Item
          name="apiKey"
          label="API Key"
          rules={[{ required: true, message: t('credential.apiKeyRequired') }]}
        >
          <Input.Password style={{ width: 250 }} placeholder="sk-..." />
        </Form.Item>
        <Form.Item
          name="priority"
          label={t('credential.priority')}
          rules={[{ required: true, message: t('credential.priorityRequired') }]}
          initialValue={1}
        >
          <InputNumber min={1} max={10} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item
          name="weight"
          label={t('credential.weight')}
          rules={[{ required: true, message: t('credential.weightRequired') }]}
          initialValue={50}
        >
          <InputNumber min={1} max={100} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item name="description" label={t('credential.description')}>
          <Input style={{ width: 150 }} placeholder={t('credential.descriptionPlaceholder')} />
        </Form.Item>
        <Space>
          <Button type="primary" size="small" onClick={handleSave} loading={loading}>
            {t('drawer.save')}
          </Button>
          <Button size="small" onClick={onCancel}>
            {t('drawer.cancel')}
          </Button>
        </Space>
      </Form>
    );
  };

  /**
   * 删除凭证：弹危险确认 Modal（任务 8.4）。
   * description 含 keyMasked + "删除后无法恢复，使用此 Key 的请求将立即失败"。
   */
  const handleDelete = (credential: ChannelCredential) => {
    // keyMasked 取 prefix（已脱敏）+ 省略号；apiKeyPrefix 由后端脱敏返回
    const keyMasked = credential.apiKeyPrefix
      ? `${credential.apiKeyPrefix}…`
      : `Key #${credential.id}`;
    confirmDelete({
      titleKey: 'credential.deleteTitle',
      descriptionKey: 'credential.deleteDescription',
      descriptionParams: { keyMasked },
      onOk: async () => {
        try {
          await deleteCredential.mutateAsync({ channelId, id: credential.id });
          message.success(t('credential.deleteSuccess'));
        } catch (err) {
          // 兜底 toast：useSavePulse 未覆盖此 mutation，由本处反馈
          const reason = extractErrorMessage(err);
          message.error(
            reason
              ? t('common:message.saveFailed', { reason })
              : t('credential.deleteFail')
          );
          // 必须 throw，让 useDangerConfirm 阻止 modal 关闭，便于用户重试
          throw err;
        }
      },
    });
  };

  return (
    <>
      {/* useDangerConfirm 的 contextHolder 必须挂载到组件树，否则 modal 不出现 */}
      {dangerContextHolder}
      <InlineEditableList
        items={credentials}
        renderItem={renderItem}
        renderEditForm={renderEditForm}
        renderAddForm={renderAddForm}
        onAdd={() => {
          form.resetFields();
        }}
        onDelete={handleDelete}
        getKey={(credential) => credential.id}
        addLabel={t('credential.addKey')}
      />

      {/* API Key 编辑弹窗 */}
      {editingId !== null && (
        <ApiKeyEditModal
          open={true}
          channelId={channelId}
          credentialId={editingId}
          keyPlain={credentials.find(c => c.id === editingId)?.apiKeyPlain || ''}
          onClose={() => setEditingId(null)}
          onSuccess={() => {
            message.success(t('credential.keyUpdated'));
          }}
          onUpdate={async (chId, credId, data) => {
            await updateCredential.mutateAsync({ channelId: chId, id: credId, data });
          }}
        />
      )}
    </>
  );
}
