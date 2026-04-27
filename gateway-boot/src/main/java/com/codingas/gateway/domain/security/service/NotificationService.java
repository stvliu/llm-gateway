package com.codingas.gateway.domain.security.service;

import java.time.Instant;

/**
 * 通知服务接口
 *
 * <p>用于发送 API Key 过期提醒等通知。</p>
 */
public interface NotificationService {

    /**
     * 发送 API Key 过期警告
     *
     * @param email 收件人邮箱
     * @param username 用户名
     * @param keyCode Key 代码
     * @param keyName Key 名称
     * @param expiresAt 过期时间
     * @return 是否发送成功
     */
    boolean sendExpirationWarning(String email, String username, String keyCode, String keyName, Instant expiresAt);
}
