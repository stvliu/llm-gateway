/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
/**
 * 渠道健康检查相关类型（与后端 ChannelHealthStatus / ChannelHealthSource 枚举对齐）。
 *
 * <p>来源：第 1 章后端实现（gateway-boot），数据库列：
 * last_health_check_at / last_health_status / last_health_source。</p>
 *
 * <p>状态语义：</p>
 * <ul>
 *   <li>HEALTHY：所有 Key 通过认证</li>
 *   <li>DEGRADED：部分 Key 通过</li>
 *   <li>FAILED：全部 Key 失败</li>
 *   <li>UNKNOWN：尚未测试 / 零 Key 等无效检查</li>
 * </ul>
 */
export type ChannelHealthStatus = 'HEALTHY' | 'DEGRADED' | 'FAILED' | 'UNKNOWN';

/**
 * 健康检查触发来源。
 * <ul>
 *   <li>CARD：（保留语义）卡片就地触发；当前 9.1 已改为转抽屉，仍保留枚举供后端兼容</li>
 *   <li>DRAWER：详情抽屉触发，会持久化健康字段</li>
 *   <li>PRECHECK：预检工具触发，后端跳过持久化</li>
 * </ul>
 */
export type ChannelHealthSource = 'CARD' | 'DRAWER' | 'PRECHECK';
