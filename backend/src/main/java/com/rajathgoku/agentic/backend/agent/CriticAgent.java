package com.rajathgoku.agentic.backend.agent;
import com.rajathgoku.agentic.backend.llm.LlmClient;
import org.springframework.stereotype.Component;

/**
 * The CriticAgent is responsible for evaluating and critiquing the quality of task executions
 * within the agentic AI system. It provides comprehensive analysis of step results, overall
 * run evaluations, and generates detailed reports with recommendations for improvement.
 * 
 * <p>This agent serves as the quality assurance component in the multi-agent orchestration
 * system, working alongside {@link com.rajathgoku.agentic.backend.agent.PlannerAgent} and
 * {@link com.rajathgoku.agentic.backend.agent.ExecutorAgent} to ensure high-quality outputs.</p>
 * 
 * <h2>Key Responsibilities:</h2>
 * <ul>
 *   <li>Review individual step execution results</li>
 *   <li>Evaluate overall task completion quality</li>
 *   <li>Generate comprehensive evaluation reports</li>
 *   <li>Provide improvement recommendations</li>
 *   <li>Determine success/failure status using AI-powered analysis</li>
 * </ul>
 * 
 * <h2>Integration:</h2>
 * <p>The CriticAgent integrates with the {@link com.rajathgoku.agentic.backend.engine.RunOrchestrator}
 * as the final phase of task execution, providing quality assessment and artifact generation.</p>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * CriticAgent critic = new CriticAgent(llmClient);
 * String[] stepResults = {"Step 1 completed", "Step 2 implemented successfully"};
 * CriticResult result = critic.evaluateRunWithArtifacts("Create a web page", stepResults);
 * 
 * if (result.isSuccessful()) {
 *     System.out.println("Task completed successfully: " + result.evaluation());
 * }
 * }</pre>
 * 
 * @author Rajath Gokhale
 * @version 1.0
 * @since 1.0
 * @see com.rajathgoku.agentic.backend.engine.RunOrchestrator
 * @see com.rajathgoku.agentic.backend.agent.PlannerAgent
 * @see com.rajathgoku.agentic.backend.agent.ExecutorAgent
 */
@Component
@SuppressWarnings({"unused", "java:S1220"}) // Suppress IDE warnings about package mismatch
public class CriticAgent {
    
    /** The LLM client used for AI-powered evaluation and analysis */
    private final LlmClient llmClient;
    
    /**
     * Constructs a new CriticAgent with the specified LLM client.
     * 
     * @param llmClient the LLM client for AI-powered evaluations, must not be null
     * @throws IllegalArgumentException if llmClient is null
     */
    public CriticAgent(LlmClient llmClient) {
        if (llmClient == null) {
            throw new IllegalArgumentException("LlmClient cannot be null");
        }
        this.llmClient = llmClient;
    }
    
