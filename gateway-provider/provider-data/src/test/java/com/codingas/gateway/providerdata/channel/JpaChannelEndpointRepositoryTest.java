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
package com.codingas.gateway.providerdata.channel;

import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.codingas.gateway.protocol.Protocol;
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
 * JpaChannelEndpointRepository 单元测试：mock Repository 验证委托与 model↔DO 双向转换
 *
 * <p>覆盖 JpaChannelEndpointRepository 全部 public 方法（save/findById/findByChannelId/
 * findByChannelIdAndProtocol/deleteById）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaChannelEndpointRepository 单元测试")
class ChannelEndpointGatewayImplTest {

    @Mock
    private ChannelEndpointJpaRepository channelEndpointRepository;

    @InjectMocks
    private JpaChannelEndpointRepository gateway;

    private ChannelEndpoint sampleEndpoint(Long id, Long channelId, Protocol protocol) {
        ChannelEndpoint e = new ChannelEndpoint();
        e.setId(id);
        e.setChannelId(channelId);
        e.setProtocol(protocol);
        e.setEndpointUrl("https://api.example.com/v1");
        e.setCreatedBy(10L);
        e.setUpdatedBy(20L);
        e.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        e.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return e;
    }

    private ChannelEndpointDo sampleDo(Long id, Long channelId, Protocol protocol) {
        ChannelEndpointDo doObj = new ChannelEndpointDo();
        doObj.setId(id);
        doObj.setChannelId(channelId);
        doObj.setProtocol(protocol);
        doObj.setEndpointUrl("https://api.example.com/v1");
        doObj.setCreatedBy(10L);
        doObj.setUpdatedBy(20L);
        doObj.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        doObj.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return doObj;
    }

    @Test
    @DisplayName("save：toDo 写字段 + 委托 save + toEntity 读字段（双向转换）")
    void save_convertsBothWaysAndDelegates() {
        ChannelEndpoint endpoint = sampleEndpoint(1L, 10L, Protocol.OPENAI);
        when(channelEndpointRepository.save(any(ChannelEndpointDo.class))).thenAnswer(inv -> inv.getArgument(0));

        ChannelEndpoint result = gateway.save(endpoint);

        // toEntity 读字段（createdAt/updatedAt 由 JPA 审计填充，toDo 不写，save 往返不携带）
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getChannelId()).isEqualTo(10L);
        assertThat(result.getProtocol()).isEqualTo(Protocol.OPENAI);
        assertThat(result.getEndpointUrl()).isEqualTo("https://api.example.com/v1");

        // toDo 写字段
        ArgumentCaptor<ChannelEndpointDo> captor = ArgumentCaptor.forClass(ChannelEndpointDo.class);
        verify(channelEndpointRepository).save(captor.capture());
        ChannelEndpointDo written = captor.getValue();
        assertThat(written.getChannelId()).isEqualTo(10L);
        assertThat(written.getProtocol()).isEqualTo(Protocol.OPENAI);
        assertThat(written.getEndpointUrl()).isEqualTo("https://api.example.com/v1");
        assertThat(written.getCreatedBy()).isEqualTo(10L);
        assertThat(written.getUpdatedBy()).isEqualTo(20L);
    }

    @Test
    @DisplayName("findById：存在时转换返回，不存在返回空")
    void findById_returnsConvertedOrEmpty() {
        when(channelEndpointRepository.findById(1L)).thenReturn(Optional.of(sampleDo(1L, 10L, Protocol.OPENAI)));
        when(channelEndpointRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(gateway.findById(1L)).isPresent()
                .get().extracting(ChannelEndpoint::getProtocol).isEqualTo(Protocol.OPENAI);
        assertThat(gateway.findById(99L)).isEmpty();
    }

    @Test
    @DisplayName("findByChannelId：按渠道查询并转换")
    void findByChannelId_convertsMatches() {
        when(channelEndpointRepository.findByChannelId(10L)).thenReturn(List.of(
                sampleDo(1L, 10L, Protocol.OPENAI),
                sampleDo(2L, 10L, Protocol.ANTHROPIC)));

        List<ChannelEndpoint> result = gateway.findByChannelId(10L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ChannelEndpoint::getProtocol)
                .containsExactly(Protocol.OPENAI, Protocol.ANTHROPIC);
    }

    @Test
    @DisplayName("findByChannelIdAndProtocol：命中时转换返回，未命中返回空")
    void findByChannelIdAndProtocol_returnsConvertedOrEmpty() {
        when(channelEndpointRepository.findByChannelIdAndProtocol(10L, Protocol.OPENAI))
                .thenReturn(Optional.of(sampleDo(1L, 10L, Protocol.OPENAI)));
        when(channelEndpointRepository.findByChannelIdAndProtocol(10L, Protocol.GEMINI))
                .thenReturn(Optional.empty());

        assertThat(gateway.findByChannelIdAndProtocol(10L, Protocol.OPENAI)).isPresent();
        assertThat(gateway.findByChannelIdAndProtocol(10L, Protocol.GEMINI)).isEmpty();
    }

    @Test
    @DisplayName("deleteById：委托 Repository 删除")
    void deleteById_delegates() {
        gateway.deleteById(1L);
        verify(channelEndpointRepository).deleteById(1L);
    }
}
