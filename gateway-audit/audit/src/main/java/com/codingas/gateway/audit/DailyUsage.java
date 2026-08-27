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
package com.codingas.gateway.audit;

/**
 * 按天调用用量聚合结果
 *
 * @param date        日期（yyyy-MM-dd）
 * @param requestCount 请求数
 * @param tokenCount    Token 消耗（输入 + 输出）
 */
public record DailyUsage(String date, long requestCount, long tokenCount) {
}
