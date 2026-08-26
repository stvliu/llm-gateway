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

import com.codingas.gateway.provider.channel.ChannelRepository;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import com.codingas.gateway.iam.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报表服务
 *
 * <p>通过各域核心 Gateway 端口获取统计计数（端口调用，不依赖绑定模块 Repository）。</p>
 */
@Service
@RequiredArgsConstructor
public class StatsManager {

    private final ProviderRepository providerRepository;
    private final ChannelRepository channelRepository;
    private final ModelRepository modelRepository;
    private final UserRepository userRepository;

    /**
     * 获取系统统计数据
     */
    @Transactional(readOnly = true)
    public StatsResult getStats() {
        long providerCount = providerRepository.count();
        long channelCount = channelRepository.count();
        long modelCount = modelRepository.count();
        long userCount = userRepository.count();
        // TODO: 接入真实的请求统计和 Token 用量数据
        long todayRequests = 0;
        String tokenUsage = "0";
        return new StatsResult(
                providerCount,
                channelCount,
                modelCount,
                userCount,
                todayRequests,
                tokenUsage
        );
    }
}
