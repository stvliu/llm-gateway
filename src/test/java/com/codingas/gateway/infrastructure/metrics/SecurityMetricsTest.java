package com.codingas.gateway.infrastructure.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityMetrics 单元测试
 */
@DisplayName("SecurityMetrics")
class SecurityMetricsTest {

    private SecurityMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new SecurityMetrics();
    }

    @Nested
    @DisplayName("认证指标测试")
    class AuthMetricsTest {

        @Test
        @DisplayName("recordAuthSuccess 增加成功计数")
        void recordAuthSuccess_incrementsCount() {
            metrics.recordAuthSuccess();
            metrics.recordAuthSuccess();

            assertThat(metrics.getAuthSuccessCount().get()).isEqualTo(2);
        }

        @Test
        @DisplayName("recordAuthFailure 增加失败计数")
        void recordAuthFailure_incrementsCount() {
            metrics.recordAuthFailure();
            metrics.recordAuthFailure();
            metrics.recordAuthFailure();

            assertThat(metrics.getAuthFailureCount().get()).isEqualTo(3);
        }

        @Test
        @DisplayName("无认证记录时成功率为 1.0")
        void getAuthSuccessRate_noRecords_returnsOne() {
            assertThat(metrics.getAuthSuccessRate()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("仅有成功记录时成功率为 1.0")
        void getAuthSuccessRate_onlySuccess_returnsOne() {
            metrics.recordAuthSuccess();
            metrics.recordAuthSuccess();

            assertThat(metrics.getAuthSuccessRate()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("仅有失败记录时成功率为 0.0")
        void getAuthSuccessRate_onlyFailure_returnsZero() {
            metrics.recordAuthFailure();

            assertThat(metrics.getAuthSuccessRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("混合记录计算正确成功率")
        void getAuthSuccessRate_mixedRecords_calculatesCorrectly() {
            metrics.recordAuthSuccess();
            metrics.recordAuthSuccess();
            metrics.recordAuthSuccess();
            metrics.recordAuthFailure();

            // 3 成功 / 4 总数 = 0.75
            assertThat(metrics.getAuthSuccessRate()).isCloseTo(0.75, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        @DisplayName("recordAuthLatency 正确累加延迟")
        void recordAuthLatency_accumulatesLatency() {
            metrics.recordAuthLatency(100);
            metrics.recordAuthLatency(200);

            assertThat(metrics.getAverageAuthLatency()).isEqualTo(150.0);
        }

        @Test
        @DisplayName("无延迟记录时平均延迟为 0")
        void getAverageAuthLatency_noRecords_returnsZero() {
            assertThat(metrics.getAverageAuthLatency()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("单次延迟记录正确计算平均值")
        void getAverageAuthLatency_singleRecord_returnsLatency() {
            metrics.recordAuthLatency(50);

            assertThat(metrics.getAverageAuthLatency()).isEqualTo(50.0);
        }
    }

    @Nested
    @DisplayName("限流指标测试")
    class RateLimitMetricsTest {

        @Test
        @DisplayName("recordRateLimitExceeded 增加计数")
        void recordRateLimitExceeded_incrementsCount() {
            metrics.recordRateLimitExceeded();
            metrics.recordRateLimitExceeded();
            metrics.recordRateLimitExceeded();

            assertThat(metrics.getRateLimitExceededCount().get()).isEqualTo(3);
        }

        @Test
        @DisplayName("recordRateLimitAllowed 增加计数")
        void recordRateLimitAllowed_incrementsCount() {
            metrics.recordRateLimitAllowed();
            metrics.recordRateLimitAllowed();

            assertThat(metrics.getRateLimitAllowedCount().get()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("暴力破解防护指标测试")
    class BruteForceMetricsTest {

        @Test
        @DisplayName("recordBruteForceBlock 增加总数和当前计数")
        void recordBruteForceBlock_incrementsBothCounts() {
            metrics.recordBruteForceBlock();
            metrics.recordBruteForceBlock();

            assertThat(metrics.getBruteForceBlockCount().get()).isEqualTo(2);
            assertThat(metrics.getCurrentBruteForceBlocked().get()).isEqualTo(2);
        }

        @Test
        @DisplayName("recordBruteForceUnblock 减少当前计数")
        void recordBruteForceUnblock_decrementsCurrentCount() {
            metrics.recordBruteForceBlock();
            metrics.recordBruteForceBlock();
            metrics.recordBruteForceUnblock();

            assertThat(metrics.getCurrentBruteForceBlocked().get()).isEqualTo(1);
            assertThat(metrics.getBruteForceBlockCount().get()).isEqualTo(2); // 总数不变
        }
    }

    @Nested
    @DisplayName("脱敏指标测试")
    class MaskingMetricsTest {

        @Test
        @DisplayName("recordMaskingApplied 增加应用计数")
        void recordMaskingApplied_incrementsCount() {
            metrics.recordMaskingApplied();

            assertThat(metrics.getMaskingAppliedCount().get()).isEqualTo(1);
        }

        @Test
        @DisplayName("recordMaskingHit 增加命中计数")
        void recordMaskingHit_incrementsCount() {
            metrics.recordMaskingHit();
            metrics.recordMaskingHit();
            metrics.recordMaskingHit();

            assertThat(metrics.getMaskingHitCount().get()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("IP 黑名单指标测试")
    class IpBlockMetricsTest {

        @Test
        @DisplayName("recordIpBlocked 增加总数和当前计数")
        void recordIpBlocked_incrementsBothCounts() {
            metrics.recordIpBlocked();

            assertThat(metrics.getIpBlockedCount().get()).isEqualTo(1);
            assertThat(metrics.getCurrentBlockedIps().get()).isEqualTo(1);
        }

        @Test
        @DisplayName("recordIpUnblocked 减少当前计数")
        void recordIpUnblocked_decrementsCurrentCount() {
            metrics.recordIpBlocked();
            metrics.recordIpUnblocked();

            assertThat(metrics.getCurrentBlockedIps().get()).isEqualTo(0);
            assertThat(metrics.getIpBlockedCount().get()).isEqualTo(1); // 总数不变
        }
    }

    @Nested
    @DisplayName("指标快照测试")
    class SnapshotTest {

        @Test
        @DisplayName("getSnapshot 返回完整快照")
        void getSnapshot_returnsCompleteSnapshot() {
            // 设置各项指标
            metrics.recordAuthSuccess();
            metrics.recordAuthFailure();
            metrics.recordAuthLatency(100);
            metrics.recordRateLimitExceeded();
            metrics.recordRateLimitAllowed();
            metrics.recordBruteForceBlock();
            metrics.recordBruteForceUnblock();
            metrics.recordMaskingApplied();
            metrics.recordMaskingHit();
            metrics.recordIpBlocked();
            metrics.recordIpUnblocked();

            var snapshot = metrics.getSnapshot();

            assertThat(snapshot.authSuccess()).isEqualTo(1);
            assertThat(snapshot.authFailure()).isEqualTo(1);
            assertThat(snapshot.authSuccessRate()).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.001));
            assertThat(snapshot.avgAuthLatencyMs()).isEqualTo(100.0);
            assertThat(snapshot.rateLimitExceeded()).isEqualTo(1);
            assertThat(snapshot.rateLimitAllowed()).isEqualTo(1);
            assertThat(snapshot.bruteForceBlocks()).isEqualTo(1);
            assertThat(snapshot.currentBruteForceBlocked()).isEqualTo(0);
            assertThat(snapshot.maskingApplied()).isEqualTo(1);
            assertThat(snapshot.maskingHits()).isEqualTo(1);
            assertThat(snapshot.ipBlocked()).isEqualTo(1);
            assertThat(snapshot.currentBlockedIps()).isEqualTo(0);
        }

        @Test
        @DisplayName("空指标快照返回默认值")
        void getSnapshot_emptyMetrics_returnsDefaults() {
            var snapshot = metrics.getSnapshot();

            assertThat(snapshot.authSuccess()).isEqualTo(0);
            assertThat(snapshot.authFailure()).isEqualTo(0);
            assertThat(snapshot.authSuccessRate()).isEqualTo(1.0);
            assertThat(snapshot.avgAuthLatencyMs()).isEqualTo(0.0);
            assertThat(snapshot.currentBruteForceBlocked()).isEqualTo(0);
            assertThat(snapshot.currentBlockedIps()).isEqualTo(0);
        }
    }
}