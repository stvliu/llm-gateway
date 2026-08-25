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

import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.providerdata.dataobject.ModelDo;
import com.codingas.gateway.providerdata.repository.ModelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ModelGatewayImpl 单元测试：mock Repository 验证委托与 model↔DO 双向转换
 *
 * <p>覆盖 ModelGatewayImpl 全部 public 方法（save/findById/findByModelName/
 * findAll/findByIds/count/delete/findByKeyword/findByCapability）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelGatewayImpl 单元测试")
class ModelGatewayImplTest {

    @Mock
    private ModelRepository modelRepository;

    @InjectMocks
    private ModelGatewayImpl gateway;

    private Model sampleModel(Long id, String modelName) {
        Model m = new Model();
        m.setId(id);
        m.setModelName(modelName);
        m.setDisplayName("DeepSeek V4");
        m.setModelFamily("deepseek");
        m.setContextWindow(128000);
        m.setMaxInputTokens(64000);
        m.setMaxOutputTokens(8192);
        m.setKnowledgeCutoff("2026-06");
        m.setCapabilities(Map.of("chat", true));
        m.setModalities(List.of("text"));
        m.setDeprecatedAt(null);
        m.setScheduledRetiredAt(null);
        m.setDeprecationMessage(null);
        m.setCreatedBy(10L);
        m.setUpdatedBy(20L);
        m.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        m.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return m;
    }

    private ModelDo sampleDo(Long id, String modelName) {
        ModelDo doObj = new ModelDo();
        doObj.setId(id);
        doObj.setModelName(modelName);
        doObj.setDisplayName("DeepSeek V4");
        doObj.setModelFamily("deepseek");
        doObj.setContextWindow(128000);
        doObj.setMaxInputTokens(64000);
        doObj.setMaxOutputTokens(8192);
        doObj.setKnowledgeCutoff("2026-06");
        doObj.setCapabilities(Map.of("chat", true));
        doObj.setModalities(List.of("text"));
        doObj.setDeprecatedAt(null);
        doObj.setScheduledRetiredAt(null);
        doObj.setDeprecationMessage(null);
        doObj.setCreatedBy(10L);
        doObj.setUpdatedBy(20L);
        doObj.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        doObj.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return doObj;
    }

    @Test
    @DisplayName("save：toDo 写字段 + 委托 save + toEntity 读字段（双向转换）")
    void save_convertsBothWaysAndDelegates() {
        Model model = sampleModel(1L, "deepseek-v4");
        when(modelRepository.save(any(ModelDo.class))).thenAnswer(inv -> inv.getArgument(0));

        Model result = gateway.save(model);

        // toEntity 读字段
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getModelName()).isEqualTo("deepseek-v4");
        assertThat(result.getDisplayName()).isEqualTo("DeepSeek V4");
        assertThat(result.getModelFamily()).isEqualTo("deepseek");
        assertThat(result.getContextWindow()).isEqualTo(128000);
        assertThat(result.getMaxInputTokens()).isEqualTo(64000);
        assertThat(result.getMaxOutputTokens()).isEqualTo(8192);
        assertThat(result.getKnowledgeCutoff()).isEqualTo("2026-06");
        assertThat(result.getCapabilities()).containsEntry("chat", true);
        assertThat(result.getModalities()).containsExactly("text");

        // toDo 写字段
        ArgumentCaptor<ModelDo> captor = ArgumentCaptor.forClass(ModelDo.class);
        verify(modelRepository).save(captor.capture());
        ModelDo written = captor.getValue();
        assertThat(written.getModelName()).isEqualTo("deepseek-v4");
        assertThat(written.getDisplayName()).isEqualTo("DeepSeek V4");
        assertThat(written.getModelFamily()).isEqualTo("deepseek");
        assertThat(written.getContextWindow()).isEqualTo(128000);
        assertThat(written.getMaxInputTokens()).isEqualTo(64000);
        assertThat(written.getMaxOutputTokens()).isEqualTo(8192);
        assertThat(written.getKnowledgeCutoff()).isEqualTo("2026-06");
        assertThat(written.getCapabilities()).containsEntry("chat", true);
        assertThat(written.getModalities()).containsExactly("text");
        assertThat(written.getCreatedBy()).isEqualTo(10L);
        assertThat(written.getUpdatedBy()).isEqualTo(20L);
    }

    @Test
    @DisplayName("findById：存在时转换返回，不存在返回空")
    void findById_returnsConvertedOrEmpty() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(sampleDo(1L, "deepseek-v4")));
        when(modelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(gateway.findById(1L)).isPresent()
                .get().extracting(Model::getModelName).isEqualTo("deepseek-v4");
        assertThat(gateway.findById(99L)).isEmpty();
    }

    @Test
    @DisplayName("findByModelName：命中时转换返回，未命中返回空")
    void findByModelName_returnsConvertedOrEmpty() {
        when(modelRepository.findByModelName("deepseek-v4")).thenReturn(Optional.of(sampleDo(1L, "deepseek-v4")));
        when(modelRepository.findByModelName("unknown")).thenReturn(Optional.empty());

        assertThat(gateway.findByModelName("deepseek-v4")).isPresent();
        assertThat(gateway.findByModelName("unknown")).isEmpty();
    }

    @Test
    @DisplayName("findAll：全部转换返回")
    void findAll_convertsAll() {
        when(modelRepository.findAll()).thenReturn(List.of(
                sampleDo(1L, "deepseek-v4"),
                sampleDo(2L, "gpt-4o")));

        assertThat(gateway.findAll()).hasSize(2)
                .extracting(Model::getModelName)
                .containsExactly("deepseek-v4", "gpt-4o");
    }

    @Test
    @DisplayName("findByIds：按 id 集合批量查询并转换")
    void findByIds_convertsMatches() {
        when(modelRepository.findByIdIn(List.of(1L, 2L))).thenReturn(List.of(
                sampleDo(1L, "deepseek-v4"),
                sampleDo(2L, "gpt-4o")));

        List<Model> result = gateway.findByIds(List.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Model::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("count：委托 Repository 统计并原样返回")
    void count_returnsRepositoryCount() {
        when(modelRepository.count()).thenReturn(7L);
        assertThat(gateway.count()).isEqualTo(7L);
    }

    @Test
    @DisplayName("delete：按实体 id 委托 deleteById")
    void delete_delegatesToDeleteById() {
        gateway.delete(sampleModel(3L, "deepseek-v4"));
        verify(modelRepository).deleteById(3L);
    }

    @Test
    @DisplayName("findByKeyword：按 modelName/displayName 关键字搜索并转换")
    void findByKeyword_convertsMatches() {
        when(modelRepository.findByModelNameContainingOrDisplayNameContaining("deep", "deep"))
                .thenReturn(List.of(sampleDo(1L, "deepseek-v4")));

        List<Model> result = gateway.findByKeyword("deep");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getModelName()).isEqualTo("deepseek-v4");
    }

    @Test
    @DisplayName("findByCapability：按能力过滤并转换，未命中返回空列表")
    void findByCapability_convertsMatchesOrEmpty() {
        when(modelRepository.findByCapability("vision")).thenReturn(List.of(sampleDo(1L, "gpt-4o")));
        when(modelRepository.findByCapability("none")).thenReturn(List.of());

        assertThat(gateway.findByCapability("vision")).hasSize(1);
        assertThat(gateway.findByCapability("none")).isEmpty();
    }
}
