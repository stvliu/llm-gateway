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

/**
 * 前端角色 → UI 权限映射（基于 USER/ADMIN 两角色授权）
 *
 * <p>与后端 PermissionInterceptor 的 USER 白名单语义一致：
 * 普通用户可用 = 仪表盘 + 模型只读 + 体验中心 + 自己的 API Key + 应用只读。
 * 后端按同一 role 强制校验兜底，前端映射仅控制 UI 显隐。</p>
 */
const ROLE_PERMISSIONS: Record<string, Permission[]> = {
  ADMIN: Object.values(P),
  USER: [P.DASHBOARD, P.MODEL_READ, P.QUICKSTART, P.KEY_READ, P.KEY_WRITE, P.APPLICATION_READ],
};

/** 根据角色获取权限集合 */
export function getPermissionsByRole(role: string): Permission[] {
  return ROLE_PERMISSIONS[role] ?? ROLE_PERMISSIONS['USER'];
}