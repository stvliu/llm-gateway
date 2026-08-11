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
import { Tag, Tooltip, Button, Space, Popconfirm } from 'antd';
import {
  ThunderboltOutlined,
  UndoOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { App } from 'antd';
import {
  useCircuitBreakerState,
  useForceOpenCircuitBreaker,
  useForceCloseCircuitBreaker,
} from '@/services/query/useResilience';
import { extractErrorMessage } from '@/utils/errorMessage';
import type { CircuitBreakerState } from '@/types/resilience';

interface CircuitBreakerButtonProps {
  channelId: number;
  endpointId: number;
}

/** 熔断器状态 → 颜色/文案 */
function stateMeta(state: CircuitBreakerState | undefined, t: (k: string) => string) {
  switch (state) {
    case 'OPEN':
      return { color: 'red' as const, label: t('channels.state.OPEN') };
    case 'HALF_OPEN':
      return { color: 'orange' as const, label: t('channels.state.HALF_OPEN') };
    case 'CLOSED':
    default:
      return { color: 'green' as const, label: t('channels.state.CLOSED') };
  }
}

/**
 * 端点熔断器应急操作组件
 *
 * <p>「选而非填」范式应急操作落地：展示端点熔断器当前状态，
 * 提供一键熔断（force-open）/ 一键恢复（force-close）按钮。</p>
 *
 * <p>熔断操作复用既有 CircuitBreaker，与自动熔断同语义，HealthRouter 自动过滤。</p>
 */
export function CircuitBreakerButton({ channelId, endpointId }: CircuitBreakerButtonProps) {
  const { t } = useTranslation('resilience');
  const { message } = App.useApp();
  const { data: stateData, isLoading } = useCircuitBreakerState(channelId, endpointId);
  const forceOpen = useForceOpenCircuitBreaker();
  const forceClose = useForceCloseCircuitBreaker();

  const state = stateData?.state;
  const meta = stateMeta(state, t);
  const isOpen = state === 'OPEN';

  /** 一键熔断 */
  const handleForceOpen = async () => {
    try {
      await forceOpen.mutateAsync({ channelId, endpointId });
      message.success(t('channels.forceOpenSuccess'));
    } catch (err) {
      message.error(extractErrorMessage(err) || t('channels.operationFailed'));
    }
  };

  /** 一键恢复 */
  const handleForceClose = async () => {
    try {
      await forceClose.mutateAsync({ channelId, endpointId });
      message.success(t('channels.forceCloseSuccess'));
    } catch (err) {
      message.error(extractErrorMessage(err) || t('channels.operationFailed'));
    }
  };

  return (
    <Space size={4}>
      <Tag color={meta.color} data-testid={`cb-state-${endpointId}`}>
        {meta.label}
      </Tag>
      {!isOpen ? (
        <Popconfirm
          title={t('channels.forceOpen')}
          description={t('channels.forceOpenConfirm')}
          okType="danger"
          onConfirm={handleForceOpen}
          disabled={forceOpen.isPending}
        >
          <Tooltip title={t('channels.forceOpen')}>
            <Button
              type="text"
              size="small"
              danger
              icon={<ThunderboltOutlined />}
              loading={forceOpen.isPending || isLoading}
              data-testid={`cb-force-open-${endpointId}`}
            />
          </Tooltip>
        </Popconfirm>
      ) : (
        <Popconfirm
          title={t('channels.forceClose')}
          description={t('channels.forceCloseConfirm')}
          onConfirm={handleForceClose}
          disabled={forceClose.isPending}
        >
          <Tooltip title={t('channels.forceClose')}>
            <Button
              type="text"
              size="small"
              icon={<UndoOutlined />}
              loading={forceClose.isPending || isLoading}
              data-testid={`cb-force-close-${endpointId}`}
            />
          </Tooltip>
        </Popconfirm>
      )}
    </Space>
  );
}
