/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.iam.auth;

import com.codingas.gateway.iam.auth.AuthenticationService;
import com.codingas.gateway.iam.valueobject.Identity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证应用服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationService authenticationService;

    @Override
    public Identity authenticate(String apiKey, String clientIp) {
        try {
            var result = authenticationService.authenticateUser(apiKey);
            log.info("API Key authenticated: userId={}, credentialId={}, ip={}",
                    result.userId(), result.credentialId(), clientIp);
            return result;
        } catch (Exception e) {
            log.warn("API Key authentication failed: ip={}, reason={}", clientIp, e.getMessage());
            return null;
        }
    }

}
