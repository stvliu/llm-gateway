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
package com.codingas.simulator.service;

import java.time.Instant;

/**
 * 请求记录，保存每次模拟请求的方法、路径和时间戳。
 *
 * @param method    HTTP 方法（如 GET、POST）
 * @param path      请求路径
 * @param timestamp 请求时间戳
 */
public record RequestRecord(String method, String path, Instant timestamp) {
}
