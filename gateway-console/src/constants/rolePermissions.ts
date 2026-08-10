/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { P, type Permission } from './permissions';

/** 后端 role 单值 → 前端 permissions 数组 */
const ROLE_PERMISSIONS: Record<string, Permission[]> = {
  ADMIN: Object.values(P),
  USER: [P.DASHBOARD, P.MODEL_READ, P.USER_READ, P.SETTINGS_READ],
};

/** 根据角色获取权限集合 */
export function getPermissionsByRole(role: string): Permission[] {
  return ROLE_PERMISSIONS[role] ?? ROLE_PERMISSIONS['USER'];
}