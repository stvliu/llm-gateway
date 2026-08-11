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
        set({ token });
      },
      logout: () => {
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
