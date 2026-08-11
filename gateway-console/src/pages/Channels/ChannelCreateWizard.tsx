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
