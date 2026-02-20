package com.rajathgoku.agentic.backend.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("real")
public class BedrockLlmClient implements LlmClient {
    
    private static final Logger logger = LoggerFactory.getLogger(BedrockLlmClient.class);
    
    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;
    private final String modelId;
    private final int maxTokens;
    private final double temperature;
    
    public BedrockLlmClient(
            @Value("${aws.bedrock.region:us-east-1}") String region,
            @Value("${aws.bedrock.model.id:anthropic.claude-3-haiku-20240307-v1:0}") String modelId,
            @Value("${aws.bedrock.model.max-tokens:4000}") int maxTokens,
            @Value("${aws.bedrock.model.temperature:0.7}") double temperature) {
        
        this.modelId = modelId;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.objectMapper = new ObjectMapper();
        
        // Initialize Bedrock client with default credentials
        this.bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        
        logger.info("Initialized BedrockLlmClient with model: {} in region: {}", modelId, region);
    }
    
    @Override
    public String generateResponse(String prompt) {
        try {
            // Create request payload for Claude 3
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("anthropic_version", "bedrock-2023-05-31");
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            
            // Create messages array for Claude 3
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", List.of(message));
            
            String jsonPayload = objectMapper.writeValueAsString(requestBody);
            logger.debug("Sending request to Bedrock: {}", jsonPayload);
            
            // Invoke the model
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(jsonPayload))
                    .build();
            
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            logger.debug("Received response from Bedrock: {}", responseBody);
            
            // Parse response
            JsonNode responseJson = objectMapper.readTree(responseBody);
            JsonNode contentArray = responseJson.get("content");
            
            if (contentArray != null && contentArray.isArray() && contentArray.size() > 0) {
                JsonNode firstContent = contentArray.get(0);
                if (firstContent.has("text")) {
                    return firstContent.get("text").asText();
                }
            }
            
            logger.warn("Unexpected response format from Bedrock: {}", responseBody);
            return "Error: Unable to parse response from Claude 3";
            
        } catch (Exception e) {
            logger.error("Error calling Bedrock Claude 3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate response from Claude 3", e);
        }
    }
    
    @Override
    public String generateResponse(List<Message> messages) {
        if (messages.isEmpty()) {
            return "No messages provided.";
        }
        
        try {
            // Create request payload for Claude 3 with conversation
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("anthropic_version", "bedrock-2023-05-31");
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            
            // Convert messages to Claude 3 format
            List<Map<String, String>> claudeMessages = messages.stream()
                    .map(msg -> {
                        Map<String, String> claudeMsg = new HashMap<>();
                        // Map roles (Claude expects "user" or "assistant")
                        String role = "user".equals(msg.role()) || "assistant".equals(msg.role()) 
                                     ? msg.role() : "user";
                        claudeMsg.put("role", role);
                        claudeMsg.put("content", msg.content());
                        return claudeMsg;
                    })
                    .toList();
            
            requestBody.put("messages", claudeMessages);
            
            String jsonPayload = objectMapper.writeValueAsString(requestBody);
            logger.debug("Sending conversation request to Bedrock: {}", jsonPayload);
            
            // Invoke the model
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(jsonPayload))
                    .build();
            
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            logger.debug("Received conversation response from Bedrock: {}", responseBody);
            
            // Parse response
            JsonNode responseJson = objectMapper.readTree(responseBody);
            JsonNode contentArray = responseJson.get("content");
            
            if (contentArray != null && contentArray.isArray() && contentArray.size() > 0) {
                JsonNode firstContent = contentArray.get(0);
                if (firstContent.has("text")) {
                    return firstContent.get("text").asText();
                }
            }
            
            logger.warn("Unexpected conversation response format from Bedrock: {}", responseBody);
            return "Error: Unable to parse conversation response from Claude 3";
            
        } catch (Exception e) {
            logger.error("Error calling Bedrock Claude 3 with conversation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate conversation response from Claude 3", e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Simple health check by sending a minimal request
            String healthCheckResponse = generateResponse("Hello");
            return healthCheckResponse != null && !healthCheckResponse.startsWith("Error:");
        } catch (Exception e) {
            logger.warn("Health check failed for BedrockLlmClient: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getModelName() {
        return modelId;
    }
}