import { useEffect } from 'react';
import { Form, Input } from 'antd';
import { useTranslation } from 'react-i18next';
import type { FC } from 'react';

/**
 * 供应商表单受控数据形态
 *
 * <p>字段命名与后端 ProvisionRequest.InlineProvider DTO 保持一致：
 * code / name / description / websiteUrl / apiDocUrl。</p>
 */
export interface ProviderFormValue {
  /** 品牌标识（全局唯一） */
  code: string;
  /** 供应商名称 */
  name: string;
  /** 描述 */
  description?: string;
  /** 官网地址 */
  websiteUrl?: string;
  /** API 文档地址 */
  apiDocUrl?: string;
}

interface ProviderFormProps {
  /** 受控值 */
  value: ProviderFormValue;
  /** 受控变更回调 */
  onChange: (next: ProviderFormValue) => void;
  /**
   * 期望的供应商代码：当提供时，code 字段必须与之严格一致；
   * 用于 QuickOnboardMode Step 0.5 内联创建场景，确保与 planCatalog.providerCode 对齐。
   */
  expectedProviderCode?: string;
}

/**
 * 供应商表单（受控组件）
 *
 * <p>从 ProviderCreateModal 的表单部分抽离而来，支持复用：</p>
 * <ul>
 *   <li>批量导入路径仍可由 ProviderCreateModal 包装使用</li>
 *   <li>QuickOnboardMode Step 0.5 内联创建直接复用</li>
 * </ul>
 *
 * <p>校验规则：</p>
 * <ul>
 *   <li>code 必填 + 仅允许小写字母 / 数字 / 下划线 / 中划线</li>
 *   <li>当 expectedProviderCode 提供时，code 必须严格与之相等</li>
 *   <li>name 必填</li>
 * </ul>
 */
export const ProviderForm: FC<ProviderFormProps> = ({
  value,
  onChange,
  expectedProviderCode,
}) => {
  const { t } = useTranslation('channels');
  const [form] = Form.useForm<ProviderFormValue>();

  // 外部 value 变化时同步 antd 表单内部状态
  useEffect(() => {
    form.setFieldsValue(value);
  }, [value, form]);

  /** 表单字段任意变更：合并到 ProviderFormValue 并冒泡 */
  const handleValuesChange = (
    _changed: Partial<ProviderFormValue>,
    all: ProviderFormValue,
  ) => {
    onChange({ ...value, ...all });
  };

  return (
    <Form
      form={form}
      layout="vertical"
      initialValues={value}
      onValuesChange={handleValuesChange}
      style={{ marginTop: 16 }}
    >
      <Form.Item
        name="code"
        label={t('providerCreate.code')}
        rules={[
          { required: true, message: t('providerCreate.codeRequired') },
          { pattern: /^[a-z0-9_-]+$/, message: t('providerCreate.codePattern') },
          // 当 expectedProviderCode 存在时，强制 code 与之一致（中文文案含 expected）
          {
            validator: (_rule, fieldValue: string) => {
              if (!expectedProviderCode || !fieldValue) {
                return Promise.resolve();
              }
              if (fieldValue !== expectedProviderCode) {
                return Promise.reject(
                  new Error(
                    t('provider.codeMustMatch', { expected: expectedProviderCode }),
                  ),
                );
              }
              return Promise.resolve();
            },
          },
        ]}
        extra={t('providerCreate.codeExtra')}
      >
        <Input placeholder={t('providerCreate.codePlaceholder')} />
      </Form.Item>
      <Form.Item
        name="name"
        label={t('providerCreate.name')}
        rules={[{ required: true, message: t('providerCreate.nameRequired') }]}
      >
        <Input placeholder={t('providerCreate.namePlaceholder')} />
      </Form.Item>
      <Form.Item name="description" label={t('providerCreate.description')}>
        <Input.TextArea rows={3} />
      </Form.Item>
      <Form.Item name="websiteUrl" label={t('providerCreate.websiteUrl')}>
        <Input placeholder={t('providerCreate.websiteUrlPlaceholder')} />
      </Form.Item>
      <Form.Item name="apiDocUrl" label={t('providerCreate.apiDocUrl')}>
        <Input placeholder={t('providerCreate.apiDocUrlPlaceholder')} />
      </Form.Item>
    </Form>
  );
};
