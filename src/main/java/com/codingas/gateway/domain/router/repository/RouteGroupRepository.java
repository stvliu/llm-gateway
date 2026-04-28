package com.codingas.gateway.domain.router.repository;

import com.codingas.gateway.domain.router.entity.RouteGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteGroupRepository extends JpaRepository<RouteGroup, Long> {
    Optional<RouteGroup> findByGroupCode(String groupCode);
    List<RouteGroup> findAllActive();
}
