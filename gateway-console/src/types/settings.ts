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

/**
 * 系统设置类型定义
 *
 * <p>类型字段与后端 SystemSettingResponse（GET /api/v1/settings）、
 * SettingUpdateRequest（PUT /api/v1/settings/{key}）响应对齐。</p>
 */

/** 系统设置项（与后端 SystemSettingResponse 对齐） */
export interface SystemSetting {
  /** 设置键（业务唯一，如 audit.retention.days） */
  settingKey: string;
  /** 设置值（字符串形式） */
  settingValue?: string;
  /** 分组名（如 AUDIT / CATALOG） */
  groupName?: string;
  /** 设置描述 */
  description?: string;
  /** 值类型（NUMBER / BOOLEAN / ENUM / STRING） */
  valueType?: string;
  /** 是否允许运行时修改 */
  editable?: boolean;
}

/** 审计日志清理结果（与后端 DELETE /api/v1/audit-logs 返回的 {deleted: N} 对齐） */
export interface AuditCleanupResult {
  /** 删除条数 */
  deleted: number;
}

/** 模型目录自动同步周期（与后端 SyncInterval 枚举 code 对齐） */
export type CatalogSyncInterval = 'DAILY' | 'WEEKLY' | 'MONTHLY';
