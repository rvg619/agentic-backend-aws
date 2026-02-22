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
     * Create a comprehensive plan for executing the given task
     */
    public String createPlan(String taskDescription) {
        String prompt = String.format(
            "You are an expert planning agent. Create a detailed, professional execution plan for the following task:\n\n" +
            "Task: %s\n\n" +
            "Please provide:\n" +
            "1. Overview and Objectives\n" +
            "2. Detailed Step-by-Step Plan (numbered)\n" +
            "3. Key Deliverables\n" +
            "4. Success Criteria\n" +
            "5. Risk Considerations\n\n" +
            "Format your response professionally with clear sections and bullet points.",
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

    /**
     * Create a structured plan in JSON format for complex tasks
     */
    public String createStructuredPlan(String taskDescription) {
        String prompt = String.format(
            "Create a structured JSON execution plan for: %s\n\n" +
            "Return a JSON object with these fields:\n" +
            "{\n" +
            "  \"title\": \"Task title\",\n" +
            "  \"objective\": \"Main goal\",\n" +
            "  \"steps\": [\n" +
            "    {\"step\": 1, \"title\": \"Step title\", \"description\": \"Detailed description\", \"duration\": \"estimated time\"},\n" +
            "    ...\n" +
            "  ],\n" +
            "  \"deliverables\": [\"deliverable 1\", \"deliverable 2\"],\n" +
            "  \"risks\": [\"risk 1\", \"risk 2\"],\n" +
            "  \"successCriteria\": [\"criteria 1\", \"criteria 2\"]\n" +
            "}",
            taskDescription
        );
        
        return llmClient.generateResponse(prompt);
    }
}