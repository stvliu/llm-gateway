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
import { api } from './client';
import type { LoginRequest, LoginResponseData, CurrentUser } from '@/types/user';

export const authApi = {
  /** 登录 */
  login: (data: LoginRequest) =>
    api.post<LoginResponseData>('/auth/login', data),

  /** 登出 */
  logout: () =>
    api.post<void>('/auth/logout'),

  /** 获取当前用户信息 */
  me: () =>
    api.get<CurrentUser>('/auth/me'),

  /** 修改密码 */
  changePassword: (data: { currentPassword: string; newPassword: string }) =>
    api.patch<void>('/auth/me/password', data),
};
