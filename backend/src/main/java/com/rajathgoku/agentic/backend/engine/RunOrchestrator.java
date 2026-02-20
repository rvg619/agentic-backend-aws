package com.rajathgoku.agentic.backend.engine;

import com.rajathgoku.agentic.backend.entity.Run;
import com.rajathgoku.agentic.backend.entity.RunStatus;
import com.rajathgoku.agentic.backend.entity.Step;
import com.rajathgoku.agentic.backend.entity.Task;
import com.rajathgoku.agentic.backend.service.RunService;
import com.rajathgoku.agentic.backend.service.StepService;
import com.rajathgoku.agentic.backend.service.ArtifactService;
import com.rajathgoku.agentic.backend.service.TaskService;
import com.rajathgoku.agentic.backend.agent.PlannerAgent;
import com.rajathgoku.agentic.backend.agent.ExecutorAgent;
import com.rajathgoku.agentic.backend.agent.CriticAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;

@Component
@EnableScheduling
@EnableAsync
public class RunOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(RunOrchestrator.class);

    // Configuration properties
    @Value("${orchestrator.worker.pool.size:4}")
    private int workerPoolSize;
    
    @Value("${orchestrator.step.timeout.minutes:10}")
    private int stepTimeoutMinutes;
    
    @Value("${orchestrator.step.max.retries:3}")
    private int maxRetries;
    
    @Value("${orchestrator.retry.backoff.seconds:5}")
    private int retryBackoffSeconds;

    private final RunService runService;
    private final StepService stepService;
    private final ArtifactService artifactService;
    private final TaskService taskService;
    private final PlannerAgent plannerAgent;
    private final ExecutorAgent executorAgent;
    private final CriticAgent criticAgent;

    // Worker pool for concurrent step execution
    private ThreadPoolExecutor stepExecutorPool;
    private final AtomicInteger activeWorkers = new AtomicInteger(0);

    public RunOrchestrator(RunService runService,
                          StepService stepService,
                          ArtifactService artifactService,
                          TaskService taskService,
                          PlannerAgent plannerAgent,
                          ExecutorAgent executorAgent,
                          CriticAgent criticAgent) {
        this.runService = runService;
        this.stepService = stepService;
        this.artifactService = artifactService;
        this.taskService = taskService;
        this.plannerAgent = plannerAgent;
        this.executorAgent = executorAgent;
        this.criticAgent = criticAgent;
    }

    @PostConstruct
    public void initializeThreadPool() {
        // Initialize worker pool with configurable size after Spring injection
        this.stepExecutorPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(workerPoolSize);
        logger.info("Initialized RunOrchestrator with {} worker threads", workerPoolSize);
    }

    private volatile boolean isRunning = true;

    /**
     * Scheduled method that claims PENDING runs every 3 seconds
     */
    @Scheduled(fixedDelay = 3000)
    public void pollAndClaimRuns() {
        if (!isRunning) {
            return;
        }

        try {
            Optional<Run> claimedRun = runService.claimNextPendingRun();
            
            if (claimedRun.isPresent()) {
                logger.info("Claimed run ID: {} for processing", claimedRun.get().getId());
                processRunAsync(claimedRun.get());
            } else {
                logger.debug("No PENDING runs available to claim");
            }
            
        } catch (Exception e) {
            logger.error("Error while polling for runs: {}", e.getMessage(), e);
        }
    }

    /**
     * Cleanup stale runs every 2 minutes
     */
    @Scheduled(fixedDelay = 120000)
    public void cleanupStaleRuns() {
        try {
            runService.cleanupStaleRuns(Duration.ofMinutes(10));
        } catch (Exception e) {
            logger.error("Error during stale run cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Crash recovery: Resume RUNNING steps every 5 minutes
     */
    @Scheduled(fixedDelay = 300000)
    public void recoverStaleRunningSteps() {
        if (!isRunning) {
            return;
        }
        
        try {
            List<Run> staleRunningRuns = runService.getStaleRunningRuns(Duration.ofMinutes(15));
            
            for (Run run : staleRunningRuns) {
                logger.warn("Recovering stale RUNNING run ID: {}", run.getId());
                
                // Reset run status to PENDING so it can be picked up again
                runService.resetRunToPending(run.getId());
                
                // Reset any RUNNING steps to PENDING for retry
                List<Step> runningSteps = stepService.getRunningStepsForRun(run.getId());
                for (Step step : runningSteps) {
                    logger.warn("Resetting stale RUNNING step ID: {} to PENDING", step.getId());
                    stepService.resetStepToPending(step.getId());
                }
            }
            
            if (!staleRunningRuns.isEmpty()) {
                logger.info("Recovered {} stale running runs", staleRunningRuns.size());
            }
            
        } catch (Exception e) {
            logger.error("Error during crash recovery: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes a run using AI agents with improved concurrency handling
     * 🗄️ DATABASE OPERATIONS HIGHLIGHTED THROUGHOUT
     */
    @Async
    public CompletableFuture<Void> processRunAsync(Run run) {
        int workerId = activeWorkers.incrementAndGet();
        logger.info("Worker #{} starting AI-driven processing of run ID: {}", workerId, run.getId());
        
        try {
            // 🔍 DB READ: Get the task details from database
            Task task = taskService.getTaskById(run.getTaskId());
            String taskDescription = task.getDescription();
            logger.info("Worker #{} processing task: {}", workerId, taskDescription);

            // === PHASE 1: PLANNING ===
            Step planningStep = executeStepWithRetry(run, "AI Planning Phase", 1, () -> {
                logger.info("Worker #{} Phase 1: Planning with AI agent for run ID: {}", workerId, run.getId());
                return plannerAgent.createPlan(taskDescription);
            }).get();

            String executionPlan = planningStep.getResult();
            artifactService.createTextArtifact(planningStep, "execution_plan.txt", executionPlan);
            logger.info("Worker #{} Planning completed for run ID: {}", workerId, run.getId());

            // === PHASE 2: SEQUENTIAL EXECUTION (Changed from parallel to avoid race conditions) ===
            String[] planSteps = plannerAgent.breakDownPlan(executionPlan);
            List<Step> completedSteps = new ArrayList<>();
            
            // Execute steps sequentially to avoid concurrency issues
            for (int i = 0; i < planSteps.length; i++) {
                if (planSteps[i].trim().isEmpty()) continue;
                
                final int stepIndex = i;
                final String stepDescription = planSteps[i];
                
                Step stepResult = executeStepWithRetry(run, 
                    "Execute: " + stepDescription.substring(0, Math.min(50, stepDescription.length())), 
                    stepIndex + 2, 
                    () -> {
                        logger.info("Worker #{} Phase 2: Executing step {} of {} for run ID: {}", 
                                  workerId, stepIndex + 1, planSteps.length, run.getId());
                        
                        // Get previous step results for context (thread-safe)
                        String context = null;
                        if (stepIndex > 0 && !completedSteps.isEmpty()) {
                            List<String> previousResults = new ArrayList<>();
                            for (Step prevStep : completedSteps) {
                                if (prevStep != null && prevStep.getResult() != null) {
                                    previousResults.add(prevStep.getResult());
                                }
                            }
                            context = String.join("\n", previousResults);
                        }
                        
                        ExecutorAgent.ExecutionResult result = executorAgent.executeWithArtifact(stepDescription, context);
                        
                        // Store artifact for this step execution
                        Step currentStep = stepService.getCurrentStepForWorker();
                        if (currentStep != null) {
                            artifactService.createTextArtifact(currentStep, 
                                String.format("step_%d_result.txt", stepIndex + 1), 
                                result.artifactContent());
                        }
                        
                        return result.result();
                    }).get(); // Get the result immediately for sequential processing
                
                completedSteps.add(stepResult);
                logger.info("Worker #{} Completed step {} of {} for run ID: {}", 
                          workerId, stepIndex + 1, planSteps.length, run.getId());
            }
            
            logger.info("Worker #{} All {} execution steps completed for run ID: {}", 
                      workerId, completedSteps.size(), run.getId());

            // Collect all step results
            String[] stepResults = new String[completedSteps.size()];
            for (int i = 0; i < completedSteps.size(); i++) {
                Step completedStep = completedSteps.get(i);
                stepResults[i] = completedStep != null ? completedStep.getResult() : "";
            }

            // === PHASE 3: CRITIQUE & EVALUATION ===
            Step critiqueStep = executeStepWithRetry(run, "AI Critique & Evaluation", planSteps.length + 2, () -> {
                logger.info("Worker #{} Phase 3: Critiquing execution with AI agent for run ID: {}", workerId, run.getId());
                String overallEvaluation = criticAgent.evaluateRun(taskDescription, stepResults);
                boolean isSuccessful = criticAgent.isRunSuccessful(taskDescription, stepResults, overallEvaluation);
                
                // Store evaluation report
                Step currentStep = stepService.getCurrentStepForWorker();
                if (currentStep != null) {
                    artifactService.createJsonArtifact(currentStep, "evaluation_report.json", 
                        String.format("{\"success\": %b, \"evaluation\": \"%s\", \"completedSteps\": %d}", 
                            isSuccessful, overallEvaluation.replace("\"", "\\\""), planSteps.length));
                }
                
                return overallEvaluation + "|SUCCESS:" + isSuccessful;
            }).get();

            // Parse critique results
            String critiqueResult = critiqueStep.getResult();
            boolean isSuccessful = critiqueResult.contains("|SUCCESS:true");
            String evaluation = critiqueResult.split("\\|SUCCESS:")[0];

            // === FINALIZE RUN ===
            if (isSuccessful) {
                runService.markRunAsCompleted(run.getId());
                logger.info("Worker #{} ✅ Successfully completed run ID: {} with {} execution steps", 
                          workerId, run.getId(), planSteps.length);
            } else {
                runService.markRunAsFailed(run.getId(), "Critique agent determined execution was not successful: " + evaluation);
                logger.warn("Worker #{} ❌ Run ID: {} marked as failed after critique evaluation", workerId, run.getId());
            }

        } catch (Exception e) {
            logger.error("Worker #{} Error processing run ID: {}, marking as FAILED. Error: {}", workerId, run.getId(), e.getMessage(), e);
            runService.markRunAsFailed(run.getId(), "Execution failed: " + e.getMessage());
        } finally {
            activeWorkers.decrementAndGet();
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Execute a single step with improved retry logic and concurrency handling
     */
    private CompletableFuture<Step> executeStepWithRetry(Run run, String stepName, int stepOrder, 
                                                         StepExecutor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Step step = null;
            Exception lastException = null;
            
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    // Create step only once, reuse for retries
                    if (step == null) {
                        step = stepService.createStep(run, stepName, stepOrder);
                        logger.debug("Created step ID: {} for attempt {}", step.getId(), attempt);
                    }
                    
                    // Start step execution with concurrency protection
                    Step startedStep = stepService.startStep(step);
                    logger.debug("Starting step execution (attempt {}/{}): {}", attempt, maxRetries, stepName);
                    
                    // Set step context for worker thread
                    stepService.setCurrentStepForWorker(startedStep);
                    
                    try {
                        // Execute step with timeout in a separate thread to avoid blocking
                        CompletableFuture<String> stepExecution = CompletableFuture.supplyAsync(() -> {
                            try {
                                return executor.execute();
                            } catch (Exception e) {
                                throw new RuntimeException("Step execution failed", e);
                            }
                        }, stepExecutorPool);
                        
                        String result = stepExecution.get(stepTimeoutMinutes, TimeUnit.MINUTES);
                        
                        // Complete step with result
                        Step completedStep = stepService.completeStep(startedStep, result);
                        logger.debug("Step completed successfully on attempt {}: {}", attempt, stepName);
                        return completedStep;
                        
                    } finally {
                        // Always clear step context
                        stepService.clearCurrentStepForWorker();
                    }
                    
                } catch (TimeoutException e) {
                    lastException = e;
                    logger.warn("Step execution timeout on attempt {}/{} for step: {}", attempt, maxRetries, stepName);
                    if (step != null) {
                        stepService.markStepAsFailed(step, "Timeout after " + stepTimeoutMinutes + " minutes");
                    }
                } catch (Exception e) {
                    lastException = e;
                    logger.warn("Step execution failed on attempt {}/{} for step: {}. Error: {}", 
                              attempt, maxRetries, stepName, e.getMessage());
                    if (step != null) {
                        stepService.markStepAsFailed(step, "Attempt " + attempt + " failed: " + e.getMessage());
                    }
                }
                
                // Wait before retry (except on last attempt)
                if (attempt < maxRetries && step != null) {
                    try {
                        int backoffTime = retryBackoffSeconds * attempt; // Linear backoff
                        Thread.sleep(backoffTime * 1000L);
                        logger.debug("Retrying step after {}s backoff: {}", backoffTime, stepName);
                        
                        // Reset step to pending for retry with concurrency protection
                        step = stepService.resetStepToPending(step.getId());
                        
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            // All retries exhausted
            String errorMsg = String.format("Step failed after %d attempts. Last error: %s", 
                                          maxRetries, lastException != null ? lastException.getMessage() : "Unknown");
            if (step != null) {
                stepService.markStepAsFailed(step, errorMsg);
            }
            throw new RuntimeException(errorMsg, lastException);
            
        }, stepExecutorPool);
    }

    /**
     * Functional interface for step execution
     */
    @FunctionalInterface
    private interface StepExecutor {
        String execute() throws Exception;
    }

    /**
     * Manually process a specific run
     */
    public void processRun(UUID runId) {
        try {
            Optional<Run> runOpt = runService.getRunById(runId);
            if (runOpt.isPresent()) {
                Run run = runOpt.get();
                if (run.canBeClaimed()) {
                    run.markAsRunning(runService.getInstanceId());
                    processRunAsync(run);
                } else {
                    logger.warn("Run ID: {} cannot be claimed, current status: {}", runId, run.getStatus());
                }
            } else {
                logger.warn("Run ID: {} not found", runId);
            }
        } catch (Exception e) {
            logger.error("Error manually processing run ID: {}, Error: {}", runId, e.getMessage(), e);
        }
    }

    public boolean isRunning() { return isRunning; }
    
    public void stop() { 
        logger.info("Stopping RunOrchestrator and shutting down worker pool"); 
        isRunning = false;
        stepExecutorPool.shutdown();
        try {
            if (!stepExecutorPool.awaitTermination(60, TimeUnit.SECONDS)) {
                stepExecutorPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            stepExecutorPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public void start() { 
        logger.info("Starting RunOrchestrator"); 
        isRunning = true; 
    }

    public RunStatistics getStatistics() {
        try {
            long pendingCount = runService.getRunCountByStatus(RunStatus.PENDING);
            long runningCount = runService.getRunCountByStatus(RunStatus.RUNNING);
            long doneCount = runService.getRunCountByStatus(RunStatus.DONE);
            long failedCount = runService.getRunCountByStatus(RunStatus.FAILED);

            return new RunStatistics(pendingCount, runningCount, doneCount, failedCount, 
                                   activeWorkers.get(), stepExecutorPool.getActiveCount(), 
                                   stepExecutorPool.getPoolSize());
        } catch (Exception e) {
            logger.error("Error getting run statistics: {}", e.getMessage(), e);
            return new RunStatistics(0, 0, 0, 0, 0, 0, workerPoolSize);
        }
    }

    public static class RunStatistics {
        private final long pending, running, done, failed;
        private final int activeWorkers, activeThreads, totalThreads;

        public RunStatistics(long pending, long running, long done, long failed, 
                           int activeWorkers, int activeThreads, int totalThreads) {
            this.pending = pending; 
            this.running = running; 
            this.done = done; 
            this.failed = failed;
            this.activeWorkers = activeWorkers;
            this.activeThreads = activeThreads;
            this.totalThreads = totalThreads;
        }

        public long getPending() { return pending; }
        public long getRunning() { return running; }
        public long getDone() { return done; }
        public long getFailed() { return failed; }
        public long getTotal() { return pending + running + done + failed; }
        public int getActiveWorkers() { return activeWorkers; }
        public int getActiveThreads() { return activeThreads; }
        public int getTotalThreads() { return totalThreads; }

        @Override
        public String toString() {
            return String.format("RunStatistics{pending=%d, running=%d, done=%d, failed=%d, total=%d, " +
                               "activeWorkers=%d, activeThreads=%d, totalThreads=%d}", 
                               pending, running, done, failed, getTotal(), 
                               activeWorkers, activeThreads, totalThreads);
        }
    }
}