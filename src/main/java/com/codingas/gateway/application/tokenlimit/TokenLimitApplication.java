package com.codingas.gateway.application.tokenlimit;

import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitCreateRequest;
import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitQueryRequest;
import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitResponse;
import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
import com.codingas.gateway.domain.router.gateway.ProviderGateway;
import com.codingas.gateway.domain.security.entity.TokenLimit;
import com.codingas.gateway.domain.security.entity.TokenLimit.TokenLimitStatus;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.TokenLimitGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Token 限额应用服务
 *
 * <p>处理 Token 限额管理的业务逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenLimitApplication {

    private final TokenLimitGateway tokenLimitGateway;
    private final UserGateway userGateway;
    private final ProviderGateway providerGateway;
    private final ModelGateway modelGateway;

    /**
     * 创建 Token 限额
     */
    @Transactional
    public TokenLimitResponse create(TokenLimitCreateRequest request) {
        // 检查限额代码唯一性
        if (tokenLimitGateway.existsByLimitCode(request.getLimitCode())) {
            throw new DuplicateResourceException("TokenLimit", "limitCode");
        }

        // 查找用户
        User user = userGateway.findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        // 查找提供商（可选）
        Provider provider = null;
        if (request.getProviderId() != null) {
            provider = providerGateway.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider", request.getProviderId()));
        }

        // 查找模型（可选）
        Model model = null;
        if (request.getModelId() != null) {
            model = modelGateway.findById(request.getModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Model", request.getModelId()));
        }

        // 查找切换模型（可选）
        Model switchModel = null;
        if (request.getSwitchModelId() != null) {
            switchModel = modelGateway.findById(request.getSwitchModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Model", request.getSwitchModelId()));
        }

        // 创建限额
        TokenLimit tokenLimit = new TokenLimit();
        tokenLimit.setLimitCode(request.getLimitCode());
        tokenLimit.setUser(user);
        tokenLimit.setProvider(provider);
        tokenLimit.setModel(model);
        tokenLimit.setLimitType(request.getLimitType());
        tokenLimit.setMaxTokens(request.getMaxTokens());
        tokenLimit.setUsedTokens(BigDecimal.ZERO);
        tokenLimit.setPeriodType(request.getPeriodType());
        tokenLimit.setPeriodDayOfWeek(request.getPeriodDayOfWeek());
        tokenLimit.setPeriodDayOfMonth(request.getPeriodDayOfMonth());
        tokenLimit.setExceededAction(request.getExceededAction());
        tokenLimit.setSwitchModel(switchModel);
        tokenLimit.setStatus(TokenLimitStatus.ACTIVE);

        TokenLimit savedTokenLimit = tokenLimitGateway.save(tokenLimit);
        return toResponse(savedTokenLimit);
    }

    /**
     * 根据 ID 获取 Token 限额
     */
    public TokenLimitResponse getById(Long id) {
        TokenLimit tokenLimit = tokenLimitGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));
        return toResponse(tokenLimit);
    }

    /**
     * 查询 Token 限额列表
     */
    public PageResponse<TokenLimitResponse> query(TokenLimitQueryRequest request) {
        List<TokenLimit> tokenLimits = tokenLimitGateway.findAll();

        // 过滤
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().toLowerCase();
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getLimitCode().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        if (request.getUserId() != null) {
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getUser().getId().equals(request.getUserId()))
                .collect(Collectors.toList());
        }

        if (request.getProviderId() != null) {
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getProvider() != null && t.getProvider().getId().equals(request.getProviderId()))
                .collect(Collectors.toList());
        }

        if (request.getModelId() != null) {
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getModel() != null && t.getModel().getId().equals(request.getModelId()))
                .collect(Collectors.toList());
        }

        if (request.getStatus() != null) {
            tokenLimits = tokenLimits.stream()
                .filter(t -> t.getStatus() == request.getStatus())
                .collect(Collectors.toList());
        }

        // 统计
        long total = tokenLimits.size();

        // 分页
        int offset = request.getOffset();
        int limit = request.getLimit();
        List<TokenLimit> pagedTokenLimits = tokenLimits.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        List<TokenLimitResponse> responses = pagedTokenLimits.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return PageResponse.of(responses, request.getPage(), limit, total);
    }

    /**
     * 更新 Token 限额
     */
    @Transactional
    public TokenLimitResponse update(Long id, TokenLimitUpdateRequest request) {
        TokenLimit tokenLimit = tokenLimitGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));

        if (request.getMaxTokens() != null) {
            tokenLimit.setMaxTokens(request.getMaxTokens());
        }
        if (request.getPeriodType() != null) {
            tokenLimit.setPeriodType(request.getPeriodType());
        }
        if (request.getPeriodDayOfWeek() != null) {
            tokenLimit.setPeriodDayOfWeek(request.getPeriodDayOfWeek());
        }
        if (request.getPeriodDayOfMonth() != null) {
            tokenLimit.setPeriodDayOfMonth(request.getPeriodDayOfMonth());
        }
        if (request.getExceededAction() != null) {
            tokenLimit.setExceededAction(request.getExceededAction());
        }
        if (request.getSwitchModelId() != null) {
            Model switchModel = modelGateway.findById(request.getSwitchModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Model", request.getSwitchModelId()));
            tokenLimit.setSwitchModel(switchModel);
        }
        if (request.getEnabled() != null) {
            tokenLimit.setStatus(request.getEnabled() ? TokenLimitStatus.ACTIVE : TokenLimitStatus.SUSPENDED);
        }

        return toResponse(tokenLimitGateway.save(tokenLimit));
    }

    /**
     * 删除 Token 限额（软删除）
     */
    @Transactional
    public void delete(Long id) {
        TokenLimit tokenLimit = tokenLimitGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));
        tokenLimit.setDeletedAt(Instant.now());
        tokenLimitGateway.save(tokenLimit);
    }

    /**
     * 重置已使用量
     */
    @Transactional
    public TokenLimitResponse resetUsage(Long id) {
        TokenLimit tokenLimit = tokenLimitGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TokenLimit", id));
        tokenLimit.setUsedTokens(BigDecimal.ZERO);
        return toResponse(tokenLimitGateway.save(tokenLimit));
    }

    /**
     * 转换为响应 DTO
     */
    private TokenLimitResponse toResponse(TokenLimit tokenLimit) {
        TokenLimitResponse response = new TokenLimitResponse();
        response.setId(tokenLimit.getId());
        response.setLimitCode(tokenLimit.getLimitCode());
        response.setUserId(tokenLimit.getUser().getId());
        response.setUsername(tokenLimit.getUser().getUsername());
        if (tokenLimit.getProvider() != null) {
            response.setProviderId(tokenLimit.getProvider().getId());
            response.setProviderName(tokenLimit.getProvider().getProviderName());
        }
        if (tokenLimit.getModel() != null) {
            response.setModelId(tokenLimit.getModel().getId());
            response.setModelName(tokenLimit.getModel().getDisplayName());
        }
        response.setLimitType(tokenLimit.getLimitType());
        response.setMaxTokens(tokenLimit.getMaxTokens());
        response.setUsedTokens(tokenLimit.getUsedTokens());
        if (tokenLimit.getMaxTokens() != null && tokenLimit.getUsedTokens() != null) {
            response.setRemainingTokens(tokenLimit.getMaxTokens().subtract(tokenLimit.getUsedTokens()));
        }
        response.setPeriodType(tokenLimit.getPeriodType());
        response.setPeriodDayOfWeek(tokenLimit.getPeriodDayOfWeek());
        response.setPeriodDayOfMonth(tokenLimit.getPeriodDayOfMonth());
        response.setExceededAction(tokenLimit.getExceededAction());
        if (tokenLimit.getSwitchModel() != null) {
            response.setSwitchModelId(tokenLimit.getSwitchModel().getId());
            response.setSwitchModelName(tokenLimit.getSwitchModel().getDisplayName());
        }
        response.setStatus(tokenLimit.getStatus());
        response.setEnabled(tokenLimit.getStatus() == TokenLimitStatus.ACTIVE);
        response.setCreatedAt(tokenLimit.getCreatedAt());
        response.setUpdatedAt(tokenLimit.getUpdatedAt());
        return response;
    }
}
