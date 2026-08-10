/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import AdminView from './AdminView';
import UserView from './UserView';

export default function Dashboard() {
  const { hasPermission } = useAuthStore();

  return hasPermission(P.DASHBOARD_ADMIN) ? <AdminView /> : <UserView />;
}