package com.codingas.gateway.application.resilience;

import com.codingas.gateway.application.resilience.dto.ResilienceProfileRequest;
import com.codingas.gateway.application.resilience.dto.ResilienceProfileResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.resilience.entity.ResilienceMode;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.resilience.gateway.ResilienceProfileGateway;
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
 * ResilienceProfileServiceImpl 单元测试
 *
 * <p>Mock {@link ResilienceProfileGateway}，验证应用服务的业务校验与转换逻辑。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("容灾画像应用服务测试")
class ResilienceProfileServiceImplTest {

    @Mock
    private ResilienceProfileGateway resilienceProfileGateway;

    @InjectMocks
    private ResilienceProfileServiceImpl resilienceProfileService;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建容灾画像成功")
        void create_validRequest_returnsResponse() {
            ResilienceProfileRequest request = buildRequest("default", "默认画像", "STANDARD");
            when(resilienceProfileGateway.findByCode("default")).thenReturn(null);
            when(resilienceProfileGateway.save(any())).thenReturn(buildSavedProfile(1L, "default", "默认画像"));

            ResilienceProfileResponse result = resilienceProfileService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCode()).isEqualTo("default");
            assertThat(result.getMode()).isEqualTo("STANDARD");
        }

        @Test
        @DisplayName("code 重复时抛出 GatewayRequestException")
        void create_duplicateCode_throwsException() {
            ResilienceProfileRequest request = buildRequest("default", "默认画像", "STANDARD");
            when(resilienceProfileGateway.findByCode("default"))
                    .thenReturn(buildSavedProfile(1L, "default", "已存在画像"));

            assertThatThrownBy(() -> resilienceProfileService.create(request))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("default");
            verify(resilienceProfileGateway, never()).save(any());
        }

        @Test
        @DisplayName("非法 mode 抛出 GatewayRequestException 并含合法值提示")
        void create_invalidMode_throwsException() {
            ResilienceProfileRequest request = buildRequest("p1", "画像", "INVALID_MODE");

            assertThatThrownBy(() -> resilienceProfileService.create(request))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("INVALID_MODE");
            verify(resilienceProfileGateway, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("更新容灾画像成功")
        void update_validRequest_returnsUpdated() {
            ResilienceProfileRequest request = buildRequest("default", "新名称", "STRICT");
            ResilienceProfile existing = buildSavedProfile(1L, "default", "旧名称");
            when(resilienceProfileGateway.findById(1L)).thenReturn(existing);
            when(resilienceProfileGateway.save(any())).thenReturn(buildSavedProfileWithMode(1L, "default", "新名称", ResilienceMode.STRICT));

            ResilienceProfileResponse result = resilienceProfileService.update(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("新名称");
            assertThat(result.getMode()).isEqualTo("STRICT");
        }

        @Test
        @DisplayName("画像不存在时抛出异常")
        void update_notFound_throwsException() {
            ResilienceProfileRequest request = buildRequest("default", "名称", "STANDARD");
            when(resilienceProfileGateway.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> resilienceProfileService.update(999L, request))
                    .isInstanceOf(GatewayRequestException.class);
        }

        @Test
        @DisplayName("修改 code 时校验新 code 不与他画像冲突")
        void update_changeCodeToExisting_throwsException() {
            ResilienceProfileRequest request = buildRequest("p2", "名称", "STANDARD");
            ResilienceProfile existing = buildSavedProfile(1L, "p1", "旧名称");
            when(resilienceProfileGateway.findById(1L)).thenReturn(existing);
            when(resilienceProfileGateway.findByCode("p2"))
                    .thenReturn(buildSavedProfile(2L, "p2", "占用画像"));

            assertThatThrownBy(() -> resilienceProfileService.update(1L, request))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("p2");
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("存在时返回响应")
        void getById_existing_returnsResponse() {
            ResilienceProfile profile = buildSavedProfile(1L, "default", "默认画像");
            when(resilienceProfileGateway.findById(1L)).thenReturn(profile);

            ResilienceProfileResponse result = resilienceProfileService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("default");
        }

        @Test
        @DisplayName("不存在时抛出异常")
        void getById_notFound_throwsException() {
            when(resilienceProfileGateway.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> resilienceProfileService.getById(999L))
                    .isInstanceOf(GatewayRequestException.class);
        }
    }

    @Nested
    @DisplayName("getAll 方法测试")
    class GetAllTests {

        @Test
        @DisplayName("返回全部画像列表")
        void getAll_returnsList() {
            when(resilienceProfileGateway.findAll()).thenReturn(List.of(
                    buildSavedProfile(1L, "default", "默认画像"),
                    buildSavedProfile(2L, "strict", "严格画像")));

            List<ResilienceProfileResponse> result = resilienceProfileService.getAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ResilienceProfileResponse::getCode)
                    .containsExactly("default", "strict");
        }
    }

    // ===== Helper methods =====

    private ResilienceProfileRequest buildRequest(String code, String name, String mode) {
        ResilienceProfileRequest request = new ResilienceProfileRequest();
        request.setCode(code);
        request.setName(name);
        request.setMode(mode);
        request.setEnableL2ModelDegradation(true);
        request.setDegradationMaxDepth(2);
        request.setEnableSessionAffinity(false);
        request.setSessionAffinityTtlMinutes(30);
        request.setEnablePinnedModel(false);
        request.setPinnedModelId(null);
        request.setTimeout(0);
        return request;
    }

    private ResilienceProfile buildSavedProfile(Long id, String code, String name) {
        return buildSavedProfileWithMode(id, code, name, ResilienceMode.STANDARD);
    }

    private ResilienceProfile buildSavedProfileWithMode(Long id, String code, String name, ResilienceMode mode) {
        ResilienceProfile profile = new ResilienceProfile();
        profile.setId(id);
        profile.setCode(code);
        profile.setName(name);
        profile.setMode(mode);
        profile.setEnableL2ModelDegradation(true);
        profile.setDegradationMaxDepth(2);
        profile.setEnableSessionAffinity(false);
        profile.setSessionAffinityTtlMinutes(30);
        profile.setEnablePinnedModel(false);
        profile.setTimeout(0);
        return profile;
    }
}
