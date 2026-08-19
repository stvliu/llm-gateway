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
package com.codingas.gateway.common.enums;

/**
 * 应用级失败处理策略（三选一互斥）
 *
 * <p>控制 ChannelFailoverInvoker 的 L0（同渠道换 Key）/L1（换渠道）行为：
 * <ul>
 *   <li>FAIL_FAST — L0 不跑、L1 不跑，首个 Key 失败立即抛错</li>
 *   <li>FAIL_RETRY — L0 跑、L1 不跑，同渠道换 Key 不换渠道（默认）</li>
 *   <li>FAIL_OVER — L0 跑、L1 跑，换 Key + 换渠道，全耗尽抛错</li>
 * </ul>
 * 递进关系：FAIL_FAST ⊂ FAIL_RETRY ⊂ FAIL_OVER。
 *
 * <p>轻量单字段挂 Application，不演变为已删的 ResilienceProfile（独立实体）。</p>
 */
public enum FailureStrategy {
    /** 快速失败：首个 Key 失败立即抛错 */
    FAIL_FAST,
    /** 失败重试：同渠道换 Key，不换渠道（默认） */
    FAIL_RETRY,
    /** 失败转移：换 Key + 换渠道，全耗尽抛错 */
    FAIL_OVER
}
