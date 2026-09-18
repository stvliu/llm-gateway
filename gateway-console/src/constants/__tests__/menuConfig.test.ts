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
// sidebar 菜单结构测试：镜像 Gitee wiki 侧边栏「管理员指南」单节结构
//
// wiki 顶层：Home → 快速开始 → 管理员指南 → 开发者指南 → 参考；
// 管理台仅 Home（仪表盘）与 管理员指南（全部管理页）有对应内容，
// 故侧边栏为：仪表盘（顶层）+ 单一分组 adminGuide（10 项，组内为既有相对顺序）。
import { describe, it, expect } from 'vitest';
import { topLevelMenuItems, menuGroups } from '@/constants/menuConfig';

describe('menuConfig：sidebar 镜像 wiki「管理员指南」结构', () => {
  it('顶层仅仪表盘（对应 wiki Home）', () => {
    expect(topLevelMenuItems.map((item) => item.key)).toEqual(['/dashboard']);
  });

  it('单一分组 adminGuide，10 个菜单项顺序固定', () => {
    expect(menuGroups).toHaveLength(1);
    const [group] = menuGroups;
    expect(group.key).toBe('adminGuide');
    expect(group.items.map((item) => item.key)).toEqual([
      '/channels',
      '/models',
      '/catalog',
      '/keys',
      '/applications',
      '/users',
      '/token-limits',
      '/resilience/overview',
      '/audit-logs',
      '/settings',
    ]);
  });

  it('重排不改变任何菜单项的权限声明', () => {
    const all = [...topLevelMenuItems, ...menuGroups.flatMap((g) => g.items)];
    expect(Object.fromEntries(all.map((item) => [item.key, item.permission]))).toEqual({
      '/dashboard': undefined,
      '/channels': 'channel:read',
      '/models': 'model:read',
      '/catalog': 'catalog:read',
      '/keys': 'key:read',
      '/applications': 'application:read',
      '/users': 'user:read',
      '/token-limits': 'token-limit:manage',
      '/resilience/overview': 'resilience:read',
      '/audit-logs': 'audit:read',
      '/settings': 'settings:read',
    });
  });
});
