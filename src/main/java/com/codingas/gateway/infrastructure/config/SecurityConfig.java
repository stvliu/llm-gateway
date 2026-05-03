package com.codingas.gateway.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全配置
 *
 * <p>提供密码编码器等安全相关的 Bean。</p>
 */
@Configuration
public class SecurityConfig {

    /**
     * 密码编码器
     *
     * <p>使用 BCrypt 算法进行密码哈希。</p>
     *
     * @return PasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
