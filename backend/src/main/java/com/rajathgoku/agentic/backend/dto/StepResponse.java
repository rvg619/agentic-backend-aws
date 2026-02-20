package com.rajathgoku.agentic.backend.dto;

import com.rajathgoku.agentic.backend.entity.Step;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class StepResponse {
    private Long id;
    private String name;
    private String description;
    private String status;
    private List<ArtifactResponse> artifacts;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public StepResponse() {}

    public StepResponse(Step step) {
        this.id = step.getId();
        this.name = step.getName();
        this.description = step.getDescription();
        this.status = step.getStatus().name();
        this.artifacts = step.getArtifacts().stream()
                .map(ArtifactResponse::new)
                .collect(Collectors.toList());
        this.createdAt = step.getCreatedAt();
        this.updatedAt = step.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ArtifactResponse> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<ArtifactResponse> artifacts) {
        this.artifacts = artifacts;
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
}