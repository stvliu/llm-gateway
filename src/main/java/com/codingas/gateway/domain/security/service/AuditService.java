package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.AuditContext;
import com.codingas.gateway.domain.security.entity.AuditLog;
import com.codingas.gateway.domain.security.gateway.AuditGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务
 *
 * <p>记录所有 API 调用日志。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditGateway auditGateway;
    private final SensitiveDataMasker sensitiveDataMasker;

    /**
     * 记录 API 调用
     */
    @Async
    public void logApiCall(AuditContext context) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(context.userId());
            auditLog.setAction(context.action());
            auditLog.setResource(context.resource());
            auditLog.setResult(context.errorMessage() != null ? "FAILURE" : "SUCCESS");
            auditLog.setIpAddress(context.ipAddress());

            auditGateway.save(auditLog);

            log.debug("Audit log saved: action={}, userId={}, traceId={}",
                context.action(), context.userId(), context.traceId());
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * 记录认证成功
     */
    @Async
    public void logAuthSuccess(Long userId, String apiKeyCode, String ipAddress, String traceId) {
        logApiCall(AuditContext.builder()
            .userId(userId)
            .action("AUTH_SUCCESS")
            .resource(apiKeyCode)
            .ipAddress(ipAddress)
            .traceId(traceId)
            .build());
    }

    /**
     * 记录认证失败
     */
    @Async
    public void logAuthFailure(String apiKey, String reason, String ipAddress, String traceId) {
        logApiCall(AuditContext.builder()
            .action("AUTH_FAILURE")
            .resource(maskApiKey(apiKey))
            .errorMessage(reason)
            .ipAddress(ipAddress)
            .traceId(traceId)
            .build());
    }

    /**
     * 记录限流触发
     */
    @Async
    public void logRateLimitExceeded(Long userId, String apiKeyCode, String ipAddress, String traceId) {
        logApiCall(AuditContext.builder()
            .userId(userId)
            .action("RATE_LIMIT_EXCEEDED")
            .resource(apiKeyCode)
            .ipAddress(ipAddress)
            .traceId(traceId)
            .build());
    }

    /**
     * API Key 脱敏（只显示前后4位）
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 10) {
            return "***";
        }
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }
}
