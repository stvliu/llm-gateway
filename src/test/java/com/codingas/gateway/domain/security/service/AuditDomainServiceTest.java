package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.AuditContext;
import com.codingas.gateway.domain.security.entity.AuditLog;
import com.codingas.gateway.domain.security.gateway.AuditGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuditDomainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditDomainService")
class AuditDomainServiceTest {

    @Mock
    private AuditGateway auditGateway;

    @Mock
    private SensitiveDataMasker sensitiveDataMasker;

    @InjectMocks
    private AuditDomainService auditService;

    @Test
    @DisplayName("logAuthSuccess 应保存成功审计日志")
    void logAuthSuccess_savesSuccessLog() {
        auditService.logAuthSuccess(1L, "sk-test12345678", "192.168.1.1", "trace-123");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditGateway).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getAction()).isEqualTo("AUTH_SUCCESS");
        assertThat(saved.getResource()).isEqualTo("sk-test12345678");
        assertThat(saved.getResult()).isEqualTo("SUCCESS");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("logAuthFailure 应保存失败审计日志")
    void logAuthFailure_savesFailureLog() {
        auditService.logAuthFailure("sk-test12345678", "Invalid key", "192.168.1.1", "trace-123");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditGateway).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("AUTH_FAILURE");
        assertThat(saved.getResource()).isEqualTo("sk-t***5678"); // 脱敏后
        assertThat(saved.getResult()).isEqualTo("FAILURE");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("logRateLimitExceeded 应保存限流审计日志")
    void logRateLimitExceeded_savesLog() {
        auditService.logRateLimitExceeded(1L, "sk-test12345678", "192.168.1.1", "trace-123");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditGateway).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("RATE_LIMIT_EXCEEDED");
        assertThat(saved.getResource()).isEqualTo("sk-test12345678");
    }

    @Test
    @DisplayName("maskApiKey 短 key 应返回 ***")
    void maskApiKey_shortKey_returnsMask() {
        // 通过 logAuthFailure 测试 maskApiKey
        auditService.logAuthFailure("sk-123", "Invalid", "192.168.1.1", "trace-123");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditGateway).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getResource()).isEqualTo("***");
    }
}
