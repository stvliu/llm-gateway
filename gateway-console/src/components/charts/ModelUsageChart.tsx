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
import { Pie } from '@ant-design/charts';
import { theme, Empty, Spin } from 'antd';
import { useTranslation } from 'react-i18next';
import type { StatsModelUsageItem } from '@/services/api/stats';

interface ModelUsageChartProps {
  /** 模型用量分布（真实后端数据） */
  data?: StatsModelUsageItem[];
  loading?: boolean;
}

/**
 * 模型使用分布饼图组件
 *
 * <p>展示各模型调用量占比。数据来自后端统计端点，
 * 无数据时显示空态，不再内置模拟数据。</p>
 */
export function ModelUsageChart({ data, loading }: ModelUsageChartProps) {
  const { token } = theme.useToken();
  const { t } = useTranslation('dashboard');

  if (loading) {
    return <div style={{ textAlign: 'center', padding: 48 }}><Spin /></div>;
  }
  if (!data || data.length === 0) {
    return <Empty description={t('modelUsage.empty', { defaultValue: '暂无模型用量数据' })} />;
  }

  const pieData = data.map((d) => ({ model: d.model, value: d.requestCount }));

  const config = {
    data: pieData,
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
        content: t('modelUsage.total', { defaultValue: '总计' }),
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
