package com.rajathgoku.agentic.backend.entity;

/**
 * Status enumeration for Step entities
 */
public enum StepStatus {
    PENDING,    // Step is created and waiting to be executed
    RUNNING,    // Step is currently being executed
    DONE,       // Step completed successfully
    FAILED,     // Step failed with an error
    SKIPPED     // Step was skipped in the execution flow
}