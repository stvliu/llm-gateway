package com.codingas.gateway.infrastructure.team.gateway.database.dataobject;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 团队数据对象
 */
@Data
@Entity
@Table(name = "teams")
public class TeamDo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 256)
    private String description;

    @Column(name = "state", length = 16)
    private String state;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
