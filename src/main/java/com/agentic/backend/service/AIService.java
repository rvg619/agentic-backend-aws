package com.agentic.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AIService {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.bedrock.model-id:anthropic.claude-v2}")
    private String modelId;

    private final ObjectMapper objectMapper;
    private BedrockRuntimeClient bedrockClient;

    public AIService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private BedrockRuntimeClient getBedrockClient() {
        if (bedrockClient == null) {
            try {
                bedrockClient = BedrockRuntimeClient.builder()
                        .region(Region.of(awsRegion))
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build();
            } catch (Exception e) {
                log.warn("Could not initialize Bedrock client: {}. Using mock mode.", e.getMessage());
            }
        }
        return bedrockClient;
    }

    public String processWithAI(String prompt) {
        log.debug("Processing AI prompt: {}", prompt);
        
        try {
            BedrockRuntimeClient client = getBedrockClient();
            if (client == null) {
                return processMockAI(prompt);
            }

            // Prepare the request body for Claude
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("prompt", "\n\nHuman: " + prompt + "\n\nAssistant:");
            requestBody.put("max_tokens_to_sample", 2000);
            requestBody.put("temperature", 0.7);
            requestBody.put("top_p", 1.0);

            String requestBodyString = objectMapper.writeValueAsString(requestBody);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(requestBodyString))
                    .build();

            InvokeModelResponse response = client.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            String completion = (String) responseMap.get("completion");
            
            log.debug("AI response received: {}", completion);
            return completion != null ? completion.trim() : "No response from AI";
            
        } catch (Exception e) {
            log.error("Error processing with AI: {}", e.getMessage());
            return processMockAI(prompt);
        }
    }

    private String processMockAI(String prompt) {
        log.info("Using mock AI processing for prompt");
        return "Mock AI Response: Processed task - " + prompt.substring(0, Math.min(50, prompt.length())) + "...";
    }

    public Map<String, String> analyzeTask(String taskDescription) {
        String analysisPrompt = "Analyze the following task and provide: 1) Task complexity (LOW/MEDIUM/HIGH), " +
                "2) Estimated time, 3) Required skills. Task: " + taskDescription;
        
        String response = processWithAI(analysisPrompt);
        
        Map<String, String> analysis = new HashMap<>();
        analysis.put("complexity", "MEDIUM");
        analysis.put("estimatedTime", "2 hours");
        analysis.put("requiredSkills", "General processing");
        analysis.put("aiAnalysis", response);
        
        return analysis;
    }

    public String generateTaskPlan(String taskDescription) {
        String planPrompt = "Create a step-by-step execution plan for this task: " + taskDescription;
        return processWithAI(planPrompt);
    }
}
