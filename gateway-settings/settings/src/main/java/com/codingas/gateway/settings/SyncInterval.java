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
package com.codingas.gateway.settings;

/**
 * 同步周期枚举
 *
 * <p>用于目录同步等周期性任务配置项（如 {@code catalog.sync.interval}）的取值约束，
 * 与前端展示及调度逻辑（Task 5 同步自动执行）共用。</p>
 */
public enum SyncInterval {

    /** 每天同步 */
    DAILY,

    /** 每周同步 */
    WEEKLY,

    /** 每月同步 */
    MONTHLY
}
