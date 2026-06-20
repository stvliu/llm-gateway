package com.codingas.gateway.application.resilience;

import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.application.entity.Application;
import com.codingas.gateway.domain.application.gateway.ApplicationGateway;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.resilience.gateway.ResilienceProfileGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 容灾画像解析器
 *
 * <p>实现解析链 Application → Global（见 design.md D5）：
 * <ol>
 *   <li>按 applicationId 查 Application；不存在则 fail-fast 抛
 *       {@link GatewayRequestException}（APPLICATION_NOT_FOUND）。</li>
 *   <li>Application 挂 resilienceProfileId 时，按 ID 查画像；命中则返回应用级画像。</li>
 *   <li>Application 未挂画像、或画像已被删（findById 返回 null）时，
 *       回退全局 default 画像（findByCode("default")）。</li>
 *   <li>default 画像不存在时 fail-fast 抛 {@link GatewayRequestException}
 *       （RESILIENCE_DEFAULT_PROFILE_MISSING），暴露系统初始化异常。</li>
 * </ol>
 *
 * <p>预设档位（default/strict/aggressive/batch）由初始化数据写入（Task 4.4 seed），
 * 运行期 default 画像应始终存在；缺失即视为系统未正确初始化。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilienceResolver {

    /** 全局 default 画像编码 */
    private static final String DEFAULT_PROFILE_CODE = "default";

    private final ApplicationGateway applicationGateway;
    private final ResilienceProfileGateway resilienceProfileGateway;

    /**
     * 解析应用对应的容灾画像
     *
     * <p>解析链：Application 画像优先；无画像或画像被删时回退全局 default 画像。
     * Application 不存在或 default 画像缺失时 fail-fast 抛异常。</p>
     *
     * @param applicationId 应用 ID
     * @return 命中的容灾画像（应用级或全局 default）
     * @throws GatewayRequestException Application 不存在（APPLICATION_NOT_FOUND）
     *                                 或 default 画像缺失（RESILIENCE_DEFAULT_PROFILE_MISSING）
     */
    public ResilienceProfile resolve(Long applicationId) {
        // 1. 查 Application；不存在即数据完整性异常，fail-fast
        Application application = applicationGateway.findById(applicationId);
        if (application == null) {
            throw new GatewayRequestException("APPLICATION_NOT_FOUND",
                    "应用不存在: " + applicationId);
        }

        // 2. Application 挂画像时优先返回应用级画像
        Long profileId = application.getResilienceProfileId();
        if (profileId != null) {
            ResilienceProfile appProfile = resilienceProfileGateway.findById(profileId);
            if (appProfile != null) {
                log.debug("命中应用级画像: applicationId={}, profileId={}", applicationId, profileId);
                return appProfile;
            }
            // 画像被删，回退 default（不抛异常，保持解析链鲁棒性）
            log.warn("应用画像已被删除，回退 default: applicationId={}, profileId={}", applicationId, profileId);
        }

        // 3. 回退全局 default 画像
        ResilienceProfile defaultProfile = resilienceProfileGateway.findByCode(DEFAULT_PROFILE_CODE);
        if (defaultProfile == null) {
            // default 画像缺失即系统未正确初始化，fail-fast 暴露问题
            throw new GatewayRequestException("RESILIENCE_DEFAULT_PROFILE_MISSING",
                    "全局 default 容灾画像不存在，请检查初始化数据: code=" + DEFAULT_PROFILE_CODE);
        }
        log.debug("回退全局 default 画像: applicationId={}", applicationId);
        return defaultProfile;
    }
}
