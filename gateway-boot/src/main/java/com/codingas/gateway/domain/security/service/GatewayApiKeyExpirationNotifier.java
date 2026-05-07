package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * API Key 过期提醒服务
 *
 * <p>提前 7 天通知用户即将过期的 API Key。</p>
 * <p>包含：1) 扫描任务执行日志记录；2) 漏发告警机制；3) 手动触发接口。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayApiKeyExpirationNotifier {

    private static final int ADVANCE_DAYS = 7;

    private final ApiKeyGateway apiKeyGateway;
    private final UserGateway userGateway;
    private final NotificationDomainService notificationService;

    /**
     * 定时扫描即将过期的 Key（每天凌晨 2 点执行）
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(readOnly = true)
    public void scanExpiringKeys() {
        log.info("Starting expiration scan for API Keys");

        Instant now = Instant.now();
        Instant warningThreshold = now.plus(ADVANCE_DAYS, ChronoUnit.DAYS);

        // 查找 7 天内即将过期的 Key
        Page<GatewayApiKey> expiringKeys = apiKeyGateway.findExpiringKeys(now, warningThreshold, Pageable.unpaged());

        long sentCount = 0;
        long failedCount = 0;

        for (GatewayApiKey apiKey : expiringKeys) {
            try {
                boolean sent = sendExpirationWarning(apiKey);
                if (sent) {
                    sentCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to send expiration warning for key: {}", apiKey.getId(), e);
                failedCount++;
            }
        }

        log.info("Expiration scan completed: total={}, sent={}, failed={}",
            expiringKeys.getTotalElements(), sentCount, failedCount);

        // 漏发告警机制：记录失败数量
        if (failedCount > 0) {
            log.warn("Expiration notification delivery issues: failed={}", failedCount);
            // 触发告警（可以集成到监控系统）
            alertNotificationFailure(failedCount);
        }
    }

    /**
     * 手动触发过期扫描（用于补发）
     */
    @Transactional(readOnly = true)
    public ScanResult manualScan() {
        log.info("Manual expiration scan triggered");

        Instant now = Instant.now();
        Instant warningThreshold = now.plus(ADVANCE_DAYS, ChronoUnit.DAYS);

        Page<GatewayApiKey> expiringKeys = apiKeyGateway.findExpiringKeys(now, warningThreshold, Pageable.unpaged());

        long sentCount = 0;
        long failedCount = 0;

        for (GatewayApiKey apiKey : expiringKeys) {
            try {
                boolean sent = sendExpirationWarning(apiKey);
                if (sent) {
                    sentCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to send expiration warning for key: {}", apiKey.getId(), e);
                failedCount++;
            }
        }

        return new ScanResult(expiringKeys.getTotalElements(), sentCount, failedCount);
    }

    /**
     * 手动触发单个 Key 的过期提醒
     */
    @Transactional(readOnly = true)
    public boolean manualNotify(Long apiKeyId) {
        GatewayApiKey apiKey = apiKeyGateway.findById(apiKeyId).orElse(null);
        if (apiKey == null) {
            log.warn("API Key not found for manual notification: id={}", apiKeyId);
            return false;
        }

        return sendExpirationWarning(apiKey);
    }

    /**
     * 发送过期警告
     */
    private boolean sendExpirationWarning(GatewayApiKey apiKey) {
        Long userId = apiKey.getUserId();
        if (userId == null) {
            log.warn("User ID not found for API Key: id={}", apiKey.getId());
            return false;
        }

        User user = userGateway.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found for API Key: id={}", apiKey.getId());
            return false;
        }

        // 发送通知（邮件/短信/Webhook）
        return notificationService.sendExpirationWarning(
            user.getEmail(),
            user.getUsername(),
            String.valueOf(apiKey.getId()),
            apiKey.getName(),
            apiKey.getExpiresAt()
        );
    }

    /**
     * 漏发告警
     */
    private void alertNotificationFailure(long failedCount) {
        // 这里可以集成监控系统，如 Prometheus、Slack 等
        log.error("API Key expiration notification failure alert: count={}", failedCount);
    }

    /**
     * 扫描结果
     */
    public record ScanResult(long total, long sent, long failed) {}
}
