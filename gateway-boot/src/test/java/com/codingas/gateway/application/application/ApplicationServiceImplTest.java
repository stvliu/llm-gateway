package com.codingas.gateway.application.application;

import com.codingas.gateway.application.application.dto.ApplicationChannelItem;
import com.codingas.gateway.application.application.dto.ApplicationRequest;
import com.codingas.gateway.application.application.dto.ApplicationResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.application.entity.Application;
import com.codingas.gateway.domain.application.entity.ApplicationChannel;
import com.codingas.gateway.domain.application.entity.ApplicationState;
import com.codingas.gateway.domain.application.enums.FailureStrategy;
import com.codingas.gateway.domain.application.gateway.ApplicationChannelGateway;
import com.codingas.gateway.domain.application.gateway.ApplicationGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * ApplicationServiceImpl 单元测试
 *
 * <p>验证应用聚合根的 CRUD 与渠道授权绑定业务逻辑：
 * code 唯一校验、状态默认值、渠道授权先删后建、timeout 透传等。</p>
 *
 * <p>Task 8：{@code resilienceProfileId}/bindResilienceProfile 退场，改为 {@code timeout} 透传。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationServiceImpl 测试")
class ApplicationServiceImplTest {

    @Mock
    private ApplicationGateway applicationGateway;

    @Mock
    private ApplicationChannelGateway applicationChannelGateway;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Nested
    @DisplayName("create 方法测试")

    class CreateTests {

        @Test
        @DisplayName("创建应用成功，状态默认 ACTIVE")
        void create_validRequest_returnsResponseWithActiveState() {
            ApplicationRequest request = new ApplicationRequest();
            request.setCode("APP-001");
            request.setName("测试应用");
            request.setDescription("描述");
            when(applicationGateway.findByCode("APP-001")).thenReturn(null);
            Application saved = buildSavedApplication(1L, "APP-001", "测试应用");
            when(applicationGateway.save(any())).thenReturn(saved);

            ApplicationResponse result = applicationService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCode()).isEqualTo("APP-001");
            assertThat(result.getState()).isEqualTo("ACTIVE");
            // 验证传入 save 的实体状态为 ACTIVE
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(applicationGateway).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo(ApplicationState.ACTIVE);
        }

        @Test
        @DisplayName("create 透传 timeout 到实体")
        void create_passesTimeoutToEntity() {
            ApplicationRequest request = new ApplicationRequest();
            request.setCode("APP-001");
            request.setName("测试应用");
            request.setTimeout(60);
            when(applicationGateway.findByCode("APP-001")).thenReturn(null);
            Application saved = buildSavedApplication(1L, "APP-001", "测试应用");
            saved.setTimeout(60);
            when(applicationGateway.save(any())).thenReturn(saved);

            ApplicationResponse result = applicationService.create(request);

            assertThat(result.getTimeout()).isEqualTo(60);
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(applicationGateway).save(captor.capture());
            assertThat(captor.getValue().getTimeout()).isEqualTo(60);
        }

        @Test
        @DisplayName("code 重复时抛出 GatewayRequestException")
        void create_duplicateCode_throwsException() {
            ApplicationRequest request = new ApplicationRequest();
            request.setCode("APP-001");
            request.setName("测试应用");
            when(applicationGateway.findByCode("APP-001"))
                    .thenReturn(buildSavedApplication(1L, "APP-001", "已存在应用"));

            assertThatThrownBy(() -> applicationService.create(request))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("APP-001");
            verify(applicationGateway, never()).save(any());
        }

        @Test
        @DisplayName("create 未传 failureStrategy 时默认 FAIL_RETRY")
        void create_withoutFailureStrategy_defaultsToFailRetry() {
            ApplicationRequest request = new ApplicationRequest();
            request.setCode("APP-TEST");
            request.setName("测试应用");
            request.setTimeout(0);
            // 不设置 failureStrategy，验证后端默认 FAIL_RETRY
            when(applicationGateway.findByCode("APP-TEST")).thenReturn(null);
            Application saved = buildSavedApplication(1L, "APP-TEST", "测试应用");
            saved.setFailureStrategy(FailureStrategy.FAIL_RETRY);
            when(applicationGateway.save(any())).thenReturn(saved);

            ApplicationResponse result = applicationService.create(request);

            assertThat(result.getFailureStrategy()).isEqualTo("FAIL_RETRY");
            // 验证传入 save 的实体 failureStrategy 被设为默认 FAIL_RETRY
            verify(applicationGateway).save(argThat(a -> a.getFailureStrategy() == FailureStrategy.FAIL_RETRY));
        }

