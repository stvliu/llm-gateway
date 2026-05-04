import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AuthGuard, RoleGuard } from './guards';
import Login from '@/pages/Login';
import AdminLayout from '@/components/layout/AdminLayout';
import UserLayout from '@/components/layout/UserLayout';
import AdminModels from '@/pages/admin/Models';

// 临时占位组件，后续实现
function AdminUsers() {
  return <div>Admin Users Page (to be implemented)</div>;
}

function AdminSettings() {
  return <div>Admin Settings Page (to be implemented)</div>;
}

function UserModels() {
  return <div>User Models Page (to be implemented)</div>;
}

function UserApiKeys() {
  return <div>User API Keys Page (to be implemented)</div>;
}

function UserSettings() {
  return <div>User Settings Page (to be implemented)</div>;
}

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
