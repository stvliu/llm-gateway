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
package com.codingas.gateway.providerdata.model;

import com.codingas.gateway.provider.model.ModelInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JpaModelInstanceRepository 单元测试：mock Repository 验证委托与 model↔DO 双向转换
 *
 * <p>覆盖 JpaModelInstanceRepository 全部 public 方法（save/findById/findByChannelId/
 * findActiveByChannelId/findActiveByModelIdOrderByPriority/
 * existsByChannelIdAndModelId/saveAll/deleteById）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaModelInstanceRepository 单元测试")
class ModelInstanceGatewayImplTest {

    @Mock
    private ModelInstanceJpaRepository modelInstanceRepository;

    @InjectMocks
    private JpaModelInstanceRepository gateway;

    private ModelInstance sampleInstance(Long id, Long channelId, Long modelId, ModelInstance.State state) {
        ModelInstance mi = new ModelInstance();
        mi.setId(id);
        mi.setChannelId(channelId);
        mi.setModelId(modelId);
        mi.setUpstreamModelName("upstream-" + modelId);
        mi.setCapabilitiesOverride(Map.of("chat", true));
        mi.setContextWindowOverride(64000);
        mi.setPriority(50);
        mi.setWeight(80);
        mi.setQuotaLimit(1_000_000L);
        mi.setState(state);
        mi.setCreatedBy(10L);
        mi.setUpdatedBy(20L);
        return mi;
    }

    private ModelInstanceDo sampleDo(Long id, Long channelId, Long modelId, String state) {
        ModelInstanceDo doObj = new ModelInstanceDo();
        doObj.setId(id);
        doObj.setChannelId(channelId);
        doObj.setModelId(modelId);
        doObj.setUpstreamModelName("upstream-" + modelId);
        doObj.setCapabilitiesOverride(Map.of("chat", true));
        doObj.setContextWindowOverride(64000);
        doObj.setPriority(50);
        doObj.setWeight(80);
        doObj.setQuotaLimit(1_000_000L);
        doObj.setState(state);
        doObj.setCreatedBy(10L);
        doObj.setUpdatedBy(20L);
        return doObj;
    }

    @Test
    @DisplayName("save：toDo 写字段 + 委托 save + toEntity 读字段（双向转换）")
    void save_convertsBothWaysAndDelegates() {
        ModelInstance instance = sampleInstance(1L, 10L, 100L, ModelInstance.State.ACTIVE);
        when(modelInstanceRepository.save(any(ModelInstanceDo.class))).thenAnswer(inv -> inv.getArgument(0));

        ModelInstance result = gateway.save(instance);

        // toEntity 读字段（含 state 枚举转换）
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getChannelId()).isEqualTo(10L);
        assertThat(result.getModelId()).isEqualTo(100L);
        assertThat(result.getUpstreamModelName()).isEqualTo("upstream-100");
        assertThat(result.getCapabilitiesOverride()).containsEntry("chat", true);
        assertThat(result.getContextWindowOverride()).isEqualTo(64000);
        assertThat(result.getPriority()).isEqualTo(50);
        assertThat(result.getWeight()).isEqualTo(80);
        assertThat(result.getQuotaLimit()).isEqualTo(1_000_000L);
        assertThat(result.getState()).isEqualTo(ModelInstance.State.ACTIVE);

