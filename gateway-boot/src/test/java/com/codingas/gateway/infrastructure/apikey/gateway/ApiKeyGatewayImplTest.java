package com.codingas.gateway.infrastructure.apikey.gateway;

import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.infrastructure.security.database.GatewayApiKeyRepository;
import com.codingas.gateway.infrastructure.security.database.dataobject.GatewayApiKeyDo;
import com.codingas.gateway.infrastructure.security.ApiKeyGatewayImpl;
import com.codingas.gateway.infrastructure.security.database.dataobject.UserDo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ApiKeyGatewayImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyGatewayImpl 测试")
class ApiKeyGatewayImplTest {

    @Mock
    private GatewayApiKeyRepository repository;

    @InjectMocks
    private ApiKeyGatewayImpl gateway;

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存 API Key 成功")
        void save_validEntity_returnsSaved() {
            // given
            GatewayApiKey entity = createTestEntity();
            GatewayApiKeyDo savedDo = createTestDo();

            when(repository.save(any())).thenReturn(savedDo);

            // when
            GatewayApiKey result = gateway.save(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("找到 API Key 返回实体")
        void findById_existingId_returnsEntity() {
            // given
            GatewayApiKeyDo doEntity = createTestDo();
            when(repository.findById(1L)).thenReturn(Optional.of(doEntity));

            // when
            Optional<GatewayApiKey> result = gateway.findById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("未找到返回空")
        void findById_nonExistingId_returnsEmpty() {
            // given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<GatewayApiKey> result = gateway.findById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByKeyHash 方法测试")
    class FindByKeyHashTests {

        @Test
        @DisplayName("通过哈希找到 API Key")
        void findByKeyHash_existingHash_returnsEntity() {
            // given
            GatewayApiKeyDo doEntity = createTestDo();
            when(repository.findByKeyHash("hash123")).thenReturn(Optional.of(doEntity));

            // when
            GatewayApiKey result = gateway.findByKeyHash("hash123");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getKeyHash()).isEqualTo("hash123");
        }

        @Test
        @DisplayName("未找到返回 null")
        void findByKeyHash_nonExistingHash_returnsNull() {
            // given
            when(repository.findByKeyHash("unknown")).thenReturn(Optional.empty());

            // when
            GatewayApiKey result = gateway.findByKeyHash("unknown");

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findByUserId 方法测试")
    class FindByUserIdTests {

        @Test
        @DisplayName("找到用户的 API Key 列表")
        void findByUserId_existingUser_returnsList() {
            // given
            GatewayApiKeyDo doEntity = createTestDo();
            when(repository.findByUserId(1L)).thenReturn(List.of(doEntity));

            // when
            List<GatewayApiKey> result = gateway.findByUserId(1L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUser().getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("findAll 方法测试")
    class FindAllTests {

        @Test
        @DisplayName("返回所有 API Key")
        void findAll_returnsAll() {
            // given
            GatewayApiKeyDo doEntity1 = createTestDo();
            GatewayApiKeyDo doEntity2 = createTestDo();
            doEntity2.setId(2L);
            when(repository.findAll()).thenReturn(List.of(doEntity1, doEntity2));

            // when
            List<GatewayApiKey> result = gateway.findAll();

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
            when(repository.count()).thenReturn(10L);

            // when
            long result = gateway.count();

            // then
            assertThat(result).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除成功")
        void delete_existingEntity_deletes() {
            // given
            GatewayApiKey entity = createTestEntity();
            doNothing().when(repository).delete(any());

            // when
            gateway.delete(entity);

            // then
            verify(repository).delete(any());
        }
    }

    @Nested
    @DisplayName("updateLastUsed 方法测试")
    class UpdateLastUsedTests {

        @Test
        @DisplayName("更新最后使用时间")
        void updateLastUsed_existingId_updatesTime() {
            // given
            GatewayApiKeyDo doEntity = createTestDo();
            Instant now = Instant.now();
            when(repository.findById(1L)).thenReturn(Optional.of(doEntity));
            when(repository.save(any())).thenReturn(doEntity);

            // when
            gateway.updateLastUsed(1L, now);

            // then
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("findExpiringKeys 方法测试")
    class FindExpiringKeysTests {

        @Test
        @DisplayName("返回即将过期的 Key 列表")
        void findExpiringKeys_returnsPage() {
            // given
            GatewayApiKeyDo doEntity = createTestDo();
            Page<GatewayApiKeyDo> page = new PageImpl<>(List.of(doEntity));
            when(repository.findExpiringKeys(any(), any(), any())).thenReturn(page);

            // when
            Page<GatewayApiKey> result = gateway.findExpiringKeys(
                Instant.now(), Instant.now().plusSeconds(86400), PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    // Helper methods
    private GatewayApiKey createTestEntity() {
        GatewayApiKey entity = new GatewayApiKey();
        entity.setId(1L);
        entity.setKeyHash("hash123");
        entity.setName("Test Key");
        entity.setStatus(GatewayApiKey.ApiKeyStatus.ACTIVE);
        User user = new User();
        user.setId(1L);
        entity.setUser(user);
        return entity;
    }

    private GatewayApiKeyDo createTestDo() {
        GatewayApiKeyDo doEntity = new GatewayApiKeyDo();
        doEntity.setId(1L);
        doEntity.setKeyHash("hash123");
        doEntity.setName("Test Key");
        doEntity.setStatus(GatewayApiKeyDo.ApiKeyStatus.ACTIVE);
        UserDo userDo = new UserDo();
        userDo.setId(1L);
        userDo.setUsername("testuser");
        doEntity.setUser(userDo);
        return doEntity;
    }
}
