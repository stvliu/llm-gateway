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
package com.codingas.gateway.web.api.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 密码 UTF-8 字节级长度校验。
 *
 * <p>Jakarta {@code @Size} 校验的是字符数（UTF-16 code unit），而 BCrypt 的
 * 输入上限按 UTF-8 字节计（72 字节，一个汉字占 3 字节）——仅靠字符数校验
 * 无法拦住多字节超限密码。本注解按字节校验，让超限请求在 Bean Validation
 * 阶段即被 400 + 中文文案拦截，而非落入服务层的库异常。</p>
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordByteSizeValidator.class)
public @interface PasswordByteSize {

    /**
     * UTF-8 编码字节上限
     *
     * @return 字节数上限，默认 72（BCrypt 输入上限）
     */
    int max() default 72;

    /**
     * 校验失败提示文案
     *
     * @return 消息模板
     */
    String message() default "密码过长（UTF-8 编码不得超过 {max} 字节，一个汉字占 3 字节）";

    /**
     * 分组
     *
     * @return 校验分组
     */
    Class<?>[] groups() default {};

    /**
     * 负载
     *
     * @return Payload
     */
    Class<? extends Payload>[] payload() default {};
}
