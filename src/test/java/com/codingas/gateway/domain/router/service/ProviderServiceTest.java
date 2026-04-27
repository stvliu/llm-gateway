package com.codingas.gateway.domain.router.service;

import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.gateway.ProviderGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProviderService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderService")
class ProviderServiceTest {

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProviderService providerService;

    @Test
    @DisplayName("delete 应将提供商 enabled 设置为 false")
    void delete_setsEnabledToFalse() {
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setEnabled(true);

        when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));
        when(providerGateway.save(any(Provider.class))).thenReturn(provider);

        providerService.delete(1L);

        verify(providerGateway).save(any(Provider.class));
        assertThat(provider.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("delete 不存在的提供商应抛出异常")
    void delete_notFound_throwsException() {
        when(providerGateway.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> providerService.delete(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider not found");
    }

    @Test
    @DisplayName("create enabled 为 null 应设置为 true")
    void create_nullEnabled_setsToTrue() {
        Provider provider = new Provider();
        provider.setProviderCode("openai");
        provider.setEnabled(null);

        when(providerGateway.save(any(Provider.class))).thenReturn(provider);

        Provider result = providerService.create(provider);

        assertThat(result.getEnabled()).isTrue();
    }
}
