import { RouterProvider } from 'react-router-dom';
import { router } from '@/router';
import '@/i18n';

/**
 * 应用根组件
 * 配置路由提供者和国际化
 */
function App() {
  return <RouterProvider router={router} />;
}

export default App;
