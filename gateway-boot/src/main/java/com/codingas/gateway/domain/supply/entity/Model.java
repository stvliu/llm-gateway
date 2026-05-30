package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.ModelState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 模型规格实体（全局模型注册表）
 *
 * <p>Model 是模型固有规格，与渠道无关。modelName 是用户请求时传的值，路由匹配的唯一键。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Model extends BaseEntity {

    /** 用户面标识（如 "deepseek-v4-flash"，路由匹配用） */
    private String modelName;

    private String displayName;

    private String modelFamily;

    private Integer contextWindow;

    private Integer maxInputTokens;

    private Integer maxOutputTokens;

    private Map<String, Boolean> capabilities;

    private List<String> modalities;

    private ModelState state = ModelState.ACTIVE;

    /**
     * 检查模型是否可用
     */
    public boolean isAvailable() {
        return ModelState.ACTIVE.equals(state);
    }
}