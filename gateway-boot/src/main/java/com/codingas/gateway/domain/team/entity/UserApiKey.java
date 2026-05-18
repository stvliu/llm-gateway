package com.codingas.gateway.domain.team.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 用户 API Key 实体
 *
 * <p>用户侧访问密钥，用于访问 llm-gateway。</p>
 * <p>绑定特定产品，实现细粒度权限控制。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class UserApiKey extends BaseEntity {

    /** 所属团队 ID */
    private Long teamId;

    /** 创建者用户 ID */
    private Long ownerUserId;

    /** 绑定的产品 ID */
    private Long productId;

    /** Key 哈希值，用于认证 */
    private String keyHash;

    /** Key 前缀，用于识别 */
    private String keyPrefix;

    /** 密钥名称 */
    private String name;

    /** 可访问的模型列表（子集），为空表示可访问产品全部模型 */
    private List<String> models;

    /** Key 级别的额度限制 */
    private Long quotaLimit;

    /** 密钥状态 */
    private UserApiKeyState state = UserApiKeyState.ACTIVE;

    /**
     * 检查密钥是否可用
     */
    public boolean isAvailable() {
        return state.isAvailable();
    }

    /**
     * 检查是否有权访问指定模型
     */
    public boolean canAccessModel(String modelName) {
        if (models == null || models.isEmpty()) {
            return true;
        }
        return models.contains(modelName);
    }
}
