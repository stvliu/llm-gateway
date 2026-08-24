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
package com.codingas.gateway.providerdata.gateway;

import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelHealthSource;
import com.codingas.gateway.provider.channel.ChannelHealthStatus;
import com.codingas.gateway.provider.channel.ChannelState;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.providerdata.dataobject.ChannelDo;
import com.codingas.gateway.providerdata.repository.ChannelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChannelGatewayImpl 单元测试：mock Repository 验证委托与 model↔DO 双向转换
 *
 * <p>覆盖 ChannelGatewayImpl 全部 public 方法（save/findById/findByProviderId/findAllActive/
 * findAll/deleteById/findByIds/existsByProviderIdAndName/findByProviderIdAndBillingMode/
 * findByProviderIdAndName/count）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelGatewayImpl 单元测试")
class ChannelGatewayImplTest {

    @Mock
    private ChannelRepository channelRepository;

    @InjectMocks
    private ChannelGatewayImpl gateway;

    private Channel sampleChannel(Long id, Long providerId, String name) {
        Channel c = new Channel();
        c.setId(id);
        c.setProviderId(providerId);
        c.setName(name);
        c.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        c.setQuotaLimit(5_000_000L);
        c.setTimeout(60);
        c.setMaxRetries(3);
        c.setState(ChannelState.ACTIVE);
        c.setLastHealthCheckAt(Instant.parse("2026-08-01T00:00:00Z"));
        c.setLastHealthStatus(ChannelHealthStatus.HEALTHY);
        c.setLastHealthSource(ChannelHealthSource.CARD);
        c.setCreatedBy(10L);
        c.setUpdatedBy(20L);
        c.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        c.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return c;
    }

    private ChannelDo sampleDo(Long id, Long providerId, String name, String state) {
        ChannelDo doObj = new ChannelDo();
        doObj.setId(id);
        doObj.setProviderId(providerId);
        doObj.setName(name);
        doObj.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        doObj.setQuotaLimit(5_000_000L);
        doObj.setTimeout(60);
        doObj.setMaxRetries(3);
        doObj.setState(state);
        doObj.setLastHealthCheckAt(Instant.parse("2026-08-01T00:00:00Z"));
        doObj.setLastHealthStatus(ChannelHealthStatus.HEALTHY);
        doObj.setLastHealthSource(ChannelHealthSource.CARD);
        doObj.setCreatedBy(10L);
        doObj.setUpdatedBy(20L);
        doObj.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        doObj.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return doObj;
    }

    @Test
    @DisplayName("save：toDo 写字段 + 委托 save + toEntity 读字段（双向转换）")
    void save_convertsBothWaysAndDelegates() {
        Channel channel = sampleChannel(1L, 10L, "主渠道");
        when(channelRepository.save(any(ChannelDo.class))).thenAnswer(inv -> inv.getArgument(0));

        Channel result = gateway.save(channel);

        // toEntity 读字段（state 枚举转换 + 健康字段透传）
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProviderId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("主渠道");
        assertThat(result.getBillingMode()).isEqualTo(BillingMode.PAY_AS_YOU_GO);
        assertThat(result.getQuotaLimit()).isEqualTo(5_000_000L);
        assertThat(result.getTimeout()).isEqualTo(60);
        assertThat(result.getMaxRetries()).isEqualTo(3);
        assertThat(result.getState()).isEqualTo(ChannelState.ACTIVE);
        assertThat(result.getLastHealthCheckAt()).isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
        assertThat(result.getLastHealthStatus()).isEqualTo(ChannelHealthStatus.HEALTHY);
        assertThat(result.getLastHealthSource()).isEqualTo(ChannelHealthSource.CARD);

        // toDo 写字段（state 转字符串）
        ArgumentCaptor<ChannelDo> captor = ArgumentCaptor.forClass(ChannelDo.class);
        verify(channelRepository).save(captor.capture());
        ChannelDo written = captor.getValue();
        assertThat(written.getProviderId()).isEqualTo(10L);
        assertThat(written.getName()).isEqualTo("主渠道");
        assertThat(written.getBillingMode()).isEqualTo(BillingMode.PAY_AS_YOU_GO);
        assertThat(written.getQuotaLimit()).isEqualTo(5_000_000L);
        assertThat(written.getTimeout()).isEqualTo(60);
        assertThat(written.getMaxRetries()).isEqualTo(3);
        assertThat(written.getState()).isEqualTo("ACTIVE");
        assertThat(written.getLastHealthCheckAt()).isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
        assertThat(written.getLastHealthStatus()).isEqualTo(ChannelHealthStatus.HEALTHY);
        assertThat(written.getLastHealthSource()).isEqualTo(ChannelHealthSource.CARD);
    }

