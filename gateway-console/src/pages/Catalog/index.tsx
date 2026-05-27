import { useState } from 'react';
import { Card, Breadcrumb, Button } from 'antd';
import { useTranslation } from 'react-i18next';
import { HomeOutlined } from '@ant-design/icons';
import ProviderCatalogView from './ProviderCatalogView';
import PlanCatalogView from './PlanCatalogView';
import ModelCatalogView from './ModelCatalogView';
import MaterializeModal from './MaterializeModal';
import type { MaterializeType } from '@/types/catalog';

/** 目录管理主页面 — 三级联动导航：供应商目录 → 套餐目录 → 模型目录 */
export default function CatalogPage() {
  const { t } = useTranslation('catalog');

  // 三级导航状态
  const [providerCode, setProviderCode] = useState<string | undefined>();
  const [providerName, setProviderName] = useState<string>('');
  const [planCode, setPlanCode] = useState<string | undefined>();
  const [planName, setPlanName] = useState<string>('');

  // 物化弹窗状态
  const [materializeModal, setMaterializeModal] = useState<{
    open: boolean;
    type: MaterializeType;
    code: string;
    name: string;
  }>({ open: false, type: 'PROVIDER', code: '', name: '' });

  /** 打开物化确认弹窗 */
  const handleMaterialize = (type: MaterializeType, code: string, name: string) => {
    setMaterializeModal({ open: true, type, code, name });
  };

  /** 关闭物化弹窗 */
  const handleCloseMaterialize = () => {
    setMaterializeModal((prev) => ({ ...prev, open: false }));
  };

  /** 选择供应商 → 进入套餐目录 */
  const handleSelectProvider = (code: string, name: string) => {
    setProviderCode(code);
    setProviderName(name);
    setPlanCode(undefined);
    setPlanName('');
  };

  /** 选择套餐 → 进入模型目录 */
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
                {t('tabs.models')}
                {planName && (
                  <span style={{ marginLeft: 4, fontSize: 12, opacity: 0.65 }}>
                    ({planName})
                  </span>
                )}
              </span>
            ),
          },
        ]
      : []),
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card>
        {/* 面包屑导航 */}
        <div style={{ marginBottom: 16 }}>
          <Breadcrumb items={breadcrumbItems} />
        </div>

        {/* 三级视图切换 */}
        {!providerCode && (
          <ProviderCatalogView
            onSelectProvider={handleSelectProvider}
            onMaterialize={handleMaterialize}
          />
        )}
        {providerCode && !planCode && (
          <PlanCatalogView
            providerCode={providerCode}
            onSelectPlan={handleSelectPlan}
            onMaterialize={handleMaterialize}
          />
        )}
        {providerCode && planCode && (
          <ModelCatalogView
            providerCode={providerCode}
            planCode={planCode}
            onMaterialize={handleMaterialize}
          />
        )}
      </Card>

      {/* 物化确认弹窗 */}
      <MaterializeModal
        open={materializeModal.open}
        type={materializeModal.type}
        code={materializeModal.code}
        name={materializeModal.name}
        onClose={handleCloseMaterialize}
      />
    </div>
  );
}
