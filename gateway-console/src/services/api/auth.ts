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
    api.post<void>('/auth/change-password', data),
};
