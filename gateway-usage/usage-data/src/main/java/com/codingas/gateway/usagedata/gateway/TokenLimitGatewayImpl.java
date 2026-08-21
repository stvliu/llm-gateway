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
package com.codingas.gateway.usagedata.gateway;

import com.codingas.gateway.usage.tokenlimit.TokenLimit;
import com.codingas.gateway.usage.tokenlimit.TokenLimitGateway;
import com.codingas.gateway.usagedata.dataobject.TokenLimitDo;
import com.codingas.gateway.usagedata.repository.TokenLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Token 限额网关实现
 *
 * <p>实现 TokenLimitGateway 接口，负责 DO ↔ Entity 转换。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenLimitGatewayImpl implements TokenLimitGateway {

    private final TokenLimitRepository tokenLimitRepository;

    @Override
    public TokenLimit save(TokenLimit tokenLimit) {
        TokenLimitDo doEntity = toDo(tokenLimit);
        TokenLimitDo saved = tokenLimitRepository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<TokenLimit> findById(Long id) {
        return tokenLimitRepository.findById(id).map(this::toEntity);
    }

    @Override
    public List<TokenLimit> findByUserId(Long userId) {
        return tokenLimitRepository.findByUserId(userId).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<TokenLimit> findAll() {
        return tokenLimitRepository.findAll().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return tokenLimitRepository.count();
    }

    @Override
    public void delete(TokenLimit tokenLimit) {
        tokenLimitRepository.delete(toDo(tokenLimit));
    }

    @Override
    public void deductUsage(Long userId, Long inputTokens, Long outputTokens) {
        tokenLimitRepository.findAll().stream()
                .filter(t -> t.getUserId() != null && t.getUserId().equals(userId))
                .findFirst()
                .ifPresent(t -> {
                    BigDecimal currentUsed = t.getUsedTokens() != null ? t.getUsedTokens() : BigDecimal.ZERO;
                    t.setUsedTokens(currentUsed.add(BigDecimal.valueOf(inputTokens + outputTokens)));
                    tokenLimitRepository.save(t);
                });
    }

    /**
     * DO 转 Entity
     */
    private TokenLimit toEntity(TokenLimitDo doEntity) {
        if (doEntity == null) {
            return null;
        }
        TokenLimit entity = new TokenLimit();
        entity.setId(doEntity.getId());
        entity.setMaxTokens(doEntity.getMaxTokens());
        entity.setUsedTokens(doEntity.getUsedTokens());
        entity.setPeriodDayOfWeek(doEntity.getPeriodDayOfWeek());
        entity.setPeriodDayOfMonth(doEntity.getPeriodDayOfMonth());
        entity.setDeletedAt(doEntity.getDeletedAt());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        // 枚举转换 - DO使用公共枚举，直接赋值
        entity.setLimitType(doEntity.getLimitType() != null ?
            TokenLimit.LimitType.valueOf(doEntity.getLimitType().name()) : null);
        entity.setPeriodType(doEntity.getPeriodType());
        entity.setExceededAction(doEntity.getExceededAction());
        // 枚举转换 - Entity使用内部枚举，需要转换
        if (doEntity.getState() != null) {
            entity.setState(TokenLimit.TokenLimitState.valueOf(doEntity.getState().name()));
        }
        // User/Provider/Model/SwitchModel 关联暂不处理，由调用方通过相应 Gateway 获取
        return entity;
    }

    /**
     * Entity 转 DO
     */
    private TokenLimitDo toDo(TokenLimit entity) {
        if (entity == null) {
            return null;
        }
        TokenLimitDo doEntity = new TokenLimitDo();
        if (entity.getId() != null) {
            doEntity.setId(entity.getId());
        }
        doEntity.setMaxTokens(entity.getMaxTokens());
        doEntity.setUsedTokens(entity.getUsedTokens());
        doEntity.setPeriodDayOfWeek(entity.getPeriodDayOfWeek());
        doEntity.setPeriodDayOfMonth(entity.getPeriodDayOfMonth());
        doEntity.setDeletedAt(entity.getDeletedAt());
        // 枚举转换 - Entity使用内部枚举，需要转换
        if (entity.getLimitType() != null) {
            doEntity.setLimitType(TokenLimitDo.LimitType.valueOf(entity.getLimitType().name()));
        }
        // 枚举转换 - DO使用公共枚举，直接赋值
        doEntity.setPeriodType(entity.getPeriodType());
        doEntity.setExceededAction(entity.getExceededAction());
        if (entity.getState() != null) {
            doEntity.setState(TokenLimitDo.TokenLimitStatus.valueOf(entity.getState().name()));
        }
        return doEntity;
    }
}