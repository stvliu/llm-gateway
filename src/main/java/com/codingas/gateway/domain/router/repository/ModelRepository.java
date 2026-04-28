package com.codingas.gateway.domain.router.repository;

import com.codingas.gateway.domain.router.entity.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {
    Optional<Model> findByModelCode(String modelCode);
    List<Model> findAllActive();
    List<Model> findByProviderId(Long providerId);
    boolean existsByModelCode(String modelCode);
}
