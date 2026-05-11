import { Skeleton, theme } from 'antd';

export interface DrawerSkeletonProps {
  /** 显示 Tab 骨架 */
  showTabs?: boolean;
  /** Tab 数量 */
  tabCount?: number;
  /** 信息组数量 */
  groupCount?: number;
  /** 每组信息项数量 */
  itemsPerGroup?: number;
  /** 显示导航骨架 */
  showNavigation?: boolean;
}

/**
 * 抽屉骨架屏组件
 * 使用 Ant Design token 响应主题切换
 */
export function DrawerSkeleton({
  showTabs = true,
  tabCount = 4,
  groupCount = 2,
  itemsPerGroup = 5,
  showNavigation = true,
}: DrawerSkeletonProps) {
  const { token } = theme.useToken();

  return (
    <div style={{ animation: 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite' }}>
      {/* Tab 骨架 */}
      {showTabs && (
        <div style={{ display: 'flex', gap: 16, marginBottom: 24, padding: '0 16px' }}>
          {Array.from({ length: tabCount }).map((_, i) => (
            <div
              key={i}
              style={{
                height: 32,
                width: 64,
                borderRadius: 6,
                background: token.colorFillSecondary,
              }}
            />
          ))}
        </div>
      )}

      {/* 信息组骨架 */}
      {Array.from({ length: groupCount }).map((_, groupIndex) => (
        <div key={groupIndex} style={{ marginBottom: 24 }}>
          {/* 组标题骨架 */}
          <div
            style={{
              height: 20,
              width: '33%',
              borderRadius: 6,
              background: token.colorFillSecondary,
              marginBottom: 12,
            }}
          />

          {/* 分隔线骨架 */}
          <div
            style={{
              height: 1,
              width: '100%',
              background: token.colorBorderSecondary,
              marginBottom: 16,
            }}
          />

          {/* 信息项骨架 */}
          {Array.from({ length: itemsPerGroup }).map((_, itemIndex) => (
            <div key={itemIndex} style={{ display: 'flex', padding: '12px 0' }}>
              {/* 标签骨架 */}
              <div
                style={{
                  width: 96,
                  height: 16,
                  borderRadius: 6,
                  background: token.colorFillSecondary,
                  marginRight: 16,
                }}
              />
              {/* 值骨架 */}
              <div
                style={{
                  flex: 1,
                  height: 16,
                  borderRadius: 6,
                  background: token.colorFillSecondary,
                }}
              />
            </div>
          ))}
        </div>
      ))}

      {/* 导航骨架 */}
      {showNavigation && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '16px 0',
            borderTop: `1px solid ${token.colorBorderSecondary}`,
            marginTop: 16,
          }}
        >
          <div style={{ height: 16, width: 64, borderRadius: 6, background: token.colorFillSecondary }} />
          <div style={{ height: 16, width: 48, borderRadius: 6, background: token.colorFillSecondary }} />
          <div style={{ height: 16, width: 64, borderRadius: 6, background: token.colorFillSecondary }} />
        </div>
      )}

      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.5; }
        }
      `}</style>
    </div>
  );
}

/**
 * 简化版骨架屏（用于快速加载）
 */
export function DrawerSkeletonSimple() {
  return (
    <div style={{ padding: 16 }}>
      <Skeleton active paragraph={{ rows: 6 }} />
    </div>
  );
}
