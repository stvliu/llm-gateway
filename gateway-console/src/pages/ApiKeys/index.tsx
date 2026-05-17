import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import AdminView from './AdminView';
import UserView from './UserView';

export default function ApiKeys() {
  const { hasPermission, user, isAuthenticated } = useAuthStore();

  // 调试信息
  console.log('[ApiKeys] isAuthenticated:', isAuthenticated);
  console.log('[ApiKeys] user:', user);
  console.log('[ApiKeys] hasPermission(P.USER_READ):', hasPermission(P.USER_READ));
  console.log('[ApiKeys] hasPermission(P.APIKEY_MANAGE):', hasPermission(P.APIKEY_MANAGE));

  if (!isAuthenticated || !user) {
    return <div style={{ padding: 24 }}>加载中...</div>;
  }

  return hasPermission(P.USER_READ) ? <AdminView /> : <UserView />;
}
