import { useState } from 'react';
import { Button, Modal, Select, Tooltip } from 'antd';
import { AimOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { App } from 'antd';
import { useClusters, useSwitchCluster } from '@/services/query/useResilience';
import { extractErrorMessage } from '@/utils/errorMessage';

interface SwitchClusterButtonProps {
  channelId: number;
  disabled?: boolean;
}

/**
 * 紧急切换渠道到目标故障域（紧切域）按钮
 *
 * <p>「选而非填」范式应急操作：主域故障时，强制把流量钉到某 Cluster。
 * 弹 Modal 选择目标故障域，确认后调用 PUT /channels/{id}/cluster。</p>
 *
 * <p>后端不校验目标域健康（运维决策）。</p>
 */
export function SwitchClusterButton({ channelId, disabled }: SwitchClusterButtonProps) {
  const { t } = useTranslation('resilience');
  const { message } = App.useApp();
  const { data: clusters } = useClusters();
  const switchCluster = useSwitchCluster();
  const [open, setOpen] = useState(false);
  const [targetClusterId, setTargetClusterId] = useState<number | undefined>(undefined);

  const handleConfirm = async () => {
    if (!targetClusterId) {
      message.warning(t('channels.selectCluster'));
      return;
    }
    try {
      await switchCluster.mutateAsync({ channelId, clusterId: targetClusterId });
      message.success(t('channels.switchClusterSuccess'));
      setOpen(false);
      setTargetClusterId(undefined);
    } catch (err) {
      message.error(extractErrorMessage(err) || t('channels.operationFailed'));
    }
  };

  return (
    <>
      <Tooltip title={t('channels.switchCluster')}>
        <Button
          type="text"
          size="small"
          icon={<AimOutlined />}
          onClick={() => setOpen(true)}
          disabled={disabled}
          data-testid="switch-cluster-btn"
        />
      </Tooltip>
      <Modal
        title={t('channels.switchCluster')}
        open={open}
        onOk={handleConfirm}
        onCancel={() => {
          setOpen(false);
          setTargetClusterId(undefined);
        }}
        confirmLoading={switchCluster.isPending}
        okText={t('channels.switchCluster')}
      >
        <div style={{ marginBottom: 8, color: 'rgba(0,0,0,0.65)' }}>
          {t('channels.switchClusterConfirm')}
        </div>
        <Select
          placeholder={t('channels.selectCluster')}
          value={targetClusterId}
          onChange={setTargetClusterId}
          style={{ width: '100%' }}
          options={(clusters ?? []).map((c) => ({
            value: c.id,
            label: `${c.code} (${c.name})`,
          }))}
          data-testid="switch-cluster-select"
        />
      </Modal>
    </>
  );
}
