/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.usage.gateway.database;

import com.codingas.gateway.infrastructure.usage.gateway.database.dataobject.TokenLimitDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TokenLimitRepository extends JpaRepository<TokenLimitDo, Long> {
    List<TokenLimitDo> findByUserId(Long userId);
}
