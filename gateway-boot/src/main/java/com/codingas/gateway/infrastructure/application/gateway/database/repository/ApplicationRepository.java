package com.codingas.gateway.infrastructure.application.gateway.database.repository;

import com.codingas.gateway.infrastructure.application.gateway.database.dataobject.ApplicationDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 应用 JPA Repository
 */
public interface ApplicationRepository extends JpaRepository<ApplicationDo, Long> {

    /**
     * 按应用编码查找
     *
     * @param code 应用编码
     * @return 命中的 DO；不存在时返回空
     */
    Optional<ApplicationDo> findByCode(String code);
}
