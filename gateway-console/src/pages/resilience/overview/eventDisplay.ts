/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
/**
 * 容灾总览转移事件展示辅助纯函数
 *
 * <p>提取自 OverviewPage，便于单测。包含：
 * <ul>
 *   <li>errorTypeMeta：ProviderErrorType → Tag 颜色映射（按严重程度着色）</li>
 *   <li>formatRoute：from→to 渠道路径格式化（exhausted 无目标时返回标记 key）</li>
 *   <li>decisionMeta：L1/L2 决策 → Tag 颜色与 i18n key</li>
 * </ul>
 * </p>
 *
 * <p>颜色约定：
 * <ul>
 *   <li>red：认证/请求格式错误（配置问题，需人工介入）</li>
 *   <li>orange：限流/配额（供给侧容量问题，可恢复）</li>
 *   <li>volcano：超时/网络/上游不可用（瞬时故障，重试可恢复）</li>
 *   <li>default：未知错误</li>
 * </ul>
 * </p>
 */
import type { FailoverEvent } from '@/types/resilience';

/** Tag 颜色类型（与 antd Tag color 接受值对齐） */
export type TagColor =
  | 'red'
  | 'orange'
  | 'volcano'
  | 'blue'
  | 'purple'
  | 'green'
  | 'default';

/** errorType 元信息 */
export interface ErrorTypeMeta {
  /** Tag 颜色 */
  color: TagColor;
}

/** decision 元信息 */
export interface DecisionMeta {
  /** Tag 颜色 */
  color: TagColor;
  /** i18n 文案 key 后缀（如 'overview.decisions.L1' 或原始值） */
  labelKey: string;
}

/**
 * 按 ProviderErrorType 返回 Tag 颜色
 *
 * @param errorType 错误类型枚举名（后端以字符串返回）
 */
export function errorTypeMeta(errorType?: string | null): ErrorTypeMeta {
  switch (errorType) {
    // 认证/请求格式错误：配置问题，需人工介入，红色
    case 'AUTHENTICATION_ERROR':
    case 'INVALID_REQUEST':
      return { color: 'red' };
    // 限流/配额：供给侧容量问题，橙色
    case 'RATE_LIMIT_ERROR':
    case 'QUOTA_EXCEEDED':
      return { color: 'orange' };
    // 超时/网络/上游不可用：瞬时故障，volcano
    case 'TIMEOUT_ERROR':
    case 'NETWORK_ERROR':
    case 'SERVICE_UNAVAILABLE':
    case 'UPSTREAM_ERROR':
      return { color: 'volcano' };
    // 未知错误
    default:
      return { color: 'default' };
  }
}

/**
 * 格式化转移路径
 *
 * <p>有 to 渠道时显示「from → to」；exhausted 且无 to 渠道时返回 'exhaustedNoTarget'
 * 标记 key（由调用方走 i18n）；from 缺失用 '?' 占位。</p>
 *
 * @param ev 转移事件
 * @returns 路径字符串或标记 key
 */
export function formatRoute(ev: FailoverEvent): string {
  const from = ev.fromChannelId ?? '?';
  const to = ev.toChannelId;
  // exhausted 且无目标：候选全部耗尽，显示标记 key
  if (ev.exhausted && (to === null || to === undefined)) {
    return 'exhaustedNoTarget';
  }
  return `${from} → ${to ?? '?'}`;
}

/**
 * 返回决策 Tag 元信息
 *
 * @param decision 决策枚举名（L1/L2）
 */
export function decisionMeta(decision?: string | null): DecisionMeta {
  switch (decision) {
    case 'L1':
      return { color: 'blue', labelKey: 'overview.decisions.L1' };
    case 'L2':
      return { color: 'purple', labelKey: 'overview.decisions.L2' };
    default:
      // 未知决策：回退默认色，labelKey 用原始值
      return { color: 'default', labelKey: decision ?? '' };
  }
}
