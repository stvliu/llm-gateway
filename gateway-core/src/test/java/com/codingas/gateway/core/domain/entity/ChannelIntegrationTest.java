package com.codingas.gateway.core.domain.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Channel 实体集成测试
 *
 * <p>使用 @DataJpaTest 进行 JPA 层面测试。</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ChannelIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldPersistChannel() {
        // 创建 Provider
        Provider provider = new Provider();
        provider.setProviderCode("openai");
        provider.setProviderName("OpenAI");
        entityManager.persist(provider);

        // 创建 Team
        Team team = new Team();
        team.setTeamCode("test-team");
        team.setTeamName("Test Team");
        entityManager.persist(team);

        // 创建 Channel
        Channel channel = new Channel();
        channel.setChannelCode("ch-openai-1");
        channel.setChannelName("OpenAI Channel 1");
        channel.setTeamId(team.getId());
        channel.setProviderId(provider.getId());
        channel.setBaseUrl("https://api.openai.com/v1");
        channel.setModels("[\"gpt-4o\", \"gpt-3.5-turbo\"]");
        entityManager.persist(channel);
        entityManager.flush();

        // 验证
        Channel found = entityManager.find(Channel.class, channel.getId());
        assertThat(found.getChannelCode()).isEqualTo("ch-openai-1");
        assertThat(found.getChannelName()).isEqualTo("OpenAI Channel 1");
        assertThat(found.getStatus()).isEqualTo(Channel.ChannelStatus.ACTIVE);
        assertThat(found.getPriority()).isEqualTo(100);
    }

    @Test
    void shouldFindChannelByCode() {
        // 创建 Provider
        Provider provider = new Provider();
        provider.setProviderCode("anthropic");
        provider.setProviderName("Anthropic");
        entityManager.persist(provider);

        // 创建 Channel
        Channel channel = new Channel();
        channel.setChannelCode("ch-anthropic-1");
        channel.setChannelName("Anthropic Channel");
        channel.setProviderId(provider.getId());
        channel.setTeamId(1L);
        entityManager.persist(channel);
        entityManager.flush();

        // 验证
        assertThat(channel.getId()).isNotNull();
    }
}
