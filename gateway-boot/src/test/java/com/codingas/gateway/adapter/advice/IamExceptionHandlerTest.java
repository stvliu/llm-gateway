package com.codingas.gateway.adapter.advice;

import com.codingas.gateway.domain.iam.exception.AuthenticationFailedException;
import com.codingas.gateway.domain.iam.exception.ForbiddenException;
import com.codingas.gateway.domain.iam.exception.IamException;
import com.codingas.gateway.domain.iam.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * IAM 子域异常处理器测试
 */
class IamExceptionHandlerTest {

    private final IamExceptionHandler handler = new IamExceptionHandler();

    @Test
    @DisplayName("处理未认证异常 - 返回401")
    void handleUnauthorized() {
        UnauthorizedException e = new UnauthorizedException();
        ResponseEntity<?> response = handler.handleUnauthorized(e);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("处理无权限异常 - 返回403")
    void handleForbidden() {
        ForbiddenException e = new ForbiddenException();
        ResponseEntity<?> response = handler.handleForbidden(e);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("处理认证失败异常 - 返回401")
    void handleAuthenticationFailed() {
        AuthenticationFailedException e = new AuthenticationFailedException();
        ResponseEntity<?> response = handler.handleAuthenticationFailed(e);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("处理 IAM 兜底异常 - 返回403")
    void handleIamException() {
        IamException e = new IamException("IAM_ERROR", "IAM错误");
        ResponseEntity<?> response = handler.handleIamException(e);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}