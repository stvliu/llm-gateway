package com.codingas.gateway.application.resilience;

import com.codingas.gateway.application.resilience.dto.ClusterRequest;
import com.codingas.gateway.application.resilience.dto.ClusterResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.resilience.entity.Cluster;
import com.codingas.gateway.domain.resilience.gateway.ClusterGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ClusterServiceImpl 单元测试
 *
 * <p>Mock {@link ClusterGateway}，验证应用服务的业务校验与转换逻辑。</p>
 *
 * <p>Task 6 变更：Cluster 字段瘦身为 code/name/description/providerId + 审计，
 * 删除 region/priority/healthStatus；create 不再设置默认 healthStatus。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("故障域应用服务测试")
class ClusterServiceImplTest {

    @Mock
    private ClusterGateway clusterGateway;

    @InjectMocks
    private ClusterServiceImpl clusterService;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建故障域成功，description 透传到响应")
        void create_validRequest_returnsResponse() {
            ClusterRequest request = buildRequest("openai-us", "OpenAI 美东", 10L);
            when(clusterGateway.findByCode("openai-us")).thenReturn(null);
            when(clusterGateway.save(any())).thenReturn(buildSavedCluster(1L, "openai-us", "OpenAI 美东"));

            ClusterResponse result = clusterService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCode()).isEqualTo("openai-us");
            assertThat(result.getDescription()).isEqualTo("OpenAI 美东共因特征说明");
        }

        @Test
        @DisplayName("code 重复时抛出 GatewayRequestException")
        void create_duplicateCode_throwsException() {
            ClusterRequest request = buildRequest("openai-us", "OpenAI 美东", 10L);
            when(clusterGateway.findByCode("openai-us"))
                    .thenReturn(buildSavedCluster(1L, "openai-us", "已存在"));

            assertThatThrownBy(() -> clusterService.create(request))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("openai-us");
            verify(clusterGateway, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("更新故障域成功")
        void update_validRequest_returnsUpdated() {
            ClusterRequest request = buildRequest("openai-us", "新名称", 10L);
            Cluster existing = buildSavedCluster(1L, "openai-us", "旧名称");
            when(clusterGateway.findById(1L)).thenReturn(existing);
            when(clusterGateway.save(any())).thenReturn(buildSavedCluster(1L, "openai-us", "新名称"));

            ClusterResponse result = clusterService.update(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("新名称");
        }

        @Test
        @DisplayName("故障域不存在时抛出异常")
        void update_notFound_throwsException() {
            ClusterRequest request = buildRequest("openai-us", "名称", 10L);
            when(clusterGateway.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> clusterService.update(999L, request))
                    .isInstanceOf(GatewayRequestException.class);
        }

        @Test
        @DisplayName("修改 code 时校验新 code 不与他故障域冲突")
        void update_changeCodeToExisting_throwsException() {
            ClusterRequest request = buildRequest("claude-sg", "名称", 20L);
            Cluster existing = buildSavedCluster(1L, "openai-us", "旧名称");
            when(clusterGateway.findById(1L)).thenReturn(existing);
            when(clusterGateway.findByCode("claude-sg"))
                    .thenReturn(buildSavedCluster(2L, "claude-sg", "占用"));

            assertThatThrownBy(() -> clusterService.update(1L, request))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("claude-sg");
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("存在时返回响应")
        void getById_existing_returnsResponse() {
            Cluster cluster = buildSavedCluster(1L, "openai-us", "OpenAI 美东");
            when(clusterGateway.findById(1L)).thenReturn(cluster);

            ClusterResponse result = clusterService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("openai-us");
        }

        @Test
        @DisplayName("不存在时抛出异常")
        void getById_notFound_throwsException() {
            when(clusterGateway.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> clusterService.getById(999L))
                    .isInstanceOf(GatewayRequestException.class);
        }
    }

    @Nested
    @DisplayName("getAll 方法测试")
    class GetAllTests {

        @Test
        @DisplayName("返回全部故障域列表")
        void getAll_returnsList() {
            when(clusterGateway.findAll()).thenReturn(List.of(
                    buildSavedCluster(1L, "openai-us", "OpenAI 美东"),
                    buildSavedCluster(2L, "claude-sg", "Claude 新加坡")));

            List<ClusterResponse> result = clusterService.getAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ClusterResponse::getCode)
                    .containsExactly("openai-us", "claude-sg");
        }
    }

    // ===== Helper methods =====

    private ClusterRequest buildRequest(String code, String name, Long providerId) {
        ClusterRequest request = new ClusterRequest();
        request.setCode(code);
        request.setName(name);
        request.setProviderId(providerId);
        request.setDescription("OpenAI 美东共因特征说明");
        return request;
    }

    private Cluster buildSavedCluster(Long id, String code, String name) {
        Cluster cluster = new Cluster();
        cluster.setId(id);
        cluster.setCode(code);
        cluster.setName(name);
        cluster.setProviderId(10L);
        cluster.setDescription("OpenAI 美东共因特征说明");
        return cluster;
    }
}
