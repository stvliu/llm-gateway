import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AuthGuard, PermissionGuard } from './guards';
import Login from '@/pages/Login';
import AppLayout from '@/components/layout/AppLayout';
import Dashboard from '@/pages/Dashboard';
import Models from '@/pages/Models';
import Providers from '@/pages/Providers';
import Templates from '@/pages/Templates';
import ApiKeyPool from '@/pages/ApiKeyPool';
import Users from '@/pages/Users';
import ApiKeys from '@/pages/ApiKeys';
import Settings from '@/pages/Settings';
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
        path: 'models',
        element: <PermissionGuard permission={P.MODEL_READ}><Models /></PermissionGuard>,
      },
      {
        path: 'providers',
        element: <PermissionGuard permission={P.PROVIDER_READ}><Providers /></PermissionGuard>,
      },
      {
        path: 'templates',
        element: <PermissionGuard permission={P.TEMPLATE_READ}><Templates /></PermissionGuard>,
      },
      {
        path: 'api-key-pool',
        element: <PermissionGuard permission={P.APIKEY_POOL_READ}><ApiKeyPool /></PermissionGuard>,
      },
      {
        path: 'users',
        element: <PermissionGuard permission={P.USER_READ}><Users /></PermissionGuard>,
      },
      {
        path: 'api-keys',
        element: <PermissionGuard permission={P.APIKEY_MANAGE}><ApiKeys /></PermissionGuard>,
      },
      {
        path: 'settings',
        element: <PermissionGuard permission={P.SETTINGS_READ}><Settings /></PermissionGuard>,
      },
    ],
  },

  // 兼容旧路由重定向
  { path: '/admin/*', element: <Navigate to="/" replace /> },
  { path: '/user/*', element: <Navigate to="/" replace /> },

  // 默认重定向
  { path: '*', element: <Navigate to="/dashboard" replace /> },
]);