/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Line } from '@ant-design/charts';
import { theme } from 'antd';

interface TrendChartProps {
  data?: Array<{ date: string; value: number; type: string }>;
}

/**
 * 趋势折线图组件
 */
export function TrendChart({ data }: TrendChartProps) {
  const { token } = theme.useToken();

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
    color: [token.colorPrimary, token.colorSuccess],
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
          background: token.colorBgElevated,
          color: token.colorText,
          boxShadow: token.boxShadowSecondary,
        },
      },
    },
    legend: {
      position: 'top' as const,
      itemName: {
        style: {
          fill: token.colorTextSecondary,
        },
      },
    },
    axis: {
      x: {
        label: {
          style: {
            fill: token.colorTextTertiary,
          },
        },
        line: {
          style: {
            stroke: token.colorBorder,
          },
        },
      },
      y: {
        label: {
          style: {
            fill: token.colorTextTertiary,
          },
        },
        grid: {
          line: {
            style: {
              stroke: token.colorBorderSecondary,
            },
          },
        },
      },
    },
  };

  return <Line {...config} style={{ height: '100%' }} />;
}
