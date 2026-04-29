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
 * ProviderDomainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderDomainService")
class ProviderDomainServiceTest {

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProviderDomainService providerService;

    @Test
    @DisplayName("delete 应将提供商 status 设置为 DELETED")
    void delete_setsStatusToDeleted() {
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setStatus(Provider.ProviderStatus.ACTIVE);

        when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));
        when(providerGateway.save(any(Provider.class))).thenReturn(provider);

        providerService.delete(1L);

        verify(providerGateway).save(any(Provider.class));
        assertThat(provider.getStatus()).isEqualTo(Provider.ProviderStatus.DELETED);
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
    @DisplayName("create status 为 null 应设置为 ACTIVE")
    void create_nullStatus_setsToActive() {
        Provider provider = new Provider();
        provider.setProviderCode("openai");
        provider.setStatus(null);

        when(providerGateway.save(any(Provider.class))).thenReturn(provider);

        Provider result = providerService.create(provider);

        assertThat(result.getStatus()).isEqualTo(Provider.ProviderStatus.ACTIVE);
    }
}
