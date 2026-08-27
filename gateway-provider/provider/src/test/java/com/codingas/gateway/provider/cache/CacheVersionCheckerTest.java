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
package com.codingas.gateway.provider.cache;

import com.codingas.gateway.provider.channel.ChannelCredentialRepository;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheVersionChecker 测试")
class CacheVersionCheckerTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ChannelCredentialRepository channelCredentialRepository;

    @Mock
    private CacheInvalidationService cacheService;

    private CacheVersionChecker checker;

    @BeforeEach
    void setUp() {
        checker = new CacheVersionChecker(providerRepository, modelRepository, channelCredentialRepository, cacheService);
    }

    @Test
    @DisplayName("初始化基线后首次轮询不触发任何缓存刷新")
    void initVersionsThenCheck_doesNotRefresh() {
        when(providerRepository.getMaxVersion()).thenReturn(5L);
        when(modelRepository.getMaxVersion()).thenReturn(3L);
        when(channelCredentialRepository.getMaxVersion()).thenReturn(2L);

        checker.initVersions();
        checker.checkVersions();

        verify(cacheService, never()).refreshProviders();
        verify(cacheService, never()).refreshModels();
        verify(cacheService, never()).refreshApiKeys();
    }

    @Test
    @DisplayName("Provider 版本递增时刷新 Provider 缓存并更新基线")
    void providerVersionIncreased_refreshesProviders() {
        when(providerRepository.getMaxVersion()).thenReturn(5L, 6L);
        when(modelRepository.getMaxVersion()).thenReturn(3L);
        when(channelCredentialRepository.getMaxVersion()).thenReturn(2L);

        checker.initVersions();
        checker.checkVersions();

        verify(cacheService).refreshProviders();
        verify(cacheService, never()).refreshModels();
        verify(cacheService, never()).refreshApiKeys();
    }

    @Test
    @DisplayName("Model 版本递增时刷新 Model 缓存并更新基线")
    void modelVersionIncreased_refreshesModels() {
        when(providerRepository.getMaxVersion()).thenReturn(5L);
        when(modelRepository.getMaxVersion()).thenReturn(3L, 4L);
        when(channelCredentialRepository.getMaxVersion()).thenReturn(2L);

        checker.initVersions();
        checker.checkVersions();

        verify(cacheService, never()).refreshProviders();
        verify(cacheService).refreshModels();
        verify(cacheService, never()).refreshApiKeys();
    }

    @Test
    @DisplayName("凭据版本递增时刷新凭据缓存并更新基线")
    void credentialVersionIncreased_refreshesApiKeys() {
        when(providerRepository.getMaxVersion()).thenReturn(5L);
        when(modelRepository.getMaxVersion()).thenReturn(3L);
        when(channelCredentialRepository.getMaxVersion()).thenReturn(2L, 3L);

        checker.initVersions();
        checker.checkVersions();

        verify(cacheService, never()).refreshProviders();
        verify(cacheService, never()).refreshModels();
        verify(cacheService).refreshApiKeys();
    }

    @Test
    @DisplayName("多域版本同时递增时分别触发各自缓存刷新")
    void multipleVersionsIncreased_refreshAll() {
        when(providerRepository.getMaxVersion()).thenReturn(5L, 6L);
        when(modelRepository.getMaxVersion()).thenReturn(3L, 4L);
        when(channelCredentialRepository.getMaxVersion()).thenReturn(2L, 3L);

        checker.initVersions();
        checker.checkVersions();

        verify(cacheService).refreshProviders();
        verify(cacheService).refreshModels();
        verify(cacheService).refreshApiKeys();
    }
}
