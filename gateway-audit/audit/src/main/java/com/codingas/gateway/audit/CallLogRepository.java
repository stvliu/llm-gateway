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

import java.time.Instant;
import java.util.List;

/**
 * 调用日志网关接口
 */
public interface CallLogRepository {

    /**
     * 保存调用日志
     */
    CallLog save(CallLog callLog);

    /**
     * 统计指定时间之后的调用次数
     *
     * @param since 起始时间（含）
     * @return 调用次数
     */
    long countSince(Instant since);

    /**
     * 统计指定时间之后的 Token 消耗（输入 + 输出）
     *
     * @param since 起始时间（含）
     * @return Token 总量
     */
    long sumTokensSince(Instant since);

    /**
     * 按天聚合指定时间范围内的调用用量
     *
     * @param start 起始时间（含）
     * @param end   结束时间（含）
     * @return 按天聚合列表
     */
    List<DailyUsage> findDailyUsage(Instant start, Instant end);

    /**
     * 按模型聚合调用量（Top N）
     *
     * @param limit 返回条数上限
     * @return 按请求数降序的模型用量
     */
    List<ModelUsage> findModelUsage(int limit);
}
