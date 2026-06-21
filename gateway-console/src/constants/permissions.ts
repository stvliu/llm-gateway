/** 权限常量 */
export const P = {
  DASHBOARD: 'dashboard',
  DASHBOARD_ADMIN: 'dashboard:admin',
  MODEL_READ: 'model:read',
  MODEL_WRITE: 'model:write',
  PROVIDER_READ: 'provider:read',
  PROVIDER_WRITE: 'provider:write',
  CATALOG_READ: 'catalog:read',
  CATALOG_WRITE: 'catalog:write',
  USER_READ: 'user:read',
  USER_WRITE: 'user:write',
  SETTINGS_READ: 'settings:read',
  SETTINGS_WRITE: 'settings:write',
  KEY_READ: 'key:read',
  KEY_WRITE: 'key:write',
  CHANNEL_READ: 'channel:read',
  CHANNEL_WRITE: 'channel:write',
  APPLICATION_READ: 'application:read',
  APPLICATION_WRITE: 'application:write',
  RESILIENCE_READ: 'resilience:read',
  RESILIENCE_WRITE: 'resilience:write',
  QUICKSTART: 'quickstart:access',
  AUDIT_READ: 'audit:read',
} as const;

export type Permission = (typeof P)[keyof typeof P];