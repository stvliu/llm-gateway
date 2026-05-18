package com.codingas.gateway.domain.team.gateway;

import com.codingas.gateway.domain.team.entity.Team;

import java.util.List;
import java.util.Optional;

/**
 * 团队 Gateway 接口
 */
public interface TeamGateway {

    Team save(Team team);

    Optional<Team> findById(Long id);

    List<Team> findAllActive();

    void deleteById(Long id);

    boolean existsByName(String name);
}
