package com.codingas.gateway.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA 审计配置
 *
 * <p>启用 JPA 审计功能，自动填充 created_by、created_at、updated_by、updated_at 字段。</p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        // 默认使用系统用户 ID (0 表示系统操作)
        return () -> Optional.of(0L);
    }
}
