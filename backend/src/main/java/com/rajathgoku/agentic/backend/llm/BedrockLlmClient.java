package com.rajathgoku.agentic.backend.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BedrockLlmClient implements LlmClient {
    
    private static final Logger logger = LoggerFactory.getLogger(BedrockLlmClient.class);
    
    private BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;
    private final String modelId;
    private final int maxTokens;
    private final double temperature;
    private final String region;
    private String credentialsError = null;
    
    public BedrockLlmClient(
            @Value("${aws.bedrock.region:us-east-1}") String region,
            @Value("${aws.bedrock.model.id:amazon.titan-text-premier-v1:0}") String modelId,
            @Value("${aws.bedrock.model.max-tokens:4000}") int maxTokens,
            @Value("${aws.bedrock.model.temperature:0.7}") double temperature) {
        
        this.region = region;
        this.modelId = modelId;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.objectMapper = new ObjectMapper();
        
        try {
            // Test credentials availability before creating the client
            DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
            credentialsProvider.resolveCredentials(); // This will throw if credentials are not available
            
            // Initialize Bedrock client only if credentials are available
            this.bedrockClient = BedrockRuntimeClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();
            
            logger.info("Initialized BedrockLlmClient with model: {} in region: {}", modelId, region);
        } catch (SdkClientException e) {
            this.credentialsError = "AWS credentials are not configured. Please set up your AWS credentials to use Bedrock. " +
                    "You can configure credentials using: AWS CLI (aws configure), environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY), " +
                    "or EC2 instance profile. Error: " + e.getMessage();
            logger.error("AWS credentials not available for BedrockLlmClient: {}", e.getMessage());
            this.bedrockClient = null;
        }
    }
    
    @Override
    public String generateResponse(String prompt) {
        if (credentialsError != null) {
            throw new RuntimeException(credentialsError);
        }
        
        if (bedrockClient == null) {
            throw new RuntimeException("AWS Bedrock client is not initialized. Please check your AWS credentials and configuration.");
        }
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            
            // Handle different model formats
            if (modelId.startsWith("anthropic.claude")) {
                // Claude 3 format
                requestBody.put("anthropic_version", "bedrock-2023-05-31");
                requestBody.put("max_tokens", maxTokens);
                requestBody.put("temperature", temperature);
                
                Map<String, String> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", prompt);
                requestBody.put("messages", List.of(message));
            } else if (modelId.startsWith("amazon.nova")) {
                // Amazon Nova format
                Map<String, Object> inferenceConfig = new HashMap<>();
                inferenceConfig.put("maxTokens", maxTokens);
                inferenceConfig.put("temperature", temperature);
                requestBody.put("inferenceConfig", inferenceConfig);
                
                Map<String, Object> message = new HashMap<>();
                message.put("role", "user");
                Map<String, String> content = new HashMap<>();
                content.put("text", prompt);
                message.put("content", List.of(content));
                requestBody.put("messages", List.of(message));
            } else if (modelId.startsWith("amazon.titan")) {
                // Amazon Titan format (for backwards compatibility)
                requestBody.put("inputText", prompt);
                
                Map<String, Object> textGenerationConfig = new HashMap<>();
                textGenerationConfig.put("maxTokenCount", maxTokens);
                textGenerationConfig.put("temperature", temperature);
                textGenerationConfig.put("topP", 0.9);
                requestBody.put("textGenerationConfig", textGenerationConfig);
            } else {
                // Default to Nova format for unknown models
                Map<String, Object> inferenceConfig = new HashMap<>();
                inferenceConfig.put("maxTokens", maxTokens);
                inferenceConfig.put("temperature", temperature);
                requestBody.put("inferenceConfig", inferenceConfig);
                
                Map<String, Object> message = new HashMap<>();
                message.put("role", "user");
                Map<String, String> content = new HashMap<>();
                content.put("text", prompt);
                message.put("content", List.of(content));
                requestBody.put("messages", List.of(message));
            }
            
            String jsonPayload = objectMapper.writeValueAsString(requestBody);
            logger.debug("Sending request to Bedrock ({}): {}", modelId, jsonPayload);
            
            // Invoke the model
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(jsonPayload))
                    .build();
            
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            logger.debug("Received response from Bedrock ({}): {}", modelId, responseBody);
            
            // Parse response based on model type
            JsonNode responseJson = objectMapper.readTree(responseBody);
            
            if (modelId.startsWith("anthropic.claude")) {
                // Claude 3 response format
                JsonNode contentArray = responseJson.get("content");
                if (contentArray != null && contentArray.isArray() && contentArray.size() > 0) {
                    JsonNode firstContent = contentArray.get(0);
                    if (firstContent.has("text")) {
                        return firstContent.get("text").asText();
                    }
                }
            } else if (modelId.startsWith("amazon.nova")) {
                // Amazon Nova response format
                JsonNode output = responseJson.get("output");
                if (output != null && output.has("message")) {
                    JsonNode message = output.get("message");
                    JsonNode content = message.get("content");
                    if (content != null && content.isArray() && content.size() > 0) {
                        JsonNode firstContent = content.get(0);
                        if (firstContent.has("text")) {
                            return firstContent.get("text").asText();
                        }
                    }
                }
            } else if (modelId.startsWith("amazon.titan")) {
                // Amazon Titan response format
                JsonNode results = responseJson.get("results");
                if (results != null && results.isArray() && results.size() > 0) {
                    JsonNode firstResult = results.get(0);
                    if (firstResult.has("outputText")) {
                        return firstResult.get("outputText").asText().trim();
                    }
                }
            }
            
            logger.warn("Unexpected response format from Bedrock ({}): {}", modelId, responseBody);
            return "Error: Unable to parse response from " + modelId;
            
        } catch (Exception e) {
            logger.error("Error calling Bedrock {}: {}", modelId, e.getMessage());
            throw new RuntimeException("Failed to generate response from AWS Bedrock " + modelId + ": " + e.getMessage(), e);
        }
    }
    
    @Override
    public String generateResponse(List<Message> messages) {
        if (credentialsError != null) {
            throw new RuntimeException(credentialsError);
        }
        
        if (bedrockClient == null) {
            throw new RuntimeException("AWS Bedrock client is not initialized. Please check your AWS credentials and configuration.");
        }
        
        if (messages.isEmpty()) {
            return "No messages provided.";
        }
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            
            // Handle different model formats for conversation
            if (modelId.startsWith("anthropic.claude")) {
                // Claude 3 format
                requestBody.put("anthropic_version", "bedrock-2023-05-31");
                requestBody.put("max_tokens", maxTokens);
                requestBody.put("temperature", temperature);
                
                List<Map<String, String>> claudeMessages = messages.stream()
                        .map(msg -> {
                            Map<String, String> claudeMsg = new HashMap<>();
                            String role = "user".equals(msg.role()) || "assistant".equals(msg.role()) 
                                         ? msg.role() : "user";
                            claudeMsg.put("role", role);
                            claudeMsg.put("content", msg.content());
                            return claudeMsg;
                        })
                        .toList();
                
                requestBody.put("messages", claudeMessages);
            } else if (modelId.startsWith("amazon.nova")) {
                // Amazon Nova format - supports conversation
                Map<String, Object> inferenceConfig = new HashMap<>();
                inferenceConfig.put("maxTokens", maxTokens);
                inferenceConfig.put("temperature", temperature);
                requestBody.put("inferenceConfig", inferenceConfig);
                
                List<Map<String, Object>> novaMessages = messages.stream()
                        .map(msg -> {
                            Map<String, Object> novaMsg = new HashMap<>();
                            String role = "user".equals(msg.role()) || "assistant".equals(msg.role()) 
                                         ? msg.role() : "user";
                            novaMsg.put("role", role);
                            Map<String, String> content = new HashMap<>();
                            content.put("text", msg.content());
                            novaMsg.put("content", List.of(content));
                            return novaMsg;
                        })
                        .toList();
                
                requestBody.put("messages", novaMessages);
            } else if (modelId.startsWith("amazon.titan")) {
                // Amazon Titan format - convert conversation to single prompt
                StringBuilder conversationText = new StringBuilder();
                for (Message msg : messages) {
                    String role = "assistant".equals(msg.role()) ? "Assistant" : "Human";
                    conversationText.append(role).append(": ").append(msg.content()).append("\n\n");
                }
                conversationText.append("Assistant: ");
                
                requestBody.put("inputText", conversationText.toString());
                
                Map<String, Object> textGenerationConfig = new HashMap<>();
                textGenerationConfig.put("maxTokenCount", maxTokens);
                textGenerationConfig.put("temperature", temperature);
                textGenerationConfig.put("topP", 0.9);
                requestBody.put("textGenerationConfig", textGenerationConfig);
            } else {
                // Default to Nova format for unknown models
                Map<String, Object> inferenceConfig = new HashMap<>();
                inferenceConfig.put("maxTokens", maxTokens);
                inferenceConfig.put("temperature", temperature);
                requestBody.put("inferenceConfig", inferenceConfig);
                
                String lastMessage = messages.get(messages.size() - 1).content();
                Map<String, Object> message = new HashMap<>();
                message.put("role", "user");
                Map<String, String> content = new HashMap<>();
                content.put("text", lastMessage);
                message.put("content", List.of(content));
                requestBody.put("messages", List.of(message));
            }
            
            String jsonPayload = objectMapper.writeValueAsString(requestBody);
            logger.debug("Sending conversation request to Bedrock ({}): {}", modelId, jsonPayload);
            
            // Invoke the model
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(jsonPayload))
                    .build();
            
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            logger.debug("Received conversation response from Bedrock ({}): {}", modelId, responseBody);
            
            // Parse response based on model type
            JsonNode responseJson = objectMapper.readTree(responseBody);
            
            if (modelId.startsWith("anthropic.claude")) {
                // Claude 3 response format
                JsonNode contentArray = responseJson.get("content");
                if (contentArray != null && contentArray.isArray() && contentArray.size() > 0) {
                    JsonNode firstContent = contentArray.get(0);
                    if (firstContent.has("text")) {
                        return firstContent.get("text").asText();
                    }
                }
            } else if (modelId.startsWith("amazon.nova")) {
                // Amazon Nova response format
                JsonNode output = responseJson.get("output");
                if (output != null && output.has("message")) {
                    JsonNode message = output.get("message");
                    JsonNode content = message.get("content");
                    if (content != null && content.isArray() && content.size() > 0) {
                        JsonNode firstContent = content.get(0);
                        if (firstContent.has("text")) {
                            return firstContent.get("text").asText();
                        }
                    }
                }
            } else if (modelId.startsWith("amazon.titan")) {
                // Amazon Titan response format
                JsonNode results = responseJson.get("results");
                if (results != null && results.isArray() && results.size() > 0) {
                    JsonNode firstResult = results.get(0);
                    if (firstResult.has("outputText")) {
                        return firstResult.get("outputText").asText().trim();
                    }
                }
            }
            
            logger.warn("Unexpected conversation response format from Bedrock ({}): {}", modelId, responseBody);
            return "Error: Unable to parse conversation response from " + modelId;
            
        } catch (Exception e) {
            logger.error("Error calling Bedrock {} with conversation: {}", modelId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate conversation response from AWS Bedrock " + modelId + ": " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        if (credentialsError != null || bedrockClient == null) {
            return false;
        }
        
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
        return credentialsError != null ? "AWS-Credentials-Missing" : modelId;
    }
    
    /**
     * Get the current credentials error message if any
     * @return the credentials error message or null if no error
     */
    public String getCredentialsError() {
        return credentialsError;
    }
}