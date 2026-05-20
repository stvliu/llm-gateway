package com.codingas.gateway.application.userapikey;

import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.team.entity.UserApiKey;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import com.codingas.gateway.domain.team.gateway.UserApiKeyGateway;
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
 * UserApiKeyServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserApiKeyServiceImplTest {

    @Mock
    private UserApiKeyGateway userApiKeyGateway;

    @InjectMocks
    private UserApiKeyServiceImpl service;

    private static final Long TEAM_ID = 1L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long API_KEY_ID = 100L;

    @Test
    void create_success() {
        UserApiKey saved = createSampleApiKey();
        when(userApiKeyGateway.save(any(UserApiKey.class))).thenReturn(saved);

        UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                TEAM_ID, PRODUCT_ID, "test-key", List.of("gpt-4o"), 100000L
        );
        UserApiKeyCreateResponse response = service.create(request);

        assertNotNull(response);
        assertNotNull(response.apiKeyPlain());
        assertTrue(response.apiKeyPlain().startsWith("sk-"));
        assertEquals(API_KEY_ID, response.id());
        verify(userApiKeyGateway).save(argThat(key ->
                key.getKeyPlain() != null && key.getKeyPlain().startsWith("sk-")
        ));
    }

    @Test
    void listByTeamId_success() {
        UserApiKey apiKey = createSampleApiKey();
        when(userApiKeyGateway.findByTeamId(TEAM_ID)).thenReturn(List.of(apiKey));

        List<UserApiKeyResponse> responses = service.listByTeamId(TEAM_ID);

        assertEquals(1, responses.size());
        assertEquals(API_KEY_ID, responses.get(0).id());
        assertEquals(TEAM_ID, responses.get(0).teamId());
    }

    @Test
    void getById_success() {
        UserApiKey apiKey = createSampleApiKey();
        when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        UserApiKeyResponse response = service.getById(API_KEY_ID);

        assertNotNull(response);
        assertEquals(API_KEY_ID, response.id());
        assertEquals("test-key", response.name());
    }

    @Test
    void getById_notFound() {
        when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(API_KEY_ID));
    }

    @Test
    void getDetailById_success() {
        UserApiKey apiKey = createSampleApiKey();
        when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        UserApiKeyDetailResponse response = service.getDetailById(API_KEY_ID);

        assertNotNull(response);
        assertEquals(API_KEY_ID, response.id());
        assertEquals("sk-abc1xxxxx", response.keyPlain());
        assertEquals("test-key", response.name());
    }

    @Test
    void getDetailById_notFound() {
        when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getDetailById(API_KEY_ID));
    }

    @Test
    void update_nameAndModels() {
        UserApiKey apiKey = createSampleApiKey();
        when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
        when(userApiKeyGateway.save(any(UserApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(
                "updated-name", List.of("claude-3-5-sonnet"), null, null
        );
        UserApiKeyResponse response = service.update(API_KEY_ID, request);

        assertNotNull(response);
        verify(userApiKeyGateway).save(argThat(key ->
                "updated-name".equals(key.getName()) &&
                key.getModels().equals(List.of("claude-3-5-sonnet"))
        ));
    }

    @Test
    void update_notFound() {
        when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());

        UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(
                "updated", null, null, null
        );
        assertThrows(ResourceNotFoundException.class, () -> service.update(API_KEY_ID, request));
    }

    @Test
    void delete_success() {
        UserApiKey apiKey = createSampleApiKey();
        when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        service.delete(API_KEY_ID);

        verify(userApiKeyGateway).deleteById(API_KEY_ID);
    }

    @Test
    void delete_notFound() {
        when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(API_KEY_ID));
    }

    private UserApiKey createSampleApiKey() {
        UserApiKey apiKey = new UserApiKey();
        apiKey.setId(API_KEY_ID);
        apiKey.setTeamId(TEAM_ID);
        apiKey.setProductId(PRODUCT_ID);
        apiKey.setKeyPlain("sk-abc1xxxxx");
        apiKey.setKeyPrefix("sk-abc1");
        apiKey.setName("test-key");
        apiKey.setModels(List.of("gpt-4o"));
        apiKey.setQuotaLimit(100000L);
        apiKey.setState(UserApiKeyState.ACTIVE);
        return apiKey;
    }
}