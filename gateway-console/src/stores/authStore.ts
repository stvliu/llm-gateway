import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { getPermissionsByRole } from '@/constants/rolePermissions';
import type { CurrentUser, LoginUserResponse } from '@/types/user';
import type { Permission } from '@/constants/permissions';

interface AuthState {
  user: CurrentUser | null;
  token: string | null;
  isAuthenticated: boolean;
  setUser: (user: LoginUserResponse | null) => void;
  setToken: (token: string | null) => void;
  logout: () => void;
  hasPermission: (permission: Permission) => boolean;
  hasAnyPermission: (permissions: Permission[]) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      setUser: (loginUser) =>
        set({
          user: loginUser
            ? {
                ...loginUser,
                permissions: getPermissionsByRole(loginUser.role),
              }
            : null,
          isAuthenticated: !!loginUser,
        }),
      setToken: (token) => {
        if (token) {
          localStorage.setItem('token', token);
        } else {
          localStorage.removeItem('token');
        }
        set({ token });
      },
      logout: () => {
        localStorage.removeItem('token');
        set({
          user: null,
          token: null,
          isAuthenticated: false,
        });
      },
      hasPermission: (permission) => {
        const { user } = get();
        return user?.permissions?.includes(permission) ?? false;
      },
      hasAnyPermission: (permissions) => {
        const { user } = get();
        return permissions.some((p) => user?.permissions?.includes(p)) ?? false;
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
      }),
      onRehydrateStorage: () => (state) => {
        if (state?.token) {
          localStorage.setItem('token', state.token);
        }
        // 旧数据可能没有 permissions 字段，从 role 补全
        if (state?.user && !state.user.permissions) {
          state.user = {
            ...state.user,
            permissions: getPermissionsByRole(state.user.role),
          };
        }
      },
    }
  )
);