    /**
     * Reviews and critiques a single step execution result.
     * 
     * <p>This method provides detailed analysis of individual step performance,
     * including quality assessment, completeness evaluation, and specific
     * suggestions for improvement.</p>
     * 
     * @param stepDescription a clear description of what the step was supposed to accomplish
     * @param stepResult the actual result or output produced by the step execution
     * @return a detailed critique and analysis of the step execution quality
     * @throws IllegalArgumentException if stepDescription or stepResult is null or empty
     * @throws RuntimeException if LLM client fails to generate response
     * 
     * @see #evaluateRun(String, String[])
     */
    public String reviewStep(String stepDescription, String stepResult) {
        // Validate input parameters
        if (stepDescription == null || stepDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Step description cannot be null or empty");
        }
        if (stepResult == null || stepResult.trim().isEmpty()) {
            throw new IllegalArgumentException("Step result cannot be null or empty");
        }

        String prompt = String.format(
            "You are a critic agent. Review the following step execution:\n\n" +
            "Step: %s\n\n" +
            "Result: %s\n\n" +
            "Please provide a critical analysis of the execution quality, completeness, and suggestions for improvement.",
            stepDescription,
            stepResult
        );
        
        try {
            return llmClient.generateResponse(prompt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate step review: " + e.getMessage(), e);
        }
    }
    
    /**
     * Evaluates the overall execution of a complete task run.
     * 
     * <p>This method analyzes all step results collectively to provide a comprehensive
     * assessment of task completion quality. It considers the coherence between steps,
     * overall goal achievement, and identifies areas for improvement.</p>
     * 
     * @param taskDescription the original task description that was executed
     * @param stepResults array of results from each execution step, may contain null elements
     * @return a comprehensive evaluation of the overall run quality and completeness
     * @throws IllegalArgumentException if taskDescription is null or empty, or if stepResults is null
     * @throws RuntimeException if LLM client fails to generate response
     * 
     * @see #isRunSuccessful(String, String[], String)
     * @see #evaluateRunWithArtifacts(String, String[])
     */
    public String evaluateRun(String taskDescription, String[] stepResults) {
        // Input validation
        if (taskDescription == null || taskDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Task description cannot be null or empty");
        }
        if (stepResults == null) {
            throw new IllegalArgumentException("Step results array cannot be null");
        }

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
        
        try {
            return llmClient.generateResponse(prompt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate run evaluation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Determines if the run should be marked as successful.
     * 
     * <p>This method uses a heuristic approach to evaluate the success of the task run.
     * It considers the quality of step results and the overall evaluation to make a
     * success/failure decision.</p>
     * 
     * @param taskDescription the original task description that was executed
     * @param stepResults array of results from each execution step, may contain null elements
     * @param overallEvaluation the overall evaluation of the task run
     * @return true if the run is considered successful, false otherwise
     * @throws IllegalArgumentException if taskDescription, stepResults, or overallEvaluation is null
     * @throws RuntimeException if LLM client fails to generate response
     * 
     * @see #evaluateRun(String, String[])
     * @see #evaluateRunWithArtifacts(String, String[])
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
     * Improved heuristic fallback for success evaluation.
     * 
     * <p>This method provides a fallback mechanism for evaluating task success using
     * improved keyword analysis and heuristic rules when the LLM client fails.</p>
     * 
     * @param stepResults array of results from each execution step, may contain null elements
     * @param overallEvaluation the overall evaluation of the task run
     * @return true if the run is considered successful, false otherwise
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
     * Evaluates a run with comprehensive artifact creation.
     * 
     * <p>This method provides a detailed evaluation of the task run, including
     * comprehensive critique artifacts such as detailed reports, quality metrics,
     * improvement suggestions, and executive summaries.</p>
     * 
     * @param taskDescription the original task description that was executed
     * @param stepResults array of results from each execution step, may contain null elements
     * @return a CriticResult object containing the evaluation and generated artifacts
     * @throws IllegalArgumentException if taskDescription or stepResults is null
     * 
     * @see CriticResult
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
     * Creates a detailed evaluation report.
     * 
     * <p>This method generates a comprehensive report detailing the task execution,
     * including step-by-step analysis, overall evaluation, quality indicators, and
     * recommendations for improvement.</p>
     * 
     * @param taskDescription the original task description that was executed
     * @param stepResults array of results from each execution step, may contain null elements
     * @param evaluation the overall evaluation of the task run
     * @param isSuccessful true if the run is considered successful, false otherwise
     * @return a detailed evaluation report as a string
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
     * Generates quality metrics in JSON format.
     * 
     * <p>This method calculates various quality metrics based on the step results
     * and returns them in a JSON formatted string.</p>
     * 
     * @param stepResults array of results from each execution step, may contain null elements
     * @return a JSON formatted string containing quality metrics
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
     * Generates improvement suggestions.
     * 
     * <p>This method provides specific recommendations for process optimizations,
     * technical improvements, and best practices based on the task execution results.</p>
     * 
     * @param taskDescription the original task description that was executed
     * @param stepResults array of results from each execution step, may contain null elements
     * @return a string containing improvement suggestions
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
     * Generates an executive summary.
     * 
     * <p>This method provides a high-level summary of the task execution, including
     * key findings, strategic impact, and next steps based on the evaluation results.</p>
     * 
     * @param taskDescription the original task description that was executed
     * @param evaluation the overall evaluation of the task run
     * @param isSuccessful true if the run is considered successful, false otherwise
     * @return an executive summary as a string
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
     * Helper methods for quality assessment.
     * 
     * <p>These methods provide utility functions for evaluating the quality of step results,
     * calculating completion and success rates, and generating quality scores and ratings.</p>
     * 
     * @param stepResult the result of a single step execution
     * @return a string representing the quality status of the step result
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
     * Result record for critic evaluation with multiple artifacts.
     * 
     * <p>This record encapsulates the results of a comprehensive evaluation, including
     * the overall evaluation, detailed report, quality metrics, improvement suggestions,
     * executive summary, and success status.</p>
     * 
     * @param evaluation the overall evaluation of the task run
     * @param detailedReport a detailed evaluation report
     * @param qualityMetrics quality metrics in JSON format
     * @param improvementSuggestions specific recommendations for improvement
     * @param executiveSummary a high-level summary of the task execution
     * @param isSuccessful true if the run is considered successful, false otherwise
     */
    public record CriticResult(String evaluation, String detailedReport, String qualityMetrics, 
                              String improvementSuggestions, String executiveSummary, boolean isSuccessful) {}
}