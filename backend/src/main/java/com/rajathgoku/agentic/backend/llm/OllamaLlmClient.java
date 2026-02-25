package com.rajathgoku.agentic.backend.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ollama LLM Client - Free local LLM for the Agentic AI system
 * 
 * Prerequisites:
 * 1. Install Ollama: curl -fsSL https://ollama.com/install.sh | sh
 * 2. Pull a model: ollama pull llama3.1:8b
 * 3. Start Ollama: ollama serve
 */
@Component
public class OllamaLlmClient implements LlmClient {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaLlmClient.class);
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final String ollamaUrl = "http://host.docker.internal:11434/api/generate";
    private final String modelName = "llama3.1:8b";
    
    public OllamaLlmClient() {
        logger.info("🦙 OllamaLlmClient initialized - using free local Llama 3.1 8B model");
        logger.info("📡 Connecting to Ollama at: {}", ollamaUrl);
    }
    
    @Override
    public String generateResponse(String prompt) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("model", modelName);
            request.put("prompt", prompt);
            request.put("stream", false);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(ollamaUrl, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                return (String) responseBody.get("response");
            } else {
                throw new RuntimeException("Ollama API error: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error calling Ollama: {}", e.getMessage());
            throw new RuntimeException("Failed to generate response from Ollama: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String generateResponse(List<Message> messages) {
        if (messages.isEmpty()) {
            return generateResponse("Hello! How can I help you?");
        }
        
        // Convert messages to a single prompt
        StringBuilder prompt = new StringBuilder();
        for (Message message : messages) {
            prompt.append(message.role()).append(": ").append(message.content()).append("\n");
        }
        
        return generateResponse(prompt.toString());
    }
    
    @Override
    public boolean isHealthy() {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("model", modelName);
            request.put("prompt", "test");
            request.put("stream", false);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(ollamaUrl, entity, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.warn("Ollama health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getModelName() {
        return modelName;
    }
}