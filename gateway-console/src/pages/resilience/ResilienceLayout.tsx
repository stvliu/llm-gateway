import { useState } from 'react';
import { Tabs } from 'antd';
import { SafetyOutlined, AppstoreOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate, useLocation } from 'react-router-dom';
import OverviewPage from './overview';
import ProfilesPage from './profiles';

/**
 * 容灾管理布局：顶部 Tabs 切换「容灾总览」与「画像模板」两屏。
 *
 * <p>「选而非填」范式三屏中的屏1（总览，只读）与屏2（画像模板，低频配）。
 * 屏3（绑定关系）复用 Applications 页，不在此布局内。</p>
 */
export default function ResilienceLayout() {
  const { t } = useTranslation('resilience');
  const navigate = useNavigate();
  const location = useLocation();

  // 根据当前路径推导激活的 Tab
  const activeKey = location.pathname.includes('/profiles') ? 'profiles' : 'overview';

  const [key, setKey] = useState(activeKey);

  const handleChange = (newKey: string) => {
    setKey(newKey);
    navigate(`/resilience/${newKey}`);
  };

  return (
    <Tabs
      activeKey={key}
      onChange={handleChange}
      destroyInactiveTabPane
      items={[
        {
          key: 'overview',
          label: (
            <span>
              <SafetyOutlined /> {t('menu.overview')}
            </span>
          ),
          children: <OverviewPage />,
        },
        {
          key: 'profiles',
          label: (
            <span>
              <AppstoreOutlined /> {t('menu.profiles')}
            </span>
          ),
          children: <ProfilesPage />,
        },
      ]}
    />
  );
}
