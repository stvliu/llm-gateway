import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AuthGuard, PermissionGuard } from './guards';
import Login from '@/pages/Login';
import AppLayout from '@/components/layout/AppLayout';
import Dashboard from '@/pages/Dashboard';
import Providers from '@/pages/Providers';
import Metadata from '@/pages/Metadata';
import Users from '@/pages/Users';
import ChangePassword from '@/pages/ChangePassword';
import TeamsPage from '@/pages/Teams';
import { P } from '@/constants/permissions';

export const router = createBrowserRouter([
  // 公共路由
  {
    path: '/login',
    element: <Login />,
  },

  // 应用路由（统一入口）
  {
    path: '/',
    element: (
      <AuthGuard>
        <AppLayout />
      </AuthGuard>
    ),
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <Dashboard /> },
      {
        path: 'providers',
        element: <PermissionGuard permission={P.PROVIDER_READ}><Providers /></PermissionGuard>,
      },
      {
        path: 'metadata',
        element: <PermissionGuard permission={P.METADATA_READ}><Metadata /></PermissionGuard>,
      },
      {
        path: 'users',
        element: <PermissionGuard permission={P.USER_READ}><Users /></PermissionGuard>,
      },
      {
        path: 'teams',
        element: <PermissionGuard permission={P.USER_READ}><TeamsPage /></PermissionGuard>,
      },
      {
        path: 'change-password',
        element: <ChangePassword />,
      },
    ],
  },

  // 兼容旧路由重定向
  { path: '/admin/*', element: <Navigate to="/" replace /> },
  { path: '/user/*', element: <Navigate to="/" replace /> },
  { path: '/models', element: <Navigate to="/providers" replace /> },
  { path: '/experience', element: <Navigate to="/dashboard" replace /> },
  { path: '/api-key-pool', element: <Navigate to="/providers" replace /> },
  { path: '/api-keys', element: <Navigate to="/teams" replace /> },

  // 默认重定向
  { path: '*', element: <Navigate to="/dashboard" replace /> },
]);
