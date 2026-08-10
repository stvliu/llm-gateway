/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { Pie } from '@ant-design/charts';
import { theme } from 'antd';

interface ModelUsageChartProps {
  data?: Array<{ model: string; value: number }>;
}

/**
 * 模型使用分布饼图组件
 */
export function ModelUsageChart({ data }: ModelUsageChartProps) {
  const { token } = theme.useToken();

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
    color: [token.colorPrimary, token.colorSuccess, token.colorInfo, token.colorWarning, token.colorError],
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
        fill: token.colorTextSecondary,
      },
    },
    legend: {
      position: 'bottom' as const,
      itemName: {
        style: {
          fill: token.colorTextSecondary,
        },
      },
    },
    tooltip: {
      domStyles: {
        'g2-tooltip': {
          background: token.colorBgElevated,
          color: token.colorText,
          boxShadow: token.boxShadowSecondary,
        },
      },
    },
    statistic: {
      title: {
        content: '总计',
        style: {
          fill: token.colorTextTertiary,
          fontSize: 12,
        },
      },
      content: {
        style: {
          fill: token.colorText,
          fontSize: 18,
          fontWeight: 600,
        },
      },
    },
  };

  return <Pie {...config} style={{ height: '100%' }} />;
}
