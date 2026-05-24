package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.productapikey.ProductApiKeyService;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyCreateRequest;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyCreateResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyDetailResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyUpdateRequest;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProductApiKeyController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ProductApiKeyControllerTest {

    @Mock
    private ProductApiKeyService productApiKeyService;

    @InjectMocks
    private ProductApiKeyController controller;

    private static final Long PRODUCT_ID = 100L;

    private ProductApiKeyResponse createResponse(Long id, CredentialState state) {
        return new ProductApiKeyResponse(
                id, PRODUCT_ID, "sk-test-", "test-key", "test key",
                1, 1, state, Instant.now(), Instant.now()
        );
    }

    @Test
    void list_success() {
        when(productApiKeyService.listByProductId(PRODUCT_ID))
                .thenReturn(List.of(createResponse(1L, CredentialState.ACTIVE)));

        List<ProductApiKeyResponse> result = controller.list(PRODUCT_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void get_success() {
        ProductApiKeyDetailResponse detailResponse = new ProductApiKeyDetailResponse(
                1L, PRODUCT_ID, "sk-test-", "sk-test-api-key-12345",
                "test-key", "test key",
                1, 1, CredentialState.ACTIVE, Instant.now(), Instant.now()
        );
        when(productApiKeyService.getDetailById(PRODUCT_ID, 1L)).thenReturn(detailResponse);

        ProductApiKeyDetailResponse result = controller.get(PRODUCT_ID, 1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.apiKeyPlain()).isEqualTo("sk-test-api-key-12345");
    }

    @Test
    void create_success() {
        ProductApiKeyCreateRequest request = new ProductApiKeyCreateRequest(
                PRODUCT_ID, "sk-test-key", 1, 1, "test key"
        );
        ProductApiKeyCreateResponse createResponse = new ProductApiKeyCreateResponse(
                1L, "sk-test-", "sk-test-key"
        );
        when(productApiKeyService.create(eq(PRODUCT_ID), any(ProductApiKeyCreateRequest.class))).thenReturn(createResponse);

        ProductApiKeyCreateResponse result = controller.create(PRODUCT_ID, request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.apiKeyPlain()).isEqualTo("sk-test-key");
    }

    @Test
    void update_success() {
        ProductApiKeyUpdateRequest request = new ProductApiKeyUpdateRequest(
                10, 5, null, null
        );
        when(productApiKeyService.update(eq(PRODUCT_ID), eq(1L), any(ProductApiKeyUpdateRequest.class)))
                .thenReturn(createResponse(1L, CredentialState.ACTIVE));

        ProductApiKeyResponse result = controller.update(PRODUCT_ID, 1L, request);

        assertThat(result).isNotNull();
    }

    @Test
    void delete_success() {
        controller.delete(PRODUCT_ID, 1L);
        verify(productApiKeyService).delete(PRODUCT_ID, 1L);
    }
}