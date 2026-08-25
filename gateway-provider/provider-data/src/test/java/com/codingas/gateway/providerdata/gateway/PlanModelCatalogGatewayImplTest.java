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

import com.codingas.gateway.provider.catalog.PlanModelCatalog;
import com.codingas.gateway.providerdata.dataobject.PlanModelCatalogDo;
import com.codingas.gateway.providerdata.repository.PlanModelCatalogRepository;
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
 * PlanModelCatalogGatewayImpl 单元测试：mock Repository 验证委托与 model↔DO 双向转换
 *
 * <p>覆盖 PlanModelCatalogGatewayImpl 全部 public 方法（save/findByPlanCodeAndModelName/
 * findByModelName/findAll）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanModelCatalogGatewayImpl 单元测试")
class PlanModelCatalogGatewayImplTest {

    @Mock
    private PlanModelCatalogRepository repository;

    @InjectMocks
    private PlanModelCatalogGatewayImpl gateway;

    private PlanModelCatalog sampleCatalog(Long id, String planCode, String modelName) {
        PlanModelCatalog c = new PlanModelCatalog();
        c.setId(id);
        c.setPlanCode(planCode);
        c.setModelName(modelName);
        c.setCreatedBy(10L);
        c.setUpdatedBy(20L);
        c.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        c.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return c;
    }

    private PlanModelCatalogDo sampleDo(Long id, String planCode, String modelName) {
        PlanModelCatalogDo doObj = new PlanModelCatalogDo();
        doObj.setId(id);
        doObj.setPlanCode(planCode);
        doObj.setModelName(modelName);
        doObj.setCreatedBy(10L);
        doObj.setUpdatedBy(20L);
        doObj.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        doObj.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return doObj;
    }

    @Test
    @DisplayName("save：toDo 写字段 + 委托 save + toEntity 读字段（双向转换）")
    void save_convertsBothWaysAndDelegates() {
        PlanModelCatalog catalog = sampleCatalog(1L, "plan-a", "deepseek-v4");
        when(repository.save(any(PlanModelCatalogDo.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanModelCatalog result = gateway.save(catalog);

        // toEntity 读字段（createdAt/updatedAt 由 JPA 审计填充，toDo 不写，save 往返不携带）
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPlanCode()).isEqualTo("plan-a");
        assertThat(result.getModelName()).isEqualTo("deepseek-v4");

        // toDo 写字段
        ArgumentCaptor<PlanModelCatalogDo> captor = ArgumentCaptor.forClass(PlanModelCatalogDo.class);
        verify(repository).save(captor.capture());
        PlanModelCatalogDo written = captor.getValue();
        assertThat(written.getPlanCode()).isEqualTo("plan-a");
        assertThat(written.getModelName()).isEqualTo("deepseek-v4");
        assertThat(written.getCreatedBy()).isEqualTo(10L);
        assertThat(written.getUpdatedBy()).isEqualTo(20L);
    }

    @Test
    @DisplayName("findByPlanCodeAndModelName：命中时转换返回，未命中返回空")
    void findByPlanCodeAndModelName_returnsConvertedOrEmpty() {
        when(repository.findByPlanCodeAndModelName("plan-a", "deepseek-v4"))
                .thenReturn(Optional.of(sampleDo(1L, "plan-a", "deepseek-v4")));
        when(repository.findByPlanCodeAndModelName("plan-a", "unknown")).thenReturn(Optional.empty());

        assertThat(gateway.findByPlanCodeAndModelName("plan-a", "deepseek-v4")).isPresent()
                .get().extracting(PlanModelCatalog::getModelName).isEqualTo("deepseek-v4");
        assertThat(gateway.findByPlanCodeAndModelName("plan-a", "unknown")).isEmpty();
    }

    @Test
    @DisplayName("findByModelName：按模型名查询并转换")
    void findByModelName_convertsMatches() {
        when(repository.findByModelName("deepseek-v4")).thenReturn(List.of(
                sampleDo(1L, "plan-a", "deepseek-v4"),
                sampleDo(2L, "plan-b", "deepseek-v4")));

        List<PlanModelCatalog> result = gateway.findByModelName("deepseek-v4");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PlanModelCatalog::getPlanCode)
                .containsExactly("plan-a", "plan-b");
    }

    @Test
    @DisplayName("findAll：全部转换返回")
    void findAll_convertsAll() {
        when(repository.findAll()).thenReturn(List.of(
                sampleDo(1L, "plan-a", "deepseek-v4"),
                sampleDo(2L, "plan-b", "gpt-4o")));

        List<PlanModelCatalog> result = gateway.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PlanModelCatalog::getId).containsExactly(1L, 2L);
    }
}
