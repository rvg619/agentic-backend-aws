package com.rajathgoku.agentic.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Entity
@Table(name = "steps", indexes = {
    @Index(name = "idx_steps_run_id", columnList = "run_id"),
    @Index(name = "idx_steps_status", columnList = "status"),
    @Index(name = "idx_steps_id", columnList = "id"),
    @Index(name = "idx_steps_created_at", columnList = "created_at")
})
public class Step {
    public enum StepStatus {
        PENDING,    // Step is created and waiting to be executed
        RUNNING,    // Step is currently being executed
        COMPLETED,  // Step completed successfully (was DONE)
        DONE,       // Step completed successfully
        FAILED,     // Step failed with an error
        SKIPPED     // Step was skipped in the execution flow
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    // Add version field for optimistic locking to prevent race conditions
    @Version
    private Long version;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "step_number")
    private Integer stepNumber;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false, columnDefinition = "UUID")
    private Run run;

    @OneToMany(mappedBy = "step", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Artifact> artifacts = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status = StepStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Add transient lock for in-memory synchronization
    @Transient
    private final ReentrantLock stepLock = new ReentrantLock();

    // Constructors
    public Step() {}

    public Step(String name, String description, Run run) {
        this.name = name;
        this.description = description;
        this.run = run;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getResult() {
        return result;
    }

    // Modified setResult to be thread-safe
    public void setResult(String result) {
        updateResult(result);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // Modified setErrorMessage to be thread-safe
    public void setErrorMessage(String errorMessage) {
        updateErrorMessage(errorMessage);
    }

    public Run getRun() {
        return run;
    }

    public void setRun(Run run) {
        this.run = run;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    public StepStatus getStatus() {
        return status;
    }

    // Modified setStatus to be thread-safe
    public void setStatus(StepStatus status) {
        updateStatus(status);
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Getters and Setters with version support
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // Thread-safe status update method
    public synchronized void updateStatus(StepStatus newStatus) {
        stepLock.lock();
        try {
            this.status = newStatus;
            this.updatedAt = Instant.now();

            // Update relevant timestamps based on status
            switch (newStatus) {
                case PENDING -> {
                    // No specific action for PENDING
                }
                case RUNNING -> this.startedAt = Instant.now();
                case COMPLETED -> {
                    this.finishedAt = Instant.now();
                    this.completedAt = Instant.now();
                }
                case DONE -> {  // Changed from COMPLETED to DONE
                    this.finishedAt = Instant.now();
                    this.completedAt = Instant.now();
                }
                case FAILED -> this.finishedAt = Instant.now();
                case SKIPPED -> {
                    // No specific action for SKIPPED
                }
            }
        } finally {
            stepLock.unlock();
        }
    }

    // Thread-safe result update
    public synchronized void updateResult(String result) {
        stepLock.lock();
        try {
            this.result = result;
            this.updatedAt = Instant.now();
        } finally {
            stepLock.unlock();
        }
    }

    // Thread-safe error message update
    public synchronized void updateErrorMessage(String errorMessage) {
        stepLock.lock();
        try {
            this.errorMessage = errorMessage;
            this.updatedAt = Instant.now();
        } finally {
            stepLock.unlock();
        }
    }

    // Check if step can be safely updated
    public boolean canBeUpdated() {
        return stepLock.tryLock();
    }

    public void releaseLock() {
        if (stepLock.isHeldByCurrentThread()) {
            stepLock.unlock();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}