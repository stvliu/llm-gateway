import type { ReactNode } from 'react';
import { useAuthStore } from '@/stores/authStore';
import type { Permission } from '@/constants/permissions';

interface PermissionGuardProps {
  permission: Permission;
  fallback?: ReactNode;
  children: ReactNode;
}

/** 声明式权限包裹组件，无权限时渲染 fallback */
export function PermissionGuard({
  permission,
  fallback = null,
  children,
}: PermissionGuardProps) {
  const { hasPermission } = useAuthStore();
  return hasPermission(permission) ? children : fallback;
}