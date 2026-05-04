package com.codingas.gateway.infrastructure.alert.gateway;

import com.codingas.gateway.domain.security.entity.IpBlocklist;
import com.codingas.gateway.domain.security.gateway.IpBlockGateway;
import com.codingas.gateway.infrastructure.alert.gateway.database.IpBlocklistRepository;
import com.codingas.gateway.infrastructure.alert.gateway.database.dataobject.IpBlocklistDo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * IP 黑名单网关实现
 *
 * <p>实现 IpBlockGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class IpBlockGatewayImpl implements IpBlockGateway {

    private final IpBlocklistRepository repository;
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
            // 将 DO 转换为领域实体，执行领域逻辑
            IpBlocklist blocklist = converter.toDomain(existingDo.get());
            
            // 在领域实体上设置属性（未来可以添加领域验证逻辑）
            blocklist.setBlockReason(reason);
            blocklist.setBlockedBy(blockedBy);
            blocklist.setExpiresAt(expiresAt);
            
            // 转换回 DO 并保存
            blocklistDo = converter.toDataObject(blocklist);
        } else {
            // 创建新的领域实体
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

