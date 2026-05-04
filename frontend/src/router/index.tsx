import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AuthGuard, RoleGuard } from './guards';
import Login from '@/pages/Login';
import AdminLayout from '@/components/layout/AdminLayout';
import UserLayout from '@/components/layout/UserLayout';
import AdminModels from '@/pages/admin/Models';
import AdminUsers from '@/pages/admin/Users';
import AdminSettings from '@/pages/admin/Settings';
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
      { index: true, element: <Navigate to="/admin/models" replace /> },
      { path: 'models', element: <AdminModels /> },
      { path: 'users', element: <AdminUsers /> },
      { path: 'settings', element: <AdminSettings /> },
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
      { index: true, element: <Navigate to="/user/models" replace /> },
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
