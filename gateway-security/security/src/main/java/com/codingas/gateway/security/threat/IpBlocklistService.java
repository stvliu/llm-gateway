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
package com.codingas.gateway.security.threat;

import com.codingas.gateway.security.threat.IpBlocklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * IP 黑名单服务
 *
 * <p>提供动态 IP 封禁/解封、黑名单查询功能。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpBlocklistService {

    private final IpBlocklistRepository ipBlockRepository;

    /**
     * 检查 IP 是否在黑名单中
     */
    public boolean isBlocked(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        return ipBlockRepository.isBlocked(ipAddress);
    }

    /**
     * 封禁 IP（永久）
     */
    public void blockIp(String ipAddress, String reason, Long blockedBy) {
        ipBlockRepository.block(ipAddress, reason, blockedBy, null);
        log.info("IP blocked: address={}, reason={}, by={}", ipAddress, reason, blockedBy);
    }

    /**
     * 封禁 IP（临时）
     */
    public void blockIp(String ipAddress, String reason, Long blockedBy, long durationMinutes) {
        java.time.Instant expiresAt = java.time.Instant.now().plusSeconds(durationMinutes * 60);
        ipBlockRepository.block(ipAddress, reason, blockedBy, expiresAt);
        log.info("IP blocked: address={}, reason={}, by={}, expires={}",
            ipAddress, reason, blockedBy, expiresAt);
    }

    /**
     * 解封 IP
     */
    public void unblockIp(String ipAddress) {
        ipBlockRepository.unblock(ipAddress);
        log.info("IP unblocked: address={}", ipAddress);
    }
}