        @Test
        @DisplayName("create 透传指定的 failureStrategy")
        void create_withFailFast_propagatesStrategy() {
            ApplicationRequest request = new ApplicationRequest();
            request.setCode("APP-FF");
            request.setName("快速失败应用");
            request.setFailureStrategy(FailureStrategy.FAIL_FAST);
            when(applicationGateway.findByCode("APP-FF")).thenReturn(null);
            Application saved = buildSavedApplication(1L, "APP-FF", "快速失败应用");
            saved.setFailureStrategy(FailureStrategy.FAIL_FAST);
            when(applicationGateway.save(any())).thenReturn(saved);

            ApplicationResponse result = applicationService.create(request);

            assertThat(result.getFailureStrategy()).isEqualTo("FAIL_FAST");
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("更新应用成功")
        void update_validRequest_returnsUpdated() {
            ApplicationRequest request = new ApplicationRequest();
            request.setCode("APP-001");
            request.setName("新名称");
            request.setDescription("新描述");
            Application existing = buildSavedApplication(1L, "APP-001", "旧名称");
            when(applicationGateway.findById(1L)).thenReturn(existing);
            when(applicationGateway.save(any())).thenReturn(
                    buildSavedApplication(1L, "APP-001", "新名称"));

            ApplicationResponse result = applicationService.update(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("新名称");
        }

        @Test
        @DisplayName("应用不存在时抛出异常")
        void update_notFound_throwsException() {
            ApplicationRequest request = new ApplicationRequest();
            request.setCode("APP-001");
            request.setName("名称");
            when(applicationGateway.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> applicationService.update(999L, request))
                    .isInstanceOf(GatewayRequestException.class);
        }

        @Test
        @DisplayName("修改 code 时校验新 code 不与他应用冲突")
        void update_changeCodeToExisting_throwsException() {
            ApplicationRequest request = new ApplicationRequest();
            request.setCode("APP-002");
            request.setName("名称");
            Application existing = buildSavedApplication(1L, "APP-001", "旧名称");
            when(applicationGateway.findById(1L)).thenReturn(existing);
            // APP-002 已被其他应用占用
            when(applicationGateway.findByCode("APP-002"))
                    .thenReturn(buildSavedApplication(2L, "APP-002", "占用应用"));

            assertThatThrownBy(() -> applicationService.update(1L, request))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("APP-002");
        }

        @Test
        @DisplayName("update 透传 timeout 到实体")
        void update_passesTimeoutToEntity() {
            ApplicationRequest request = new ApplicationRequest();
            request.setCode("APP-001");
            request.setName("名称");
            request.setTimeout(30);
            Application existing = buildSavedApplication(1L, "APP-001", "旧名称");
            when(applicationGateway.findById(1L)).thenReturn(existing);
            Application saved = buildSavedApplication(1L, "APP-001", "名称");
            saved.setTimeout(30);
            when(applicationGateway.save(any())).thenReturn(saved);

            ApplicationResponse result = applicationService.update(1L, request);

            assertThat(result.getTimeout()).isEqualTo(30);
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(applicationGateway).save(captor.capture());
            assertThat(captor.getValue().getTimeout()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("存在时返回响应")
        void getById_existing_returnsResponse() {
            Application app = buildSavedApplication(1L, "APP-001", "应用");
            when(applicationGateway.findById(1L)).thenReturn(app);

            ApplicationResponse result = applicationService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("APP-001");
        }

        @Test
        @DisplayName("不存在时抛出异常")
        void getById_notFound_throwsException() {
            when(applicationGateway.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> applicationService.getById(999L))
                    .isInstanceOf(GatewayRequestException.class);
        }
    }

    @Nested
    @DisplayName("getAll 方法测试")
    class GetAllTests {

        @Test
        @DisplayName("返回全部应用列表")
        void getAll_returnsList() {
            when(applicationGateway.findAll()).thenReturn(List.of(
                    buildSavedApplication(1L, "APP-001", "应用一"),
                    buildSavedApplication(2L, "APP-002", "应用二")));

            List<ApplicationResponse> result = applicationService.getAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ApplicationResponse::getCode)
                    .containsExactly("APP-001", "APP-002");
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除应用并级联清理渠道授权关联")
        void delete_cascadesChannelAuthorizations() {
            applicationService.delete(1L);

            verify(applicationGateway).deleteById(1L);
            verify(applicationChannelGateway).deleteByApplicationId(1L);
        }
    }

    @Nested
    @DisplayName("渠道授权方法测试")
    class ChannelAuthorizationTests {

        @Test
        @DisplayName("listChannels 返回应用授权的渠道及其 priority")
        void listChannels_returnsChannelsWithPriority() {
            // 模拟 gateway 返回含 priority 的关联列表
            ApplicationChannel rel1 = new ApplicationChannel(1L, 10L, 1);
            ApplicationChannel rel2 = new ApplicationChannel(1L, 20L, null);
            when(applicationChannelGateway.findByApplicationId(1L))
                    .thenReturn(List.of(rel1, rel2));

            List<ApplicationChannelItem> result = applicationService.listChannels(1L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ApplicationChannelItem::channelId)
                    .containsExactlyInAnyOrder(10L, 20L);
            // priority 原样透传（null 表示未配置）
            assertThat(result).filteredOn(i -> i.channelId().equals(10L))
                    .singleElement()
                    .extracting(ApplicationChannelItem::priority)
                    .isEqualTo(1);
            assertThat(result).filteredOn(i -> i.channelId().equals(20L))
                    .singleElement()
                    .extracting(ApplicationChannelItem::priority)
                    .isNull();
        }

        @Test
        @DisplayName("updateChannels 用三参构造器保存含 priority 的关联")
        void updateChannels_savesAuthorizationsWithPriority() {
            when(applicationGateway.findById(1L))
                    .thenReturn(buildSavedApplication(1L, "APP-001", "应用"));

            // channel 10 配 priority=1，channel 20 不配（null）
            applicationService.updateChannels(1L, List.of(
                    new ApplicationChannelItem(10L, 1),
                    new ApplicationChannelItem(20L, null)));

            // 先删后建
            verify(applicationChannelGateway).deleteByApplicationId(1L);
            ArgumentCaptor<List<ApplicationChannel>> captor =
                    ArgumentCaptor.forClass(List.class);
            verify(applicationChannelGateway).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
            assertThat(captor.getValue())
                    .extracting(ApplicationChannel::getChannelId)
                    .containsExactlyInAnyOrder(10L, 20L);
            assertThat(captor.getValue())
                    .allMatch(rel -> rel.getApplicationId().equals(1L));
            // 验证 priority 透传到实体
            assertThat(captor.getValue()).filteredOn(r -> r.getChannelId().equals(10L))
                    .singleElement()
                    .extracting(ApplicationChannel::getPriority)
                    .isEqualTo(1);
            assertThat(captor.getValue()).filteredOn(r -> r.getChannelId().equals(20L))
                    .singleElement()
                    .extracting(ApplicationChannel::getPriority)
                    .isNull();
        }

        @Test
        @DisplayName("updateChannels 应用不存在时抛出异常")
        void updateChannels_notFound_throwsException() {
            when(applicationGateway.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> applicationService.updateChannels(999L,
                    List.of(new ApplicationChannelItem(10L, 1))))
                    .isInstanceOf(GatewayRequestException.class);
        }

        @Test
        @DisplayName("updateChannels 空渠道列表时仅清空不保存")
        void updateChannels_emptyList_onlyDeletes() {
            when(applicationGateway.findById(1L))
                    .thenReturn(buildSavedApplication(1L, "APP-001", "应用"));

            applicationService.updateChannels(1L, List.of());

            verify(applicationChannelGateway).deleteByApplicationId(1L);
            verify(applicationChannelGateway, never()).saveAll(any());
        }
    }

    // ===== Helper methods =====

    private Application buildSavedApplication(Long id, String code, String name) {
        Application app = new Application();
        app.setId(id);
        app.setCode(code);
        app.setName(name);
        app.setDescription("描述");
        app.setState(ApplicationState.ACTIVE);
        return app;
    }
}
