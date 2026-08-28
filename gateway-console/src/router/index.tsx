/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import OverviewPage from '@/pages/resilience/overview';
import AuditLogs from '@/pages/AuditLogs';
import TokenLimits from '@/pages/TokenLimits';
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
        element: <PermissionGuard permission={P.RESILIENCE_READ}><OverviewPage /></PermissionGuard>,
      },
      {
        path: 'resilience/:tab',
        element: <PermissionGuard permission={P.RESILIENCE_READ}><OverviewPage /></PermissionGuard>,
      },
      {
        path: 'audit-logs',
        element: <PermissionGuard permission={P.AUDIT_READ}><AuditLogs /></PermissionGuard>,
      },
      {
        path: 'token-limits',
        element: <PermissionGuard permission={P.TOKEN_LIMIT}><TokenLimits /></PermissionGuard>,
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
  { path: '/experience', element: <Navigate to="/dashboard" replace /> },
  { path: '/api-key-pool', element: <Navigate to="/providers" replace /> },
  { path: '/metadata', element: <Navigate to="/catalog" replace /> },
  { path: '/developer', element: <Navigate to="/quickstart" replace /> },
  { path: '/change-password', element: <Navigate to="/dashboard" replace /> },

  // 默认重定向
  { path: '*', element: <Navigate to="/dashboard" replace /> },
]);
