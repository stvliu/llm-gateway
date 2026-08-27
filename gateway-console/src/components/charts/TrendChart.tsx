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
import { theme, Empty, Spin } from 'antd';
import { useTranslation } from 'react-i18next';
import type { StatsTrendItem } from '@/services/api/stats';

interface TrendChartProps {
  /** 按天趋势数据（真实后端数据） */
  data?: StatsTrendItem[];
  loading?: boolean;
}

/**
 * 趋势折线图组件
 *
 * <p>展示最近 N 天请求数/Token 消耗双序列。数据来自后端统计端点，
 * 不再内置模拟数据。后端趋势端点恒返回 N 天补零序列，故无数据时
 * （加载失败 data=undefined）显示空态，全零数据渲染为零线。</p>
 */
export function TrendChart({ data, loading }: TrendChartProps) {
  const { token } = theme.useToken();
  const { t } = useTranslation('dashboard');

  if (loading) {
    return <div style={{ textAlign: 'center', padding: 48 }}><Spin /></div>;
  }
  if (!data || data.length === 0) {
    return <Empty description={t('trend.empty', { defaultValue: '暂无趋势数据' })} />;
  }

  const chartData = data.flatMap((d) => [
    { date: d.date, value: d.requestCount, type: t('trend.requests', { defaultValue: '请求数' }) },
    { date: d.date, value: d.tokenCount, type: t('trend.tokens', { defaultValue: 'Token数' }) },
  ]);

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
