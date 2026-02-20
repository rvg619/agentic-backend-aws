package com.rajathgoku.agentic.backend.llm;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Profile("!real")
public class FakeLlmClient implements LlmClient {
    
    private final Random random = ThreadLocalRandom.current();
    
    private static final String[] PLANNING_RESPONSES = {
        "1. Analyze the requirements\n2. Design the solution architecture\n3. Implement core functionality\n4. Test and validate",
        "Step 1: Research existing solutions\nStep 2: Create detailed specifications\nStep 3: Develop prototype\nStep 4: Refine and optimize",
        "• First, gather all necessary resources\n• Then, break down into manageable tasks\n• Execute each task systematically\n• Review and iterate"
    };
    
    private static final String[] EXECUTION_RESPONSES = {
        "Executing task... Created initial implementation with basic functionality. Added error handling and logging.",
        "Task in progress... Implemented core logic, added validation, and integrated with existing systems.",
        "Working on task... Developed solution components, performed testing, and documented the changes."
    };
    
    private static final String[] CRITIQUE_RESPONSES = {
        "Analysis complete. The implementation looks solid. Suggestions: Add more error handling and improve performance.",
        "Review finished. Good approach overall. Consider: Better documentation and additional test coverage.",
        "Evaluation done. Strong foundation established. Recommendations: Optimize algorithms and enhance user experience."
    };
    
    @Override
    public String generateResponse(String prompt) {
        // Simulate realistic LLM response time
        simulateDelay();
        
        // Simple keyword-based response selection for demo
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("plan") || lowerPrompt.contains("strategy") || lowerPrompt.contains("approach")) {
            return PLANNING_RESPONSES[random.nextInt(PLANNING_RESPONSES.length)];
        } else if (lowerPrompt.contains("execute") || lowerPrompt.contains("implement") || lowerPrompt.contains("build")) {
            return EXECUTION_RESPONSES[random.nextInt(EXECUTION_RESPONSES.length)];
        } else if (lowerPrompt.contains("review") || lowerPrompt.contains("critique") || lowerPrompt.contains("analyze")) {
            return CRITIQUE_RESPONSES[random.nextInt(CRITIQUE_RESPONSES.length)];
        }
        
        // Default response
        return "I understand your request: '" + prompt + "'. Here's my analysis and recommended approach for moving forward.";
    }
    
    @Override
    public String generateResponse(List<Message> messages) {
        if (messages.isEmpty()) {
            return "No messages provided.";
        }
        
        // Use the last user message for response generation
        String lastUserMessage = messages.stream()
                .filter(msg -> "user".equals(msg.role()))
                .reduce((first, second) -> second)
                .map(Message::content)
                .orElse("Hello");
                
        return generateResponse(lastUserMessage);
    }
    
    @Override
    public boolean isHealthy() {
        return true; // Fake client is always healthy
    }
    
    @Override
    public String getModelName() {
        return "fake-llm-v1.0";
    }
    
    private void simulateDelay() {
        try {
            // Simulate realistic LLM response time (500ms to 2s)
            Thread.sleep(500 + random.nextInt(1500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}