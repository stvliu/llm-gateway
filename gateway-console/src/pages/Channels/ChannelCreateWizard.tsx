import { useState } from 'react';
import { Drawer, Segmented } from 'antd';
import { QuickOnboardMode } from './QuickOnboardMode';
import { ExpertConfigMode } from './ExpertConfigMode';

interface ChannelCreateWizardProps {
  open: boolean;
  onClose: () => void;
}

type CreateMode = 'quick' | 'expert';

/**
 * 渠道创建向导——快速接入/专家配置双模式
 */
export function ChannelCreateWizard({ open, onClose }: ChannelCreateWizardProps) {
  const [mode, setMode] = useState<CreateMode>('quick');

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
        <QuickOnboardMode onComplete={handleComplete} onCancel={onClose} />
      ) : (
        <ExpertConfigMode onComplete={handleComplete} onCancel={onClose} />
      )}
    </Drawer>
  );
}
