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