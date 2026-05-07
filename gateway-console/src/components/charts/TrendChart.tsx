import { Line } from '@ant-design/charts';
import { useThemeStore } from '@/stores/themeStore';

interface TrendChartProps {
  data?: Array<{ date: string; value: number; type: string }>;
}

/**
 * 趋势折线图组件
 */
export function TrendChart({ data }: TrendChartProps) {
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  // 默认模拟数据
  const defaultData = [
    { date: '2024-01', value: 320, type: '请求数' },
    { date: '2024-02', value: 450, type: '请求数' },
    { date: '2024-03', value: 380, type: '请求数' },
    { date: '2024-04', value: 520, type: '请求数' },
    { date: '2024-05', value: 680, type: '请求数' },
    { date: '2024-06', value: 750, type: '请求数' },
    { date: '2024-07', value: 890, type: '请求数' },
    { date: '2024-01', value: 12000, type: 'Token数' },
    { date: '2024-02', value: 18000, type: 'Token数' },
    { date: '2024-03', value: 15000, type: 'Token数' },
    { date: '2024-04', value: 22000, type: 'Token数' },
    { date: '2024-05', value: 28000, type: 'Token数' },
    { date: '2024-06', value: 32000, type: 'Token数' },
    { date: '2024-07', value: 38000, type: 'Token数' },
  ];

  const chartData = data || defaultData;

  const config = {
    data: chartData,
    xField: 'date',
    yField: 'value',
    seriesField: 'type',
    color: ['#1677ff', '#52c41a'],
    smooth: true,
    animation: {
      appear: {
        animation: 'path-in',
        duration: 1000,
      },
    },
    point: {
      shape: 'circle',
      size: 4,
    },
    tooltip: {
      domStyles: {
        'g2-tooltip': {
          background: isDark ? '#1f1f1f' : '#fff',
          color: isDark ? 'rgba(255, 255, 255, 0.85)' : 'rgba(0, 0, 0, 0.85)',
        },
      },
    },
    legend: {
      position: 'top' as const,
      itemName: {
        style: {
          fill: isDark ? 'rgba(255, 255, 255, 0.65)' : 'rgba(0, 0, 0, 0.65)',
        },
      },
    },
    axis: {
      x: {
        label: {
          style: {
            fill: isDark ? 'rgba(255, 255, 255, 0.45)' : 'rgba(0, 0, 0, 0.45)',
          },
        },
        line: {
          style: {
            stroke: isDark ? '#303030' : '#e8e8e8',
          },
        },
      },
      y: {
        label: {
          style: {
            fill: isDark ? 'rgba(255, 255, 255, 0.45)' : 'rgba(0, 0, 0, 0.45)',
          },
        },
        grid: {
          line: {
            style: {
              stroke: isDark ? '#303030' : '#f0f0f0',
            },
          },
        },
      },
    },
  };

  return <Line {...config} style={{ height: '100%' }} />;
}
