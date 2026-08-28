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
import { useState } from 'react';
import { Card, Breadcrumb, Button, Tag, Space, App, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { HomeOutlined, SyncOutlined } from '@ant-design/icons';
import PlanCatalogView from './PlanCatalogView';
import { ChannelCreateWizard } from '@/pages/Channels/ChannelCreateWizard';
import {
  useCatalogSync,
  useCatalogSyncStatus,
} from '@/services/query/useCatalogSync';

const { Text } = Typography;

/** 目录管理主页面 — 套餐目录浏览 */
export default function CatalogPage() {
  const { t } = useTranslation('catalog');
  const { message } = App.useApp();

  // 模型目录（models.dev）同步状态与触发
  const {
    data: syncStatus,
    isLoading: syncStatusLoading,
  } = useCatalogSyncStatus();
  const syncMutation = useCatalogSync();

  /** 手工触发模型目录同步 */
  const handleSync = async () => {
    try {
      const report = await syncMutation.mutateAsync();
      message.success(
        `同步完成：新增 ${report.addedCount}、更新 ${report.updatedCount}、跳过 ${report.skippedCount}、失败 ${report.failedCount}`,
      );
    } catch {
      message.error(t('sync.syncFailed', { defaultValue: '模型目录同步失败' }));
    }
  };

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
      {/* 模型目录同步区域 */}
      <Card
        size="small"
        title={t('sync.modelsDevTitle', { defaultValue: '模型目录同步' })}
        style={{ marginBottom: 16 }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
          <div>
            {syncStatusLoading ? (
              <Text type="secondary" style={{ fontSize: 12 }}>
                {t('sync.loading', { defaultValue: '加载中...' })}
              </Text>
            ) : syncStatus ? (
              <Space direction="vertical" size={2}>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {t('sync.lastSyncAt', { defaultValue: '最近同步' })}：
                  {new Date(syncStatus.syncedAt).toLocaleString('zh-CN')}
                </Text>
                <Text style={{ fontSize: 12 }}>
                  {t('sync.result', { defaultValue: '结果' })}：
                  <Tag color={syncStatus.result === 'SUCCESS' ? 'success' : 'error'}>
                    {syncStatus.result === 'SUCCESS' ? '成功' : '失败'}
                  </Tag>
                  （{t('sync.added', { defaultValue: '新增' })} {syncStatus.addedCount} /{' '}
                  {t('sync.updated', { defaultValue: '更新' })} {syncStatus.updatedCount} /{' '}
                  {t('sync.skipped', { defaultValue: '跳过' })} {syncStatus.skippedCount} /{' '}
                  {t('sync.failed', { defaultValue: '失败' })} {syncStatus.failedCount}）
                </Text>
              </Space>
            ) : (
              <Text type="secondary" style={{ fontSize: 12 }}>
                {t('sync.notSynced', { defaultValue: '尚未同步' })}
              </Text>
            )}
          </div>
          <Button
            type="primary"
            icon={<SyncOutlined />}
            loading={syncMutation.isPending}
            onClick={handleSync}
          >
            {t('sync.modelsDev', { defaultValue: '同步模型目录' })}
          </Button>
        </div>
      </Card>

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