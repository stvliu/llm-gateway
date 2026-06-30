package com.codingas.gateway.infrastructure.resilience.gateway;

import com.codingas.gateway.domain.resilience.entity.FailoverEvent;
import com.codingas.gateway.domain.resilience.gateway.FailoverEventGateway;
import com.codingas.gateway.domain.supply.enums.FailoverDecision;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FailoverEventRepository 集成测试（连 H2）
 *
 * <p>守护 423600a 修复核心点：{@code findRecent} 的 clusterId 过滤基于冗余
 * {@code fromClusterId}/{@code toClusterId} 字段匹配（任一命中即返回）。Mockito 单测
 * （{@link FailoverEventGatewayImplTest}）只验证参数透传，无法验证 Repository @Query 的
 * SQL 过滤逻辑真实生效。本测试连 H2 执行真实 SQL，消除 Mockito 盲区。</p>
 *
 * <p>参照 {@code ChannelHealthRepositoryIT} 的 {@code @SpringBootTest} + H2 范式
 * （application-test.yml：H2 PostgreSQL 兼容模式 + ddl-auto create-drop + flyway disabled，
 * 表由 JPA 实体自动建）。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("FailoverEventRepository 集成测试（H2）")
class FailoverEventRepositoryTest {

    @Autowired
    private FailoverEventGateway failoverEventGateway;

    /**
     * clusterId 过滤命中 fromClusterId：仅返回 fromClusterId 匹配的事件
     *
     * <p>验证 Repository @Query 的 {@code e.fromClusterId = :clusterId} 分支真实生效，
     * 非内存过滤或参数透传假象。</p>
     */
    @Test
    @DisplayName("findRecent clusterId 过滤命中 fromClusterId 字段")
    void findRecent_byClusterId_matchesFromClusterId() {
        FailoverEvent e1 = buildEvent("trace-from-1", 10L, 20L, 100L);
        FailoverEvent e2 = buildEvent("trace-other", 30L, 40L, 999L);
        failoverEventGateway.save(e1);
        failoverEventGateway.save(e2);

        List<FailoverEvent> result = failoverEventGateway.findRecent(null, null, 100L, 100);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTraceId()).isEqualTo("trace-from-1");
        assertThat(result.get(0).getFromClusterId()).isEqualTo(100L);
    }

    /**
     * clusterId 过滤命中 toClusterId：仅返回 toClusterId 匹配的事件
     *
     * <p>验证 Repository @Query 的 {@code e.toClusterId = :clusterId} 分支真实生效。</p>
     */
    @Test
    @DisplayName("findRecent clusterId 过滤命中 toClusterId 字段")
    void findRecent_byClusterId_matchesToClusterId() {
        FailoverEvent e1 = buildEvent("trace-to-1", 10L, 20L, null);
        e1.setToClusterId(200L);
        FailoverEvent e2 = buildEvent("trace-other", 30L, 40L, 999L);
        failoverEventGateway.save(e1);
        failoverEventGateway.save(e2);

        List<FailoverEvent> result = failoverEventGateway.findRecent(null, null, 200L, 100);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTraceId()).isEqualTo("trace-to-1");
        assertThat(result.get(0).getToClusterId()).isEqualTo(200L);
    }

    /**
     * clusterId 过滤：同一事件 fromClusterId 与 toClusterId 不同时，按 fromClusterId 命中
     */
    @Test
    @DisplayName("findRecent clusterId 过滤：事件同时有 from/to clusterId，按 from 命中")
    void findRecent_byClusterId_eventHasBothClusters_matchesFrom() {
        FailoverEvent e = buildEvent("trace-both", 10L, 20L, 100L);
        e.setToClusterId(200L);
        failoverEventGateway.save(e);

        // 按 fromClusterId=100 查，应命中
        List<FailoverEvent> byFrom = failoverEventGateway.findRecent(null, null, 100L, 100);
        assertThat(byFrom).hasSize(1);

        // 按 toClusterId=200 查，应命中
        List<FailoverEvent> byTo = failoverEventGateway.findRecent(null, null, 200L, 100);
        assertThat(byTo).hasSize(1);

        // 按不存在的 clusterId=888 查，应空
        List<FailoverEvent> byNone = failoverEventGateway.findRecent(null, null, 888L, 100);
        assertThat(byNone).isEmpty();
    }

    /**
     * clusterId 为 null（不过滤）：返回全部事件
     */
    @Test
    @DisplayName("findRecent clusterId 为 null 时返回全部（不过滤）")
    void findRecent_nullClusterId_returnsAll() {
        failoverEventGateway.save(buildEvent("trace-a", 10L, 20L, 100L));
        failoverEventGateway.save(buildEvent("trace-b", 30L, 40L, 200L));

        List<FailoverEvent> result = failoverEventGateway.findRecent(null, null, null, 100);

        assertThat(result).hasSize(2);
    }

    /**
     * findExhausted 真实 SQL 过滤：仅返回 exhausted=true 的事件
     */
    @Test
    @DisplayName("findExhausted 仅返回 exhausted=true 的事件")
    void findExhausted_returnsOnlyExhausted() {
        FailoverEvent exhausted = buildEvent("trace-ex", 10L, null, 100L);
        exhausted.setExhausted(true);
        exhausted.setToChannelId(null);
        exhausted.setToEndpointId(null);
        exhausted.setDecision(FailoverDecision.L1);
        FailoverEvent normal = buildEvent("trace-norm", 10L, 20L, 100L);
        normal.setExhausted(false);
        failoverEventGateway.save(exhausted);
        failoverEventGateway.save(normal);

        List<FailoverEvent> result = failoverEventGateway.findExhausted(null, 100);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTraceId()).isEqualTo("trace-ex");
        assertThat(result.get(0).isExhausted()).isTrue();
    }

    /**
     * 构造测试转移事件
     *
     * @param traceId       Trace ID
     * @param fromChannelId 失败渠道 ID
     * @param toChannelId   目标渠道 ID（null 表示耗尽）
     * @param fromClusterId 失败渠道所属故障域 ID
     */
    private FailoverEvent buildEvent(String traceId, Long fromChannelId, Long toChannelId, Long fromClusterId) {
        FailoverEvent e = new FailoverEvent();
        e.setTraceId(traceId);
        e.setApplicationId(7L);
        e.setFromChannelId(fromChannelId);
        e.setFromEndpointId(20L);
        e.setToChannelId(toChannelId);
        e.setToEndpointId(toChannelId != null ? 21L : null);
        e.setFromClusterId(fromClusterId);
        e.setToClusterId(null);
        e.setErrorType(ProviderErrorType.AUTHENTICATION_ERROR);
        e.setDecision(FailoverDecision.L1);
        e.setExhausted(false);
        e.setOccurredAt(Instant.now());
        return e;
    }
}
