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
package com.codingas.gateway.stats;

import com.codingas.gateway.stats.dto.StatsResponse;
import com.codingas.gateway.providerdata.repository.ChannelRepository;
import com.codingas.gateway.providerdata.repository.ModelRepository;
import com.codingas.gateway.providerdata.repository.ProviderRepository;
import com.codingas.gateway.iamdata.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 统计服务
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final ProviderRepository providerRepository;
    private final ChannelRepository channelRepository;
    private final ModelRepository modelRepository;
    private final UserRepository userRepository;

    /**
     * 获取系统统计数据
     */
    @Transactional(readOnly = true)
    public StatsResponse getStats() {
        long providerCount = providerRepository.count();
        long channelCount = channelRepository.count();
        long modelCount = modelRepository.count();
        long userCount = userRepository.count();

        // TODO: 接入真实的请求统计和 Token 用量数据
        // 目前返回模拟数据，后续需要从审计日志或时序数据库中统计
        long todayRequests = 0;
        String tokenUsage = "0";

        return new StatsResponse(
            providerCount,
            channelCount,
            modelCount,
            userCount,
            todayRequests,
            tokenUsage
        );
    }
}
