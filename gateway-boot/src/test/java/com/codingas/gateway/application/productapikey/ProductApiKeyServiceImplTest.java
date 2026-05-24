package com.codingas.gateway.application.productapikey;

import com.codingas.gateway.application.productapikey.dto.ProductApiKeyCreateRequest;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyCreateResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyDetailResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyResponse;
import com.codingas.gateway.application.productapikey.dto.ProductApiKeyUpdateRequest;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
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
    private ChannelCredentialGateway channelCredentialGateway;

    @InjectMocks
    private ProductApiKeyServiceImpl service;

    private static final Long CHANNEL_ID = 10L;
    private static final Long API_KEY_ID = 100L;

    @Test
    void create_success() {
        ChannelCredential saved = createSampleApiKey();
        when(channelCredentialGateway.save(any(ChannelCredential.class))).thenReturn(saved);

        ProductApiKeyCreateRequest request = new ProductApiKeyCreateRequest(
                CHANNEL_ID, "sk-test-api-key-12345", 1, 1, "test-key"
        );
        ProductApiKeyCreateResponse response = service.create(CHANNEL_ID, request);

        assertNotNull(response);
        assertNotNull(response.apiKeyPlain());
        assertEquals(API_KEY_ID, response.id());
        verify(channelCredentialGateway).save(argThat(key ->
                key.getApiKeyPlain() != null && key.getApiKeyPlain().startsWith("sk-")
        ));
    }

    @Test
    void listByProductId_success() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialGateway.findByChannelId(CHANNEL_ID)).thenReturn(List.of(apiKey));

        List<ProductApiKeyResponse> responses = service.listByProductId(CHANNEL_ID);

        assertEquals(1, responses.size());
        assertEquals(API_KEY_ID, responses.get(0).id());
        assertEquals(CHANNEL_ID, responses.get(0).channelId());
    }

    @Test
    void getById_success() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        ProductApiKeyResponse response = service.getById(CHANNEL_ID, API_KEY_ID);

        assertNotNull(response);
        assertEquals(API_KEY_ID, response.id());
        assertEquals("test-key", response.name());
    }

    @Test
    void getById_notFound() {
        when(channelCredentialGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(CHANNEL_ID, API_KEY_ID));
    }

    @Test
    void getDetailById_success() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        ProductApiKeyDetailResponse response = service.getDetailById(CHANNEL_ID, API_KEY_ID);

        assertNotNull(response);
        assertEquals(API_KEY_ID, response.id());
        assertEquals("sk-test-api-key-12345", response.apiKeyPlain());
    }

    @Test
    void getDetailById_notFound() {
        when(channelCredentialGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getDetailById(CHANNEL_ID, API_KEY_ID));
    }

    @Test
    void update_descriptionAndWeight() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
        when(channelCredentialGateway.save(any(ChannelCredential.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductApiKeyUpdateRequest request = new ProductApiKeyUpdateRequest(
                5, 10, null, "updated-description"
        );
        ProductApiKeyResponse response = service.update(CHANNEL_ID, API_KEY_ID, request);

        assertNotNull(response);
        verify(channelCredentialGateway).save(argThat(key ->
                key.getPriority() == 5 && key.getWeight() == 10
        ));
    }

    @Test
    void update_notFound() {
        when(channelCredentialGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());

        ProductApiKeyUpdateRequest request = new ProductApiKeyUpdateRequest(
                null, null, null, "updated"
        );
        assertThrows(ResourceNotFoundException.class, () -> service.update(CHANNEL_ID, API_KEY_ID, request));
    }

    @Test
    void delete_success() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        service.delete(CHANNEL_ID, API_KEY_ID);

        verify(channelCredentialGateway).deleteById(API_KEY_ID);
    }

    @Test
    void delete_notFound() {
        when(channelCredentialGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(CHANNEL_ID, API_KEY_ID));
    }

    private ChannelCredential createSampleApiKey() {
        ChannelCredential apiKey = new ChannelCredential();
        apiKey.setId(API_KEY_ID);
        apiKey.setChannelId(CHANNEL_ID);
        apiKey.setApiKeyPlain("sk-test-api-key-12345");
        apiKey.setApiKeyPrefix("sk-test-");
        apiKey.setName("test-key");
        apiKey.setWeight(1);
        apiKey.setPriority(1);
        apiKey.setState(CredentialState.ACTIVE);
        return apiKey;
    }
}
