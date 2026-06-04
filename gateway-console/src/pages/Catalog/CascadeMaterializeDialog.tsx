import { useState, useEffect } from 'react';
import { Modal, Typography, Tag, Space, Table, Spin, Checkbox } from 'antd';
import { useTranslation } from 'react-i18next';
import { App } from 'antd';
import type { PlanCatalog } from '@/types/catalog';
import { usePlanCatalogs, useMaterializeProviderWithPlans } from '@/services/query/useCatalog';

const { Text } = Typography;

/** 计费模式标签颜色 */
const BILLING_MODE_CONFIG: Record<string, { color: string }> = {
  pay_as_you_go: { color: 'green' },
  subscription: { color: 'purple' },
  package: { color: 'orange' },
};

interface CascadeMaterializeDialogProps {
  /** 弹窗是否打开 */
  open: boolean;
  /** 供应商编码 */
  providerCode: string;
  /** 供应商名称 */
  providerName: string;
  /** 关闭弹窗 */
  onClose: () => void;
}

/**
 * 级联物化确认弹窗
 *
 * <p>展示供应商关联的套餐清单，允许选择性物化。</p>
 */
export default function CascadeMaterializeDialog({
  open,
  providerCode,
  providerName,
  onClose,
}: CascadeMaterializeDialogProps) {
  const { t } = useTranslation('catalog');
  const { message } = App.useApp();

  // 查询该供应商的 Plans
  const { data: plans, isLoading } = usePlanCatalogs(providerCode);
  const cascadeMutation = useMaterializeProviderWithPlans();

  // 勾选状态：planCode → checked
  const [selectedPlanCodes, setSelectedPlanCodes] = useState<Set<string>>(new Set());

  /** 获取未物化的 Plans */
  const availablePlans = (plans ?? []).filter((p) => !p.materialized);

  // 弹窗打开时默认全选未物化 Plans
  useEffect(() => {
    if (open && availablePlans.length > 0) {
      setSelectedPlanCodes(new Set(availablePlans.map((p) => p.planCode)));
    } else if (!open) {
      setSelectedPlanCodes(new Set());
    }
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  // 弹窗关闭时重置 mutation 状态
  useEffect(() => {
    if (!open) {
      cascadeMutation.reset();
    }
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  /** 切换单条 Plan 勾选 */
  const handleToggle = (planCode: string, checked: boolean) => {
    setSelectedPlanCodes((prev) => {
      const next = new Set(prev);
      if (checked) {
        next.add(planCode);
      } else {
        next.delete(planCode);
      }
      return next;
    });
  };

  /** 全选/全不选 */
  const handleToggleAll = (checked: boolean) => {
    if (checked) {
      setSelectedPlanCodes(new Set(availablePlans.map((p) => p.planCode)));
    } else {
      setSelectedPlanCodes(new Set());
    }
  };

  /** 确认级联物化 */
  const handleConfirm = async () => {
    if (selectedPlanCodes.size === 0) {
      message.warning(t('materialize.cascadeDesc'));
      return;
    }
    try {
      const planCodes = Array.from(selectedPlanCodes);
      const result = await cascadeMutation.mutateAsync({
        providerCode,
        data: { planCodes },
      });

      const summary = t('materialize.resultSummary', {
        success: result.successCount,
        skipped: result.skippedCount,
        failed: result.failedCount,
      });
      message.success(summary);
      onClose();
    } catch {
      message.error(t('message.materializeFailed'));
    }
  };

  const allChecked = availablePlans.length > 0 && selectedPlanCodes.size === availablePlans.length;

  /** 表格列定义 */
  const columns = [
    {
      title: (
        <Checkbox
          checked={allChecked}
          indeterminate={selectedPlanCodes.size > 0 && !allChecked}
          onChange={(e) => handleToggleAll(e.target.checked)}
        />
      ),
      key: 'select',
      width: 48,
      render: (_: unknown, record: PlanCatalog) => (
        <Checkbox
          checked={selectedPlanCodes.has(record.planCode)}
          onChange={(e) => handleToggle(record.planCode, e.target.checked)}
        />
      ),
    },
    {
      title: t('plan.planName'),
      dataIndex: 'planName',
      key: 'planName',
      render: (name: string) => <span style={{ fontWeight: 500 }}>{name}</span>,
    },
    {
      title: t('plan.planCode'),
      dataIndex: 'planCode',
      key: 'planCode',
      width: 160,
      render: (code: string) => <Text code style={{ fontSize: 11 }}>{code}</Text>,
    },
    {
      title: t('plan.billingMode'),
      dataIndex: 'billingMode',
      key: 'billingMode',
      width: 120,
      render: (mode: string) => {
        const config = BILLING_MODE_CONFIG[mode];
        return <Tag color={config?.color ?? 'default'}>{t(`billingMode.${mode}`, { defaultValue: mode })}</Tag>;
      },
    },
  ];

  return (
    <Modal
      title={t('materialize.cascadeTitle', { name: providerName })}
      open={open}
      onCancel={onClose}
      onOk={handleConfirm}
      confirmLoading={cascadeMutation.isPending}
      okText={t('materialize.cascade')}
      okButtonProps={{ disabled: selectedPlanCodes.size === 0 }}
      width={640}
    >
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        {/* 描述 */}
        <Text type="secondary">{t('materialize.cascadeDesc')}</Text>

        {/* 已选数量统计 */}
        {availablePlans.length > 0 && !isLoading && (
          <Text type="secondary" style={{ fontSize: 12 }}>
            {t('materialize.cascadeSelected', { count: selectedPlanCodes.size })}
            {' / '}
            {t('materialize.cascadeTotal', { count: availablePlans.length })}
          </Text>
        )}

        {/* Plan 列表 */}
        <Spin spinning={isLoading}>
          <Table
            dataSource={availablePlans}
            columns={columns}
            rowKey="planCode"
            size="small"
            pagination={false}
            locale={{ emptyText: t('message.noData', { defaultValue: '暂无数据' }) }}
          />
        </Spin>
      </Space>
    </Modal>
  );
}