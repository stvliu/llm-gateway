import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import AdminView from './AdminView';
import UserView from './UserView';

export default function Dashboard() {
  const { hasPermission } = useAuthStore();

  return hasPermission(P.DASHBOARD_ADMIN) ? <AdminView /> : <UserView />;
}