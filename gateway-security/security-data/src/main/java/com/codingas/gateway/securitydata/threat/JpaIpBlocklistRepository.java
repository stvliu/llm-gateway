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
package com.codingas.gateway.securitydata.threat;

import com.codingas.gateway.security.threat.IpBlocklist;
import com.codingas.gateway.security.threat.IpBlocklistRepository;
import com.codingas.gateway.securitydata.threat.IpBlocklistJpaRepository;
import com.codingas.gateway.securitydata.threat.IpBlocklistDo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * IP 黑名单网关实现
 *
 * <p>实现 IpBlocklistRepository 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaIpBlocklistRepository implements IpBlocklistRepository {

    private final IpBlocklistJpaRepository repository;
    private final IpBlocklistConverter converter;

    @Override
    public boolean isBlocked(String ipAddress) {
        return repository.findByIpAddress(ipAddress)
                .filter(ip -> ip.getExpiresAt() == null || ip.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    @Override
    public void block(String ipAddress, String reason, Long blockedBy, Instant expiresAt) {
        Optional<IpBlocklistDo> existingDo = repository.findByIpAddress(ipAddress);

        IpBlocklistDo blocklistDo;
        if (existingDo.isPresent()) {
            // 将 DO 转换为实体，执行领域逻辑
            IpBlocklist blocklist = converter.toDomain(existingDo.get());
            
            // 在实体上设置属性（未来可以添加领域验证逻辑）
            blocklist.setBlockReason(reason);
            blocklist.setBlockedBy(blockedBy);
            blocklist.setExpiresAt(expiresAt);
            
            // 转换回 DO 并保存
            blocklistDo = converter.toDataObject(blocklist);
        } else {
            // 创建新的实体
            IpBlocklist blocklist = new IpBlocklist();
            blocklist.setIpAddress(ipAddress);
            blocklist.setBlockReason(reason);
            blocklist.setBlockedBy(blockedBy);
            blocklist.setExpiresAt(expiresAt);
            blocklist.setBlockedAt(Instant.now());
            
            // 转换为 DO 并保存
            blocklistDo = converter.toDataObject(blocklist);
        }
        repository.save(blocklistDo);
        
        log.info("IP {} blocked: reason={}, expiresAt={}", ipAddress, reason, expiresAt);
    }

    @Override
    public void unblock(String ipAddress) {
        repository.findByIpAddress(ipAddress)
                .ifPresent(blocklist -> {
                    repository.delete(blocklist);
                    log.info("IP {} unblocked", ipAddress);
                });
    }
}

