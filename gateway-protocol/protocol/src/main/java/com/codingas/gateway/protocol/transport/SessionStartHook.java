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
package com.codingas.gateway.protocol.transport;

import java.util.List;

/**
 * 出站会话开始钩子（SessionStart Hook）
 *
 * <p>在 {@link UpstreamClient} 真正发起上游 HTTP 请求<b>之前</b>触发，用于初始化
 * 会话级上下文：审计打点、用量累计、追踪关联等。OpenAI / Anthropic / Gemini
 * 三个出站协议插件以相同方式接入——注册一个 {@link SessionStartHook} Bean，
 * 即对所有协议统一生效。</p>
 */
@FunctionalInterface
public interface SessionStartHook {

    /** 空实现：无 hook 注册时行为不变（向后兼容） */
    SessionStartHook NOOP = context -> {
    };

    /**
     * 会话开始回调（上游请求发出前）
     *
     * @param context 会话开始上下文
     */
    void onSessionStart(SessionStartContext context);

    /**
     * 组合多个 hook 为单个（按列表顺序依次调用）
     *
     * @param hooks hook 列表；null 或空时返回 {@link #NOOP}
     * @return 组合后的 hook
     */
    static SessionStartHook composite(List<SessionStartHook> hooks) {
        if (hooks == null || hooks.isEmpty()) {
            return NOOP;
        }
        return context -> hooks.forEach(hook -> hook.onSessionStart(context));
    }
}
