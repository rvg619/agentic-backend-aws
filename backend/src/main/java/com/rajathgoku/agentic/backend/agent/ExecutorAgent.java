package com.rajathgoku.agentic.backend.agent;

import com.rajathgoku.agentic.backend.llm.LlmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExecutorAgent {
    
    private final LlmClient llmClient;
    
    @Autowired
    public ExecutorAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }
    
    /**
     * Execute a specific step and return the result
     */
    public String executeStep(String stepDescription, String context) {
        String prompt = String.format(
            "You are an execution agent. Execute the following step:\n\n" +
            "Step: %s\n\n" +
            "Context: %s\n\n" +
            "Please execute this step and provide a detailed result of what was accomplished.",
            stepDescription,
            context != null ? context : "No additional context provided"
        );
        
        return llmClient.generateResponse(prompt);
    }
    
    /**
     * Execute a step with previous step results as context
     */
    public String executeStepWithHistory(String stepDescription, String[] previousResults) {
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Previous step results:\n");
        
        for (int i = 0; i < previousResults.length; i++) {
            if (previousResults[i] != null && !previousResults[i].trim().isEmpty()) {
                contextBuilder.append(String.format("Step %d result: %s\n", i + 1, previousResults[i]));
            }
        }
        
        return executeStep(stepDescription, contextBuilder.toString());
    }
    
    /**
     * Simulate creating an artifact during execution
     */
    public ExecutionResult executeWithArtifact(String stepDescription, String context) {
        String result = executeStep(stepDescription, context);
        
        // Generate a simple artifact (in real implementation, this could be files, data, etc.)
        String artifactContent = String.format(
            "Artifact generated during step execution:\n" +
            "Step: %s\n" +
            "Timestamp: %s\n" +
            "Result: %s",
            stepDescription,
            java.time.Instant.now().toString(),
            result
        );
        
        return new ExecutionResult(result, artifactContent);
    }
    
    /**
     * Result of step execution including any generated artifacts
     */
    public record ExecutionResult(String result, String artifactContent) {}
}