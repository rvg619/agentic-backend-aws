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
        
        // Use LLM to make intelligent assessment instead of simple keyword matching
        String prompt = String.format(
            "You are evaluating whether a task execution should be considered successful. " +
            "Please analyze the task and results, then respond with ONLY 'SUCCESS' or 'FAILURE' (no other text).\n\n" +
            "Original Task: %s\n\n" +
            "Step Results:\n%s\n\n" +
            "Overall Evaluation: %s\n\n" +
            "Consider that:\n" +
            "- Appropriately handling invalid/nonsensical inputs is SUCCESS\n" +
            "- Clear explanations of why something cannot be done is SUCCESS\n" +
            "- Partial completion with good reasoning is SUCCESS\n" +
            "- Only mark as FAILURE if there were genuine errors or complete inability to help\n\n" +
            "Decision (SUCCESS or FAILURE):",
            taskDescription,
            String.join("\n", stepResults),
            overallEvaluation
        );
        
        try {
            String llmResponse = llmClient.generateResponse(prompt);
            String decision = llmResponse.trim().toUpperCase();
            
            // Log the decision for debugging
            System.out.println("LLM Success Decision: " + decision);
            
            return decision.contains("SUCCESS");
        } catch (Exception e) {
            // Fallback to improved heuristic if LLM fails
            return evaluateSuccessWithImprovedHeuristic(stepResults, overallEvaluation);
        }
    }
    
    /**
     * Improved heuristic fallback for success evaluation
     */
    private boolean evaluateSuccessWithImprovedHeuristic(String[] stepResults, String overallEvaluation) {
        // Check if most steps completed successfully (improved keyword analysis)
        int meaningfulSteps = 0;
        int positiveSteps = 0;
        
        for (String result : stepResults) {
            if (result != null && !result.trim().isEmpty()) {
                meaningfulSteps++;
                String lowerResult = result.toLowerCase();
                
                // Positive indicators
                if (lowerResult.contains("completed") || lowerResult.contains("successful") || 
                    lowerResult.contains("implemented") || lowerResult.contains("created") ||
                    lowerResult.contains("explained") || lowerResult.contains("provided") ||
                    lowerResult.contains("cannot") && lowerResult.contains("because") ||
                    lowerResult.contains("unable") && lowerResult.contains("reason")) {
                    positiveSteps++;
                }
                
                // Don't penalize appropriate responses to invalid inputs
                if ((lowerResult.contains("nonsensical") || lowerResult.contains("unclear") || 
                     lowerResult.contains("invalid")) && lowerResult.contains("help")) {
                    positiveSteps++;
                }
            }
        }
        
        // Also check the overall evaluation tone
        if (overallEvaluation != null) {
            String lowerEval = overallEvaluation.toLowerCase();
            if (lowerEval.contains("worked well") || lowerEval.contains("appropriate") || 
                lowerEval.contains("correctly") || lowerEval.contains("well handled")) {
                positiveSteps++; // Boost for positive overall evaluation
            }
        }
        
        // Consider run successful if at least 50% of steps are meaningful and positive
        // or if there's clear positive evaluation
        return meaningfulSteps > 0 && ((double) positiveSteps / meaningfulSteps >= 0.5);
    }
    
    /**
     * Evaluate run with comprehensive artifact creation
     */
    public CriticResult evaluateRunWithArtifacts(String taskDescription, String[] stepResults) {
        String evaluation = evaluateRun(taskDescription, stepResults);
        boolean isSuccessful = isRunSuccessful(taskDescription, stepResults, evaluation);
        
        // Create comprehensive critique artifacts
        String detailedReport = createDetailedEvaluationReport(taskDescription, stepResults, evaluation, isSuccessful);
        String qualityMetrics = generateQualityMetrics(stepResults);
        String improvementSuggestions = generateImprovementSuggestions(taskDescription, stepResults);
        String executiveSummary = generateExecutiveSummary(taskDescription, evaluation, isSuccessful);
        
        return new CriticResult(evaluation, detailedReport, qualityMetrics, improvementSuggestions, executiveSummary, isSuccessful);
    }
    
    /**
     * Create detailed evaluation report
     */
    private String createDetailedEvaluationReport(String taskDescription, String[] stepResults, String evaluation, boolean isSuccessful) {
        StringBuilder report = new StringBuilder();
        report.append("# COMPREHENSIVE EVALUATION REPORT\n\n");
        report.append("**Generated by**: Critic Agent\n");
        report.append("**Timestamp**: ").append(java.time.Instant.now().toString()).append("\n");
        report.append("**Overall Status**: ").append(isSuccessful ? "✅ SUCCESS" : "❌ NEEDS IMPROVEMENT").append("\n\n");
        
        report.append("## Task Overview\n");
        report.append("**Original Task**: ").append(taskDescription).append("\n\n");
        
        report.append("## Step-by-Step Analysis\n");
        for (int i = 0; i < stepResults.length; i++) {
            report.append("### Step ").append(i + 1).append("\n");
            report.append("**Result**: ").append(stepResults[i] != null ? stepResults[i] : "No result available").append("\n");
            report.append("**Status**: ").append(evaluateStepQuality(stepResults[i])).append("\n\n");
        }
        
        report.append("## Overall Evaluation\n");
        report.append(evaluation).append("\n\n");
        
        report.append("## Quality Indicators\n");
        report.append("- **Completion Rate**: ").append(calculateCompletionRate(stepResults)).append("%\n");
        report.append("- **Step Success Rate**: ").append(calculateSuccessRate(stepResults)).append("%\n");
        report.append("- **Overall Quality**: ").append(isSuccessful ? "High" : "Needs Improvement").append("\n\n");
        
        report.append("---\n");
        report.append("*This report was generated automatically by the Agentic AI system's Critic Agent*\n");
        
        return report.toString();
    }
    
    /**
     * Generate quality metrics in JSON format
     */
    private String generateQualityMetrics(String[] stepResults) {
        int totalSteps = stepResults.length;
        int completedSteps = 0;
        int successfulSteps = 0;
        int failedSteps = 0;
        
        for (String result : stepResults) {
            if (result != null && !result.trim().isEmpty()) {
                completedSteps++;
                String lowerResult = result.toLowerCase();
                if (lowerResult.contains("completed") || lowerResult.contains("successful") || 
                    lowerResult.contains("implemented") || lowerResult.contains("created")) {
                    successfulSteps++;
                } else if (lowerResult.contains("failed") || lowerResult.contains("error") || 
                          lowerResult.contains("unsuccessful")) {
                    failedSteps++;
                }
            }
        }
        
        return String.format("""
            {
                "evaluation_metrics": {
                    "total_steps": %d,
                    "completed_steps": %d,
                    "successful_steps": %d,
                    "failed_steps": %d,
                    "completion_rate": %.2f,
                    "success_rate": %.2f,
                    "quality_score": %.2f,
                    "timestamp": "%s"
                },
                "performance_indicators": {
                    "efficiency": "%s",
                    "completeness": "%s",
                    "reliability": "%s"
                }
            }
            """, 
            totalSteps, completedSteps, successfulSteps, failedSteps,
            (double) completedSteps / totalSteps * 100,
            (double) successfulSteps / totalSteps * 100,
            calculateQualityScore(stepResults),
            java.time.Instant.now().toString(),
            getEfficiencyRating(successfulSteps, totalSteps),
            getCompletenessRating(completedSteps, totalSteps),
            getReliabilityRating(failedSteps, totalSteps)
        );
    }
    
    /**
     * Generate improvement suggestions
     */
    private String generateImprovementSuggestions(String taskDescription, String[] stepResults) {
        StringBuilder suggestions = new StringBuilder();
        suggestions.append("# IMPROVEMENT RECOMMENDATIONS\n\n");
        
        suggestions.append("## Process Optimizations\n");
        if (calculateSuccessRate(stepResults) < 80) {
            suggestions.append("- **Step Execution**: Consider adding more validation and error handling\n");
            suggestions.append("- **Quality Control**: Implement intermediate checkpoints\n");
        }
        
        if (calculateCompletionRate(stepResults) < 90) {
            suggestions.append("- **Completeness**: Ensure all steps produce meaningful outputs\n");
            suggestions.append("- **Monitoring**: Add better progress tracking\n");
        }
        
        suggestions.append("\n## Technical Recommendations\n");
        suggestions.append("- Add unit tests for critical components\n");
        suggestions.append("- Implement rollback mechanisms for failed steps\n");
        suggestions.append("- Consider parallel execution for independent steps\n");
        suggestions.append("- Add comprehensive logging and monitoring\n\n");
        
        suggestions.append("## Best Practices\n");
        suggestions.append("- Follow established coding standards\n");
        suggestions.append("- Document all major decisions\n");
        suggestions.append("- Include error handling in all components\n");
        suggestions.append("- Regularly review and update processes\n\n");
        
        return suggestions.toString();
    }
    
    /**
     * Generate executive summary
     */
    private String generateExecutiveSummary(String taskDescription, String evaluation, boolean isSuccessful) {
        StringBuilder summary = new StringBuilder();
        summary.append("# EXECUTIVE SUMMARY\n\n");
        summary.append("**Task**: ").append(taskDescription).append("\n");
        summary.append("**Status**: ").append(isSuccessful ? "✅ COMPLETED SUCCESSFULLY" : "⚠️ REQUIRES ATTENTION").append("\n");
        summary.append("**Date**: ").append(java.time.LocalDateTime.now().toString()).append("\n\n");
        
        summary.append("## Key Findings\n");
        if (isSuccessful) {
            summary.append("- Task completed within expected parameters\n");
            summary.append("- All critical objectives achieved\n");
            summary.append("- System performed reliably throughout execution\n");
        } else {
            summary.append("- Task completion encountered some challenges\n");
            summary.append("- Some objectives may require additional attention\n");
            summary.append("- Recommendations provided for improvement\n");
        }
        
        summary.append("\n## Strategic Impact\n");
        summary.append("This task execution demonstrates the capabilities of the agentic AI system ");
        summary.append("in handling complex multi-step operations with automated planning, ");
        summary.append("execution, and quality assessment.\n\n");
        
        summary.append("## Next Steps\n");
        if (isSuccessful) {
            summary.append("- Archive results for future reference\n");
            summary.append("- Consider scaling similar tasks\n");
        } else {
            summary.append("- Review detailed recommendations\n");
            summary.append("- Implement suggested improvements\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Helper methods for quality assessment
     */
    private String evaluateStepQuality(String stepResult) {
        if (stepResult == null || stepResult.trim().isEmpty()) return "❌ No Output";
        
        String lower = stepResult.toLowerCase();
        if (lower.contains("completed") || lower.contains("successful") || lower.contains("implemented")) {
            return "✅ Success";
        } else if (lower.contains("failed") || lower.contains("error")) {
            return "❌ Failed";
        }
        return "⚠️ Partial";
    }
    
    private double calculateCompletionRate(String[] stepResults) {
        int completed = 0;
        for (String result : stepResults) {
            if (result != null && !result.trim().isEmpty()) completed++;
        }
        return stepResults.length > 0 ? (double) completed / stepResults.length * 100 : 0;
    }
    
    private double calculateSuccessRate(String[] stepResults) {
        int successful = 0;
        for (String result : stepResults) {
            if (result != null && !result.trim().isEmpty()) {
                String lower = result.toLowerCase();
                if (lower.contains("completed") || lower.contains("successful") || lower.contains("implemented")) {
                    successful++;
                }
            }
        }
        return stepResults.length > 0 ? (double) successful / stepResults.length * 100 : 0;
    }
    
    private double calculateQualityScore(String[] stepResults) {
        return (calculateCompletionRate(stepResults) + calculateSuccessRate(stepResults)) / 2;
    }
    
    private String getEfficiencyRating(int successful, int total) {
        double rate = total > 0 ? (double) successful / total : 0;
        if (rate >= 0.9) return "Excellent";
        if (rate >= 0.7) return "Good";
        if (rate >= 0.5) return "Fair";
        return "Needs Improvement";
    }
    
    private String getCompletenessRating(int completed, int total) {
        double rate = total > 0 ? (double) completed / total : 0;
        if (rate >= 0.95) return "Complete";
        if (rate >= 0.8) return "Mostly Complete";
        if (rate >= 0.6) return "Partially Complete";
        return "Incomplete";
    }
    
    private String getReliabilityRating(int failed, int total) {
        double rate = total > 0 ? (double) failed / total : 0;
        if (rate <= 0.1) return "Highly Reliable";
        if (rate <= 0.2) return "Reliable";
        if (rate <= 0.3) return "Moderately Reliable";
        return "Needs Reliability Improvements";
    }
    
    /**
     * Result record for critic evaluation with multiple artifacts
     */
    public record CriticResult(String evaluation, String detailedReport, String qualityMetrics, 
                              String improvementSuggestions, String executiveSummary, boolean isSuccessful) {}
}