    @Test
    @DisplayName("save：state 缺省补 ACTIVE")
    void save_defaultsStateToActive() {
        Channel channel = sampleChannel(2L, 10L, "主渠道");
        channel.setState(null);
        when(channelRepository.save(any(ChannelDo.class))).thenAnswer(inv -> inv.getArgument(0));

        gateway.save(channel);

        ArgumentCaptor<ChannelDo> captor = ArgumentCaptor.forClass(ChannelDo.class);
        verify(channelRepository).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("findById：存在时转换返回，不存在返回空")
    void findById_returnsConvertedOrEmpty() {
        when(channelRepository.findById(1L)).thenReturn(Optional.of(sampleDo(1L, 10L, "主渠道", "ACTIVE")));
        when(channelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(gateway.findById(1L)).isPresent()
                .get().extracting(Channel::getState).isEqualTo(ChannelState.ACTIVE);
        assertThat(gateway.findById(99L)).isEmpty();
    }

    @Test
    @DisplayName("findByProviderId：按供应商查询并转换")
    void findByProviderId_convertsMatches() {
        when(channelRepository.findByProviderId(10L)).thenReturn(List.of(
                sampleDo(1L, 10L, "主渠道", "ACTIVE"),
                sampleDo(2L, 10L, "备渠道", "ACTIVE")));

        List<Channel> result = gateway.findByProviderId(10L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Channel::getName).containsExactly("主渠道", "备渠道");
    }

    @Test
    @DisplayName("findAllActive：按 ACTIVE 状态查询并转换")
    void findAllActive_queriesActiveState() {
        when(channelRepository.findByState("ACTIVE")).thenReturn(List.of(
                sampleDo(1L, 10L, "主渠道", "ACTIVE")));

        List<Channel> result = gateway.findAllActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("主渠道");
        verify(channelRepository).findByState("ACTIVE");
    }

    @Test
    @DisplayName("findAll：全部转换返回")
    void findAll_convertsAll() {
        when(channelRepository.findAll()).thenReturn(List.of(
                sampleDo(1L, 10L, "主渠道", "ACTIVE"),
                sampleDo(2L, 10L, "备渠道", "SUSPENDED")));

        List<Channel> result = gateway.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Channel::getState)
                .containsExactly(ChannelState.ACTIVE, ChannelState.SUSPENDED);
    }

    @Test
    @DisplayName("deleteById：委托 Repository 删除")
    void deleteById_delegates() {
        gateway.deleteById(1L);
        verify(channelRepository).deleteById(1L);
    }

    @Test
    @DisplayName("findByIds：按 id 集合批量查询并转换")
    void findByIds_convertsMatches() {
        when(channelRepository.findByIdIn(List.of(1L, 2L))).thenReturn(List.of(
                sampleDo(1L, 10L, "主渠道", "ACTIVE"),
                sampleDo(2L, 10L, "备渠道", "ACTIVE")));

        List<Channel> result = gateway.findByIds(List.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Channel::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("existsByProviderIdAndName：委托 Repository 判断")
    void existsByProviderIdAndName_returnsRepositoryResult() {
        when(channelRepository.existsByProviderIdAndName(10L, "主渠道")).thenReturn(true);

        assertThat(gateway.existsByProviderIdAndName(10L, "主渠道")).isTrue();
    }

    @Test
    @DisplayName("findByProviderIdAndBillingMode：按供应商+计费模式查询并转换")
    void findByProviderIdAndBillingMode_convertsMatches() {
        ChannelDo subscriptionDo = sampleDo(1L, 10L, "订阅渠道", "ACTIVE");
        subscriptionDo.setBillingMode(BillingMode.SUBSCRIPTION);
        when(channelRepository.findByProviderIdAndBillingMode(10L, BillingMode.SUBSCRIPTION))
                .thenReturn(List.of(subscriptionDo));

        List<Channel> result = gateway.findByProviderIdAndBillingMode(10L, BillingMode.SUBSCRIPTION);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBillingMode()).isEqualTo(BillingMode.SUBSCRIPTION);
    }

    @Test
    @DisplayName("findByProviderIdAndName：命中时转换返回，未命中返回空")
    void findByProviderIdAndName_returnsConvertedOrEmpty() {
        when(channelRepository.findByProviderIdAndName(10L, "主渠道"))
                .thenReturn(Optional.of(sampleDo(1L, 10L, "主渠道", "ACTIVE")));
        when(channelRepository.findByProviderIdAndName(10L, "不存在")).thenReturn(Optional.empty());

        assertThat(gateway.findByProviderIdAndName(10L, "主渠道")).isPresent();
        assertThat(gateway.findByProviderIdAndName(10L, "不存在")).isEmpty();
    }

    @Test
    @DisplayName("count：委托 Repository 统计并原样返回")
    void count_returnsRepositoryCount() {
        when(channelRepository.count()).thenReturn(15L);
        assertThat(gateway.count()).isEqualTo(15L);
    }
}
