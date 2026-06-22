package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.application.ApplicationService;
import com.codingas.gateway.application.application.dto.ApplicationRequest;
import com.codingas.gateway.application.application.dto.ApplicationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用管理 REST 控制器
 *
 * <p>提供应用聚合根的 CRUD 与渠道授权绑定 API。
 * Application 是权限+行为双聚合根，承载 Key 归属与渠道可见性。</p>
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * 创建应用
     *
     * @param request 创建请求（code/name/description）
     * @return 创建后的应用响应
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@Valid @RequestBody ApplicationRequest request) {
        return applicationService.create(request);
    }

    /**
     * 更新应用
     *
     * @param id      应用 ID
     * @param request 更新请求
     * @return 更新后的应用响应
     */
    @PutMapping("/{id}")
    public ApplicationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRequest request) {
        return applicationService.update(id, request);
    }

    /**
     * 查询应用详情
     *
     * @param id 应用 ID
     * @return 应用响应
     */
    @GetMapping("/{id}")
    public ApplicationResponse getById(@PathVariable Long id) {
        return applicationService.getById(id);
    }

    /**
     * 查询全部应用列表
     *
     * @return 应用响应列表
     */
    @GetMapping
    public List<ApplicationResponse> list() {
        return applicationService.getAll();
    }

    /**
     * 删除应用（级联清理渠道授权关联）
     *
     * @param id 应用 ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        applicationService.delete(id);
    }

    /**
     * 查询应用授权的渠道 ID 列表
     *
     * @param id 应用 ID
     * @return 渠道 ID 列表
     */
    @GetMapping("/{id}/channels")
    public List<Long> listChannels(@PathVariable Long id) {
        return applicationService.listChannelIds(id);
    }

    /**
     * 更新应用渠道授权（先清空旧关联，再批量保存新关联）
     *
     * @param id      应用 ID
     * @param request 渠道授权请求（channelIds）
     */
    @PutMapping("/{id}/channels")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateChannels(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationChannelRequest request) {
        applicationService.updateChannels(id, request.channelIds());
    }

    /**
     * 绑定（或解绑）应用的容灾画像
     *
     * <p>独立绑定端点，REST 语义：PUT /api/v1/applications/{id}/resilience。
     * body 为 {@link ResilienceBindingRequest}，resilienceProfileId 为 null 时解绑。</p>
     *
     * @param id      应用 ID
     * @param request 绑定请求（resilienceProfileId，可空）
     * @return 绑定/解绑后的应用响应
     */
    @PutMapping("/{id}/resilience")
    public ApplicationResponse bindResilienceProfile(
            @PathVariable Long id,
            @RequestBody ResilienceBindingRequest request) {
        return applicationService.bindResilienceProfile(id, request.resilienceProfileId());
    }

    /**
     * 容灾画像绑定请求 DTO（内部 record）
     *
     * <p>resilienceProfileId 为 null 时表示解绑应用与容灾画像的关联。</p>
     *
     * @param resilienceProfileId 容灾画像 ID（可空）
     */
    record ResilienceBindingRequest(Long resilienceProfileId) {
    }
}
