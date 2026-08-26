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
package com.codingas.gateway.usage.tokenlimit;

import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import com.codingas.gateway.usage.tokenlimit.TokenLimit;
import com.codingas.gateway.usage.tokenlimit.TokenLimit.TokenLimitState;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.usage.tokenlimit.TokenLimitRepository;
import com.codingas.gateway.iam.user.UserRepository;
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
public class TokenLimitManagerImpl implements TokenLimitManager {

    private final TokenLimitRepository tokenLimitRepository;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final ModelRepository modelRepository;

    /**
     * 创建 Token 限额
     */
    @Override
    @Transactional
    public TokenLimit create(TokenLimitCreateCommand command) {
        // 查找用户
        User user = userRepository.findById(command.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User", command.userId()));

        // 查找提供商（可选）
        Provider provider = null;
        if (command.providerId() != null) {
            provider = providerRepository.findById(command.providerId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider", command.providerId()));
        }

        // 查找模型（可选）
        Model model = null;
        if (command.modelId() != null) {
            model = modelRepository.findById(command.modelId())
                .orElseThrow(() -> new ResourceNotFoundException("Model", command.modelId()));
        }

        // 查找切换模型（可选）
        Model switchModel = null;
        if (command.switchModelId() != null) {
            switchModel = modelRepository.findById(command.switchModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Model", command.switchModelId()));
        }

        // 创建限额
        TokenLimit tokenLimit = new TokenLimit();
        tokenLimit.setUser(user);
        tokenLimit.setProvider(provider);
        tokenLimit.setModel(model);
        tokenLimit.setLimitType(command.limitType());
        tokenLimit.setMaxTokens(command.maxTokens());
        tokenLimit.setUsedTokens(BigDecimal.ZERO);
        tokenLimit.setPeriodType(command.periodType());
        tokenLimit.setPeriodDayOfWeek(command.periodDayOfWeek());
        tokenLimit.setPeriodDayOfMonth(command.periodDayOfMonth());
        tokenLimit.setExceededAction(command.exceededAction());
        tokenLimit.setSwitchModel(switchModel);
        tokenLimit.setState(TokenLimitState.ACTIVE);

        return tokenLimitRepository.save(tokenLimit);
    }

    /**
     * 根据 ID 获取 Token 限额
     */
    @Override
    public TokenLimit getById(Long id) {
        return tokenLimitRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));
    }

    /**
     * 查询 Token 限额列表
     */
    @Override
    public PageResponse<TokenLimit> query(TokenLimitQuery query) {
        List<TokenLimit> tokenLimits = tokenLimitRepository.findAll();

        // 过滤
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String keyword = query.getKeyword().toLowerCase();
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getUser().getUsername().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        if (query.getUserId() != null) {
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getUser().getId().equals(query.getUserId()))
                .collect(Collectors.toList());
        }

        if (query.getProviderId() != null) {
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getProvider() != null && t.getProvider().getId().equals(query.getProviderId()))
                .collect(Collectors.toList());
        }

        if (query.getModelId() != null) {
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getModel() != null && t.getModel().getId().equals(query.getModelId()))
                .collect(Collectors.toList());
        }

        if (query.getState() != null) {
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getState() == query.getState())
                .collect(Collectors.toList());
        }

        // 统计
        long total = tokenLimits.size();

        // 分页
        int offset = query.getOffset();
        int limit = query.getLimit();
        List<TokenLimit> pagedTokenLimits = tokenLimits.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        return PageResponse.of(pagedTokenLimits, query.getPage(), limit, total);
    }

    /**
     * 更新 Token 限额
     */
    @Override
    @Transactional
    public TokenLimit update(Long id, TokenLimitUpdateCommand command) {
        TokenLimit tokenLimit = tokenLimitRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));

        if (command.maxTokens() != null) {
            tokenLimit.setMaxTokens(command.maxTokens());
        }
        if (command.periodType() != null) {
            tokenLimit.setPeriodType(command.periodType());
        }
        if (command.periodDayOfWeek() != null) {
            tokenLimit.setPeriodDayOfWeek(command.periodDayOfWeek());
        }
        if (command.periodDayOfMonth() != null) {
            tokenLimit.setPeriodDayOfMonth(command.periodDayOfMonth());
        }
        if (command.exceededAction() != null) {
            tokenLimit.setExceededAction(command.exceededAction());
        }
        if (command.switchModelId() != null) {
            Model switchModel = modelRepository.findById(command.switchModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Model", command.switchModelId()));
            tokenLimit.setSwitchModel(switchModel);
        }
        if (command.enabled() != null) {
            tokenLimit.setState(command.enabled() ? TokenLimitState.ACTIVE : TokenLimitState.SUSPENDED);
        }

        return tokenLimitRepository.save(tokenLimit);
    }

    /**
     * 删除 Token 限额（软删除）
     */
    @Override
    @Transactional
    public void delete(Long id) {
        TokenLimit tokenLimit = tokenLimitRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));
        tokenLimit.setDeletedAt(Instant.now());
        tokenLimitRepository.save(tokenLimit);
    }

    /**
     * 重置已使用量
     */
    @Override
    @Transactional
    public TokenLimit resetUsage(Long id) {
        TokenLimit tokenLimit = tokenLimitRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));
        tokenLimit.setUsedTokens(BigDecimal.ZERO);
        return tokenLimitRepository.save(tokenLimit);
    }
}