package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
import com.codingas.gateway.domain.team.gateway.TeamChannelGateway;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * ChannelSelector 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelSelector 单元测试")
class ChannelSelectorTest {

    @Mock
    private ModelInstanceGateway modelInstanceGateway;

    @Mock
    private ChannelGateway channelGateway;

    @Mock
    private UserTeamGateway userTeamGateway;

    @Mock
    private TeamChannelGateway teamChannelGateway;

    private ChannelSelector selector;

    private static final Long USER_ID = 1L;
    private static final Long TEAM_ID = 100L;
    private static final Long MODEL_ID = 1L;

    @BeforeEach
    void setUp() {
        selector = new ChannelSelector(modelInstanceGateway, channelGateway, userTeamGateway, teamChannelGateway);
    }

    @Nested
    @DisplayName("select 方法")
    class SelectTests {

        @Test
        @DisplayName("有活跃通道时返回第一个活跃的 ModelInstance")
        void select_withActiveChannel_returnsFirstActive() {
            // Arrange — 用户属于团队，团队有渠道 10L 和 20L
            when(userTeamGateway.findTeamIdByUserId(USER_ID)).thenReturn(TEAM_ID);
            when(teamChannelGateway.findChannelIdsByTeamId(TEAM_ID)).thenReturn(List.of(100L, 200L));

            ModelInstance mi1 = mock(ModelInstance.class);
            when(mi1.getChannelId()).thenReturn(100L);
            ModelInstance mi2 = mock(ModelInstance.class);
            when(mi2.getChannelId()).thenReturn(200L);

            when(modelInstanceGateway.findActiveByModelId(MODEL_ID)).thenReturn(List.of(mi1, mi2));

            Channel ch1 = mock(Channel.class);
            when(ch1.getState()).thenReturn(ChannelState.INACTIVE);

            Channel ch2 = mock(Channel.class);
            when(ch2.getId()).thenReturn(200L);
            when(ch2.getState()).thenReturn(ChannelState.ACTIVE);

            when(channelGateway.findByIds(List.of(100L, 200L))).thenReturn(List.of(ch1, ch2));

            // Act
            ModelInstance result = selector.select(MODEL_ID, USER_ID);

            // Assert
            assertThat(result).isSameAs(mi2);
        }

        @Test
        @DisplayName("没有活跃通道时抛出 ResourceNotFoundException")
        void select_noActiveChannel_throwsException() {
            // Arrange
            when(userTeamGateway.findTeamIdByUserId(USER_ID)).thenReturn(TEAM_ID);
            when(teamChannelGateway.findChannelIdsByTeamId(TEAM_ID)).thenReturn(List.of(100L));

            ModelInstance mi1 = mock(ModelInstance.class);
            when(mi1.getChannelId()).thenReturn(100L);

            when(modelInstanceGateway.findActiveByModelId(MODEL_ID)).thenReturn(List.of(mi1));

            Channel ch1 = mock(Channel.class);
            when(ch1.getState()).thenReturn(ChannelState.INACTIVE);

            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(ch1));

            // Act & Assert
            assertThatThrownBy(() -> selector.select(MODEL_ID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("没有 ModelInstance 时抛出 ResourceNotFoundException")
        void select_noModelInstance_throwsException() {
            // Arrange
            when(userTeamGateway.findTeamIdByUserId(USER_ID)).thenReturn(TEAM_ID);
            when(teamChannelGateway.findChannelIdsByTeamId(TEAM_ID)).thenReturn(List.of(100L));
            when(modelInstanceGateway.findActiveByModelId(MODEL_ID)).thenReturn(List.of());

            // Act & Assert
            assertThatThrownBy(() -> selector.select(MODEL_ID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("用户未关联团队时抛出 ResourceNotFoundException")
        void select_noTeam_throwsException() {
            // Arrange
            when(userTeamGateway.findTeamIdByUserId(USER_ID)).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> selector.select(MODEL_ID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("团队未关联渠道时抛出 ResourceNotFoundException")
        void select_noTeamChannels_throwsException() {
            // Arrange
            when(userTeamGateway.findTeamIdByUserId(USER_ID)).thenReturn(TEAM_ID);
            when(teamChannelGateway.findChannelIdsByTeamId(TEAM_ID)).thenReturn(List.of());

            // Act & Assert
            assertThatThrownBy(() -> selector.select(MODEL_ID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}