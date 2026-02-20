package com.rajathgoku.agentic.backend.llm;

import java.util.List;

public interface LlmClient {
    
    /**
     * Send a single prompt to the LLM and get a response
     */
    String generateResponse(String prompt);
    
    /**
     * Send a conversation (list of messages) to the LLM
     */
    String generateResponse(List<Message> messages);
    
    /**
     * Check if the LLM client is available/healthy
     */
    boolean isHealthy();
    
    /**
     * Get the model name/identifier
     */
    String getModelName();
    
    /**
     * Message record for conversation-based interactions
     */
    record Message(String role, String content) {}
}