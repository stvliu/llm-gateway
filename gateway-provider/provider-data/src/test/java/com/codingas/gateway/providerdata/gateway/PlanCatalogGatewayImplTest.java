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

import com.codingas.gateway.provider.catalog.PlanCatalog;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.providerdata.dataobject.PlanCatalogDo;
import com.codingas.gateway.providerdata.repository.PlanCatalogRepository;
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
 * PlanCatalogGatewayImpl 单元测试：mock Repository 验证委托与 model↔DO 双向转换
 *
 * <p>覆盖 PlanCatalogGatewayImpl 全部 public 方法（save/findByPlanCode/existsByPlanCode/
 * findByProviderCode/findAll）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanCatalogGatewayImpl 单元测试")
class PlanCatalogGatewayImplTest {

    @Mock
    private PlanCatalogRepository repository;

    @InjectMocks
    private PlanCatalogGatewayImpl gateway;

    private PlanCatalog sampleCatalog(Long id, String planCode) {
        PlanCatalog c = new PlanCatalog();
        c.setId(id);
        c.setPlanCode(planCode);
        c.setProviderCode("volcengine");
        c.setPlanName("豆包按量套餐");
        c.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        c.setEndpoints("[{\"protocol\":\"OPENAI\",\"url\":\"https://x\"}]");
        c.setPricing("[{\"providerModelId\":\"doubao\",\"inputPrice\":1}]");
        c.setDescription("示例套餐");
        c.setCreatedBy(10L);
        c.setUpdatedBy(20L);
        c.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        c.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return c;
    }

    private PlanCatalogDo sampleDo(Long id, String planCode) {
        PlanCatalogDo doObj = new PlanCatalogDo();
        doObj.setId(id);
        doObj.setPlanCode(planCode);
        doObj.setProviderCode("volcengine");
        doObj.setPlanName("豆包按量套餐");
        doObj.setBillingMode("PAY_AS_YOU_GO");
        doObj.setEndpoints("[{\"protocol\":\"OPENAI\",\"url\":\"https://x\"}]");
        doObj.setPricing("[{\"providerModelId\":\"doubao\",\"inputPrice\":1}]");
        doObj.setDescription("示例套餐");
        doObj.setCreatedBy(10L);
        doObj.setUpdatedBy(20L);
        doObj.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        doObj.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return doObj;
    }

    @Test
    @DisplayName("save：toDo 写字段 + 委托 save + toEntity 读字段（双向转换）")
    void save_convertsBothWaysAndDelegates() {
        PlanCatalog catalog = sampleCatalog(1L, "volcengine_doubao_payg");
        when(repository.save(any(PlanCatalogDo.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanCatalog result = gateway.save(catalog);

        // toEntity 读字段（billingMode 字符串 → 枚举）
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPlanCode()).isEqualTo("volcengine_doubao_payg");
        assertThat(result.getProviderCode()).isEqualTo("volcengine");
        assertThat(result.getPlanName()).isEqualTo("豆包按量套餐");
        assertThat(result.getBillingMode()).isEqualTo(BillingMode.PAY_AS_YOU_GO);
        assertThat(result.getEndpoints()).contains("OPENAI");
        assertThat(result.getPricing()).contains("doubao");
        assertThat(result.getDescription()).isEqualTo("示例套餐");
        // createdAt/updatedAt 由 JPA 审计填充，toDo 不写，save 往返不携带

        // toDo 写字段（billingMode 枚举 → 字符串）
        ArgumentCaptor<PlanCatalogDo> captor = ArgumentCaptor.forClass(PlanCatalogDo.class);
        verify(repository).save(captor.capture());
        PlanCatalogDo written = captor.getValue();
        assertThat(written.getPlanCode()).isEqualTo("volcengine_doubao_payg");
        assertThat(written.getProviderCode()).isEqualTo("volcengine");
        assertThat(written.getPlanName()).isEqualTo("豆包按量套餐");
        assertThat(written.getBillingMode()).isEqualTo("PAY_AS_YOU_GO");
        assertThat(written.getEndpoints()).contains("OPENAI");
        assertThat(written.getPricing()).contains("doubao");
        assertThat(written.getDescription()).isEqualTo("示例套餐");
        assertThat(written.getCreatedBy()).isEqualTo(10L);
        assertThat(written.getUpdatedBy()).isEqualTo(20L);
    }

    @Test
    @DisplayName("save：billingMode 缺省补 PAY_AS_YOU_GO")
    void save_defaultsBillingModeToPayAsYouGo() {
        PlanCatalog catalog = sampleCatalog(2L, "volcengine_doubao_payg");
        catalog.setBillingMode(null);
        when(repository.save(any(PlanCatalogDo.class))).thenAnswer(inv -> inv.getArgument(0));

        gateway.save(catalog);

        ArgumentCaptor<PlanCatalogDo> captor = ArgumentCaptor.forClass(PlanCatalogDo.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBillingMode()).isEqualTo("PAY_AS_YOU_GO");
    }

    @Test
    @DisplayName("findByPlanCode：命中时转换返回，未命中返回空")
    void findByPlanCode_returnsConvertedOrEmpty() {
        when(repository.findByPlanCode("volcengine_doubao_payg"))
                .thenReturn(Optional.of(sampleDo(1L, "volcengine_doubao_payg")));
        when(repository.findByPlanCode("unknown")).thenReturn(Optional.empty());

        assertThat(gateway.findByPlanCode("volcengine_doubao_payg")).isPresent()
                .get().extracting(PlanCatalog::getBillingMode).isEqualTo(BillingMode.PAY_AS_YOU_GO);
        assertThat(gateway.findByPlanCode("unknown")).isEmpty();
    }

    @Test
    @DisplayName("existsByPlanCode：委托 Repository 判断")
    void existsByPlanCode_returnsRepositoryResult() {
        when(repository.existsByPlanCode("volcengine_doubao_payg")).thenReturn(true);

        assertThat(gateway.existsByPlanCode("volcengine_doubao_payg")).isTrue();
    }

    @Test
    @DisplayName("findByProviderCode：按供应商代码查询并转换")
    void findByProviderCode_convertsMatches() {
        when(repository.findByProviderCode("volcengine")).thenReturn(List.of(
                sampleDo(1L, "volcengine_doubao_payg"),
                sampleDo(2L, "volcengine_skylark_payg")));

        List<PlanCatalog> result = gateway.findByProviderCode("volcengine");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PlanCatalog::getPlanCode)
                .containsExactly("volcengine_doubao_payg", "volcengine_skylark_payg");
    }

    @Test
    @DisplayName("findAll：全部转换返回")
    void findAll_convertsAll() {
        when(repository.findAll()).thenReturn(List.of(
                sampleDo(1L, "volcengine_doubao_payg"),
                sampleDo(2L, "volcengine_skylark_payg")));

        List<PlanCatalog> result = gateway.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PlanCatalog::getId).containsExactly(1L, 2L);
    }
}
