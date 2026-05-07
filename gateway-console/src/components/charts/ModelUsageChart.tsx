import { Pie } from '@ant-design/charts';
import { useThemeStore } from '@/stores/themeStore';

interface ModelUsageChartProps {
  data?: Array<{ model: string; value: number }>;
}

/**
 * 模型使用分布饼图组件
 */
export function ModelUsageChart({ data }: ModelUsageChartProps) {
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  // 默认模拟数据
  const defaultData = [
    { model: 'GPT-4', value: 40 },
    { model: 'GPT-3.5', value: 25 },
    { model: 'Claude-3', value: 20 },
    { model: 'Gemini', value: 10 },
    { model: '其他', value: 5 },
  ];

  const chartData = data || defaultData;

  const config = {
    data: chartData,
    angleField: 'value',
    colorField: 'model',
    radius: 0.8,
    innerRadius: 0.6,
    color: ['#1677ff', '#52c41a', '#722ed1', '#fa8c16', '#eb2f96'],
    animation: {
      appear: {
        animation: 'grow-in',
        duration: 800,
      },
    },
    label: {
      text: 'model',
      position: 'outside' as const,
      style: {
        fill: isDark ? 'rgba(255, 255, 255, 0.65)' : 'rgba(0, 0, 0, 0.65)',
      },
    },
    legend: {
      position: 'bottom' as const,
      itemName: {
        style: {
          fill: isDark ? 'rgba(255, 255, 255, 0.65)' : 'rgba(0, 0, 0, 0.65)',
        },
      },
    },
    tooltip: {
      domStyles: {
        'g2-tooltip': {
          background: isDark ? '#1f1f1f' : '#fff',
          color: isDark ? 'rgba(255, 255, 255, 0.85)' : 'rgba(0, 0, 0, 0.85)',
        },
      },
    },
    statistic: {
      title: {
        content: '总计',
        style: {
          fill: isDark ? 'rgba(255, 255, 255, 0.45)' : 'rgba(0, 0, 0, 0.45)',
          fontSize: 12,
        },
      },
      content: {
        style: {
          fill: isDark ? 'rgba(255, 255, 255, 0.85)' : 'rgba(0, 0, 0, 0.85)',
          fontSize: 18,
          fontWeight: 600,
        },
      },
    },
  };

  return <Pie {...config} style={{ height: '100%' }} />;
}
