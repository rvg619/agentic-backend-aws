package com.rajathgoku.agentic.backend.dto;

import com.rajathgoku.agentic.backend.entity.Artifact;
import java.time.Instant;

public class ArtifactResponse {
    private Long id;
    private String name;
    private String type;
    private String content;
    private String filePath;
    private Long size;
    private Instant createdAt;

    // Constructors
    public ArtifactResponse() {}

    public ArtifactResponse(Artifact artifact) {
        this.id = artifact.getId();
        this.name = artifact.getName();
        this.type = artifact.getType();
        this.content = artifact.getContent();
        this.filePath = artifact.getFilePath();
        this.size = artifact.getSize();
        this.createdAt = artifact.getCreatedAt();
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}