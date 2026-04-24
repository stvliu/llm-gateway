package com.codingas.gateway.core.repository;

import com.codingas.gateway.core.domain.entity.IpBlocklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface IpBlocklistRepository extends JpaRepository<IpBlocklist, Long> {

    Optional<IpBlocklist> findByIpAddress(String ipAddress);

    Optional<IpBlocklist> findByIpAddressAndExpiresAtAfter(String ipAddress, Instant now);

    List<IpBlocklist> findByBlockedBy(Long blockedBy);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM IpBlocklist i " +
           "WHERE i.expiresAt IS NULL OR i.expiresAt > :now " +
           "AND (i.ipAddress = :ipAddress " +
           "OR (i.ipRangeStart IS NOT NULL AND i.ipRangeEnd IS NOT NULL " +
           "    AND :ipAddress >= i.ipRangeStart AND :ipAddress <= i.ipRangeEnd))")
    boolean existsActiveBlockForIp(@Param("ipAddress") String ipAddress, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM IpBlocklist i WHERE i.expiresAt IS NOT NULL AND i.expiresAt < :now")
    long deleteByExpiresAtBefore(@Param("now") Instant now);
}