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
package com.codingas.gateway.provider.channel;

/**
 * 触发健康测试的来源。
 *
 * <p>仅 CARD / DRAWER 会持久化到 channel 表；PRECHECK 来自创建前预检工具，不写库。</p>
 *
 * <ul>
 *   <li>CARD：渠道卡片闪电图标触发</li>
 *   <li>DRAWER：详情抽屉"测试全部"触发</li>
 *   <li>PRECHECK：创建前预检工具触发</li>
 * </ul>
 */
public enum ChannelHealthSource {
    CARD,
    DRAWER,
    PRECHECK
}
