/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { Drawer } from 'antd';
import { useTranslation } from 'react-i18next';
import { QuickOnboardMode } from './QuickOnboardMode';

export interface ChannelCreateWizardProps {
  open: boolean;
  onClose: () => void;
  /** 预选套餐编码（从目录入口传入） */
  initialPlanCode?: string;
  /** 预选套餐名称（用于显示） */
  initialPlanName?: string;
}

/**
 * 渠道创建向导——从套餐模板快速创建渠道
 */
export function ChannelCreateWizard({ open, onClose, initialPlanCode, initialPlanName }: ChannelCreateWizardProps) {
  const { t } = useTranslation('channels');
  const handleComplete = () => {
    onClose();
  };

  return (
    <Drawer
      title={t('wizard.title')}
      placement="right"
      width={720}
      open={open}
      onClose={onClose}
      destroyOnClose
    >
      <QuickOnboardMode
        onComplete={handleComplete}
        onCancel={onClose}
        initialPlanCode={initialPlanCode}
        initialPlanName={initialPlanName}
      />
    </Drawer>
  );
}
