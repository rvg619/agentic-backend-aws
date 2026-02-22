package com.rajathgoku.agentic.backend.dto;

public class RunMetricsResponse {
    private long tokensProcessed;
    private int apiCallsMade;
    private long processingTimeMs;
    private double complexityScore;
    private int stepsCompleted;
    private int artifactsGenerated;
    private String currentPhase;

    public RunMetricsResponse() {}

    public RunMetricsResponse(long tokensProcessed, int apiCallsMade, long processingTimeMs, 
                             double complexityScore, int stepsCompleted, int artifactsGenerated, 
                             String currentPhase) {
        this.tokensProcessed = tokensProcessed;
        this.apiCallsMade = apiCallsMade;
        this.processingTimeMs = processingTimeMs;
        this.complexityScore = complexityScore;
        this.stepsCompleted = stepsCompleted;
        this.artifactsGenerated = artifactsGenerated;
        this.currentPhase = currentPhase;
    }

    // Getters and setters
    public long getTokensProcessed() { return tokensProcessed; }
    public void setTokensProcessed(long tokensProcessed) { this.tokensProcessed = tokensProcessed; }

    public int getApiCallsMade() { return apiCallsMade; }
    public void setApiCallsMade(int apiCallsMade) { this.apiCallsMade = apiCallsMade; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public double getComplexityScore() { return complexityScore; }
    public void setComplexityScore(double complexityScore) { this.complexityScore = complexityScore; }

    public int getStepsCompleted() { return stepsCompleted; }
    public void setStepsCompleted(int stepsCompleted) { this.stepsCompleted = stepsCompleted; }

    public int getArtifactsGenerated() { return artifactsGenerated; }
    public void setArtifactsGenerated(int artifactsGenerated) { this.artifactsGenerated = artifactsGenerated; }

    public String getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }
}