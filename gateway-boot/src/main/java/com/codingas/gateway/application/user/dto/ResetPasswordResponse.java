/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.user.dto;

/**
 * 重置密码响应（一次性返回明文，不持久化）
 *
 * @param newPassword 新密码明文（HTTPS 传输，仅本次返回）
 */
public record ResetPasswordResponse(String newPassword) {
}
