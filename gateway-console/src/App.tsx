import { RouterProvider } from 'react-router-dom';
import { router } from '@/router';
import {
  useServiceUnavailable,
  DevServiceUnavailableModal,
  ProductionServiceUnavailablePage,
} from '@/components/common/ServiceUnavailable';
import '@/i18n';

// 环境判断
const isDev = import.meta.env.DEV;

/**
 * 服务不可用处理组件
 */
function ServiceUnavailableHandler() {
  const { visible, endpoint, error } = useServiceUnavailable();

  if (!visible) {
    return null;
  }

  if (isDev) {
    return (
      <DevServiceUnavailableModal
        visible={visible}
        endpoint={endpoint}
        error={error}
      />
    );
  }

  return <ProductionServiceUnavailablePage />;
}

/**
 * 应用根组件
 * 配置路由提供者和国际化
 */
function App() {
  return (
    <>
      <RouterProvider router={router} />
      <ServiceUnavailableHandler />
    </>
  );
}

export default App;
