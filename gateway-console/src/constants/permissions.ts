/** 权限常量 */
export const P = {
  DASHBOARD: 'dashboard',
  DASHBOARD_ADMIN: 'dashboard:admin',
  MODEL_READ: 'model:read',
  MODEL_WRITE: 'model:write',
  PROVIDER_READ: 'provider:read',
  PROVIDER_WRITE: 'provider:write',
  TEMPLATE_READ: 'template:read',
  APIKEY_POOL_READ: 'apikey-pool:read',
  USER_READ: 'user:read',
  USER_WRITE: 'user:write',
  APIKEY_MANAGE: 'apikey:manage',
  SETTINGS_READ: 'settings:read',
  SETTINGS_WRITE: 'settings:write',
} as const;

export type Permission = (typeof P)[keyof typeof P];
