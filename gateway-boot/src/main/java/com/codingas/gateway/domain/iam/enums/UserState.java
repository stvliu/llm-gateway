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
package com.codingas.gateway.domain.iam.enums;

/**
 * 用户状态枚举
 *
 * <p>企业内部 LLM Gateway 的用户账户生命周期状态。</p>
 *
 * <h3>状态转换图</h3>
 * <pre>
 * ACTIVE ⇄ INACTIVE
 *    ↓
 * LOCKED（安全事件触发）
 * </pre>
 *
 * <h3>状态说明</h3>
 * <ul>
 *   <li>ACTIVE：账户正常，可正常使用</li>
 *   <li>INACTIVE：账户停用，无法登录（可恢复）</li>
 *   <li>LOCKED：账户锁定，登录失败次数过多触发（可恢复）</li>
 * </ul>
 */
public enum UserState {
    /** 账户正常，可正常使用 */
    ACTIVE,

    /** 账户停用，无法登录（可恢复） */
    INACTIVE,

    /** 账户锁定（登录失败次数过多触发，可恢复） */
    LOCKED;

    /**
     * 判断是否可以登录
     */
    public boolean canLogin() {
        return this == ACTIVE;
    }

    /**
     * 判断是否为安全锁定状态
     */
    public boolean isLocked() {
        return this == LOCKED;
    }
}