/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.iam.encryption;

import cn.dev33.satoken.secure.SaSecureUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码编码器（IAM 域能力）。
 *
 * <p>基于 BCrypt 进行密码哈希存储（自带随机盐，成本因子默认 10，输入上限 72 字节）。
 * 校验时兼容存量无盐 SHA-256 哈希（64 位 hex），配合 {@link #needsUpgrade(String)}
 * 支持"登录成功后透明升级为 BCrypt"，存量用户无需重置密码。</p>
 * <p>由 IAM 域提供，boot 组装层（含 init 数据装载）通过 Bean 注入复用。</p>
 */
@Component
public class PasswordEncoder {

    /** BCrypt 输入字节上限（UTF-8 编码后），超出将抛出 IllegalArgumentException */
    public static final int MAX_PASSWORD_BYTES = 72;

    /** BCrypt 编码器（线程安全，可复用；默认强度 10） */
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    /** 时序对齐哑哈希（BCrypt cost-10）：认证路径在用户不存在时消耗等量计算，消除用户名枚举时序差 */
    public static final String TIMING_EQUALIZER_HASH = new BCryptPasswordEncoder().encode("timing-equalizer");

    /**
     * 编码明文密码（BCrypt，自带随机盐，同密码每次编码结果不同）
     *
     * @param rawPassword 明文密码
     * @return BCrypt 哈希（$2 前缀、60 字符）
     */
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword.toString());
    }

    /**
     * 校验明文密码与密文是否匹配。
     *
     * <p>按存储格式分派：BCrypt 格式走 BCrypt 校验；存量无盐 SHA-256（64 位 hex）
     * 走旧逻辑比对，保证透明升级期老用户可正常登录。非法/空格式一律返回 false，
     * 不因哈希内容损坏而中断认证流程。</p>
     *
     * @param rawPassword     明文密码
     * @param encodedPassword 已存储的密文
     * @return 是否匹配
     */
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        if (isBcryptHash(encodedPassword)) {
            try {
                return bcrypt.matches(rawPassword.toString(), encodedPassword);
            } catch (IllegalArgumentException e) {
                // 盐串损坏等非法 BCrypt 内容：视为不匹配，不向上抛出
                return false;
            }
        }
        return SaSecureUtil.sha256(rawPassword.toString()).equals(encodedPassword);
    }

    /**
     * 判断存量哈希是否需要升级为 BCrypt。
     *
     * <p>调用方在登录校验成功后依据此结果做透明重哈希写回（仅非 BCrypt 格式需要升级）。
     * 非法格式同样判定为需升级，下次登录即被 BCrypt 哈希覆盖修复。</p>
     *
     * @param encodedPassword 已存储的密文
     * @return true 表示需要升级（存量 SHA-256 或非法格式）
     */
    public boolean needsUpgrade(String encodedPassword) {
        return !isBcryptHash(encodedPassword);
    }

    /**
     * BCrypt 哈希格式判定：$2a$/$2b$/$2y$ 前缀 + 60 字符。
     * 存量 SHA-256 为 64 位 hex，长度即可区分，前缀做双保险。
     */
    private boolean isBcryptHash(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.length() != 60) {
            return false;
        }
        String prefix = encodedPassword.substring(0, 3);
        return "$2a".equals(prefix) || "$2b".equals(prefix) || "$2y".equals(prefix);
    }
}
