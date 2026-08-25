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
import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from '@/stores/authStore';

/**
 * authStore 权限来源测试
 *
 * <p>后端为权威：setUser 优先采用登录响应携带的 permissions；
 * 旧数据（未返回 permissions）时按 role 兜底推导（rolePermissions.ts）。</p>
 */
describe('authStore 权限来源', () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.setState({ user: null, token: null, isAuthenticated: false });
  });

  it('setUser 优先使用后端返回的权限码', () => {
    useAuthStore.getState().setUser({
      id: 1,
      username: 'dev',
      email: 'dev@example.com',
      role: 'USER',
      permissions: ['dashboard', 'model:read', 'quickstart:access', 'key:read', 'key:write', 'application:read'],
    });

    const { user, hasPermission } = useAuthStore.getState();
    expect(user?.permissions).toEqual([
      'dashboard', 'model:read', 'quickstart:access', 'key:read', 'key:write', 'application:read',
    ]);
    expect(hasPermission('quickstart:access')).toBe(true);
    expect(hasPermission('user:read')).toBe(false);
    expect(hasPermission('channel:write')).toBe(false);
  });

  it('后端未返回权限（旧数据）时按角色兜底推导', () => {
    useAuthStore.getState().setUser({
      id: 1,
      username: 'dev',
      email: 'dev@example.com',
      role: 'USER',
      permissions: [],
    });

    const { hasPermission } = useAuthStore.getState();
    // 兜底：USER 角色推导应含 model:read / quickstart，不含 user:write
    expect(hasPermission('model:read')).toBe(true);
    expect(hasPermission('quickstart:access')).toBe(true);
    expect(hasPermission('user:write')).toBe(false);
    expect(hasPermission('user:read')).toBe(false);
  });

  it('ADMIN 角色：后端返回全部权限', () => {
    useAuthStore.getState().setUser({
      id: 1,
      username: 'admin',
      email: 'admin@example.com',
      role: 'ADMIN',
      permissions: ['dashboard', 'dashboard:admin', 'user:write', 'channel:write'],
    });

    const { hasPermission } = useAuthStore.getState();
    expect(hasPermission('user:write')).toBe(true);
    expect(hasPermission('channel:write')).toBe(true);
  });
});
