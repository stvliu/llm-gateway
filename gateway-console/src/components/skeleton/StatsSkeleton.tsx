import { Skeleton, Card, Row, Col } from 'antd';
import { useThemeStore } from '@/stores/themeStore';

/**
 * 统计卡片骨架屏
 */
export function StatsSkeleton() {
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  return (
    <Row gutter={16}>
      {[1, 2, 3, 4].map((i) => (
        <Col span={6} key={i}>
          <Card
            style={{
              height: '100%',
              border: 'none',
              boxShadow: isDark
                ? '0 2px 8px rgba(0, 0, 0, 0.3)'
                : '0 2px 8px rgba(0, 0, 0, 0.06)',
            }}
            styles={{
              body: { padding: '20px 24px' },
            }}
          >
            <Skeleton active paragraph={{ rows: 1 }} />
          </Card>
        </Col>
      ))}
    </Row>
  );
}

/**
 * 图表骨架屏
 */
export function ChartSkeleton() {
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  return (
    <Card
      style={{
        height: '100%',
        border: 'none',
        boxShadow: isDark
          ? '0 2px 8px rgba(0, 0, 0, 0.3)'
          : '0 2px 8px rgba(0, 0, 0, 0.06)',
      }}
      styles={{
        body: { height: 'calc(100% - 57px)', padding: '16px 24px 24px' },
      }}
    >
      <Skeleton.Image active style={{ height: '100%', width: '100%' }} />
    </Card>
  );
}

/**
 * 表格骨架屏
 */
export function TableSkeleton({ rows = 5 }: { rows?: number }) {
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  return (
    <Card
      style={{
        border: 'none',
        boxShadow: isDark
          ? '0 2px 8px rgba(0, 0, 0, 0.3)'
          : '0 2px 8px rgba(0, 0, 0, 0.06)',
      }}
    >
      <Skeleton active paragraph={{ rows }} />
    </Card>
  );
}

/**
 * 完整 Dashboard 骨架屏
 */
export function DashboardSkeleton() {
  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <StatsSkeleton />
      <Row gutter={16} style={{ flex: 1 }}>
        <Col span={16}>
          <ChartSkeleton />
        </Col>
        <Col span={8}>
          <ChartSkeleton />
        </Col>
      </Row>
      <TableSkeleton />
    </div>
  );
}
