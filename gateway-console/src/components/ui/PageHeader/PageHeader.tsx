import { Space, Button, Tag, Breadcrumb, theme } from 'antd';
import type { ReactNode } from 'react';
import { CloseOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

export interface PageHeaderAction {
  key: string;
  label: string;
  icon?: ReactNode;
  type?: 'primary' | 'default' | 'text' | 'dashed' | 'link';
  danger?: boolean;
  onClick: () => void;
  loading?: boolean;
  disabled?: boolean;
}

export interface FilterTag {
  key: string;
  label: string;
  onRemove: () => void;
}

export interface PageHeaderProps {
  /** 页面标题 */
  title: string;
  /** 面包屑 */
  breadcrumb?: { title: string; href?: string }[];
  /** 操作按钮 */
  actions?: PageHeaderAction[];
  /** 过滤条件标签 */
  filterTags?: FilterTag[];
  /** 清除全部过滤 */
  onClearAllFilters?: () => void;
  /** 子标题/描述 */
  subtitle?: string;
  /** 返回按钮 */
  onBack?: () => void;
  /** 额外内容（如 Tab 切换） */
  extra?: ReactNode;
}

/**
 * 页面标题组件
 * 使用 Ant Design token 响应主题切换
 */
export function PageHeader({
  title,
  breadcrumb,
  actions,
  filterTags,
  onClearAllFilters,
  subtitle,
  onBack,
  extra,
}: PageHeaderProps) {
  const { t } = useTranslation();
  const { token } = theme.useToken();

  return (
    <div
      style={{
        background: token.colorBgContainer,
        borderBottom: `1px solid ${token.colorBorderSecondary}`,
      }}
    >
      {/* 面包屑 */}
      {breadcrumb && (
        <div style={{ padding: '16px 24px 0' }}>
          <Breadcrumb items={breadcrumb} />
        </div>
      )}

      {/* 主标题栏 */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          {/* 返回按钮 */}
          {onBack && (
            <Button
              type="text"
              icon={<CloseOutlined />}
              onClick={onBack}
              style={{ color: token.colorTextSecondary }}
            />
          )}

          {/* 标题 */}
          <div>
            <h1 style={{ fontSize: 20, fontWeight: 600, color: token.colorText, margin: 0 }}>
              {title}
            </h1>
            {subtitle && (
              <p style={{ fontSize: 14, color: token.colorTextSecondary, marginTop: 4, marginBottom: 0 }}>
                {subtitle}
              </p>
            )}
          </div>
        </div>

        {/* 操作按钮 */}
        {actions && (
          <Space size="small">
            {actions.map((action) => (
              <Button
                key={action.key}
                type={action.type || 'default'}
                danger={action.danger}
                icon={action.icon}
                onClick={action.onClick}
                loading={action.loading}
                disabled={action.disabled}
              >
                {action.label}
              </Button>
            ))}
          </Space>
        )}
      </div>

      {/* 过滤条件标签 */}
      {filterTags && filterTags.length > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '0 24px 12px', flexWrap: 'wrap' }}>
          <span style={{ fontSize: 14, color: token.colorTextSecondary }}>
            {t('filter.currentFilter')}:
          </span>
          {filterTags.map((tag) => (
            <Tag
              key={tag.key}
              closable
              onClose={tag.onRemove}
              style={{
                background: token.colorPrimaryBg,
                color: token.colorPrimary,
              }}
            >
              {tag.label}
            </Tag>
          ))}
          {onClearAllFilters && (
            <Button
              type="text"
              size="small"
              onClick={onClearAllFilters}
              style={{ color: token.colorPrimary }}
            >
              {t('filter.clearAll')}
            </Button>
          )}
        </div>
      )}

      {/* 额外内容 */}
      {extra && <div style={{ padding: '0 24px 16px' }}>{extra}</div>}
    </div>
  );
}
