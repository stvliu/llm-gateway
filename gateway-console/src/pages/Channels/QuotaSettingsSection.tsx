/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useState } from 'react';
import { Descriptions, InputNumber, Input, Button, Space, Form, message, Tooltip } from 'antd';
import { EditOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { Channel, UpdateChannelRequest } from '@/types/channel';
import { useUpdateChannel } from '@/services/query/useChannels';
import { extractErrorMessage } from '@/utils/errorMessage';
import { useSavePulse } from '@/components/common/useSavePulse';
import '@/components/common/SavePulse.css';

interface QuotaSettingsSectionProps {
  channel: Channel;
}

/**
 * 配额与设置区组件
 * 展示渠道的配额和配置信息，支持"编辑模式 + 批量提交"模式。
 * 与 InlineEditable 三 Section 不同：本组件无乐观更新，只在保存成功 / 失败时
 * 对编辑区容器触发同款脉冲反馈。
 */
export function QuotaSettingsSection({ channel }: QuotaSettingsSectionProps) {
  const { t } = useTranslation('channels');
  const [editing, setEditing] = useState(false);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  // 编辑区脉冲——本组件实例在编辑/展示态切换时不会被卸载，可以直接复用 useSavePulse
  const pulse = useSavePulse();

  const updateChannel = useUpdateChannel();

  /** 格式化显示值 */
  const formatValue = (value: number | null | undefined, unit?: string) => {
    if (value === null || value === undefined) return t('quota.notSet');
    return unit ? `${value}${unit}` : String(value);
  };

  /** 开始编辑 */
  const handleStartEdit = () => {
    form.setFieldsValue({
      quotaLimit: channel.quotaLimit,
      timeout: channel.timeout,
      maxRetries: channel.maxRetries,
    });
    setEditing(true);
  };

  /** 取消编辑 */
  const handleCancel = () => {
    setEditing(false);
    form.resetFields();
  };

  /** 保存编辑 */
  const handleSave = async () => {
    let values: UpdateChannelRequest;
    try {
      setLoading(true);
      values = (await form.validateFields()) as UpdateChannelRequest;
    } catch (err) {
      // 校验失败 → AntD 行内已显示，不再弹 toast；其它错误带后端原因输出
      const reason = extractErrorMessage(err);
      if (reason) {
        message.error(t('common:message.saveFailed', { reason }));
      }
      setLoading(false);
      return;
    }
    const data: UpdateChannelRequest = {
      quotaLimit: values.quotaLimit,
      timeout: values.timeout,
      maxRetries: values.maxRetries,
    };
    try {
      await updateChannel.mutateAsync({ id: channel.id, data });
      pulse.triggerSuccess();
      message.success(t('quota.updateSuccess'));
      setEditing(false);
    } catch (err) {
      const reason = extractErrorMessage(err);
      // 触发编辑区错误脉冲（即使下方 toast 不显示，行内仍有可见反馈）
      pulse.triggerError(reason || t('common:message.saveFailed', { reason: '' }));
      if (reason) {
        message.error(t('common:message.saveFailed', { reason }));
      }
    } finally {
      setLoading(false);
    }
  };

  if (editing) {
    return (
      <div className={pulse.className} style={{ padding: 4, borderRadius: 4 }}>
        <Form form={form} layout="vertical">
          <Form.Item label={t('quota.rpm')} name="rpm">
            <InputNumber min={1} style={{ width: '100%' }} placeholder={t('quota.notSupported')} disabled />
          </Form.Item>
          <Form.Item label={t('quota.tpm')} name="tpm">
            <InputNumber min={1} style={{ width: '100%' }} placeholder={t('quota.notSupported')} disabled />
          </Form.Item>
          <Form.Item label={t('quota.quotaLimit')} name="quotaLimit">
            <InputNumber min={0} style={{ width: '100%' }} placeholder={t('quota.unlimited')} />
          </Form.Item>
          <Form.Item label={t('quota.timeoutMs')} name="timeout">
            <InputNumber min={1000} style={{ width: '100%' }} placeholder={t('quota.useDefault')} />
          </Form.Item>
          <Form.Item label={t('quota.maxRetries')} name="maxRetries">
            <InputNumber min={0} max={5} style={{ width: '100%' }} placeholder={t('quota.useDefault')} />
          </Form.Item>
          <Form.Item label={t('quota.customHeader')}>
            <Input style={{ width: '100%' }} placeholder={t('quota.notSupported')} disabled />
          </Form.Item>
          <Space>
            <Button type="primary" onClick={handleSave} loading={loading}>
              {t('drawer.save')}
            </Button>
            <Button onClick={handleCancel}>{t('drawer.cancel')}</Button>
            {pulse.state === 'success' && (
              <span className="save-tip-ok">
                ✓ {t('common:message.saved', { defaultValue: '已保存' })}
              </span>
            )}
            {pulse.state === 'error' && (
              <span className="save-tip-err">✗ {pulse.errorMsg}</span>
            )}
          </Space>
        </Form>
      </div>
    );
  }

  return (
    <div className={pulse.className} style={{ padding: 4, borderRadius: 4 }}>
      <Descriptions column={1} size="small">
        <Descriptions.Item label={t('quota.rpm')}>
          {t('quota.notSupported')}
        </Descriptions.Item>
        <Descriptions.Item label={t('quota.tpm')}>
          {t('quota.notSupported')}
        </Descriptions.Item>
        <Descriptions.Item label={t('quota.quotaLimit')}>
          {formatValue(channel.quotaLimit)}
        </Descriptions.Item>
        <Descriptions.Item label={t('quota.timeout')}>
          {formatValue(channel.timeout, 'ms')}
        </Descriptions.Item>
        <Descriptions.Item label={t('quota.maxRetries')}>
          {formatValue(channel.maxRetries)}
        </Descriptions.Item>
        <Descriptions.Item label={t('quota.customHeader')}>
          {t('quota.notSupported')}
        </Descriptions.Item>
      </Descriptions>
      <Space style={{ marginTop: 12 }}>
        <Tooltip title={t('quota.editSettings')}>
          <Button type="text" icon={<EditOutlined />} onClick={handleStartEdit} />
        </Tooltip>
        {pulse.state === 'success' && (
          <span className="save-tip-ok">
            ✓ {t('common:message.saved', { defaultValue: '已保存' })}
          </span>
        )}
        {pulse.state === 'error' && (
          <span className="save-tip-err">✗ {pulse.errorMsg}</span>
        )}
      </Space>
    </div>
  );
}
