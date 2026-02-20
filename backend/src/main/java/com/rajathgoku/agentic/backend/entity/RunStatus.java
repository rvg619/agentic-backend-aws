package com.rajathgoku.agentic.backend.entity;

/**
 * Status enumeration for Run entities
 */
public enum RunStatus {
    PENDING,    // Run is created and waiting to be processed
    RUNNING,    // Run is currently being processed
    DONE,       // Run completed successfully
    FAILED,     // Run failed with an error
    CANCELLED   // Run was cancelled before completion
}