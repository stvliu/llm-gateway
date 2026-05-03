package com.codingas.gateway.infrastructure.model.gateway.database;

import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.RouteGroupDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteGroupRepository extends JpaRepository<RouteGroupDo, Long> {
    Optional<RouteGroupDo> findByGroupCode(String groupCode);
    List<RouteGroupDo> findAllActive();
}
