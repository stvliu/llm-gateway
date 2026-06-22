package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.resilience.ResilienceProfileService;
import com.codingas.gateway.application.resilience.dto.ResilienceProfileRequest;
import com.codingas.gateway.application.resilience.dto.ResilienceProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 容灾画像管理 REST 控制器
 *
 * <p>提供容灾画像聚合根的 create/update/get/list API。
 * 不提供 delete：default 画像为系统兜底禁删；其余画像因 Gateway 无 delete 方法遵循既有模式。</p>
 *
 * <p>容灾画像是应用级容灾配置的载体，承载四层容灾栈（L0 Key/L1 Channel/L2 模型/L3 抛错）的开关与参数。
 * 解析链 Application → Global（见 design.md D5）。</p>
 */
@RestController
@RequestMapping("/api/v1/resilience/profiles")
@RequiredArgsConstructor
public class ResilienceProfileController {

    private final ResilienceProfileService resilienceProfileService;

    /**
     * 创建容灾画像
     *
     * @param request 创建请求
     * @return 创建后的画像响应
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResilienceProfileResponse create(@Valid @RequestBody ResilienceProfileRequest request) {
        return resilienceProfileService.create(request);
    }

    /**
     * 更新容灾画像
     *
     * @param id      画像 ID
     * @param request 更新请求
     * @return 更新后的画像响应
     */
    @PutMapping("/{id}")
    public ResilienceProfileResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ResilienceProfileRequest request) {
        return resilienceProfileService.update(id, request);
    }

    /**
     * 查询容灾画像详情
     *
     * @param id 画像 ID
     * @return 画像响应
     */
    @GetMapping("/{id}")
    public ResilienceProfileResponse getById(@PathVariable Long id) {
        return resilienceProfileService.getById(id);
    }

    /**
     * 查询全部容灾画像列表
     *
     * @return 画像响应列表
     */
    @GetMapping
    public List<ResilienceProfileResponse> list() {
        return resilienceProfileService.getAll();
    }
}
