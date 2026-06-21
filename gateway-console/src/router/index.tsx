import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AuthGuard, PermissionGuard } from './guards';
import Login from '@/pages/Login';
import AppLayout from '@/components/layout/AppLayout';
import Dashboard from '@/pages/Dashboard';
import Models from '@/pages/Models';
import ApiKeys from '@/pages/ApiKeys';
import Quickstart from '@/pages/Quickstart';
import Channels from '@/pages/Channels';
import Catalog from '@/pages/Catalog';
import Users from '@/pages/Users';
import ApplicationsPage from '@/pages/Applications';
import ResilienceLayout from '@/pages/resilience/ResilienceLayout';
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
        path: 'channels',
        element: <PermissionGuard permission={P.CHANNEL_READ}><Channels /></PermissionGuard>,
      },
      {
        path: 'providers',
        element: <Navigate to="/channels" replace />,
      },
      {
        path: 'models',
        element: <PermissionGuard permission={P.MODEL_READ}><Models /></PermissionGuard>,
      },
      {
        path: 'keys',
        element: <PermissionGuard permission={P.KEY_READ}><ApiKeys /></PermissionGuard>,
      },
      {
        path: 'quickstart',
        element: <PermissionGuard permission={P.QUICKSTART}><Quickstart /></PermissionGuard>,
      },
      {
        path: 'catalog',
        element: <PermissionGuard permission={P.CATALOG_READ}><Catalog /></PermissionGuard>,
      },
      {
        path: 'users',
        element: <PermissionGuard permission={P.USER_READ}><Users /></PermissionGuard>,
      },
      {
        path: 'applications',
        element: <PermissionGuard permission={P.APPLICATION_READ}><ApplicationsPage /></PermissionGuard>,
      },
      {
        path: 'resilience',
        element: <PermissionGuard permission={P.RESILIENCE_READ}><ResilienceLayout /></PermissionGuard>,
      },
      {
        path: 'resilience/:tab',
        element: <PermissionGuard permission={P.RESILIENCE_READ}><ResilienceLayout /></PermissionGuard>,
      },
    ],
  },

  // 兼容旧路由重定向
  { path: '/admin/*', element: <Navigate to="/" replace /> },
  { path: '/user/*', element: <Navigate to="/" replace /> },
  { path: '/experience', element: <Navigate to="/dashboard" replace /> },
  { path: '/api-key-pool', element: <Navigate to="/providers" replace /> },
  { path: '/metadata', element: <Navigate to="/catalog" replace /> },
  { path: '/developer', element: <Navigate to="/quickstart" replace /> },
  { path: '/change-password', element: <Navigate to="/dashboard" replace /> },

  // 默认重定向
  { path: '*', element: <Navigate to="/dashboard" replace /> },
]);
