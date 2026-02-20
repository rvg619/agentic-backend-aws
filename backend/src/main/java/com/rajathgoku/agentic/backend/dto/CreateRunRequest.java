package com.rajathgoku.agentic.backend.dto;

import jakarta.validation.constraints.NotNull;

public class CreateRunRequest {

    @NotNull
    private Long taskId;

    private String status = "PENDING";

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}