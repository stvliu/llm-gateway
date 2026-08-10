/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.config;

import cn.dev33.satoken.secure.SaSecureUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 密码编码器
 *
 * <p>使用 Sa-Token 的 SHA-256 进行密码哈希。</p>
 */
@Configuration
public class SecurityConfig {

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder();
    }

    /**
     * 密码编码器（基于 Sa-Token SaDigestUtil）
     */
    public static class PasswordEncoder {

        public String encode(CharSequence rawPassword) {
            return SaSecureUtil.sha256(rawPassword.toString());
        }

        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }
    }
}
