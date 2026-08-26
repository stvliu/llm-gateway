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
package com.codingas.gateway.usagedata.tokenlimit;

import com.codingas.gateway.usage.enums.ExceededAction;
import com.codingas.gateway.usage.enums.PeriodType;
import com.codingas.gateway.usage.tokenlimit.TokenLimit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * JpaTokenLimitRepository 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaTokenLimitRepository 测试")
class JpaTokenLimitRepositoryTest {

    @Mock
    private TokenLimitJpaRepository tokenLimitRepository;

    @InjectMocks
    private JpaTokenLimitRepository gateway;

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存 TokenLimit 成功")
        void save_validEntity_returnsSaved() {
            // given
            TokenLimit entity = createTestEntity();
            TokenLimitDo savedDo = createTestDo();

            when(tokenLimitRepository.save(any())).thenReturn(savedDo);

            // when
            TokenLimit result = gateway.save(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(tokenLimitRepository).save(any());
        }
    }

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("找到 TokenLimit 返回实体")
        void findById_existingId_returnsEntity() {
            // given
            TokenLimitDo doEntity = createTestDo();
            when(tokenLimitRepository.findById(1L)).thenReturn(Optional.of(doEntity));

            // when
            Optional<TokenLimit> result = gateway.findById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("未找到返回空")
        void findById_nonExistingId_returnsEmpty() {
            // given
            when(tokenLimitRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<TokenLimit> result = gateway.findById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll 方法测试")
    class FindAllTests {

        @Test
        @DisplayName("返回所有 TokenLimit")
        void findAll_returnsAll() {
            // given
            TokenLimitDo doEntity1 = createTestDo();
            TokenLimitDo doEntity2 = createTestDo();
            doEntity2.setId(2L);
            when(tokenLimitRepository.findAll()).thenReturn(List.of(doEntity1, doEntity2));

            // when
            List<TokenLimit> result = gateway.findAll();

            // then
            assertThat(result).hasSize(2);
        }
    }

    // Helper methods
    private TokenLimit createTestEntity() {
        TokenLimit entity = new TokenLimit();
        entity.setId(1L);
        entity.setMaxTokens(BigDecimal.valueOf(100000));
        entity.setUsedTokens(BigDecimal.ZERO);
        entity.setLimitType(TokenLimit.LimitType.USER_CUSTOM);
        entity.setPeriodType(PeriodType.MONTHLY);
        entity.setExceededAction(ExceededAction.REJECT);
        entity.setState(TokenLimit.TokenLimitState.ACTIVE);
        return entity;
    }

    private TokenLimitDo createTestDo() {
        TokenLimitDo doEntity = new TokenLimitDo();
        doEntity.setId(1L);
        doEntity.setUserId(1L);
        doEntity.setMaxTokens(BigDecimal.valueOf(100000));
        doEntity.setUsedTokens(BigDecimal.ZERO);
        doEntity.setLimitType(TokenLimitDo.LimitType.USER_CUSTOM);
        doEntity.setPeriodType(PeriodType.MONTHLY);
        doEntity.setExceededAction(ExceededAction.REJECT);
        doEntity.setState(TokenLimitDo.TokenLimitStatus.ACTIVE);
        doEntity.setCreatedAt(Instant.now());
        doEntity.setUpdatedAt(Instant.now());
        return doEntity;
    }
}
