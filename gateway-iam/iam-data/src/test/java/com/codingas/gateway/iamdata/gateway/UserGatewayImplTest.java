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
package com.codingas.gateway.iamdata.gateway;

import com.codingas.gateway.iam.user.UserState;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.iamdata.gateway.UserGatewayImpl;
import com.codingas.gateway.iamdata.repository.UserRepository;
import com.codingas.gateway.iamdata.dataobject.UserDo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserGatewayImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserGatewayImpl 测试")
class UserGatewayImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserGatewayImpl gateway;

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存 User 成功")
        void save_validEntity_returnsSaved() {
            // given
            User entity = createTestEntity();
            UserDo savedDo = createTestDo();

            when(userRepository.save(any())).thenReturn(savedDo);

            // when
            User result = gateway.save(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(userRepository).save(any());
        }
    }

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("找到 User 返回实体")
        void findById_existingId_returnsEntity() {
            // given
            UserDo doEntity = createTestDo();
            when(userRepository.findById(1L)).thenReturn(Optional.of(doEntity));

            // when
            Optional<User> result = gateway.findById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("未找到返回空")
        void findById_nonExistingId_returnsEmpty() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<User> result = gateway.findById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEmail 方法测试")
    class FindByEmailTests {

        @Test
        @DisplayName("通过邮箱找到 User")
        void findByEmail_existingEmail_returnsEntity() {
            // given
            UserDo doEntity = createTestDo();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(doEntity));

            // when
            Optional<User> result = gateway.findByEmail("test@example.com");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("findAll 方法测试")
    class FindAllTests {

        @Test
        @DisplayName("返回所有 User")
        void findAll_returnsAll() {
            // given
            UserDo doEntity1 = createTestDo();
            UserDo doEntity2 = createTestDo();
            doEntity2.setId(2L);
            when(userRepository.findAll()).thenReturn(List.of(doEntity1, doEntity2));

            // when
            List<User> result = gateway.findAll();

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("count 方法测试")
    class CountTests {

        @Test
        @DisplayName("返回总数")
        void count_returnsCount() {
            // given
            when(userRepository.count()).thenReturn(100L);

            // when
            long result = gateway.count();

            // then
            assertThat(result).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除成功")
        void delete_existingEntity_deletes() {
            // given
            User entity = createTestEntity();
            doNothing().when(userRepository).delete(any());

            // when
            gateway.delete(entity);

            // then
            verify(userRepository).delete(any());
        }
    }

    @Nested
    @DisplayName("existsByEmail 方法测试")
    class ExistsByEmailTests {

        @Test
        @DisplayName("邮箱存在返回 true")
        void existsByEmail_existingEmail_returnsTrue() {
            // given
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // when
            boolean result = gateway.existsByEmail("test@example.com");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("邮箱不存在返回 false")
        void existsByEmail_nonExistingEmail_returnsFalse() {
            // given
            when(userRepository.existsByEmail("unknown@example.com")).thenReturn(false);

            // when
            boolean result = gateway.existsByEmail("unknown@example.com");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByUsername 方法测试")
    class ExistsByUsernameTests {

        @Test
        @DisplayName("用户名存在返回 true")
        void existsByUsername_existingUsername_returnsTrue() {
            // given
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            // when
            boolean result = gateway.existsByUsername("testuser");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("用户名不存在返回 false")
        void existsByUsername_nonExistingUsername_returnsFalse() {
            // given
            when(userRepository.existsByUsername("unknown")).thenReturn(false);

            // when
            boolean result = gateway.existsByUsername("unknown");

            // then
            assertThat(result).isFalse();
        }
    }

    // Helper methods
    private User createTestEntity() {
        User entity = new User();
        entity.setId(1L);
        entity.setUsername("testuser");
        entity.setEmail("test@example.com");
        entity.setPasswordHash("hashed_password");
        entity.setPhone("13800138000");
        entity.setState(UserState.ACTIVE);
        entity.setEmailVerified(true);
        return entity;
    }

    private UserDo createTestDo() {
        UserDo doEntity = new UserDo();
        doEntity.setId(1L);
        doEntity.setUsername("testuser");
        doEntity.setEmail("test@example.com");
        doEntity.setPasswordHash("hashed_password");
        doEntity.setPhone("13800138000");
        doEntity.setState(UserState.ACTIVE);
        doEntity.setEmailVerified(true);
        doEntity.setCreatedAt(Instant.now());
        doEntity.setUpdatedAt(Instant.now());
        return doEntity;
    }
}
