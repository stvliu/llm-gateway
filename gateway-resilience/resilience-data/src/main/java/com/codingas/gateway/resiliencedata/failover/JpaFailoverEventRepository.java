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
package com.codingas.gateway.resiliencedata.failover;

import com.codingas.gateway.resilience.failover.FailoverEvent;
import com.codingas.gateway.resilience.failover.FailoverEventRepository;
import com.codingas.gateway.common.enums.FailoverDecision;
import com.codingas.gateway.common.enums.ProviderErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 转移事件领域网关实现
 *
 * <p>负责 {@link FailoverEvent} 与 {@link FailoverEventDo} 的互转。
 * 审计字段（createdAt/updatedAt/createdBy/updatedBy）由
 * {@link com.codingas.gateway.common.data.BaseDo} 的
 * AuditingEntityListener 自动填充，转换时仅需透传。</p>
 *
 * <p>errorType / decision 字段以字符串存储于 DO（枚举名），读取时还原为
 * {@link ProviderErrorType} / {@link FailoverDecision} 枚举，写入时取枚举名转字符串。</p>
 *
 * <p>查询委派 Repository：{@code int limit} 转为 {@link Pageable}（{@code PageRequest.of(0, limit)}），
 * 排序由 Repository @Query 的 ORDER BY e.occurredAt DESC 保证。</p>
 *
 * <p><b>Task 6 容错</b>：errorType / decision 读取历史残留值（如 Task 4 已删除的 {@code L2}，
 * 或未知 errorType）时不再抛 {@link IllegalArgumentException}，而是回退为安全值
 * （decision→{@link FailoverDecision#NONE}，errorType→{@code null}）并 {@code log.warn} 记录原值，
 * 避免破坏管理后台容灾查询。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaFailoverEventRepository implements FailoverEventRepository {

    private final FailoverEventJpaRepository repository;

    @Override
    public FailoverEvent save(FailoverEvent event) {
        FailoverEventDo dataObject = toDataObject(event);
        FailoverEventDo saved = repository.save(dataObject);
        return toEntity(saved);
    }

    @Override
    public List<FailoverEvent> findRecent(Instant since, Long applicationId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return repository.findRecent(since, applicationId, pageable).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<FailoverEvent> findExhausted(Instant since, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return repository.findExhausted(since, pageable).stream()
                .map(this::toEntity)
                .toList();
    }

    private FailoverEvent toEntity(FailoverEventDo d) {
        FailoverEvent entity = new FailoverEvent();
        entity.setId(d.getId());
        entity.setTraceId(d.getTraceId());
        entity.setApplicationId(d.getApplicationId());
        entity.setFromChannelId(d.getFromChannelId());
        entity.setFromEndpointId(d.getFromEndpointId());
        entity.setToChannelId(d.getToChannelId());
        entity.setToEndpointId(d.getToEndpointId());
        // 枚举以字符串存储，读取时还原；历史残留未知值容错（L2/未知 errorType 不抛异常）
        entity.setErrorType(parseErrorType(d.getErrorType()));
        entity.setDecision(parseDecision(d.getDecision()));
        entity.setExhausted(d.isExhausted());
        entity.setOccurredAt(d.getOccurredAt());
        entity.setCreatedBy(d.getCreatedBy());
        entity.setCreatedAt(d.getCreatedAt());
        entity.setUpdatedBy(d.getUpdatedBy());
        entity.setUpdatedAt(d.getUpdatedAt());
        return entity;
    }

    private FailoverEventDo toDataObject(FailoverEvent entity) {
        FailoverEventDo d = new FailoverEventDo();
        d.setId(entity.getId());
        d.setTraceId(entity.getTraceId());
        d.setApplicationId(entity.getApplicationId());
        d.setFromChannelId(entity.getFromChannelId());
        d.setFromEndpointId(entity.getFromEndpointId());
        d.setToChannelId(entity.getToChannelId());
        d.setToEndpointId(entity.getToEndpointId());
        // 枚举转字符串存储
        d.setErrorType(entity.getErrorType() != null ? entity.getErrorType().name() : null);
        d.setDecision(entity.getDecision() != null ? entity.getDecision().name() : null);
        d.setExhausted(entity.isExhausted());
        d.setOccurredAt(entity.getOccurredAt());
        d.setCreatedBy(entity.getCreatedBy());
        d.setUpdatedBy(entity.getUpdatedBy());
        return d;
    }

    /**
     * 将 errorType 字符串还原为枚举，未知值容错为 null
     *
     * <p>数据库历史行可能残留枚举已删除或未知的 errorType 值，
     * {@link ProviderErrorType#valueOf} 会抛 {@link IllegalArgumentException} 破坏查询。
     * 捕获后回退为 null 并告警，保证管理后台容灾查询可用。</p>
     *
     * @param raw DO 中存储的 errorType 字符串（可空）
     * @return 还原后的枚举；入参为 null 或未知值时返回 null
     */
    private ProviderErrorType parseErrorType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return ProviderErrorType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            log.warn("转移事件 errorType 历史残留未知值，容错为 null: raw={}", raw);
            return null;
        }
    }

    /**
     * 将 decision 字符串还原为枚举，未知值容错为 NONE
     *
     * <p>Task 4 已删除 {@code FailoverDecision.L2} 枚举值，数据库 {@code failover_events}
     * 历史行可能存在 {@code decision='L2'}，{@link FailoverDecision#valueOf("L2")} 会抛
     * {@link IllegalArgumentException} 破坏管理后台容灾查询。捕获后回退为
     * {@link FailoverDecision#NONE} 并告警，记录原值。</p>
     *
     * @param raw DO 中存储的 decision 字符串（可空）
     * @return 还原后的枚举；入参为 null 时返回 null，未知值（如 L2）时返回 NONE
     */
    private FailoverDecision parseDecision(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return FailoverDecision.valueOf(raw);
        } catch (IllegalArgumentException e) {
            log.warn("转移事件 decision 历史残留未知值，容错为 NONE: raw={}", raw);
            return FailoverDecision.NONE;
        }
    }
}