        // toDo 写字段（state 转字符串）
        ArgumentCaptor<ModelInstanceDo> captor = ArgumentCaptor.forClass(ModelInstanceDo.class);
        verify(modelInstanceRepository).save(captor.capture());
        ModelInstanceDo written = captor.getValue();
        assertThat(written.getChannelId()).isEqualTo(10L);
        assertThat(written.getModelId()).isEqualTo(100L);
        assertThat(written.getUpstreamModelName()).isEqualTo("upstream-100");
        assertThat(written.getState()).isEqualTo("ACTIVE");
        assertThat(written.getPriority()).isEqualTo(50);
        assertThat(written.getWeight()).isEqualTo(80);
    }

    @Test
    @DisplayName("save：priority/weight 缺省补 100，state 缺省补 ACTIVE")
    void save_appliesDefaultsForPriorityWeightAndState() {
        ModelInstance instance = sampleInstance(2L, 10L, 100L, ModelInstance.State.ACTIVE);
        instance.setPriority(null);
        instance.setWeight(null);
        instance.setState(null);
        when(modelInstanceRepository.save(any(ModelInstanceDo.class))).thenAnswer(inv -> inv.getArgument(0));

        ModelInstance result = gateway.save(instance);

        ArgumentCaptor<ModelInstanceDo> captor = ArgumentCaptor.forClass(ModelInstanceDo.class);
        verify(modelInstanceRepository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo(100);
        assertThat(captor.getValue().getWeight()).isEqualTo(100);
        assertThat(captor.getValue().getState()).isEqualTo("ACTIVE");
        // toEntity 侧同样补默认值
        assertThat(result.getPriority()).isEqualTo(100);
        assertThat(result.getWeight()).isEqualTo(100);
        assertThat(result.getState()).isEqualTo(ModelInstance.State.ACTIVE);
    }

    @Test
    @DisplayName("findById：存在时转换返回，不存在返回空")
    void findById_returnsConvertedOrEmpty() {
        when(modelInstanceRepository.findById(1L)).thenReturn(Optional.of(sampleDo(1L, 10L, 100L, "ACTIVE")));
        when(modelInstanceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(gateway.findById(1L)).isPresent()
                .get().extracting(ModelInstance::getState).isEqualTo(ModelInstance.State.ACTIVE);
        assertThat(gateway.findById(99L)).isEmpty();
    }

    @Test
    @DisplayName("findByChannelId：按渠道查询并转换")
    void findByChannelId_convertsMatches() {
        when(modelInstanceRepository.findByChannelId(10L)).thenReturn(List.of(
                sampleDo(1L, 10L, 100L, "ACTIVE"),
                sampleDo(2L, 10L, 101L, "ACTIVE")));

        List<ModelInstance> result = gateway.findByChannelId(10L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ModelInstance::getModelId).containsExactly(100L, 101L);
    }

    @Test
    @DisplayName("findActiveByChannelId：按渠道+ACTIVE 状态查询并转换")
    void findActiveByChannelId_queriesActiveState() {
        when(modelInstanceRepository.findByChannelIdAndState(10L, "ACTIVE"))
                .thenReturn(List.of(sampleDo(1L, 10L, 100L, "ACTIVE")));

        List<ModelInstance> result = gateway.findActiveByChannelId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getModelId()).isEqualTo(100L);
        verify(modelInstanceRepository).findByChannelIdAndState(10L, "ACTIVE");
    }

    @Test
    @DisplayName("findActiveByModelIdOrderByPriority：按优先级升序查询并转换")
    void findActiveByModelIdOrderByPriority_queriesOrdered() {
        when(modelInstanceRepository.findByModelIdAndStateOrderByPriorityAsc(100L, "ACTIVE"))
                .thenReturn(List.of(sampleDo(1L, 10L, 100L, "ACTIVE")));

        List<ModelInstance> result = gateway.findActiveByModelIdOrderByPriority(100L);

        assertThat(result).hasSize(1);
        verify(modelInstanceRepository).findByModelIdAndStateOrderByPriorityAsc(100L, "ACTIVE");
    }

    @Test
    @DisplayName("findByModelId：同模型多实例（含不同状态）全量转换返回")
    void findByModelId_convertsAllStates() {
        when(modelInstanceRepository.findByModelId(100L)).thenReturn(List.of(
                sampleDo(1L, 10L, 100L, "ACTIVE"),
                sampleDo(2L, 20L, 100L, "DEPRECATED"),
                sampleDo(3L, 30L, 100L, "RETIRED")));

        List<ModelInstance> result = gateway.findByModelId(100L);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ModelInstance::getId).containsExactly(1L, 2L, 3L);
        assertThat(result).extracting(ModelInstance::getState)
                .containsExactly(ModelInstance.State.ACTIVE, ModelInstance.State.DEPRECATED, ModelInstance.State.RETIRED);
        verify(modelInstanceRepository).findByModelId(100L);
    }

    @Test
    @DisplayName("findByModelId：无实例返回空列表")
    void findByModelId_emptyWhenAbsent() {
        when(modelInstanceRepository.findByModelId(999L)).thenReturn(List.of());

        assertThat(gateway.findByModelId(999L)).isEmpty();
        verify(modelInstanceRepository).findByModelId(999L);
    }

    @Test
    @DisplayName("deleteById：委托 Repository 删除")
    void deleteById_delegates() {
        gateway.deleteById(1L);
        verify(modelInstanceRepository).deleteById(1L);
    }

    @Test
    @DisplayName("existsByChannelIdAndModelId：渠道下存在该模型返回 true")
    void existsByChannelIdAndModelId_trueWhenFound() {
        when(modelInstanceRepository.findByChannelId(10L)).thenReturn(List.of(
                sampleDo(1L, 10L, 100L, "ACTIVE")));

        assertThat(gateway.existsByChannelIdAndModelId(10L, 100L)).isTrue();
    }

    @Test
    @DisplayName("existsByChannelIdAndModelId：渠道下不存在该模型返回 false")
    void existsByChannelIdAndModelId_falseWhenAbsent() {
        when(modelInstanceRepository.findByChannelId(10L)).thenReturn(List.of(
                sampleDo(1L, 10L, 100L, "ACTIVE")));

        assertThat(gateway.existsByChannelIdAndModelId(10L, 999L)).isFalse();
        assertThat(gateway.existsByChannelIdAndModelId(99L, 100L)).isFalse();
    }

    @Test
    @DisplayName("saveAll：批量转换 + 批量委托 + 批量读回")
    void saveAll_convertsBothWays() {
        ModelInstance first = sampleInstance(1L, 10L, 100L, ModelInstance.State.ACTIVE);
        ModelInstance second = sampleInstance(2L, 10L, 101L, ModelInstance.State.ACTIVE);
        when(modelInstanceRepository.saveAll(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        List<ModelInstance> result = gateway.saveAll(List.of(first, second));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ModelInstance::getModelId).containsExactly(100L, 101L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ModelInstanceDo>> captor = ArgumentCaptor.forClass(List.class);
        verify(modelInstanceRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).extracting(ModelInstanceDo::getState).containsExactly("ACTIVE", "ACTIVE");
    }
}
