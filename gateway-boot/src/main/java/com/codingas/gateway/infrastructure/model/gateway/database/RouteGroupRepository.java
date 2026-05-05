package com.codingas.gateway.infrastructure.model.gateway.database;

import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.RouteGroupDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteGroupRepository extends JpaRepository<RouteGroupDo, Long> {

    /**
     * 查找所有启用的路由分组
     * <p>JPA 方法命名约定：findByEnabledTrue 表示 enabled = true</p>
     */
    List<RouteGroupDo> findByEnabledTrue();
}
