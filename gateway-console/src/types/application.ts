/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
/** 应用级失败处理策略（与后端 FailureStrategy 枚举一致） */
export type FailureStrategy = 'FAIL_FAST' | 'FAIL_RETRY' | 'FAIL_OVER';

/**
 * 应用信息（与后端 ApplicationResponse 一致）
 *
 * Application 是权限+行为双聚合根，承载 Key 归属与渠道可见性。
 *
 * <p>Task 8：resilienceProfileId 退场（ResilienceProfile 实体删除），
 * 改为应用级 timeout（承接原 ResilienceProfile.timeout，0 表示用渠道默认）。</p>
 *
 * <p>Task 10：新增应用级 failureStrategy，控制渠道故障时的转移行为
 * （FAIL_FAST/FAIL_RETRY/FAIL_OVER，默认 FAIL_RETRY）。</p>
 */
export interface Application {
  id: number;
  /** 应用编码，全局唯一 */
  code: string;
  /** 应用名称 */
  name: string;
  /** 应用描述 */
  description: string;
  /** 应用生命周期状态（ACTIVE/INACTIVE） */
  state: string;
  /** 请求超时秒数（0 表示用渠道默认；承接原 ResilienceProfile.timeout） */
  timeout: number;
  /** 应用级失败处理策略（FAIL_FAST/FAIL_RETRY/FAIL_OVER） */
  failureStrategy: string;
  /** 配额预算 ID（预留） */
  quotaBudgetId?: number | null;
  /** 看板 ID（预留） */
  dashboardId?: number | null;
  createdAt: string;
  updatedAt: string;
}

/** 创建应用请求 */
export interface CreateApplicationRequest {
  code: string;
  name: string;
  description?: string;
  /** 请求超时秒数（0 表示用渠道默认） */
  timeout?: number;
  /** 应用级失败处理策略（默认 FAIL_RETRY） */
  failureStrategy?: FailureStrategy;
}

/** 更新应用请求 */
export type UpdateApplicationRequest = CreateApplicationRequest;

/**
 * 应用-渠道授权项（与后端 ApplicationChannelItem 一致）
 *
 * Task gap2：转移顺序由应用级 ApplicationChannel.priority 决定，
 * 管理端通过本类型读写渠道授权及其 priority。
 *
 * - channelId：渠道 ID
 * - priority：转移优先级（数值越小越优先；null 表示未配置，后端回退默认值 100）
 */
export interface ApplicationChannelItem {
  channelId: number;
  priority: number | null;
}
