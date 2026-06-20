package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.domain.resilience.gateway.SessionAffinityStore;
import com.codingas.gateway.infrastructure.resilience.affinity.InMemorySessionAffinityStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 会话亲和配置
 *
 * <p>管理会话亲和存储的启用/禁用和 TTL 配置。
 * 通过 {@code @ConditionalOnProperty} 控制选择实现：
 * <ul>
 *   <li>生产环境（Redis 已启用且 {@code session.affinity.enabled=true}）：走
 *       {@code RedisSessionAffinityStore}（需 {@code StringRedisTemplate} bean）</li>
 *   <li>开发/测试环境（Redis 未启用）：走 {@code InMemorySessionAffinityStore}</li>
 * </ul>
 * </p>
 *
 * <p>配置项前缀：{@code session.affinity}</p>
 */
@Configuration
@ConfigurationProperties(prefix = "session.affinity")
public class SessionAffinityConfig {

    /** 是否启用会话亲和（默认启用） */
    private boolean enabled = true;

    /** TTL（分钟），默认 30 分钟 */
    private int ttlMinutes = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(int ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }

    /**
     * 内存会话亲和存储（开发/测试环境兜底）
     *
     * <p>当 {@code RedisSessionAffinityStore} 因缺少 {@code StringRedisTemplate}
     * 而无法装配时，使用 InMemory 实现作为后备。</p>
     *
     * @return InMemorySessionAffinityStore 实例
     */
    @Bean
    @ConditionalOnMissingBean(SessionAffinityStore.class)
    public InMemorySessionAffinityStore inMemorySessionAffinityStore() {
        return new InMemorySessionAffinityStore(ttlMinutes);
    }
}
