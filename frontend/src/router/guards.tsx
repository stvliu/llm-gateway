import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import type { UserRole } from '@/types/user';

interface AuthGuardProps {
  children: React.ReactNode;
}

/** 认证守卫：检查是否已登录 */
export function AuthGuard({ children }: AuthGuardProps) {
  const { isAuthenticated } = useAuthStore();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}

interface RoleGuardProps {
  children: React.ReactNode;
  allowedRoles: UserRole[];
}

/** 角色守卫：检查用户角色 */
export function RoleGuard({ children, allowedRoles }: RoleGuardProps) {
  const { user } = useAuthStore();
  const location = useLocation();

  if (user && !allowedRoles.includes(user.role)) {
    // 非管理员重定向到用户页面
    return <Navigate to="/user/models" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}
