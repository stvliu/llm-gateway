import { useState, useEffect } from 'react';
import { Drawer, Segmented } from 'antd';
import { QuickOnboardMode } from './QuickOnboardMode';
import { ExpertConfigMode } from './ExpertConfigMode';

export interface ChannelCreateWizardProps {
  open: boolean;
  onClose: () => void;
  /** 预选套餐编码（从目录入口传入） */
  initialPlanCode?: string;
  /** 预选套餐名称（用于显示） */
  initialPlanName?: string;
}

type CreateMode = 'quick' | 'expert';

/**
 * 渠道创建向导——快速接入/专家配置双模式
 * 支持从目录入口预选套餐
 */
export function ChannelCreateWizard({ open, onClose, initialPlanCode, initialPlanName }: ChannelCreateWizardProps) {
  const [mode, setMode] = useState<CreateMode>('quick');

  // 如果有预选套餐，自动进入快速接入模式
  useEffect(() => {
    if (initialPlanCode && open) {
      setMode('quick');
    }
  }, [initialPlanCode, open]);

  const handleComplete = () => {
    onClose();
  };

  return (
    <Drawer
      title="新建渠道"
      placement="right"
      width={720}
      open={open}
      onClose={onClose}
      destroyOnClose
    >
      <div style={{ marginBottom: 24 }}>
        <Segmented
          value={mode}
          onChange={v => setMode(v as CreateMode)}
          options={[
            { label: '⚡ 快速接入', value: 'quick' },
            { label: '🔧 专家配置', value: 'expert' },
          ]}
        />
      </div>

      {mode === 'quick' ? (
        <QuickOnboardMode
          onComplete={handleComplete}
          onCancel={onClose}
          initialPlanCode={initialPlanCode}
          initialPlanName={initialPlanName}
        />
      ) : (
        <ExpertConfigMode onComplete={handleComplete} onCancel={onClose} />
      )}
    </Drawer>
  );
}
