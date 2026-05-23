package com.codingas.gateway.application.productapikey;

import com.codingas.gateway.application.productapikey.dto.ProductApiKeyCreateRequest;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyCreateResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyDetailResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyUpdateRequest;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.product.entity.ProductApiKey;
import com.codingas.gateway.domain.product.enums.ProductApiKeyState;
import com.codingas.gateway.domain.product.gateway.ProductApiKeyGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProductApiKeyServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ProductApiKeyServiceImplTest {

    @Mock
    private ProductApiKeyGateway productApiKeyGateway;

    @InjectMocks
    private ProductApiKeyServiceImpl service;

    private static final Long PRODUCT_ID = 10L;
    private static final Long API_KEY_ID = 100L;

    @Test
    void create_success() {
        ProductApiKey saved = createSampleApiKey();
        when(productApiKeyGateway.save(any(ProductApiKey.class))).thenReturn(saved);

        ProductApiKeyCreateRequest request = new ProductApiKeyCreateRequest(
                PRODUCT_ID, "sk-test-api-key-12345", 1, 1, "test-key"
        );
        ProductApiKeyCreateResponse response = service.create(PRODUCT_ID, request);

        assertNotNull(response);
        assertNotNull(response.apiKeyPlain());
        assertEquals(API_KEY_ID, response.id());
        verify(productApiKeyGateway).save(argThat(key ->
                key.getApiKeyPlain() != null && key.getApiKeyPlain().startsWith("sk-")
        ));
    }

    @Test
    void listByProductId_success() {
        ProductApiKey apiKey = createSampleApiKey();
        when(productApiKeyGateway.findByProductId(PRODUCT_ID)).thenReturn(List.of(apiKey));

        List<ProductApiKeyResponse> responses = service.listByProductId(PRODUCT_ID);

        assertEquals(1, responses.size());
        assertEquals(API_KEY_ID, responses.get(0).id());
        assertEquals(PRODUCT_ID, responses.get(0).productId());
    }

    @Test
    void getById_success() {
        ProductApiKey apiKey = createSampleApiKey();
        when(productApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        ProductApiKeyResponse response = service.getById(PRODUCT_ID, API_KEY_ID);

        assertNotNull(response);
        assertEquals(API_KEY_ID, response.id());
        assertEquals("test-key", response.name());
    }

    @Test
    void getById_notFound() {
        when(productApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(PRODUCT_ID, API_KEY_ID));
    }

    @Test
    void getDetailById_success() {
        ProductApiKey apiKey = createSampleApiKey();
        when(productApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        ProductApiKeyDetailResponse response = service.getDetailById(PRODUCT_ID, API_KEY_ID);

        assertNotNull(response);
        assertEquals(API_KEY_ID, response.id());
        assertEquals("sk-test-api-key-12345", response.apiKeyPlain());
    }

    @Test
    void getDetailById_notFound() {
        when(productApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getDetailById(PRODUCT_ID, API_KEY_ID));
    }

    @Test
    void update_descriptionAndWeight() {
        ProductApiKey apiKey = createSampleApiKey();
        when(productApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
        when(productApiKeyGateway.save(any(ProductApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductApiKeyUpdateRequest request = new ProductApiKeyUpdateRequest(
                5, 10, null, "updated-description"
        );
        ProductApiKeyResponse response = service.update(PRODUCT_ID, API_KEY_ID, request);

        assertNotNull(response);
        verify(productApiKeyGateway).save(argThat(key ->
                key.getPriority() == 5 && key.getWeight() == 10
        ));
    }

    @Test
    void update_notFound() {
        when(productApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());

        ProductApiKeyUpdateRequest request = new ProductApiKeyUpdateRequest(
                null, null, null, "updated"
        );
        assertThrows(ResourceNotFoundException.class, () -> service.update(PRODUCT_ID, API_KEY_ID, request));
    }

    @Test
    void delete_success() {
        ProductApiKey apiKey = createSampleApiKey();
        when(productApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        service.delete(PRODUCT_ID, API_KEY_ID);

        verify(productApiKeyGateway).deleteById(API_KEY_ID);
    }

    @Test
    void delete_notFound() {
        when(productApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(PRODUCT_ID, API_KEY_ID));
    }

    private ProductApiKey createSampleApiKey() {
        ProductApiKey apiKey = new ProductApiKey();
        apiKey.setId(API_KEY_ID);
        apiKey.setProductId(PRODUCT_ID);
        apiKey.setApiKeyPlain("sk-test-api-key-12345");
        apiKey.setApiKeyPrefix("sk-test-");
        apiKey.setName("test-key");
        apiKey.setDescription("test-key");
        apiKey.setWeight(1);
        apiKey.setPriority(1);
        apiKey.setState(ProductApiKeyState.ACTIVE);
        return apiKey;
    }
}
