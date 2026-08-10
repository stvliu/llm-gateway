/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.experience.dto;

/**
 * 体验模型响应
 *
 * <p>用于模型体验功能，返回简化的模型信息。</p>
 */
public record ExperienceModelResponse(
    Long id,
    String modelName,
    String displayName
) {}
