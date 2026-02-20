package com.rajathgoku.agentic.backend.agent;

import com.rajathgoku.agentic.backend.llm.LlmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlannerAgent {
    
    private final LlmClient llmClient;
    
    @Autowired
    public PlannerAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }
    
    /**
     * Create a plan for executing the given task
     */
    public String createPlan(String taskDescription) {
        String prompt = String.format(
            "You are a planning agent. Create a detailed step-by-step plan for the following task:\n\n" +
            "Task: %s\n\n" +
            "Please provide a clear, actionable plan with numbered steps.",
            taskDescription
        );
        
        return llmClient.generateResponse(prompt);
    }
    
    /**
     * Break down a plan into individual executable steps
     */
    public String[] breakDownPlan(String plan) {
        // Simple parsing - in a real implementation, this could use LLM to better parse
        return plan.split("\n");
    }
}