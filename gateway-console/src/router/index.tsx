import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AuthGuard, RoleGuard } from './guards';
import Login from '@/pages/Login';
import AdminLayout from '@/components/layout/AdminLayout';
import UserLayout from '@/components/layout/UserLayout';
import AdminDashboard from '@/pages/admin/Dashboard';
import AdminProviders from '@/pages/admin/Providers';
import AdminModels from '@/pages/admin/Models';
import AdminApiKeyPool from '@/pages/admin/ApiKeyPool';
import AdminUsers from '@/pages/admin/Users';
import AdminApiKeys from '@/pages/admin/ApiKeys';
import AdminSettings from '@/pages/admin/Settings';
import AdminTemplates from '@/pages/admin/Templates';
import UserDashboard from '@/pages/user/Dashboard';
import UserModels from '@/pages/user/Models';
import UserApiKeys from '@/pages/user/ApiKeys';
import UserSettings from '@/pages/user/Settings';

export const router = createBrowserRouter([
  // 公共路由
  {
    path: '/login',
    element: <Login />,
  },

  // 管理员路由
  {
    path: '/admin',
    element: (
      <AuthGuard>
        <RoleGuard allowedRoles={['ADMIN']}>
          <AdminLayout />
        </RoleGuard>
      </AuthGuard>
    ),
    children: [
      { index: true, element: <Navigate to="/admin/dashboard" replace /> },
      { path: 'dashboard', element: <AdminDashboard /> },
      { path: 'providers', element: <AdminProviders /> },
      { path: 'models', element: <AdminModels /> },
      { path: 'api-key-pool', element: <AdminApiKeyPool /> },
      { path: 'users', element: <AdminUsers /> },
      { path: 'api-keys', element: <AdminApiKeys /> },
      { path: 'settings', element: <AdminSettings /> },
      { path: 'templates', element: <AdminTemplates /> },
    ],
  },

  // 普通用户路由
  {
    path: '/user',
    element: (
      <AuthGuard>
        <UserLayout />
      </AuthGuard>
    ),
    children: [
      { index: true, element: <Navigate to="/user/dashboard" replace /> },
      { path: 'dashboard', element: <UserDashboard /> },
      { path: 'models', element: <UserModels /> },
      { path: 'api-keys', element: <UserApiKeys /> },
      { path: 'settings', element: <UserSettings /> },
    ],
  },

  // 默认重定向
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '*', element: <Navigate to="/login" replace /> },
]);

export default router;
