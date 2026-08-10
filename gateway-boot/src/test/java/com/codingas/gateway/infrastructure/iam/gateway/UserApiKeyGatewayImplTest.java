/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.iam.gateway;

import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.service.ApiKeyEncryptionDomainService;
import com.codingas.gateway.infrastructure.iam.gateway.database.dataobject.UserApiKeyDo;
import com.codingas.gateway.infrastructure.iam.gateway.database.repository.UserApiKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserApiKeyGatewayImpl 单元测试
 *
 * <p>聚焦 applicationId 权限锚点字段在 DO↔Entity 转换中的正确传递，
 * 确保权限锚点由应用承载后，持久化层不丢失该字段。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserApiKeyGatewayImpl 测试")
class UserApiKeyGatewayImplTest {

    @Mock
    private UserApiKeyRepository repository;

    @Mock
    private ApiKeyEncryptionDomainService encryptionService;

    @InjectMocks
    private UserApiKeyGatewayImpl gateway;

    @Nested
    @DisplayName("applicationId 映射测试")
    class ApplicationIdMappingTests {

        @Test
        @DisplayName("findById 将 DO 的 applicationId 映射到实体")
        void findById_mapsApplicationId() {
            UserApiKeyDo doEntity = new UserApiKeyDo();
            doEntity.setId(1L);
            doEntity.setUserId(100L);
            doEntity.setApplicationId(7L);
            doEntity.setName("test-key");
            // keyEncrypted 留空，跳过解密路径，无需桩 encryptionService
            when(repository.findById(1L)).thenReturn(Optional.of(doEntity));

            Optional<UserApiKey> result = gateway.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getApplicationId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("save 将实体的 applicationId 映射到 DO")
        void save_mapsApplicationIdToDataObject() {
            UserApiKey entity = new UserApiKey();
            entity.setUserId(100L);
            entity.setApplicationId(7L);
            entity.setName("test-key");
            // keyPlain 留空，跳过 hash/encrypt 计算，避免依赖 encryptionService
            UserApiKeyDo savedDo = new UserApiKeyDo();
            savedDo.setId(1L);
            savedDo.setUserId(100L);
            savedDo.setApplicationId(7L);
            when(repository.save(any())).thenReturn(savedDo);

            gateway.save(entity);

            ArgumentCaptor<UserApiKeyDo> captor = ArgumentCaptor.forClass(UserApiKeyDo.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getApplicationId()).isEqualTo(7L);
        }
    }
}
