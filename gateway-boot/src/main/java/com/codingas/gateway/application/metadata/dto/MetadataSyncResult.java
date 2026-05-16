package com.codingas.gateway.application.metadata.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 同步结果 DTO
 */
@Data
@Builder
public class MetadataSyncResult {

    private int syncedCount;
    private int addedCount;
    private int updatedCount;
    private Instant syncedAt;
}