package com.codingas.gateway.core.infrastructure.gateway;

import com.codingas.gateway.core.domain.entity.IpBlocklist;
import com.codingas.gateway.core.domain.gateway.IpBlockGateway;
import com.codingas.gateway.core.repository.IpBlocklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * IP 黑名单网关实现
 *
 * <p>实现 IpBlockGateway 接口，使用 JPA 进行持久化。</p>
 */
@Component
@RequiredArgsConstructor
public class JpaIpBlockGateway implements IpBlockGateway {

    private final IpBlocklistRepository repository;

    @Override
    public IpBlocklist findByIpAddress(String ipAddress) {
        return repository.findByIpAddressAndExpiresAtAfter(ipAddress, Instant.now())
                .orElse(null);
    }

    @Override
    public boolean isBlocked(String ipAddress) {
        return repository.existsActiveBlockForIp(ipAddress, Instant.now());
    }

    @Override
    public IpBlocklist save(IpBlocklist ipBlocklist) {
        return repository.save(ipBlocklist);
    }

    @Override
    public void deleteByIpAddress(String ipAddress) {
        repository.findByIpAddress(ipAddress).ifPresent(repository::delete);
    }

    @Override
    public List<IpBlocklist> findAllActive() {
        return repository.findAll().stream()
                .filter(ip -> ip.getExpiresAt() == null || ip.getExpiresAt().isAfter(Instant.now()))
                .toList();
    }
}
