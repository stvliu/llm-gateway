/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.audit.gateway;

import com.codingas.gateway.domain.audit.entity.AuditLog;
import com.codingas.gateway.infrastructure.audit.gateway.database.AuditLogRepository;
import com.codingas.gateway.infrastructure.audit.gateway.database.dataobject.AuditLogDo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuditGatewayImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditGatewayImpl 测试")
class AuditGatewayImplTest {

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private AuditGatewayImpl gateway;

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存审计日志成功")
        void save_validEntity_returnsSaved() {
            // given
            AuditLog entity = createTestEntity();
            AuditLogDo savedDo = createTestDo();

            when(repository.save(any())).thenReturn(savedDo);

            // when
            AuditLog result = gateway.save(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAction()).isEqualTo("LOGIN");
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("findByUserId 方法测试")
    class FindByUserIdTests {

        @Test
        @DisplayName("找到用户审计日志返回列表")
        void findByUserId_existingUser_returnsList() {
            // given
            AuditLogDo doEntity = createTestDo();
            when(repository.findByUserId(1L)).thenReturn(List.of(doEntity));

            // when
            List<AuditLog> result = gateway.findByUserId(1L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAction()).isEqualTo("LOGIN");
        }

        @Test
        @DisplayName("用户无审计日志返回空列表")
        void findByUserId_noLogs_returnsEmptyList() {
            // given
            when(repository.findByUserId(999L)).thenReturn(List.of());

            // when
            List<AuditLog> result = gateway.findByUserId(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // Helper methods
    private AuditLog createTestEntity() {
        AuditLog entity = new AuditLog();
        entity.setUserId(1L);
        entity.setAction("LOGIN");
        entity.setResource("/api/auth/login");
        entity.setResult("SUCCESS");
        entity.setIpAddress("192.168.1.1");
        return entity;
    }

    private AuditLogDo createTestDo() {
        AuditLogDo doEntity = new AuditLogDo();
        doEntity.setId(1L);
        doEntity.setUserId(1L);
        doEntity.setAction("LOGIN");
        doEntity.setResource("/api/auth/login");
        doEntity.setResult("SUCCESS");
        doEntity.setIpAddress("192.168.1.1");
        return doEntity;
    }
}
