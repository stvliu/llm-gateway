package com.codingas.gateway.application.init;

import com.codingas.gateway.domain.iam.gateway.UserGateway;
import com.codingas.gateway.domain.team.gateway.TeamGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 demo-data-enabled=false 时 DataInitializer 的行为。
 *
 * <p>使用独立的 profile 确保不启用演示数据。</p>
 */
@SpringBootTest
@ActiveProfiles({"test"})
@Transactional
class DataInitializerDisabledTest {

    @Autowired
    private UserGateway userGateway;

    @Autowired
    private TeamGateway teamGateway;

    @Autowired
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("admin 应始终创建")
    void adminShouldBeCreated() {
        dataInitializer.run();
        assertTrue(userGateway.findByUsername("admin").isPresent());
    }

    @Test
    @DisplayName("不应创建演示用户")
    void shouldNotCreateDemoUsers() {
        dataInitializer.run();
        assertTrue(userGateway.findByUsername("test1").isEmpty());
    }

    @Test
    @DisplayName("不应创建演示团队")
    void shouldNotCreateDemoTeams() {
        dataInitializer.run();
        assertFalse(teamGateway.existsByName("dev"));
    }
}
