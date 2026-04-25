package com.codingas.gateway.auth.authentication;

import java.time.Instant;

/**
 * 通知服务接口（实际实现可以是邮件、短信、Webhook 等）
 */
interface NotificationService {
    boolean sendExpirationWarning(String email, String username, String keyCode, String keyName, Instant expiresAt);
}
