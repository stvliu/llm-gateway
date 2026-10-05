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

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 密码策略统一校验测试。
 *
 * <p>创建用户与修改密码使用同一密码策略（6-64 字符），
 * 边界值（5/6/64/65）逐一验证。</p>
 */
@DisplayName("密码策略统一校验（6-64 字符，创建与改密同策略）")
class PasswordPolicyValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    /** 取指定 DTO 字段上的约束违规消息集合 */
    private Set<String> passwordViolations(Object request) {
        return validator.validate(request).stream()
            .filter(v -> "password".equals(v.getPropertyPath().toString())
                || "newPassword".equals(v.getPropertyPath().toString()))
            .map(ConstraintViolation::getMessage)
            .collect(java.util.stream.Collectors.toSet());
    }

    private UserCreateRequest createRequest(String password) {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword(password);
        return request;
    }

    @Test
    @DisplayName("创建用户：5 位拒绝、6 位通过、64 位通过、65 位拒绝")
    void userCreate_passwordBounds() {
        assertThat(passwordViolations(createRequest("12345"))).isNotEmpty();
        assertThat(passwordViolations(createRequest("123456"))).isEmpty();
        assertThat(passwordViolations(createRequest("a".repeat(64)))).isEmpty();
        assertThat(passwordViolations(createRequest("a".repeat(65)))).isNotEmpty();
    }

    private ChangePasswordRequest changeRequest(String newPassword) {
        return new ChangePasswordRequest("current-pass", newPassword);
    }

    @Test
    @DisplayName("修改密码：5 位拒绝、6 位通过、64 位通过、65 位拒绝（与创建同策略）")
    void changePassword_passwordBounds() {
        assertThat(passwordViolations(changeRequest("12345"))).isNotEmpty();
        assertThat(passwordViolations(changeRequest("123456"))).isEmpty();
        assertThat(passwordViolations(changeRequest("a".repeat(64)))).isEmpty();
        assertThat(passwordViolations(changeRequest("a".repeat(65)))).isNotEmpty();
    }
}
