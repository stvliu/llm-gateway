import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import AdminView from './AdminView';
import UserView from './UserView';

export default function ApiKeys() {
  const { hasPermission } = useAuthStore();

  return hasPermission(P.USER_READ) ? <AdminView /> : <UserView />;
}