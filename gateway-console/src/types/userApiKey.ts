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
 * 用户 API Key 相关类型（与后端 UserApiKeyResponse 一致）
 *
 * 一个 Key 归属一个用户，并挂载到具体应用（applicationId）作为权限锚点——
 * 通过应用-渠道授权关系（ApplicationChannel）继承渠道访问权限。
 * applicationId 为 null 时权限路由返回空集，Key 不可用。
 */

/** 用户 API Key */
export interface UserApiKey {
  id: number;
  userId: number;
  applicationId: number | null;
  keyPrefix: string;
  keyPlain: string;
  name: string;
  deleted: boolean;
  createdAt: string;
  updatedAt: string;
}

/** 用户 API Key 详情 */
export interface UserApiKeyDetail extends UserApiKey {
}

/** 创建用户 API Key 请求 */
export interface CreateUserApiKeyRequest {
  userId: number;
  applicationId: number;
  name: string;
}

/** 更新用户 API Key 请求 */
export interface UpdateUserApiKeyRequest {
  applicationId?: number;
  name?: string;
}

/** 创建用户 API Key 响应 */
export interface CreateUserApiKeyResponse {
  id: number;
  keyPrefix: string;
  keyPlain: string;
}
