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
  /** 操作按钮（支持数组或自定义 ReactNode） */
  actions?: PageHeaderAction[] | ReactNode;
  /** 过滤条件标签 */
  filterTags?: FilterTag[];
  /** 清除全部过滤 */
  onClearAllFilters?: () => void;
  /** 子标题/描述 */
  subtitle?: string;
  /** 返回按钮 */
  onBack?: () => void;
  /** 额外内容（如筛选器） */
  extra?: ReactNode;
}

/**
 * 页面标题组件
 * 单行紧凑式布局：标题左侧，操作按钮+extra 右侧
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
        <div style={{ padding: '12px 24px 0' }}>
          <Breadcrumb items={breadcrumb} />
        </div>
      )}

      {/* 主标题栏 - 单行紧凑式 */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: breadcrumb ? '12px 24px' : '16px 24px',
        gap: 24,
        flexWrap: 'wrap',
      }}>
        {/* 左侧：标题 + extra（筛选器） */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0, flexWrap: 'wrap' }}>
          {onBack && (
            <Button
              type="text"
              icon={<CloseOutlined />}
              onClick={onBack}
              style={{ color: token.colorTextSecondary, flexShrink: 0 }}
            />
          )}
          <h1 style={{
            fontSize: 18,
            fontWeight: 600,
            color: token.colorText,
            margin: 0,
            whiteSpace: 'nowrap',
          }}>
            {title}
          </h1>
          {subtitle && (
            <span style={{
              fontSize: 13,
              color: token.colorTextSecondary,
              whiteSpace: 'nowrap',
            }}>
              {subtitle}
            </span>
          )}
          {extra}
        </div>

        {/* 右侧：操作按钮 */}
        {actions && (
          Array.isArray(actions) ? (
            actions.length > 0 && (
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
            )
          ) : (
            actions
          )
        )}
      </div>

      {/* 过滤条件标签 - 单独一行 */}
      {filterTags && filterTags.length > 0 && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          padding: '0 24px 12px',
          flexWrap: 'wrap',
        }}>
          <span style={{ fontSize: 13, color: token.colorTextSecondary }}>
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
    </div>
  );
}
