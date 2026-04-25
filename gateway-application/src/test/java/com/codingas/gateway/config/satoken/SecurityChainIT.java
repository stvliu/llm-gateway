package com.codingas.gateway.config.satoken;

import com.codingas.gateway.app.GatewayApplication;
import com.codingas.gateway.core.domain.entity.User;
import com.codingas.gateway.security.authentication.AuthenticationService;
import com.codingas.gateway.security.authentication.UserAuthResult;
import com.codingas.gateway.security.interceptor.SecurityInterceptorChain;
import com.codingas.gateway.security.ipblock.IpBlocklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = GatewayApplication.class)
@AutoConfigureMockMvc
@Import({SecurityInterceptorChain.class, IPBlockCheckInterceptor.class, ApiKeyAuthInterceptor.class})
@DisplayName("SecurityInterceptorChain Integration Tests")
class SecurityChainIT {

    @MockBean
    private IpBlocklistService ipBlocklistService;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("封锁IP返回403")
    void blockedIp_returns403() throws Exception {
        when(ipBlocklistService.isBlocked(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/v1/health")
                .header("X-API-Key", "sk-test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("正常IP和有效Key返回200")
    void normalRequest_returns200() throws Exception {
        when(ipBlocklistService.isBlocked(anyString())).thenReturn(false);
        when(authenticationService.authenticate("sk-valid"))
                .thenReturn(new UserAuthResult(1L, "user1", User.UserRole.USER, 1L, "key1"));

        mockMvc.perform(get("/api/v1/health")
                .header("X-API-Key", "sk-valid"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("X-Forwarded-For中的封锁IP被正确识别")
    void xForwardedForBlockedIp_returns403() throws Exception {
        when(ipBlocklistService.isBlocked("192.168.1.100")).thenReturn(true);

        mockMvc.perform(get("/api/v1/health")
                .header("X-Forwarded-For", "192.168.1.100")
                .header("X-API-Key", "sk-test"))
                .andExpect(status().isForbidden());
    }
}