package com.codingas.gateway.infrastructure.model.gateway.database;

import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProviderRepository extends JpaRepository<ProviderDo, Long> {
    List<ProviderDo> findByStatus(ProviderDo.ProviderStatus status);

    /**
     * 获取最大版本号
     *
     * @return 最大版本号，无数据返回 null
     */
    @Query("SELECT MAX(p.version) FROM ProviderDo p")
    Long findMaxVersion();
}
