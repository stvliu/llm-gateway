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