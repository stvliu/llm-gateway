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
import {
  Card,
  Button,
  InputNumber,
  Switch,
  Select,
  Popconfirm,
  Space,
  App,
  Typography,
  Tag,
} from 'antd';
import { SyncOutlined, SaveOutlined, ClearOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import {
  useSettings,
  useUpdateSetting,
  useCleanupAuditLogs,
} from '@/services/query/useSettings';
import {
  useCatalogSync,
  useCatalogSyncStatus,
} from '@/services/query/useCatalogSync';
import type { CatalogSyncInterval } from '@/types/settings';

const { Text } = Typography;

/**
 * 从错误对象中提取可展示信息（后端 400 由拦截器转换为 Error.message）
 */
function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}

/**
 * 系统设置页
 *
 * <p>包含两个分组：审计日志（保留天数 + 立即清理）、模型目录（自动同步开关/周期 +
 * 立即同步 + 最近同步状态）。配置项修改后立即调 PUT 并 invalidate 刷新。</p>
 */
export default function SettingsPage() {
  const { t } = useTranslation('common');
  const { message } = App.useApp();

  // 系统配置列表与更新
  const { data: settings, isLoading } = useSettings();
  const updateSetting = useUpdateSetting();
  const cleanupMutation = useCleanupAuditLogs();

  // 模型目录同步状态与触发（复用 Catalog 页逻辑）
  const { data: syncStatus, isLoading: syncStatusLoading } = useCatalogSyncStatus();
  const syncMutation = useCatalogSync();

  /** 按设置键查找配置项 */
  const findSetting = (key: string) => settings?.find((s) => s.settingKey === key);

  // 审计日志保留天数：本地草稿优先，未编辑时取服务端值，缺省 90
  const [daysDraft, setDaysDraft] = useState<number | null>(null);
  const retentionSetting = findSetting('audit.retention.days');
  const days =
    daysDraft ?? (retentionSetting ? Number(retentionSetting.settingValue) || 90 : 90);

  // 模型目录自动同步开关与周期
  const enabledSetting = findSetting('catalog.sync.enabled');
  const autoSyncEnabled = enabledSetting?.settingValue === 'true';
  const intervalSetting = findSetting('catalog.sync.interval');
  const syncInterval = (intervalSetting?.settingValue ?? 'DAILY') as CatalogSyncInterval;

  /**
   * 统一保存配置项
   *
   * <p>成功提示并清空保留天数草稿（重新以服务端为准），失败展示后端 error.message。</p>
   */
  const handleSave = async (key: string, value: string) => {
    try {
      await updateSetting.mutateAsync({ key, value });
      setDaysDraft(null);
      message.success(t('settings.saveSuccess', { defaultValue: '设置已保存' }));
    } catch (error) {
      message.error(
        getErrorMessage(error, t('settings.saveFailed', { defaultValue: '保存失败' })),
      );
    }
  };

  /** 保存审计日志保留天数 */
  const handleSaveRetention = () => {
    if (!Number.isInteger(days) || days < 1) {
      message.error(
        t('settings.invalidDays', { defaultValue: '保留天数必须为正整数' }),
      );
      return;
    }
    void handleSave('audit.retention.days', String(days));
  };

  /** 立即清理审计日志（展示后端返回的删除条数） */
  const handleCleanup = async () => {
    try {
      const result = await cleanupMutation.mutateAsync(days);
      message.success(
        t('settings.cleanupSuccess', {
          defaultValue: '已清理 {{count}} 条审计日志',
          count: result.deleted,
        }),
      );
    } catch (error) {
      message.error(
        getErrorMessage(error, t('settings.cleanupFailed', { defaultValue: '清理失败' })),
      );
    }
  };

  /** 手工触发模型目录同步 */
  const handleSync = async () => {
    try {
      const report = await syncMutation.mutateAsync();
      message.success(
        t('settings.syncSuccess', {
          defaultValue: '同步完成：新增 {{added}}、更新 {{updated}}、跳过 {{skipped}}、失败 {{failed}}',
          added: report.addedCount,
          updated: report.updatedCount,
          skipped: report.skippedCount,
          failed: report.failedCount,
        }),
      );
    } catch {
      message.error(t('settings.syncFailed', { defaultValue: '模型目录同步失败' }));
    }
  };

  return (
    <div>
      {/* 审计日志分组 */}
      <Card
        size="small"
        title={t('settings.auditGroup', { defaultValue: '审计日志' })}
        style={{ marginBottom: 16 }}
      >
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
            <Text>{t('settings.retentionDays', { defaultValue: '日志保留天数' })}：</Text>
            <InputNumber
              min={1}
              value={days}
              disabled={isLoading}
              onChange={(v) => setDaysDraft(v ?? null)}
              style={{ width: 160 }}
            />
            <Button
              type="primary"
              icon={<SaveOutlined />}
              loading={updateSetting.isPending}
              onClick={handleSaveRetention}
            >
              {t('settings.save', { defaultValue: '保存' })}
            </Button>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {t('settings.cleanupHint', {
                defaultValue: '立即删除 {{count}} 天前的审计日志（不可恢复）',
                count: days,
              })}
            </Text>
            <Popconfirm
              title={t('settings.cleanupConfirm', { defaultValue: '确定清理审计日志？' })}
              okText={t('actions.confirm', { defaultValue: '确定' })}
              cancelText={t('actions.cancel', { defaultValue: '取消' })}
              onConfirm={handleCleanup}
            >
              <Button danger icon={<ClearOutlined />} loading={cleanupMutation.isPending}>
                {t('settings.cleanupNow', { defaultValue: '立即清理' })}
              </Button>
            </Popconfirm>
          </div>
        </Space>
      </Card>

      {/* 模型目录分组 */}
      <Card size="small" title={t('settings.catalogGroup', { defaultValue: '模型目录' })}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
            <Text>{t('settings.autoSync', { defaultValue: '自动同步' })}：</Text>
            <Switch
              checked={autoSyncEnabled}
              loading={isLoading || updateSetting.isPending}
              onChange={(checked) => void handleSave('catalog.sync.enabled', String(checked))}
            />
            <Text>{t('settings.syncInterval', { defaultValue: '同步周期' })}：</Text>
            <Select<CatalogSyncInterval>
              value={syncInterval}
              disabled={isLoading}
              loading={updateSetting.isPending}
              style={{ width: 140 }}
              options={[
                {
                  value: 'DAILY',
                  label: t('settings.interval.DAILY', { defaultValue: '每天' }),
                },
                {
                  value: 'WEEKLY',
                  label: t('settings.interval.WEEKLY', { defaultValue: '每周' }),
                },
                {
                  value: 'MONTHLY',
                  label: t('settings.interval.MONTHLY', { defaultValue: '每月' }),
                },
              ]}
              onChange={(v) => void handleSave('catalog.sync.interval', v)}
            />
          </div>

          {/* 最近同步状态 + 立即同步 */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              gap: 16,
              flexWrap: 'wrap',
            }}
          >
            <div>
              {syncStatusLoading ? (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {t('settings.syncLoading', { defaultValue: '加载中...' })}
                </Text>
              ) : syncStatus ? (
                <Space direction="vertical" size={2}>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {t('settings.lastSyncAt', { defaultValue: '最近同步' })}：
                    {new Date(syncStatus.syncedAt).toLocaleString('zh-CN')}
                  </Text>
                  <Text style={{ fontSize: 12 }}>
                    {t('settings.syncResult', { defaultValue: '结果' })}：
                    <Tag color={syncStatus.result === 'SUCCESS' ? 'success' : 'error'}>
                      {syncStatus.result === 'SUCCESS' ? '成功' : '失败'}
                    </Tag>
                    （{t('settings.added', { defaultValue: '新增' })} {syncStatus.addedCount} /{' '}
                    {t('settings.updated', { defaultValue: '更新' })} {syncStatus.updatedCount} /{' '}
                    {t('settings.skipped', { defaultValue: '跳过' })} {syncStatus.skippedCount} /{' '}
                    {t('settings.failed', { defaultValue: '失败' })} {syncStatus.failedCount}）
                  </Text>
                </Space>
              ) : (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {t('settings.notSynced', { defaultValue: '尚未同步' })}
                </Text>
              )}
            </div>
            <Button
              type="primary"
              icon={<SyncOutlined />}
              loading={syncMutation.isPending}
              onClick={handleSync}
            >
              {t('settings.syncNow', { defaultValue: '立即同步' })}
            </Button>
          </div>
        </Space>
      </Card>
    </div>
  );
}
