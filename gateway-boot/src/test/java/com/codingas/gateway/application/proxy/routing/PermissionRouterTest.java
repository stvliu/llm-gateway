package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.team.gateway.TeamChannelGateway;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * PermissionRouter 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionRouter 单元测试")
class PermissionRouterTest {

    @Mock
    private ChannelGateway channelGateway;

    @Mock
    private UserTeamGateway userTeamGateway;

    @Mock
    private TeamChannelGateway teamChannelGateway;

    @InjectMocks
    private PermissionRouter router;

    @Test
    @DisplayName("ADMIN 角色跳过团队过滤，返回所有活跃渠道实例")
    void admin_skipsTeamFilter() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(100L);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(200L);

        Channel ch1 = new Channel();
        ch1.setId(100L);
        ch1.setState(Channel.State.ACTIVE);
        Channel ch2 = new Channel();
        ch2.setId(200L);
        ch2.setState(Channel.State.ACTIVE);

        when(channelGateway.findAll()).thenReturn(List.of(ch1, ch2));
        when(channelGateway.findByIds(List.of(100L, 200L))).thenReturn(List.of(ch1, ch2));

        RoutingRequest request = new RoutingRequest(1L, 1L, "ADMIN", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).hasSize(2);
        verify(userTeamGateway, never()).findTeamIdByUserId(anyLong());
    }

    @Test
    @DisplayName("普通用户只返回团队渠道内的实例")
    void normalUser_filtersByTeam() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(100L);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(200L);

        Channel ch1 = new Channel();
        ch1.setId(100L);
        ch1.setState(Channel.State.ACTIVE);

        when(userTeamGateway.findTeamIdByUserId(1L)).thenReturn(1L);
        when(teamChannelGateway.findChannelIdsByTeamId(1L)).thenReturn(List.of(100L));
        when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(ch1));

        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("用户无团队时返回空列表")
    void userWithoutTeam_returnsEmpty() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setChannelId(100L);

        when(userTeamGateway.findTeamIdByUserId(1L)).thenReturn(null);

        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1), request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ADMIN 过滤掉非活跃渠道的实例")
    void admin_filtersInactiveChannel() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setChannelId(100L);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setChannelId(200L);

        Channel ch1 = new Channel();
        ch1.setId(100L);
        ch1.setState(Channel.State.ACTIVE);
        Channel ch2 = new Channel();
        ch2.setId(200L);
        ch2.setState(Channel.State.SUSPENDED);

        when(channelGateway.findAll()).thenReturn(List.of(ch1, ch2));
        when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(ch1));

        RoutingRequest request = new RoutingRequest(1L, 1L, "ADMIN", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        // ch2 是 INACTIVE，所以 mi2 被过滤掉
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("isForce 返回 true")
    void isForce_returnsTrue() {
        assertThat(router.isForce()).isTrue();
    }
}
