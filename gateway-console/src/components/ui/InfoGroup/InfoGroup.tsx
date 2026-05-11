import { useState } from 'react';
import { theme } from 'antd';
import { DownOutlined, RightOutlined } from '@ant-design/icons';
import type { ReactNode } from 'react';

export interface InfoItem {
  /** 标签 */
  label: string;
  /** 值 */
  value: ReactNode;
  /** 自定义渲染 */
  render?: (value: unknown) => ReactNode;
}

export interface InfoGroupProps {
  /** 组标题 */
  title: string;
  /** 信息项列表 */
  items: InfoItem[];
  /** 是否可折叠 */
  collapsible?: boolean;
  /** 默认折叠状态 */
  defaultCollapsed?: boolean;
  /** 右侧操作区 */
  actions?: ReactNode;
  /** 标签宽度 */
  labelWidth?: number | string;
  /** 分隔线样式 */
  bordered?: boolean;
}

/**
 * 信息项组件
 * 使用 Ant Design token 响应主题切换
 */
export function InfoGroup({
  title,
  items,
  collapsible = false,
  defaultCollapsed = false,
  actions,
  labelWidth = 120,
  bordered = true,
}: InfoGroupProps) {
  const { token } = theme.useToken();
  const [collapsed, setCollapsed] = useState(defaultCollapsed);

  return (
    <div style={{ marginBottom: 24 }}>
      {/* 组标题 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '12px 0',
          borderBottom: bordered ? `1px solid ${token.colorBorderSecondary}` : undefined,
          cursor: collapsible ? 'pointer' : undefined,
        }}
        onClick={collapsible ? () => setCollapsed(!collapsed) : undefined}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {collapsible && (
            <span style={{ color: token.colorTextDisabled }}>
              {collapsed ? <RightOutlined /> : <DownOutlined />}
            </span>
          )}
          <span style={{ fontWeight: 500, color: token.colorText }}>
            {title}
          </span>
        </div>
        {actions && <div onClick={(e) => e.stopPropagation()}>{actions}</div>}
      </div>

      {/* 信息项列表 */}
      {!collapsed && (
        <div style={{ padding: '8px 0' }}>
          {items.map((item, index) => (
            <div
              key={index}
              style={{
                display: 'flex',
                padding: '12px 0',
                borderBottom:
                  bordered && index < items.length - 1
                    ? `1px solid ${token.colorBorderSecondary}`
                    : undefined,
              }}
            >
              {/* 标签 */}
              <div
                style={{
                  flexShrink: 0,
                  color: token.colorTextSecondary,
                  fontSize: 14,
                  width: labelWidth,
                }}
              >
                {item.label}
              </div>

              {/* 值 */}
              <div style={{ flex: 1, color: token.colorText, fontSize: 14, wordBreak: 'break-word' }}>
                {item.render ? item.render(item.value) : item.value ?? '-'}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

/**
 * 信息项组容器 - 用于多个 InfoGroup 的布局
 */
export function InfoGroupContainer({ children }: { children: ReactNode }) {
  return <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>{children}</div>;
}
