package com.codingas.gateway.application.application;

import com.codingas.gateway.application.application.dto.ApplicationRequest;
import com.codingas.gateway.application.application.dto.ApplicationResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.application.entity.Application;
import com.codingas.gateway.domain.application.entity.ApplicationState;
import com.codingas.gateway.domain.application.gateway.ApplicationChannelGateway;
import com.codingas.gateway.domain.application.gateway.ApplicationGateway;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.resilience.gateway.ResilienceProfileGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ApplicationServiceImpl 单元测试
 *
 * <p>验证应用聚合根的 CRUD 与渠道授权绑定业务逻辑：
 * code 唯一校验、状态默认值、渠道授权先删后建等。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationServiceImpl 测试")
class ApplicationServiceImplTest {

    @Mock
    private ApplicationGateway applicationGateway;

    @Mock
    private ApplicationChannelGateway applicationChannelGateway;

    @Mock
    private ResilienceProfileGateway resilienceProfileGateway;

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
        @DisplayName("create 透传 resilienceProfileId 到实体")
        void create_passesResilienceProfileIdToEntity() {
            ApplicationRequest request = new ApplicationRequest();
            request.setCode("APP-001");
            request.setName("测试应用");
            request.setResilienceProfileId(7L);
            when(applicationGateway.findByCode("APP-001")).thenReturn(null);
            Application saved = buildSavedApplication(1L, "APP-001", "测试应用");
            saved.setResilienceProfileId(7L);
            when(applicationGateway.save(any())).thenReturn(saved);

            ApplicationResponse result = applicationService.create(request);

            assertThat(result.getResilienceProfileId()).isEqualTo(7L);
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(applicationGateway).save(captor.capture());
            assertThat(captor.getValue().getResilienceProfileId()).isEqualTo(7L);
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
        @DisplayName("update 透传 resilienceProfileId（非空时绑定）")
        void update_passesResilienceProfileIdToEntity() {
            ApplicationRequest request = new ApplicationRequest();
            request.setCode("APP-001");
            request.setName("名称");
            request.setResilienceProfileId(7L);
            Application existing = buildSavedApplication(1L, "APP-001", "旧名称");
            when(applicationGateway.findById(1L)).thenReturn(existing);
            Application saved = buildSavedApplication(1L, "APP-001", "名称");
            saved.setResilienceProfileId(7L);
            when(applicationGateway.save(any())).thenReturn(saved);

            ApplicationResponse result = applicationService.update(1L, request);

            assertThat(result.getResilienceProfileId()).isEqualTo(7L);
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(applicationGateway).save(captor.capture());
            assertThat(captor.getValue().getResilienceProfileId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("update 传 null resilienceProfileId 时清空绑定（透传 null）")
        void update_nullResilienceProfileId_clearsBinding() {
            ApplicationRequest request = new ApplicationRequest();
            request.setCode("APP-001");
            request.setName("名称");
            // request 未 set resilienceProfileId，即 null
            Application existing = buildSavedApplication(1L, "APP-001", "旧名称");
            existing.setResilienceProfileId(7L); // 原已绑定
            when(applicationGateway.findById(1L)).thenReturn(existing);
            Application saved = buildSavedApplication(1L, "APP-001", "名称");
            // 保存后返回的实体 resilienceProfileId 应为 null（透传 request 的 null）
            when(applicationGateway.save(any())).thenReturn(saved);

            ApplicationResponse result = applicationService.update(1L, request);

            assertThat(result.getResilienceProfileId()).isNull();
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(applicationGateway).save(captor.capture());
            assertThat(captor.getValue().getResilienceProfileId()).isNull();
        }
    }

    @Nested
    @DisplayName("bindResilienceProfile 方法测试")
    class BindResilienceProfileTests {

        @Test
        @DisplayName("正常绑定：application 与 profile 均存在，返回含画像 ID 的响应")
        void bindResilienceProfile_valid_bindsAndReturns() {
            Application existing = buildSavedApplication(1L, "APP-001", "应用");
            when(applicationGateway.findById(1L)).thenReturn(existing);
            ResilienceProfile profile = new ResilienceProfile();
            profile.setId(7L);
            when(resilienceProfileGateway.findById(7L)).thenReturn(profile);
            Application saved = buildSavedApplication(1L, "APP-001", "应用");
            saved.setResilienceProfileId(7L);
            when(applicationGateway.save(any())).thenReturn(saved);

            ApplicationResponse result = applicationService.bindResilienceProfile(1L, 7L);

            assertThat(result).isNotNull();
            assertThat(result.getResilienceProfileId()).isEqualTo(7L);
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(applicationGateway).save(captor.capture());
            assertThat(captor.getValue().getResilienceProfileId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("解绑：resilienceProfileId 为 null 时清空绑定并允许")
        void bindResilienceProfile_nullId_unbinds() {
            Application existing = buildSavedApplication(1L, "APP-001", "应用");
            existing.setResilienceProfileId(7L);
            when(applicationGateway.findById(1L)).thenReturn(existing);
            Application saved = buildSavedApplication(1L, "APP-001", "应用");
            // 保存后 resilienceProfileId 为 null
            when(applicationGateway.save(any())).thenReturn(saved);

            ApplicationResponse result = applicationService.bindResilienceProfile(1L, null);

            assertThat(result.getResilienceProfileId()).isNull();
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(applicationGateway).save(captor.capture());
            assertThat(captor.getValue().getResilienceProfileId()).isNull();
            // 解绑不应校验画像存在性
            verify(resilienceProfileGateway, never()).findById(any());
        }

        @Test
        @DisplayName("application 不存在时抛 APPLICATION_NOT_FOUND")
        void bindResilienceProfile_applicationNotFound_throws() {
            when(applicationGateway.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> applicationService.bindResilienceProfile(999L, 7L))
                    .isInstanceOf(GatewayRequestException.class)
                    .extracting("code")
                    .isEqualTo("APPLICATION_NOT_FOUND");
            verify(applicationGateway, never()).save(any());
        }

        @Test
        @DisplayName("resilienceProfileId 非空但画像不存在时抛 RESILIENCE_PROFILE_NOT_FOUND")
        void bindResilienceProfile_profileNotFound_throws() {
            Application existing = buildSavedApplication(1L, "APP-001", "应用");
            when(applicationGateway.findById(1L)).thenReturn(existing);
            when(resilienceProfileGateway.findById(7L)).thenReturn(null);

            assertThatThrownBy(() -> applicationService.bindResilienceProfile(1L, 7L))
                    .isInstanceOf(GatewayRequestException.class)
                    .extracting("code")
                    .isEqualTo("RESILIENCE_PROFILE_NOT_FOUND");
            verify(applicationGateway, never()).save(any());
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
        @DisplayName("listChannelIds 返回应用授权的渠道 ID 列表")
        void listChannelIds_returnsChannelIds() {
            when(applicationChannelGateway.findChannelIdsByApplicationId(1L))
                    .thenReturn(Set.of(10L, 20L));

            List<Long> result = applicationService.listChannelIds(1L);

            assertThat(result).containsExactlyInAnyOrder(10L, 20L);
        }

        @Test
        @DisplayName("updateChannels 先删旧关联再批量保存新关联")
        void updateChannels_replacesAuthorizations() {
            when(applicationGateway.findById(1L))
                    .thenReturn(buildSavedApplication(1L, "APP-001", "应用"));

            applicationService.updateChannels(1L, List.of(10L, 20L));

            // 先删后建
            verify(applicationChannelGateway).deleteByApplicationId(1L);
            ArgumentCaptor<List<com.codingas.gateway.domain.application.entity.ApplicationChannel>> captor =
                    ArgumentCaptor.forClass(List.class);
            verify(applicationChannelGateway).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
            assertThat(captor.getValue())
                    .extracting(com.codingas.gateway.domain.application.entity.ApplicationChannel::getChannelId)
                    .containsExactlyInAnyOrder(10L, 20L);
            assertThat(captor.getValue())
                    .allMatch(rel -> rel.getApplicationId().equals(1L));
        }

        @Test
        @DisplayName("updateChannels 应用不存在时抛出异常")
        void updateChannels_notFound_throwsException() {
            when(applicationGateway.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> applicationService.updateChannels(999L, List.of(10L)))
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
