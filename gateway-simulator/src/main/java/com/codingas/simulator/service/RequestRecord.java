/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
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
