package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.ChannelStateTransitionRequest;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * ChannelServiceImpl 状态转换测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelServiceImpl 状态转换测试")
class ChannelServiceImplStateTransitionTest {

    @Mock
    private ChannelGateway channelGateway;
    @Mock
    private ChannelEndpointGateway channelEndpointGateway;
    @Mock
    private ChannelCredentialGateway channelCredentialGateway;
    @Mock
    private ModelInstanceGateway modelInstanceGateway;
    @Mock
    private ProviderGateway providerGateway;

    @Captor
    private ArgumentCaptor<Channel> channelCaptor;

    private ChannelServiceImpl channelService;

    @BeforeEach
    void setUp() {
        channelService = new ChannelServiceImpl(
            channelGateway, channelEndpointGateway,
            channelCredentialGateway, modelInstanceGateway,
            providerGateway
        );
    }

    private Channel createChannel(Long id, ChannelState state) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setProviderId(1L);
        channel.setName("test-channel");
        channel.setState(state);
        return channel;
    }

    private ChannelStateTransitionRequest request(String targetState) {
        ChannelStateTransitionRequest req = new ChannelStateTransitionRequest();
        req.setTargetState(targetState);
        return req;
    }

    private ChannelStateTransitionRequest request(String targetState, String reason) {
        ChannelStateTransitionRequest req = request(targetState);
        req.setReason(reason);
        return req;
    }

    @Nested
    @DisplayName("PENDING→ACTIVE 测试")
    class PendingToActiveTests {

        @Test
        @DisplayName("前置条件满足时成功激活并级联激活 PENDING ModelInstance")
        void activate_success_withCascade() {
            Channel channel = createChannel(1L, ChannelState.PENDING);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

            // 前置条件满足
            when(channelEndpointGateway.findByChannelId(1L))
                .thenReturn(List.of(createEndpoint()));
            when(channelCredentialGateway.findByChannelId(1L))
                .thenReturn(List.of(createCredential()));

            ModelInstance pendingMi = createModelInstance(10L, ModelInstance.State.PENDING);
            ModelInstance activeMi = createModelInstance(11L, ModelInstance.State.ACTIVE);
            when(modelInstanceGateway.findByChannelId(1L))
                .thenReturn(List.of(pendingMi, activeMi));

            channelService.setState(1L, request("ACTIVE"));

            verify(channelGateway).save(channelCaptor.capture());
            assertThat(channelCaptor.getValue().getState()).isEqualTo(ChannelState.ACTIVE);

            // 验证级联激活：PENDING 实例变为 ACTIVE
            assertThat(pendingMi.getState()).isEqualTo(ModelInstance.State.ACTIVE);
            // ACTIVE 实例不变
            assertThat(activeMi.getState()).isEqualTo(ModelInstance.State.ACTIVE);

            verify(modelInstanceGateway).saveAll(List.of(pendingMi));
        }

        @Test
        @DisplayName("前置条件不满足（无端点）时抛出异常")
        void activate_fails_noEndpoint() {
            Channel channel = createChannel(1L, ChannelState.PENDING);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

            when(channelEndpointGateway.findByChannelId(1L)).thenReturn(List.of());

            assertThatThrownBy(() -> channelService.setState(1L, request("ACTIVE")))
                .isInstanceOf(GatewayRequestException.class)
                .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode()).isEqualTo("CHANNEL_NO_ENDPOINT"))
                .hasMessageContaining("请先添加端点");

            verify(channelGateway, never()).save(any());
        }

        @Test
        @DisplayName("前置条件不满足（无凭证）时抛出异常")
        void activate_fails_noCredential() {
            Channel channel = createChannel(1L, ChannelState.PENDING);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

            when(channelEndpointGateway.findByChannelId(1L))
                .thenReturn(List.of(createEndpoint()));
            when(channelCredentialGateway.findByChannelId(1L)).thenReturn(List.of());

            assertThatThrownBy(() -> channelService.setState(1L, request("ACTIVE")))
                .isInstanceOf(GatewayRequestException.class)
                .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode()).isEqualTo("CHANNEL_NO_CREDENTIAL"))
                .hasMessageContaining("请先添加凭证");

            verify(channelGateway, never()).save(any());
        }

        @Test
        @DisplayName("前置条件不满足（无模型实例）时抛出异常")
        void activate_fails_noModelInstance() {
            Channel channel = createChannel(1L, ChannelState.PENDING);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

            when(channelEndpointGateway.findByChannelId(1L))
                .thenReturn(List.of(createEndpoint()));
            when(channelCredentialGateway.findByChannelId(1L))
                .thenReturn(List.of(createCredential()));
            when(modelInstanceGateway.findByChannelId(1L)).thenReturn(List.of());

            assertThatThrownBy(() -> channelService.setState(1L, request("ACTIVE")))
                .isInstanceOf(GatewayRequestException.class)
                .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode()).isEqualTo("CHANNEL_NO_MODEL_INSTANCE"))
                .hasMessageContaining("请先关联模型实例");

            verify(channelGateway, never()).save(any());
        }
    }

    @Nested
    @DisplayName("ACTIVE→SUSPENDED 测试")
    class ActiveToSuspendedTests {

        @Test
        @DisplayName("暂停渠道成功")
        void suspend_success() {
            Channel channel = createChannel(1L, ChannelState.ACTIVE);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

            channelService.setState(1L, request("SUSPENDED", "供应商维护"));

            verify(channelGateway).save(channelCaptor.capture());
            assertThat(channelCaptor.getValue().getState()).isEqualTo(ChannelState.SUSPENDED);
        }
    }

    @Nested
    @DisplayName("ACTIVE→DEPRECATED 测试")
    class ActiveToDeprecatedTests {

        @Test
        @DisplayName("标记下线成功")
        void deprecate_success() {
            Channel channel = createChannel(1L, ChannelState.ACTIVE);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

            channelService.setState(1L, request("DEPRECATED", "模型升级"));

            verify(channelGateway).save(channelCaptor.capture());
            assertThat(channelCaptor.getValue().getState()).isEqualTo(ChannelState.DEPRECATED);
        }
    }

    @Nested
    @DisplayName("SUSPENDED→ACTIVE 测试")
    class SuspendedToActiveTests {

        @Test
        @DisplayName("恢复激活成功（即使无端点也仅警告）")
        void reactivate_success_withWarning() {
            Channel channel = createChannel(1L, ChannelState.SUSPENDED);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

            // 无端点 — 仅警告不阻塞
            when(channelEndpointGateway.findByChannelId(1L)).thenReturn(List.of());
            when(channelCredentialGateway.findByChannelId(1L)).thenReturn(List.of());
            when(modelInstanceGateway.findByChannelId(1L)).thenReturn(List.of());

            channelService.setState(1L, request("ACTIVE"));

            verify(channelGateway).save(channelCaptor.capture());
            assertThat(channelCaptor.getValue().getState()).isEqualTo(ChannelState.ACTIVE);
        }
    }

    @Nested
    @DisplayName("SUSPENDED→DEPRECATED 测试")
    class SuspendedToDeprecatedTests {

        @Test
        @DisplayName("暂停后标记下线成功")
        void deprecate_success() {
            Channel channel = createChannel(1L, ChannelState.SUSPENDED);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

            channelService.setState(1L, request("DEPRECATED"));

            verify(channelGateway).save(channelCaptor.capture());
            assertThat(channelCaptor.getValue().getState()).isEqualTo(ChannelState.DEPRECATED);
        }
    }

    @Nested
    @DisplayName("SUSPENDED→RETIRED 测试")
    class SuspendedToRetiredTests {

        @Test
        @DisplayName("暂停后直接废弃成功（允许直达）")
        void retire_success() {
            Channel channel = createChannel(1L, ChannelState.SUSPENDED);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

            channelService.setState(1L, request("RETIRED", "渠道下线"));

            verify(channelGateway).save(channelCaptor.capture());
            assertThat(channelCaptor.getValue().getState()).isEqualTo(ChannelState.RETIRED);
        }
    }

    @Nested
    @DisplayName("DEPRECATED→RETIRED 测试")
    class DeprecatedToRetiredTests {

        @Test
        @DisplayName("废弃后终态销毁成功")
        void retire_success() {
            Channel channel = createChannel(1L, ChannelState.DEPRECATED);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

            channelService.setState(1L, request("RETIRED"));

            verify(channelGateway).save(channelCaptor.capture());
            assertThat(channelCaptor.getValue().getState()).isEqualTo(ChannelState.RETIRED);
        }
    }

    @Nested
    @DisplayName("非法转换测试")
    class InvalidTransitionsTests {

        @Test
        @DisplayName("渠道不存在时抛出异常")
        void channelNotFound() {
            when(channelGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> channelService.setState(99L, request("ACTIVE")))
                .isInstanceOf(GatewayRequestException.class)
                .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode()).isEqualTo("CHANNEL_NOT_FOUND"))
                .hasMessageContaining("渠道不存在");
        }

        @Test
        @DisplayName("ACTIVE→PENDING 非法转换被拒绝")
        void activeToPending_invalid() {
            Channel channel = createChannel(1L, ChannelState.ACTIVE);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

            assertThatThrownBy(() -> channelService.setState(1L, request("PENDING")))
                .isInstanceOf(GatewayRequestException.class)
                .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode()).isEqualTo("INVALID_STATE_TRANSITION"))
                .hasMessageContaining("不允许从");
        }

        @Test
        @DisplayName("ACTIVE→RETIRED 非法转换被拒绝（必须经过 DEPRECATED）")
        void activeToRetired_invalid() {
            Channel channel = createChannel(1L, ChannelState.ACTIVE);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

            assertThatThrownBy(() -> channelService.setState(1L, request("RETIRED")))
                .isInstanceOf(GatewayRequestException.class)
                .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode()).isEqualTo("INVALID_STATE_TRANSITION"))
                .hasMessageContaining("不允许从");
        }

        @Test
        @DisplayName("RETIRED→任何状态非法")
        void retiredToAny_invalid() {
            Channel channel = createChannel(1L, ChannelState.RETIRED);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));

            assertThatThrownBy(() -> channelService.setState(1L, request("ACTIVE")))
                .isInstanceOf(GatewayRequestException.class)
                .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
        }
    }

    // ===== 辅助方法 =====

    private ChannelEndpoint createEndpoint() {
        ChannelEndpoint ep = new ChannelEndpoint();
        ep.setId(100L);
        ep.setChannelId(1L);
        return ep;
    }

    private ChannelCredential createCredential() {
        ChannelCredential cred = new ChannelCredential();
        cred.setId(200L);
        cred.setChannelId(1L);
        return cred;
    }

    private ModelInstance createModelInstance(Long id, ModelInstance.State state) {
        ModelInstance mi = new ModelInstance();
        mi.setId(id);
        mi.setChannelId(1L);
        mi.setState(state);
        return mi;
    }
}
