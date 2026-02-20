package com.rajathgoku.agentic.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateRunRequest {

    @NotNull
    private UUID taskId;

    private String status = "PENDING";

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}