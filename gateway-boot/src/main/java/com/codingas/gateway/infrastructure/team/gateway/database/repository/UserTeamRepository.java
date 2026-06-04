package com.codingas.gateway.infrastructure.team.gateway.database.repository;

import com.codingas.gateway.infrastructure.team.gateway.database.dataobject.UserTeamDo;
import com.codingas.gateway.infrastructure.team.gateway.database.dataobject.UserTeamDo.UserTeamId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户-团队关联 Repository
 */
@Repository
public interface UserTeamRepository extends JpaRepository<UserTeamDo, UserTeamId> {

    List<UserTeamDo> findByUserId(Long userId);

    List<UserTeamDo> findByTeamId(Long teamId);

    long countByTeamId(Long teamId);

    @Query("SELECT COUNT(ut) > 0 FROM UserTeamDo ut WHERE ut.userId = :userId AND ut.teamId = :teamId")
    boolean isMember(@Param("userId") Long userId, @Param("teamId") Long teamId);

    @Modifying
    @Query("DELETE FROM UserTeamDo ut WHERE ut.userId = :userId AND ut.teamId = :teamId")
    void deleteByUserIdAndTeamId(@Param("userId") Long userId, @Param("teamId") Long teamId);

    @Modifying
    @Query("UPDATE UserTeamDo ut SET ut.role = :role WHERE ut.userId = :userId AND ut.teamId = :teamId")
    void updateRole(@Param("userId") Long userId, @Param("teamId") Long teamId, @Param("role") String role);
}
