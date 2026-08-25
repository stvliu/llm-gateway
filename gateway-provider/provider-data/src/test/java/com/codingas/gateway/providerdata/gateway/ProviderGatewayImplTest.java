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

import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.providerdata.dataobject.ProviderDo;
import com.codingas.gateway.providerdata.repository.ProviderRepository;
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
 * ProviderGatewayImpl 单元测试：mock Repository 验证委托与 model↔DO 双向转换
 *
 * <p>覆盖 ProviderGatewayImpl 全部 public 方法（save/findById/findByCode/
 * findAll/count/delete/findByKeyword）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderGatewayImpl 单元测试")
class ProviderGatewayImplTest {

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private ProviderGatewayImpl gateway;

    private Provider sampleProvider(Long id, String code, String name) {
        Provider p = new Provider();
        p.setId(id);
        p.setCode(code);
        p.setName(name);
        p.setLogoUrl("https://example.com/logo.png");
        p.setWebsiteUrl("https://example.com");
        p.setDescription("示例供应商");
        p.setCreatedBy(10L);
        p.setUpdatedBy(20L);
        p.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        p.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return p;
    }

    private ProviderDo sampleDo(Long id, String code, String name) {
        ProviderDo doObj = new ProviderDo();
        doObj.setId(id);
        doObj.setCode(code);
        doObj.setName(name);
        doObj.setLogoUrl("https://example.com/logo.png");
        doObj.setWebsiteUrl("https://example.com");
        doObj.setDescription("示例供应商");
        doObj.setCreatedBy(10L);
        doObj.setUpdatedBy(20L);
        doObj.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        doObj.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return doObj;
    }

    @Test
    @DisplayName("save：toDo 写字段 + 委托 save + toEntity 读字段（双向转换）")
    void save_convertsBothWaysAndDelegates() {
        Provider provider = sampleProvider(1L, "openai", "OpenAI");
        when(providerRepository.save(any(ProviderDo.class))).thenAnswer(inv -> inv.getArgument(0));

        Provider result = gateway.save(provider);

        // toEntity 读字段：save 返回的 DO 转换回实体
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCode()).isEqualTo("openai");
        assertThat(result.getName()).isEqualTo("OpenAI");
        assertThat(result.getLogoUrl()).isEqualTo("https://example.com/logo.png");
        assertThat(result.getWebsiteUrl()).isEqualTo("https://example.com");
        assertThat(result.getDescription()).isEqualTo("示例供应商");
        assertThat(result.getCreatedBy()).isEqualTo(10L);
        assertThat(result.getUpdatedBy()).isEqualTo(20L);

        // toDo 写字段：捕获传给 repository.save 的 DO 逐一断言
        ArgumentCaptor<ProviderDo> captor = ArgumentCaptor.forClass(ProviderDo.class);
        verify(providerRepository).save(captor.capture());
        ProviderDo written = captor.getValue();
        assertThat(written.getId()).isEqualTo(1L);
        assertThat(written.getCode()).isEqualTo("openai");
        assertThat(written.getName()).isEqualTo("OpenAI");
        assertThat(written.getLogoUrl()).isEqualTo("https://example.com/logo.png");
        assertThat(written.getWebsiteUrl()).isEqualTo("https://example.com");
        assertThat(written.getDescription()).isEqualTo("示例供应商");
        assertThat(written.getCreatedBy()).isEqualTo(10L);
        assertThat(written.getUpdatedBy()).isEqualTo(20L);
    }

    @Test
    @DisplayName("findById：存在时转换返回实体")
    void findById_returnsConvertedEntityWhenPresent() {
        when(providerRepository.findById(1L)).thenReturn(Optional.of(sampleDo(1L, "openai", "OpenAI")));

        Optional<Provider> result = gateway.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getCode()).isEqualTo("openai");
        assertThat(result.get().getName()).isEqualTo("OpenAI");
        assertThat(result.get().getLogoUrl()).isEqualTo("https://example.com/logo.png");
        assertThat(result.get().getWebsiteUrl()).isEqualTo("https://example.com");
        assertThat(result.get().getDescription()).isEqualTo("示例供应商");
        assertThat(result.get().getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(result.get().getUpdatedAt()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
    }

    @Test
    @DisplayName("findById：不存在时返回空 Optional")
    void findById_returnsEmptyWhenAbsent() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(gateway.findById(99L)).isEmpty();
    }

    @Test
    @DisplayName("findByCode：命中时转换返回，未命中返回空")
    void findByCode_returnsConvertedOrEmpty() {
        when(providerRepository.findByCode("openai")).thenReturn(Optional.of(sampleDo(1L, "openai", "OpenAI")));
        when(providerRepository.findByCode("unknown")).thenReturn(Optional.empty());

        assertThat(gateway.findByCode("openai")).isPresent()
                .get().extracting(Provider::getCode).isEqualTo("openai");
        assertThat(gateway.findByCode("unknown")).isEmpty();
    }

    @Test
    @DisplayName("findAll：全部转换返回")
    void findAll_convertsAll() {
        when(providerRepository.findAll()).thenReturn(List.of(
                sampleDo(1L, "openai", "OpenAI"),
                sampleDo(2L, "anthropic", "Anthropic")));

        List<Provider> result = gateway.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Provider::getCode)
                .containsExactly("openai", "anthropic");
    }

    @Test
    @DisplayName("count：委托 Repository 统计并原样返回")
    void count_returnsRepositoryCount() {
        when(providerRepository.count()).thenReturn(42L);
        assertThat(gateway.count()).isEqualTo(42L);
    }

    @Test
    @DisplayName("delete：按实体 id 委托 deleteById")
    void delete_delegatesToDeleteById() {
        gateway.delete(sampleProvider(7L, "openai", "OpenAI"));
        verify(providerRepository).deleteById(7L);
    }

    @Test
    @DisplayName("findByKeyword：按 code/name 关键字搜索并转换，未命中返回空列表")
    void findByKeyword_convertsMatchesOrEmpty() {
        when(providerRepository.findByCodeContainingOrNameContaining("ai", "ai"))
                .thenReturn(List.of(sampleDo(1L, "openai", "OpenAI")));
        when(providerRepository.findByCodeContainingOrNameContaining("zzz", "zzz"))
                .thenReturn(List.of());

        List<Provider> matched = gateway.findByKeyword("ai");
        assertThat(matched).hasSize(1);
        assertThat(matched.get(0).getCode()).isEqualTo("openai");

        assertThat(gateway.findByKeyword("zzz")).isEmpty();
    }
}
