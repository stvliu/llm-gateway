package com.codingas.gateway.security.authentication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 默认通知服务实现（仅记录日志）
 *
 * <p>在实际生产环境中，应替换为真实的邮件/短信/Webhook通知服务。</p>
 */
@Slf4j
@Service
public class DefaultNotificationService implements NotificationService {

    @Override
    public boolean sendExpirationWarning(String email, String username, String keyCode, String keyName, Instant expiresAt) {
        log.info("【通知演示】API Key 过期提醒 - 收件人: {}, 用户: {}, Key: {} ({}), 过期时间: {}",
            email, username, keyCode, keyName, expiresAt);
        // TODO: 实现真实的邮件/短信/Webhook通知
        return true;
    }
}