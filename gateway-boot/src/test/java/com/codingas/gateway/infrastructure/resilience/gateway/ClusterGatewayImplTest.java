package com.codingas.gateway.infrastructure.resilience.gateway;

import com.codingas.gateway.domain.resilience.entity.Cluster;
import com.codingas.gateway.infrastructure.resilience.gateway.database.dataobject.ClusterDo;
import com.codingas.gateway.infrastructure.resilience.gateway.database.repository.ClusterRepository;
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
 * ClusterGatewayImpl 单元测试
 *
 * <p>验证 Cluster 故障域实体的持久化网关行为：findById/findByCode/findAll/save
 * 的 DO↔Entity 转换（含 description 透传）与委派逻辑。</p>
 *
 * <p>Task 6 变更：Cluster 语义改造为「跨供应商故障独立性分组」并瘦身字段，
 * 删除 region/priority/healthStatus，保留 code/name/providerId + 新增 description。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClusterGatewayImpl 测试")
class ClusterGatewayImplTest {

    @Mock
    private ClusterRepository repository;

    @InjectMocks
    private ClusterGatewayImpl gateway;

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("存在时返回 Cluster 实体（含 description 透传）")
        void findById_existingId_returnsEntity() {
            ClusterDo doEntity = createTestDo();
            when(repository.findById(1L)).thenReturn(Optional.of(doEntity));

            Cluster result = gateway.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCode()).isEqualTo("openai-us");
            assertThat(result.getName()).isEqualTo("OpenAI 美东故障域");
            assertThat(result.getProviderId()).isEqualTo(10L);
            assertThat(result.getDescription()).isEqualTo("OpenAI 美东共因特征说明");
        }

        @Test
        @DisplayName("不存在时返回 null")
        void findById_nonExistingId_returnsNull() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            Cluster result = gateway.findById(999L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findByCode 方法测试")
    class FindByCodeTests {

        @Test
        @DisplayName("按编码查找命中返回实体")
        void findByCode_existingCode_returnsEntity() {
            ClusterDo doEntity = createTestDo();
            when(repository.findByCode("openai-us")).thenReturn(Optional.of(doEntity));

            Cluster result = gateway.findByCode("openai-us");

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("openai-us");
            assertThat(result.getDescription()).isEqualTo("OpenAI 美东共因特征说明");
        }

        @Test
        @DisplayName("按编码查找未命中返回 null")
        void findByCode_nonExistingCode_returnsNull() {
            when(repository.findByCode("unknown")).thenReturn(Optional.empty());

            Cluster result = gateway.findByCode("unknown");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findAll 方法测试")
    class FindAllTests {

        @Test
        @DisplayName("返回全部 Cluster（含 description 透传）")
        void findAll_returnsAll() {
            ClusterDo d1 = createTestDo();
            ClusterDo d2 = createTestDo();
            d2.setId(2L);
            d2.setCode("claude-bedrock");
            d2.setName("Claude Bedrock 故障域");
            d2.setDescription("Claude Bedrock 跨供应商共因特征说明");
            when(repository.findAll()).thenReturn(List.of(d1, d2));

            List<Cluster> result = gateway.findAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Cluster::getCode)
                    .containsExactly("openai-us", "claude-bedrock");
            assertThat(result).extracting(Cluster::getDescription)
                    .containsExactly("OpenAI 美东共因特征说明", "Claude Bedrock 跨供应商共因特征说明");
        }
    }

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存 Cluster 并回写转换结果（description 透传）")
        void save_validEntity_returnsSaved() {
            Cluster entity = createTestEntity();
            ClusterDo savedDo = createTestDo();
            when(repository.save(any())).thenReturn(savedDo);

            Cluster result = gateway.save(entity);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getDescription()).isEqualTo("OpenAI 美东共因特征说明");
            ArgumentCaptor<ClusterDo> captor = ArgumentCaptor.forClass(ClusterDo.class);
            verify(repository).save(captor.capture());
            ClusterDo captured = captor.getValue();
            assertThat(captured.getCode()).isEqualTo("openai-us");
            assertThat(captured.getName()).isEqualTo("OpenAI 美东故障域");
            assertThat(captured.getProviderId()).isEqualTo(10L);
            assertThat(captured.getDescription()).isEqualTo("OpenAI 美东共因特征说明");
        }

        @Test
        @DisplayName("save 透传审计字段 createdBy/updatedBy")
        void save_preservesAuditFields() {
            Cluster entity = createTestEntity();
            entity.setCreatedBy(7L);
            entity.setUpdatedBy(7L);
            ClusterDo savedDo = createTestDo();
            when(repository.save(any())).thenReturn(savedDo);

            gateway.save(entity);

            ArgumentCaptor<ClusterDo> captor = ArgumentCaptor.forClass(ClusterDo.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCreatedBy()).isEqualTo(7L);
            assertThat(captor.getValue().getUpdatedBy()).isEqualTo(7L);
        }

        @Test
        @DisplayName("description 为 null 时 DO 与实体均透传 null")
        void save_nullDescription_preservesNull() {
            Cluster entity = createTestEntity();
            entity.setDescription(null);
            ClusterDo savedDo = createTestDo();
            savedDo.setDescription(null);
            when(repository.save(any())).thenReturn(savedDo);

            Cluster result = gateway.save(entity);

            ArgumentCaptor<ClusterDo> captor = ArgumentCaptor.forClass(ClusterDo.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getDescription()).isNull();
            assertThat(result.getDescription()).isNull();
        }
    }

    // ===== Helper methods =====

    private ClusterDo createTestDo() {
        ClusterDo d = new ClusterDo();
        d.setId(1L);
        d.setCode("openai-us");
        d.setName("OpenAI 美东故障域");
        d.setProviderId(10L);
        d.setDescription("OpenAI 美东共因特征说明");
        return d;
    }

    private Cluster createTestEntity() {
        Cluster entity = new Cluster();
        entity.setId(1L);
        entity.setCode("openai-us");
        entity.setName("OpenAI 美东故障域");
        entity.setProviderId(10L);
        entity.setDescription("OpenAI 美东共因特征说明");
        return entity;
    }
}
