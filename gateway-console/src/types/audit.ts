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

/** 审计日志条目（与后端 AuditLogResponse 一致） */
export interface AuditLogItem {
  id: number;
  /** 操作人用户 ID */
  userId: number;
  /** 操作动作（如 "POST /api/v1/channels"） */
  action: string;
  /** 操作资源路径 */
  resource: string;
  /** 操作结果 */
  result: 'SUCCESS' | 'FAILURE';
  /** 客户端 IP */
  ipAddress: string;
  /** 操作时间（ISO-8601） */
  createdAt: string;
}

/** 审计日志查询参数 */
export interface AuditLogQueryParams {
  page?: number;
  limit?: number;
  userId?: number;
  action?: string;
  result?: 'SUCCESS' | 'FAILURE';
  startTime?: string;
  endTime?: string;
}
