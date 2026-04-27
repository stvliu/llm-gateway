package com.codingas.gateway.infrastructure.gateway.security;

import com.codingas.gateway.domain.security.entity.IpBlocklist;
import com.codingas.gateway.domain.security.gateway.IpBlockGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * IP 黑名单网关实现
 *
 * <p>实现 IpBlockGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaIpBlockGateway implements IpBlockGateway {

    private final IpBlocklistRepository repository;

    @Override
    public boolean isBlocked(String ipAddress) {
        return repository.findAll().stream()
                .anyMatch(ip -> ip.getIpAddress().equals(ipAddress)
                        && (ip.getExpiresAt() == null || ip.getExpiresAt().isAfter(Instant.now())));
    }

    @Override
    public void block(String ipAddress, String reason, Long blockedBy, Instant expiresAt) {
        Optional<IpBlocklist> existing = repository.findAll().stream()
                .filter(ip -> ip.getIpAddress().equals(ipAddress))
                .findFirst();

        IpBlocklist blocklist;
        if (existing.isPresent()) {
            blocklist = existing.get();
            blocklist.setBlockReason(reason);
            blocklist.setBlockedBy(blockedBy);
            blocklist.setExpiresAt(expiresAt);
        } else {
            blocklist = new IpBlocklist();
            blocklist.setIpAddress(ipAddress);
            blocklist.setBlockReason(reason);
            blocklist.setBlockedBy(blockedBy);
            blocklist.setExpiresAt(expiresAt);
        }
        repository.save(blocklist);
    }

    @Override
    public void unblock(String ipAddress) {
        repository.findAll().stream()
                .filter(ip -> ip.getIpAddress().equals(ipAddress))
                .findFirst()
                .ifPresent(repository::delete);
    }
}

/**
 * IP 黑名单仓储接口
 */
interface IpBlocklistRepository {
    List<IpBlocklist> findAll();
    IpBlocklist save(IpBlocklist blocklist);
    void delete(IpBlocklist blocklist);
}
