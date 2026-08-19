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
package com.codingas.gateway.infrastructure.iam.gateway.encryption;

import cn.dev33.satoken.secure.SaSecureUtil;
import org.springframework.stereotype.Component;

/**
 * 密码编码器（IAM 域能力）。
 *
 * <p>基于 Sa-Token 的 SHA-256 进行密码哈希，供用户密码存储与校验使用。
 * 由 IAM 域提供，boot 组装层（含 init 数据装载）通过 Bean 注入复用。</p>
 */
@Component
public class PasswordEncoder {

    /**
     * 编码明文密码
     *
     * @param rawPassword 明文密码
     * @return 哈希后的密文
     */
    public String encode(CharSequence rawPassword) {
        return SaSecureUtil.sha256(rawPassword.toString());
    }

    /**
     * 校验明文密码与密文是否匹配
     *
     * @param rawPassword     明文密码
     * @param encodedPassword 已存储的密文
     * @return 是否匹配
     */
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }
}
