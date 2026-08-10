/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useState } from 'react';
import { Card, Breadcrumb, Button } from 'antd';
import { useTranslation } from 'react-i18next';
import { HomeOutlined } from '@ant-design/icons';
import PlanCatalogView from './PlanCatalogView';
import { ChannelCreateWizard } from '@/pages/Channels/ChannelCreateWizard';

/** 目录管理主页面 — 套餐目录浏览 */
export default function CatalogPage() {
  const { t } = useTranslation('catalog');

  // 套餐导航状态
  const [providerCode, setProviderCode] = useState<string | undefined>();
  const [providerName, setProviderName] = useState<string>('');
  const [planCode, setPlanCode] = useState<string | undefined>();
  const [planName, setPlanName] = useState<string>('');

  // 渠道创建向导状态
  const [wizardOpen, setWizardOpen] = useState(false);
  const [wizardPlanCode, setWizardPlanCode] = useState<string | undefined>();
  const [wizardPlanName, setWizardPlanName] = useState<string | undefined>();

  /** 打开渠道创建向导 */
  const handleQuickCreate = (planCode: string, planName: string) => {
    setWizardPlanCode(planCode);
    setWizardPlanName(planName);
    setWizardOpen(true);
  };

  /** 关闭渠道创建向导 */
  const handleCloseWizard = () => {
    setWizardOpen(false);
    setWizardPlanCode(undefined);
    setWizardPlanName(undefined);
  };

  /** 选择供应商 → 进入套餐目录 */
  const handleSelectProvider = (code: string, name: string) => {
    setProviderCode(code);
    setProviderName(name);
    setPlanCode(undefined);
    setPlanName('');
  };

  /** 选择套餐 → 进入套餐详情 */
  const handleSelectPlan = (code: string, name: string) => {
    setPlanCode(code);
    setPlanName(name);
  };

  /** 返回供应商目录 */
  const handleBackToProviders = () => {
    setProviderCode(undefined);
    setProviderName('');
    setPlanCode(undefined);
    setPlanName('');
  };

  /** 返回套餐目录 */
  const handleBackToPlans = () => {
    setPlanCode(undefined);
    setPlanName('');
  };

  /** 面包屑导航项 */
  const breadcrumbItems = [
    {
      title: (
        <Button
          type={!providerCode ? 'text' : 'link'}
          style={{ padding: 0, fontWeight: !providerCode ? 600 : undefined }}
          onClick={handleBackToProviders}
        >
          <HomeOutlined style={{ marginRight: 4 }} />
          {t('tabs.providers')}
        </Button>
      ),
    },
    ...(providerCode
      ? [
          {
            title: (
              <Button
                type={!planCode ? 'text' : 'link'}
                style={{ padding: 0, fontWeight: !planCode ? 600 : undefined }}
                onClick={handleBackToPlans}
              >
                {t('tabs.plans')}
                {providerName && (
                  <span style={{ marginLeft: 4, fontSize: 12, opacity: 0.65 }}>
                    ({providerName})
                  </span>
                )}
              </Button>
            ),
          },
        ]
      : []),
    ...(planCode
      ? [
          {
            title: (
              <span style={{ fontWeight: 600 }}>
                {planName}
              </span>
            ),
          },
        ]
      : []),
  ];

  return (
    <div>
      <Card>
        {/* 面包屑导航 */}
        <div style={{ marginBottom: 16 }}>
          <Breadcrumb items={breadcrumbItems} />
        </div>

        {/* 套餐目录视图 */}
        <PlanCatalogView
          providerCode={providerCode}
          onSelectProvider={handleSelectProvider}
          onSelectPlan={handleSelectPlan}
          onQuickCreate={handleQuickCreate}
        />
      </Card>

      {/* 渠道创建向导 */}
      <ChannelCreateWizard
        open={wizardOpen}
        onClose={handleCloseWizard}
        initialPlanCode={wizardPlanCode}
        initialPlanName={wizardPlanName}
      />
    </div>
  );
}