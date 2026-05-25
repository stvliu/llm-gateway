package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.channel.dto.ApiKeyTestResponse;
import com.codingas.gateway.application.channelcredential.ChannelCredentialService;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialCreateRequest;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialCreateResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialDetailResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 渠道凭证管理控制器
 */
@RestController
@RequestMapping("/api/v1/channels/{channelId}/credentials")
@RequiredArgsConstructor
public class ChannelCredentialController {

    private final ChannelCredentialService channelCredentialService;

    /**
     * 获取渠道下的凭证列表
     */
    @GetMapping
    public List<ChannelCredentialResponse> list(@PathVariable Long channelId) {
        return channelCredentialService.listByChannelId(channelId);
    }

    /**
     * 根据 ID 获取凭证详情（含明文，用于页面复制）
     */
    @GetMapping("/{id}")
    public ChannelCredentialDetailResponse get(
            @PathVariable Long channelId,
            @PathVariable Long id) {
        return channelCredentialService.getDetailById(channelId, id);
    }

    /**
     * 创建渠道凭证
     */
    @PostMapping
    public ChannelCredentialCreateResponse create(
            @PathVariable Long channelId,
            @Valid @RequestBody ChannelCredentialCreateRequest request) {
        return channelCredentialService.create(channelId, request);
    }

    /**
     * 更新渠道凭证
     */
    @PutMapping("/{id}")
    public ChannelCredentialResponse update(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @Valid @RequestBody ChannelCredentialUpdateRequest request) {
        return channelCredentialService.update(channelId, id, request);
    }

    /**
     * 删除渠道凭证
     */
    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long channelId,
            @PathVariable Long id) {
        channelCredentialService.delete(channelId, id);
    }

    /**
     * 测试 API Key 是否有效
     */
    @PostMapping("/{id}/test")
    public ApiKeyTestResponse testApiKey(
            @PathVariable Long channelId,
            @PathVariable Long id) {
        return channelCredentialService.testApiKey(channelId, id);
    }
}
