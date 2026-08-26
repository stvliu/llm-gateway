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
package com.codingas.gateway.provider.channel;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
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
 * ChannelCredentialManagerImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ChannelCredentialManagerImplTest {

    @Mock
    private ChannelCredentialRepository channelCredentialRepository;

    @InjectMocks
    private ChannelCredentialManagerImpl service;

    private static final Long CHANNEL_ID = 10L;
    private static final Long API_KEY_ID = 100L;

    @Test
    void create_success() {
        ChannelCredential saved = createSampleApiKey();
        when(channelCredentialRepository.save(any(ChannelCredential.class))).thenReturn(saved);

        ChannelCredential request = credential(
                CHANNEL_ID, null, "sk-test-api-key-12345", 1, 1, "test-key"
        );
        ChannelCredential response = service.create(request);

        assertNotNull(response);
        assertNotNull(response.getApiKeyPlain());
        assertEquals(API_KEY_ID, response.getId());
        verify(channelCredentialRepository).save(argThat(key ->
                key.getApiKeyPlain() != null && key.getApiKeyPlain().startsWith("sk-")
        ));
    }

    @Test
    void listByChannelId_success() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialRepository.findByChannelId(CHANNEL_ID)).thenReturn(List.of(apiKey));

        List<ChannelCredential> responses = service.listByChannelId(CHANNEL_ID);

        assertEquals(1, responses.size());
        assertEquals(API_KEY_ID, responses.get(0).getId());
        assertEquals(CHANNEL_ID, responses.get(0).getChannelId());
    }

    @Test
    void getDetailById_success() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        ChannelCredential response = service.getDetailById(CHANNEL_ID, API_KEY_ID);

        assertNotNull(response);
        assertEquals(API_KEY_ID, response.getId());
        assertEquals("sk-test-api-key-12345", response.getApiKeyPlain());
    }

    @Test
    void getDetailById_notFound() {
        when(channelCredentialRepository.findById(API_KEY_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getDetailById(CHANNEL_ID, API_KEY_ID));
    }

    @Test
    void update_descriptionAndWeight() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
        when(channelCredentialRepository.save(any(ChannelCredential.class))).thenAnswer(inv -> inv.getArgument(0));

        ChannelCredential request = credential(
                CHANNEL_ID, API_KEY_ID, null
        , 5, 10, "updated-description");
        ChannelCredential response = service.update(request);

        assertNotNull(response);
        verify(channelCredentialRepository).save(argThat(key ->
                key.getPriority() == 5 && key.getWeight() == 10
        ));
    }

    @Test
    void update_notFound() {
        when(channelCredentialRepository.findById(API_KEY_ID)).thenReturn(Optional.empty());

        ChannelCredential request = credential(
                CHANNEL_ID, API_KEY_ID, null
        , null, null, "updated");
        assertThrows(ResourceNotFoundException.class, () -> service.update(request));
    }

    @Test
    void update_replaceApiKey() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
        when(channelCredentialRepository.save(any(ChannelCredential.class))).thenAnswer(inv -> inv.getArgument(0));

        ChannelCredential request = credential(
                CHANNEL_ID, API_KEY_ID, "new-key-1234567890"
        , null, null, "updated");
        ChannelCredential response = service.update(request);

        assertNotNull(response);
        verify(channelCredentialRepository).save(argThat(key ->
                "new-key-1234567890".equals(key.getApiKeyPlain())
                        && "new-key-".equals(key.getApiKeyPrefix())
        ));
    }

    @Test
    void update_blankApiKey不替换() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
        when(channelCredentialRepository.save(any(ChannelCredential.class))).thenAnswer(inv -> inv.getArgument(0));

        // apiKey 为空白 → 不替换
        ChannelCredential request = credential(
                CHANNEL_ID, API_KEY_ID, "   "
        , 3, 4, "updated");
        ChannelCredential response = service.update(request);

        assertNotNull(response);
        verify(channelCredentialRepository).save(argThat(key ->
                "sk-test-api-key-12345".equals(key.getApiKeyPlain())
                        && key.getPriority() == 3 && key.getWeight() == 4
        ));
    }

    @Test
    void testApiKey_返回成功响应() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        ApiKeyTestResult response = service.testApiKey(CHANNEL_ID, API_KEY_ID);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals(100L, response.getLatency());
        assertEquals("gpt-4o", response.getModelName());
    }

    @Test
    void update_归属不匹配抛出异常() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        ChannelCredential request = credential(
                999L, API_KEY_ID, null
        , null, null, "updated");
        assertThrows(ResourceNotFoundException.class, () -> service.update(request));
    }

    @Test
    void delete_归属不匹配抛出异常() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        assertThrows(ResourceNotFoundException.class,
                () -> service.delete(999L, API_KEY_ID));
    }

    @Test
    void create_短Key前缀取整个Key() {
        ChannelCredential saved = createSampleApiKey();
        when(channelCredentialRepository.save(any(ChannelCredential.class))).thenReturn(saved);

        ChannelCredential request = credential(
                CHANNEL_ID, null, "ab", 1, 1, "short-key"
        );
        service.create(request);

        verify(channelCredentialRepository).save(argThat(key ->
                "ab".equals(key.getApiKeyPrefix()) && "ab".equals(key.getApiKeyPlain())
        ));
    }

    @Test
    void delete_success() {
        ChannelCredential apiKey = createSampleApiKey();
        when(channelCredentialRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

        service.delete(CHANNEL_ID, API_KEY_ID);

        verify(channelCredentialRepository).deleteById(API_KEY_ID);
    }

    @Test
    void delete_notFound() {
        when(channelCredentialRepository.findById(API_KEY_ID)).thenReturn(Optional.empty());
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
        return apiKey;
    }

    /** 构造凭证实体（create：id 为 null；update：null 字段表示不更新） */
    private ChannelCredential credential(Long channelId, Long id, String apiKey, Integer priority,
                                         Integer weight, String name) {
        ChannelCredential credential = new ChannelCredential();
        credential.setChannelId(channelId);
        credential.setId(id);
        credential.setApiKeyPlain(apiKey);
        credential.setPriority(priority);
        credential.setWeight(weight);
        credential.setName(name);
        return credential;
    }
}
