package com.codingas.gateway.infrastructure.threat.gateway.database.repository;

import com.codingas.gateway.infrastructure.threat.gateway.database.dataobject.IpBlocklistDo;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IpBlocklistRepository extends JpaRepository<IpBlocklistDo, Long> {
    Optional<IpBlocklistDo> findByIpAddress(String ipAddress);

    boolean existsByIpAddress(String ipAddress);

    @NotNull
    List<IpBlocklistDo> findAll();

    @NotNull
    Page<IpBlocklistDo> findAll(@NotNull Pageable pageable);

    @NotNull IpBlocklistDo save(@NotNull IpBlocklistDo blocklist);

    void delete(@NotNull IpBlocklistDo blocklist);
}
