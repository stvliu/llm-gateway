package com.codingas.gateway.application.channelcredential;

import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialCreateRequest;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialCreateResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialDetailResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialUpdateRequest;
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
 * ChannelCredentialServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ChannelCredentialServiceImplTest {

    @Mock
    private ChannelCredentialGateway channelCredentialGateway;

    @InjectMocks
    private ChannelCredentialServiceImpl service;

    private static final Long CHANNEL_ID = 10L;
    private static final Long API_KEY_ID = 100L;

    @Test
    void create_success() {
        ChannelCredential saved = createSampleApiKey();
        when(channelCredentialGateway.save(any(ChannelCredential.class))).thenReturn(saved);

        ChannelCredentialCreateRequest request = new ChannelCredentialCreateRequest(
                CHANNEL_ID, "sk-test-api-key-12345", 1, 1, "test-key"
        );
        ChannelCredentialCreateResponse response = service.create(request);

        assertNotNull(response);
        assertNotNull(response.apiKeyPlain());
        assertEquals(API_KEY_ID, response.id());
        verify(channelCredentialGateway).save(argThat(key ->
                key.getApiKeyPlain() != null && key.getApiKeyPlain().startsWith("sk-")
        ));
    }

    @Test
    void listByChannelId_success() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialGateway.findByChannelId(CHANNEL_ID)).thenReturn(List.of(apiKey));

        List<ChannelCredentialResponse> responses = service.listByChannelId(CHANNEL_ID);

        assertEquals(1, responses.size());
        assertEquals(API_KEY_ID, responses.get(0).id());
        assertEquals(CHANNEL_ID, responses.get(0).channelId());
    }

    @Test
    void getById_success() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        ChannelCredentialResponse response = service.getById(CHANNEL_ID, API_KEY_ID);

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

        ChannelCredentialDetailResponse response = service.getDetailById(CHANNEL_ID, API_KEY_ID);

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

        ChannelCredentialUpdateRequest request = new ChannelCredentialUpdateRequest(
                CHANNEL_ID, API_KEY_ID, 5, 10, null, "updated-description", null
        );
        ChannelCredentialResponse response = service.update(request);

        assertNotNull(response);
        verify(channelCredentialGateway).save(argThat(key ->
                key.getPriority() == 5 && key.getWeight() == 10
        ));
    }

    @Test
    void update_notFound() {
        when(channelCredentialGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());

        ChannelCredentialUpdateRequest request = new ChannelCredentialUpdateRequest(
                CHANNEL_ID, API_KEY_ID, null, null, null, "updated", null
        );
        assertThrows(ResourceNotFoundException.class, () -> service.update(request));
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
