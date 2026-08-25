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
package com.codingas.gateway.iamdata.application;

import com.codingas.gateway.iam.application.Application;
import com.codingas.gateway.iam.application.ApplicationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
/**
 * JpaApplicationRepository 单元测试
 *
 * <p>验证 Application 聚合根的持久化网关行为：findById/findByCode/findAll/save
 * 的 DO↔Entity 转换与委派逻辑。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaApplicationRepository 测试")
class JpaApplicationRepositoryTest {

    @Mock
    private ApplicationJpaRepository repository;

    @InjectMocks
    private JpaApplicationRepository gateway;

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("存在时返回 Application 实体")
        void findById_existingId_returnsEntity() {
            ApplicationDo doEntity = createTestDo();
            when(repository.findById(1L)).thenReturn(Optional.of(doEntity));

            Application result = gateway.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCode()).isEqualTo("APP-001");
            assertThat(result.getState()).isEqualTo(ApplicationState.ACTIVE);
        }

        @Test
        @DisplayName("不存在时返回 null")
        void findById_nonExistingId_returnsNull() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            Application result = gateway.findById(999L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findByCode 方法测试")
    class FindByCodeTests {

        @Test
        @DisplayName("按编码查找命中返回实体")
        void findByCode_existingCode_returnsEntity() {
            ApplicationDo doEntity = createTestDo();
            when(repository.findByCode("APP-001")).thenReturn(Optional.of(doEntity));

            Application result = gateway.findByCode("APP-001");

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("APP-001");
        }

        @Test
        @DisplayName("按编码查找未命中返回 null")
        void findByCode_nonExistingCode_returnsNull() {
            when(repository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

            Application result = gateway.findByCode("UNKNOWN");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findAll 方法测试")
    class FindAllTests {

        @Test
        @DisplayName("返回全部 Application")
        void findAll_returnsAll() {
            ApplicationDo d1 = createTestDo();
            ApplicationDo d2 = createTestDo();
            d2.setId(2L);
            d2.setCode("APP-002");
            when(repository.findAll()).thenReturn(List.of(d1, d2));

            List<Application> result = gateway.findAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Application::getCode)
                    .containsExactly("APP-001", "APP-002");
        }
    }

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存 Application 并回写转换结果")
        void save_validEntity_returnsSaved() {
            Application entity = createTestEntity();
            ApplicationDo savedDo = createTestDo();
            when(repository.save(any())).thenReturn(savedDo);

            Application result = gateway.save(entity);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            ArgumentCaptor<ApplicationDo> captor = ArgumentCaptor.forClass(ApplicationDo.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCode()).isEqualTo("APP-001");
            assertThat(captor.getValue().getState()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("deleteById 方法测试")
    class DeleteByIdTests {

        @Test
        @DisplayName("按主键删除应用，委派至 Repository")
        void deleteById_delegatesToRepository() {
            gateway.deleteById(1L);

            verify(repository).deleteById(1L);
        }
    }

    // ===== Helper methods =====

    private ApplicationDo createTestDo() {
        ApplicationDo d = new ApplicationDo();
        d.setId(1L);
        d.setCode("APP-001");
        d.setName("测试应用");
        d.setDescription("描述");
        d.setState("ACTIVE");
        return d;
    }

    private Application createTestEntity() {
        Application entity = new Application();
        entity.setId(1L);
        entity.setCode("APP-001");
        entity.setName("测试应用");
        entity.setDescription("描述");
        entity.setState(ApplicationState.ACTIVE);
        return entity;
    }
}
