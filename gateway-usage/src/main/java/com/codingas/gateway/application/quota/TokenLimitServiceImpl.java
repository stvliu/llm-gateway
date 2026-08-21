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
package com.codingas.gateway.application.quota;

import com.codingas.gateway.application.quota.dto.TokenLimitCreateRequest;
import com.codingas.gateway.application.quota.dto.TokenLimitQueryRequest;
import com.codingas.gateway.application.quota.dto.TokenLimitResponse;
import com.codingas.gateway.application.quota.dto.TokenLimitUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.model.ModelGateway;
import com.codingas.gateway.provider.vendor.ProviderGateway;
import com.codingas.gateway.domain.usage.entity.TokenLimit;
import com.codingas.gateway.domain.usage.entity.TokenLimit.TokenLimitState;
import com.codingas.gateway.domain.iam.entity.User;
import com.codingas.gateway.domain.quota.gateway.TokenLimitGateway;
import com.codingas.gateway.domain.iam.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Token 限额应用服务实现
 *
 * <p>处理 Token 限额管理的业务逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenLimitServiceImpl implements TokenLimitService {

    private final TokenLimitGateway tokenLimitGateway;
    private final UserGateway userGateway;
    private final ProviderGateway providerGateway;
    private final ModelGateway modelGateway;

    /**
     * 创建 Token 限额
     */
    @Override
    @Transactional
    public TokenLimitResponse create(TokenLimitCreateRequest request) {
        // 查找用户
        User user = userGateway.findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        // 查找提供商（可选）
        Provider provider = null;
        if (request.getProviderId() != null) {
            provider = providerGateway.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider", request.getProviderId()));
        }

        // 查找模型（可选）
        Model model = null;
        if (request.getModelId() != null) {
            model = modelGateway.findById(request.getModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Model", request.getModelId()));
        }

        // 查找切换模型（可选）
        Model switchModel = null;
        if (request.getSwitchModelId() != null) {
            switchModel = modelGateway.findById(request.getSwitchModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Model", request.getSwitchModelId()));
        }

        // 创建限额
        TokenLimit tokenLimit = new TokenLimit();
        tokenLimit.setUser(user);
        tokenLimit.setProvider(provider);
        tokenLimit.setModel(model);
        tokenLimit.setLimitType(request.getLimitType());
        tokenLimit.setMaxTokens(request.getMaxTokens());
        tokenLimit.setUsedTokens(BigDecimal.ZERO);
        tokenLimit.setPeriodType(request.getPeriodType());
        tokenLimit.setPeriodDayOfWeek(request.getPeriodDayOfWeek());
        tokenLimit.setPeriodDayOfMonth(request.getPeriodDayOfMonth());
        tokenLimit.setExceededAction(request.getExceededAction());
        tokenLimit.setSwitchModel(switchModel);
        tokenLimit.setState(TokenLimitState.ACTIVE);

        TokenLimit savedTokenLimit = tokenLimitGateway.save(tokenLimit);
        return toResponse(savedTokenLimit);
    }

    /**
     * 根据 ID 获取 Token 限额
     */
    @Override
    public TokenLimitResponse getById(Long id) {
        TokenLimit tokenLimit = tokenLimitGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));
        return toResponse(tokenLimit);
    }

    /**
     * 查询 Token 限额列表
     */
    @Override
    public PageResponse<TokenLimitResponse> query(TokenLimitQueryRequest request) {
        List<TokenLimit> tokenLimits = tokenLimitGateway.findAll();

        // 过滤
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().toLowerCase();
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getUser().getUsername().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        if (request.getUserId() != null) {
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getUser().getId().equals(request.getUserId()))
                .collect(Collectors.toList());
        }

        if (request.getProviderId() != null) {
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getProvider() != null && t.getProvider().getId().equals(request.getProviderId()))
                .collect(Collectors.toList());
        }

        if (request.getModelId() != null) {
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getModel() != null && t.getModel().getId().equals(request.getModelId()))
                .collect(Collectors.toList());
        }

        if (request.getState() != null) {
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getState() == request.getState())
                .collect(Collectors.toList());
        }

        // 统计
        long total = tokenLimits.size();

        // 分页
        int offset = request.getOffset();
        int limit = request.getLimit();
        List<TokenLimit> pagedTokenLimits = tokenLimits.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        List<TokenLimitResponse> responses = pagedTokenLimits.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return PageResponse.of(responses, request.getPage(), limit, total);
    }

    /**
     * 更新 Token 限额
     */
    @Override
    @Transactional
    public TokenLimitResponse update(Long id, TokenLimitUpdateRequest request) {
        TokenLimit tokenLimit = tokenLimitGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));

        if (request.getMaxTokens() != null) {
            tokenLimit.setMaxTokens(request.getMaxTokens());
        }
        if (request.getPeriodType() != null) {
            tokenLimit.setPeriodType(request.getPeriodType());
        }
        if (request.getPeriodDayOfWeek() != null) {
            tokenLimit.setPeriodDayOfWeek(request.getPeriodDayOfWeek());
        }
        if (request.getPeriodDayOfMonth() != null) {
            tokenLimit.setPeriodDayOfMonth(request.getPeriodDayOfMonth());
        }
        if (request.getExceededAction() != null) {
            tokenLimit.setExceededAction(request.getExceededAction());
        }
        if (request.getSwitchModelId() != null) {
            Model switchModel = modelGateway.findById(request.getSwitchModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Model", request.getSwitchModelId()));
            tokenLimit.setSwitchModel(switchModel);
        }
        if (request.getEnabled() != null) {
            tokenLimit.setState(request.getEnabled() ? TokenLimitState.ACTIVE : TokenLimitState.SUSPENDED);
        }

        return toResponse(tokenLimitGateway.save(tokenLimit));
    }

    /**
     * 删除 Token 限额（软删除）
     */
    @Override
    @Transactional
    public void delete(Long id) {
        TokenLimit tokenLimit = tokenLimitGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));
        tokenLimit.setDeletedAt(Instant.now());
        tokenLimitGateway.save(tokenLimit);
    }

    /**
     * 重置已使用量
     */
    @Override
    @Transactional
    public TokenLimitResponse resetUsage(Long id) {
        TokenLimit tokenLimit = tokenLimitGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));
        tokenLimit.setUsedTokens(BigDecimal.ZERO);
        return toResponse(tokenLimitGateway.save(tokenLimit));
    }

    /**
     * 转换为响应 DTO
     */
    private TokenLimitResponse toResponse(TokenLimit tokenLimit) {
        TokenLimitResponse response = new TokenLimitResponse();
        response.setId(tokenLimit.getId());
        response.setUserId(tokenLimit.getUser().getId());
        response.setUsername(tokenLimit.getUser().getUsername());
        if (tokenLimit.getProvider() != null) {
            response.setProviderId(tokenLimit.getProvider().getId());
            response.setProviderName(tokenLimit.getProvider().getName());
        }
        if (tokenLimit.getModel() != null) {
            response.setModelId(tokenLimit.getModel().getId());
            response.setModelName(tokenLimit.getModel() instanceof Model m ? m.getDisplayName() : null);
        }
        response.setLimitType(tokenLimit.getLimitType());
        response.setMaxTokens(tokenLimit.getMaxTokens());
        response.setUsedTokens(tokenLimit.getUsedTokens());
        if (tokenLimit.getMaxTokens() != null && tokenLimit.getUsedTokens() != null) {
            response.setRemainingTokens(tokenLimit.getMaxTokens().subtract(tokenLimit.getUsedTokens()));
        }
        response.setPeriodType(tokenLimit.getPeriodType());
        response.setPeriodDayOfWeek(tokenLimit.getPeriodDayOfWeek());
        response.setPeriodDayOfMonth(tokenLimit.getPeriodDayOfMonth());
        response.setExceededAction(tokenLimit.getExceededAction());
        if (tokenLimit.getSwitchModel() != null) {
            response.setSwitchModelId(tokenLimit.getSwitchModel().getId());
            response.setSwitchModelName(tokenLimit.getSwitchModel() instanceof Model m ? m.getDisplayName() : null);
        }
        response.setState(tokenLimit.getState());
        response.setEnabled(tokenLimit.getState() == TokenLimitState.ACTIVE);
        response.setCreatedAt(tokenLimit.getCreatedAt());
        response.setUpdatedAt(tokenLimit.getUpdatedAt());
        return response;
    }
}