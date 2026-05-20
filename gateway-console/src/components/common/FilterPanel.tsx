import { useState, useEffect } from 'react';
import { Button, Form, Select, Space, Tag, theme } from 'antd';
import { FilterOutlined } from '@ant-design/icons';
import type { ReactNode } from 'react';

interface FilterField {
  name: string;
  label: string;
  type?: 'select' | 'input' | 'date';
  options?: Array<{ value: string; label: string }>;
  placeholder?: string;
}

interface FilterPanelProps {
  fields: FilterField[];
  values: Record<string, string>;
  onChange: (values: Record<string, string>) => void;
  onReset: () => void;
  title?: string;
  children?: ReactNode;
}

/**
 * 筛选器面板
 * 按照 docs/页面设计规范.md 第十四节规范实现
 */
export function FilterPanel({
  fields,
  values,
  onChange,
  onReset,
  title = '筛选条件',
  children,
}: FilterPanelProps) {
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();
  const { token } = theme.useToken();

  // 同步外部值到表单
  useEffect(() => {
    form.setFieldsValue(values);
  }, [values, form]);

  const activeFilterCount = Object.values(values).filter((v) => v !== undefined && v !== '' && v !== 'all').length;

  const handleApply = () => {
    const formValues = form.getFieldsValue();
    onChange(formValues);
    setOpen(false);
  };

  const handleReset = () => {
    form.resetFields();
    onReset();
  };

  const handleCancel = () => {
    form.setFieldsValue(values);
    setOpen(false);
  };

  const dropdownContent = (
    <div
      style={{
        width: 320,
        background: token.colorBgContainer,
        borderRadius: token.borderRadiusLG,
        boxShadow: token.boxShadowSecondary,
        border: `1px solid ${token.colorBorderSecondary}`,
      }}
    >
      {/* 条件主体区 */}
      <Form form={form} layout="vertical" style={{ padding: 20 }}>
        {fields.map((field) => (
          <Form.Item
            key={field.name}
            name={field.name}
            label={
              <span
                style={{
                  fontSize: token.fontSizeSM,
                  color: token.colorTextDescription,
                }}
              >
                {field.label}
              </span>
            }
          >
            {field.type === 'select' || !field.type ? (
              <Select
                placeholder={field.placeholder || '全部'}
                allowClear
                options={field.options}
              />
            ) : null}
          </Form.Item>
        ))}
        {children}
      </Form>

      {/* 操作区 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          padding: '16px 20px',
          borderTop: `1px solid ${token.colorBorderSecondary}`,
        }}
      >
        <Button type="link" onClick={handleReset}>
          重置
        </Button>
        <Space>
          <Button onClick={handleCancel}>取消</Button>
          <Button type="primary" onClick={handleApply}>
            更新
          </Button>
        </Space>
      </div>
    </div>
  );

  return (
    <div style={{ position: 'relative' }}>
      {/* 始终渲染 Form 以避免 useForm 未连接 Warning */}
      <div style={{ display: open ? 'block' : 'none', position: 'absolute', top: '100%', left: 0, marginTop: 4, zIndex: 1000 }}>
        {dropdownContent}
      </div>
      <Button
        icon={<FilterOutlined />}
        onClick={() => setOpen(!open)}
        style={{ position: 'relative' }}
      >
        {title}
        {activeFilterCount > 0 && (
          <Tag
            color="processing"
            style={{ marginLeft: 8 }}
          >
            {activeFilterCount}
          </Tag>
        )}
      </Button>
      {/* 遮罩层 */}
      {open && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            zIndex: 999,
          }}
          onClick={() => setOpen(false)}
        />
      )}
    </div>
  );
}

/**
 * 激活的筛选条件标签行
 */
interface FilterTagsProps {
  filters: Array<{ key: string; label: string; value: string }>;
  onRemove: (key: string) => void;
  onClearAll: () => void;
}

export function FilterTags({ filters, onRemove, onClearAll }: FilterTagsProps) {
  const { token } = theme.useToken();

  if (filters.length === 0) return null;

  return (
    <div
      style={{
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        gap: 8,
        padding: '12px 24px',
        background: token.colorFillAlter,
        borderBottom: `1px solid ${token.colorBorderSecondary}`,
      }}
    >
      <span style={{ color: token.colorTextSecondary, fontSize: 14 }}>
        当前过滤:
      </span>
      {filters.map((filter) => (
        <Tag
          key={filter.key}
          closable
          onClose={() => onRemove(filter.key)}
          style={{
            background: token.colorPrimaryBg,
            color: token.colorPrimary,
            border: 'none',
          }}
        >
          {filter.label}: {filter.value}
        </Tag>
      ))}
      <Button type="link" size="small" onClick={onClearAll}>
        清除全部
      </Button>
    </div>
  );
}

export type { FilterField, FilterPanelProps, FilterTagsProps };