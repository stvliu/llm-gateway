package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.provider.dto.ProviderCreateRequest;
import com.codingas.gateway.application.provider.dto.ProviderKeysResponse;
import com.codingas.gateway.application.provider.dto.ProviderQueryRequest;
import com.codingas.gateway.application.provider.dto.ProviderResponse;
import com.codingas.gateway.application.provider.dto.ProviderUpdateRequest;
import com.codingas.gateway.application.provider.ProviderService;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.domain.model.enums.ProviderType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 提供商管理控制器
 *
 * <p>提供提供商 CRUD 操作的 REST API 端点。</p>
 */
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;

    /**
     * 获取支持的供应商类型列表
     *
     * @return 供应商类型列表，包含类型代码和显示名称
     */
    @GetMapping("/types")
    public List<Map<String, String>> getProviderTypes() {
        return Arrays.stream(ProviderType.values())
                .map(type -> Map.of(
                        "value", type.name(),
                        "label", getProviderTypeLabel(type)
                ))
                .toList();
    }

    /**
     * 获取供应商类型的显示名称
     */
    private String getProviderTypeLabel(ProviderType type) {
        return switch (type) {
            case OPENAI -> "OpenAI";
            case ANTHROPIC -> "Anthropic";
            case GEMINI -> "Google Gemini";
            case DEEPSEEK -> "DeepSeek";
            case MOONSHOT -> "Moonshot";
            case ZHIPU -> "智谱 GLM";
            case BAICHUAN -> "百川智能";
            case MINIMAX -> "MiniMax";
            case VOLCENGINE -> "火山引擎";
            case QWEN -> "通义千问";
            case WENXIN -> "文心一言";
            case TENCENT -> "腾讯混元";
            case XUNFEI -> "讯飞星火";
            case OTHER -> "其他";
        };
    }

    /**
     * 创建提供商
     */
    @PostMapping
    public ProviderResponse create(@Valid @RequestBody ProviderCreateRequest request) {
        return providerService.create(request);
    }

    /**
     * 获取提供商详情
     */
    @GetMapping("/{id}")
    public ProviderResponse getById(@PathVariable Long id) {
        return providerService.getById(id);
    }

    /**
     * 查询提供商列表
     */
    @GetMapping
    public PageResponse<ProviderResponse> query(@ModelAttribute ProviderQueryRequest request) {
        return providerService.query(request);
    }

    /**
     * 更新提供商
     */
    @PutMapping("/{id}")
    public ProviderResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProviderUpdateRequest request) {
        return providerService.update(id, request);
    }

    /**
     * 删除提供商
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        providerService.delete(id);
    }

    /**
     * 启用/禁用提供商
     */
    @PatchMapping("/{id}/enabled")
    public ProviderResponse setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return providerService.setEnabled(id, enabled);
    }

    /**
     * 获取 Provider 的 Key 信息
     */
    @GetMapping("/{id}/keys")
    public ProviderKeysResponse getProviderKeys(@PathVariable Long id) {
        return providerService.getProviderKeys(id);
    }
}