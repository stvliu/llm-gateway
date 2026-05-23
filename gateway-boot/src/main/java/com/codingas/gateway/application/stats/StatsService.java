package com.codingas.gateway.application.stats;

import com.codingas.gateway.application.stats.dto.StatsResponse;
import com.codingas.gateway.infrastructure.model.gateway.database.ModelRepository;
import com.codingas.gateway.infrastructure.model.gateway.database.ProviderRepository;
import com.codingas.gateway.infrastructure.iam.gateway.database.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 统计服务
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final ProviderRepository providerRepository;
    private final ModelRepository modelRepository;
    private final UserRepository userRepository;

    /**
     * 获取系统统计数据
     */
    @Transactional(readOnly = true)
    public StatsResponse getStats() {
        long providerCount = providerRepository.count();
        long modelCount = modelRepository.count();
        long userCount = userRepository.count();

        // TODO: 接入真实的请求统计和 Token 用量数据
        // 目前返回模拟数据，后续需要从审计日志或时序数据库中统计
        long todayRequests = 0;
        String tokenUsage = "0";

        return new StatsResponse(
            providerCount,
            modelCount,
            userCount,
            todayRequests,
            tokenUsage
        );
    }
}
