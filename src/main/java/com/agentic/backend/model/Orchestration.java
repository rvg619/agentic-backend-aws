package com.agentic.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orchestrations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orchestration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrchestrationStatus status;

    @Column(name = "total_tasks")
    private Integer totalTasks;

    @Column(name = "completed_tasks")
    private Integer completedTasks;

    @Column(name = "failed_tasks")
    private Integer failedTasks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = OrchestrationStatus.INITIALIZING;
        }
        if (totalTasks == null) {
            totalTasks = 0;
        }
        if (completedTasks == null) {
            completedTasks = 0;
        }
        if (failedTasks == null) {
            failedTasks = 0;
        }
    }

    public enum OrchestrationStatus {
        INITIALIZING,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED
    }
}
