package com.rajathgoku.agentic.backend.agent;

import com.rajathgoku.agentic.backend.llm.LlmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CriticAgent {
    
    private final LlmClient llmClient;
    
    @Autowired
    public CriticAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }
    
    /**
     * Review and critique a step execution result
     */
    public String reviewStep(String stepDescription, String stepResult) {
        String prompt = String.format(
            "You are a critic agent. Review the following step execution:\n\n" +
            "Step: %s\n\n" +
            "Result: %s\n\n" +
            "Please provide a critical analysis of the execution quality, completeness, and suggestions for improvement.",
            stepDescription,
            stepResult
        );
        
        return llmClient.generateResponse(prompt);
    }
    
    /**
     * Evaluate the overall run execution
     */
    public String evaluateRun(String taskDescription, String[] stepResults) {
        StringBuilder resultsBuilder = new StringBuilder();
        for (int i = 0; i < stepResults.length; i++) {
            if (stepResults[i] != null && !stepResults[i].trim().isEmpty()) {
                resultsBuilder.append(String.format("Step %d: %s\n", i + 1, stepResults[i]));
            }
        }
        
        String prompt = String.format(
            "You are a critic agent. Evaluate the overall execution of this task:\n\n" +
            "Original Task: %s\n\n" +
            "Step Results:\n%s\n\n" +
            "Please provide an overall assessment of how well the task was completed, what worked well, and what could be improved.",
            taskDescription,
            resultsBuilder.toString()
        );
        
        return llmClient.generateResponse(prompt);
    }
    
    /**
     * Determine if the run should be marked as successful
     */
    public boolean isRunSuccessful(String taskDescription, String[] stepResults, String overallEvaluation) {
        // Simple heuristic - in a real implementation, this could use LLM for more sophisticated analysis
        if (stepResults == null || stepResults.length == 0) {
            return false;
        }
        
        // Check if most steps completed successfully (simple keyword analysis)
        int successfulSteps = 0;
        for (String result : stepResults) {
            if (result != null && !result.trim().isEmpty()) {
                String lowerResult = result.toLowerCase();
                if (lowerResult.contains("completed") || lowerResult.contains("successful") || 
                    lowerResult.contains("implemented") || lowerResult.contains("created")) {
                    successfulSteps++;
                }
            }
        }
        
        // Consider run successful if at least 70% of steps appear successful
        return (double) successfulSteps / stepResults.length >= 0.7;
    }
}