package com.codingas.gateway.security.audit;

import com.codingas.gateway.core.domain.entity.AuditLog;
import com.codingas.gateway.core.repository.AuditLogRepository;
import com.codingas.gateway.security.masking.SensitiveDataMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * 审计日志服务
 *
 * <p>记录所有 API 调用日志，支持按用户/时间/操作类型查询。</p>
 * <p>采用滚动策略，保留最近 90 天数据。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final SensitiveDataMasker sensitiveDataMasker;

    /**
     * 记录 API 调用
     */
    @Async
    @Transactional
    public void logApiCall(AuditContext context) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(context.userId());
            auditLog.setAction(context.action());
            auditLog.setResource(context.resource());
            auditLog.setRequestMethod(context.requestMethod());
            auditLog.setRequestPath(context.requestPath());
            auditLog.setRequestBody(maskSensitiveData(context.requestBody()));
            auditLog.setResponseStatus(context.responseStatus());
            auditLog.setResponseTime(context.responseTime());
            auditLog.setTraceId(context.traceId());
            auditLog.setIpAddress(context.ipAddress());
            auditLog.setUserAgent(context.userAgent());
            auditLog.setErrorMessage(context.errorMessage());

            auditLogRepository.save(auditLog);

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
    @Transactional
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
    @Transactional
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
    @Transactional
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
     * 按用户查询审计日志
     */
    public Page<AuditLog> findByUserId(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * 按时间范围查询审计日志
     */
    public Page<AuditLog> findByTimeRange(Instant start, Instant end, Pageable pageable) {
        return auditLogRepository.findByCreatedAtBetween(start, end, pageable);
    }

    /**
     * 按操作类型查询审计日志
     */
    public Page<AuditLog> findByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    /**
     * 按 traceId 查询
     */
    public Optional<AuditLog> findByTraceId(String traceId) {
        return auditLogRepository.findByTraceId(traceId);
    }

    /**
     * 清理过期日志（保留 90 天）
     */
    @Transactional
    public void cleanupExpiredLogs() {
        Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        long deleted = auditLogRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Cleaned up {} expired audit logs (before {})", deleted, cutoff);
    }

    /**
     * 敏感数据脱敏
     */
    private String maskSensitiveData(String text) {
        if (text == null) {
            return null;
        }
        return sensitiveDataMasker.mask(text);
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
