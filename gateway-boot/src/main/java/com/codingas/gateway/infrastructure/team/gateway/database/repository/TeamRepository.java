package com.codingas.gateway.infrastructure.team.gateway.database.repository;

import com.codingas.gateway.infrastructure.team.gateway.database.dataobject.TeamDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 团队 Repository
 */
@Repository
public interface TeamRepository extends JpaRepository<TeamDo, Long> {

    @Query("SELECT t FROM TeamDo t WHERE t.state = 'active'")
    List<TeamDo> findAllActive();

    boolean existsByName(String name);
}
