package com.codingas.gateway.core.domain.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Team 实体单元测试
 */
class TeamTest {

    @Test
    void shouldCreateTeamWithDefaultStatus() {
        Team team = new Team();
        team.setTeamCode("test-team");
        team.setTeamName("Test Team");

        assertThat(team.getTeamCode()).isEqualTo("test-team");
        assertThat(team.getTeamName()).isEqualTo("Test Team");
        assertThat(team.getStatus()).isEqualTo(Team.TeamStatus.ACTIVE);
    }

    @Test
    void shouldSetTeamStatus() {
        Team team = new Team();
        team.setStatus(Team.TeamStatus.SUSPENDED);

        assertThat(team.getStatus()).isEqualTo(Team.TeamStatus.SUSPENDED);
    }

    @Test
    void shouldNotBeDeletedWhenCreated() {
        Team team = new Team();

        assertThat(team.isDeleted()).isFalse();
    }

    @Test
    void shouldBeDeletedWhenDeletedAtSet() {
        Team team = new Team();
        team.setDeletedAt(java.time.Instant.now());

        assertThat(team.isDeleted()).isTrue();
    }
}
