package com.rajathgoku.agentic.backend.engine;

import com.rajathgoku.agentic.backend.entity.Run;
import com.rajathgoku.agentic.backend.entity.RunStatus;
import com.rajathgoku.agentic.backend.entity.Step;
import com.rajathgoku.agentic.backend.entity.StepStatus;
import com.rajathgoku.agentic.backend.entity.Artifact;
import com.rajathgoku.agentic.backend.service.RunService;
import com.rajathgoku.agentic.backend.repository.RunRepository;
import com.rajathgoku.agentic.backend.repository.StepRepository;
import com.rajathgoku.agentic.backend.repository.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@EnableScheduling
@EnableAsync
public class RunOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(RunOrchestrator.class);

    private final RunRepository runRepository;
    private final StepRepository stepRepository;
    private final ArtifactRepository artifactRepository;
    private final RunService runService;

    public RunOrchestrator(RunRepository runRepository,
                           StepRepository stepRepository,
                           ArtifactRepository artifactRepository,
                           RunService runService) {
        this.runRepository = runRepository;
        this.stepRepository = stepRepository;
        this.artifactRepository = artifactRepository;
        this.runService = runService;
    }

    private volatile boolean isRunning = true;

    /**
     * Scheduled method that claims PENDING runs every 3 seconds
     * Uses atomic claiming to prevent double processing
     */
    @Scheduled(fixedDelay = 3000) // Poll every 3 seconds
    public void pollAndClaimRuns() {
        if (!isRunning) {
            return;
        }

        try {
            // Attempt to atomically claim one PENDING run
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
    @Scheduled(fixedDelay = 120000) // Every 2 minutes
    public void cleanupStaleRuns() {
        try {
            runService.cleanupStaleRuns(Duration.ofMinutes(10)); // Cleanup runs older than 10 minutes
        } catch (Exception e) {
            logger.error("Error during stale run cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes a single run asynchronously with comprehensive execution trace
     */
    @Async
    @Transactional
    public CompletableFuture<Void> processRunAsync(Run run) {
        logger.info("Starting async processing of claimed run ID: {}", run.getId());
        
        Step planningStep = null;
        Step executionStep = null;
        Step validationStep = null;
        
        try {
            // === PLANNING PHASE ===
            planningStep = createStep("Planning", "Analyzing task and creating execution plan", run);
            planningStep.setStatus(StepStatus.RUNNING);
            stepRepository.save(planningStep);
            logger.info("Started planning step ID: {} for run ID: {}", planningStep.getId(), run.getId());

            // Simulate planning work
            Thread.sleep(1000);
            
            // Create planning artifacts
            createArtifact("execution_plan", "TEXT", 
                          "Execution plan for task " + run.getTaskId() + ":\n1. Analyze requirements\n2. Execute main logic\n3. Validate results", 
                          planningStep);
            
            planningStep.setStatus(StepStatus.DONE);
            planningStep.setFinishedAt(Instant.now());
            stepRepository.save(planningStep);
            logger.info("Completed planning step ID: {}", planningStep.getId());

            // === EXECUTION PHASE ===
            executionStep = createStep("Execution", "Main task execution", run);
            executionStep.setStatus(StepStatus.RUNNING);
            stepRepository.save(executionStep);
            logger.info("Started execution step ID: {} for run ID: {}", executionStep.getId(), run.getId());

            // Simulate main work
            Thread.sleep(2000);
            
            // Create execution artifacts
            String resultData = "Task " + run.getTaskId() + " executed successfully at " + Instant.now() + 
                               " by instance " + runService.getInstanceId();
            createArtifact("execution_result", "TEXT", resultData, executionStep);
            
            // Create a JSON artifact with structured data
            String jsonResult = String.format(
                "{\"taskId\": \"%s\", \"runId\": \"%s\", \"timestamp\": \"%s\", \"status\": \"success\", \"processingTime\": \"2000ms\"}",
                run.getTaskId(), run.getId(), Instant.now()
            );
            createArtifact("result_metadata", "JSON", jsonResult, executionStep);
            
            executionStep.setStatus(StepStatus.DONE);
            executionStep.setFinishedAt(Instant.now());
            stepRepository.save(executionStep);
            logger.info("Completed execution step ID: {}", executionStep.getId());

            // === VALIDATION PHASE ===
            validationStep = createStep("Validation", "Validating execution results", run);
            validationStep.setStatus(StepStatus.RUNNING);
            stepRepository.save(validationStep);
            logger.info("Started validation step ID: {} for run ID: {}", validationStep.getId(), run.getId());

            // Simulate validation
            Thread.sleep(500);
            
            // Create validation artifacts
            createArtifact("validation_report", "TEXT", 
                          "Validation completed successfully. All checks passed.", validationStep);
            
            validationStep.setStatus(StepStatus.DONE);
            validationStep.setFinishedAt(Instant.now());
            stepRepository.save(validationStep);
            logger.info("Completed validation step ID: {}", validationStep.getId());

            // Mark the entire run as completed
            runService.markRunAsCompleted(run.getId());
            logger.info("Successfully completed run ID: {} with {} steps", run.getId(), 3);

        } catch (InterruptedException e) {
            logger.warn("Run ID: {} was interrupted, marking as FAILED", run.getId());
            
            handleStepFailure(planningStep, "Run was interrupted: " + e.getMessage());
            handleStepFailure(executionStep, "Run was interrupted: " + e.getMessage());
            handleStepFailure(validationStep, "Run was interrupted: " + e.getMessage());
            
            runService.markRunAsFailed(run.getId(), "Run was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            
        } catch (Exception e) {
            logger.error("Error processing run ID: {}, marking as FAILED. Error: {}", 
                        run.getId(), e.getMessage(), e);
            
            handleStepFailure(planningStep, "Execution failed: " + e.getMessage());
            handleStepFailure(executionStep, "Execution failed: " + e.getMessage());
            handleStepFailure(validationStep, "Execution failed: " + e.getMessage());
            
            runService.markRunAsFailed(run.getId(), "Execution failed: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    private Step createStep(String name, String description, Run run) {
        Step step = new Step(name, description, run);
        step.setStartedAt(Instant.now());
        return stepRepository.save(step);
    }

    private Artifact createArtifact(String name, String type, String content, Step step) {
        Artifact artifact = new Artifact(name, type, content, step);
        artifact.setSize((long) content.length());
        Artifact saved = artifactRepository.save(artifact);
        logger.debug("Created artifact ID: {} ({}) for step ID: {}", 
                    saved.getId(), name, step.getId());
        return saved;
    }

    private void handleStepFailure(Step step, String errorMessage) {
        if (step != null && step.getStatus() == StepStatus.RUNNING) {
            step.setStatus(StepStatus.FAILED);
            step.setFinishedAt(Instant.now());
            stepRepository.save(step);
            
            // Create error artifact
            createArtifact("error_details", "TEXT", errorMessage, step);
            logger.info("Marked step ID: {} as FAILED", step.getId());
        }
    }

    /**
     * Manually process a specific run (can be called from external services)
     */
    public void processRun(UUID runId) {
        try {
            Optional<Run> runOpt = runService.getRunById(runId);
            if (runOpt.isPresent()) {
                Run run = runOpt.get();
                if (run.canBeClaimed()) {
                    // Try to claim and process
                    run.markAsRunning(runService.getInstanceId());
                    runRepository.save(run);
                    processRunAsync(run);
                } else {
                    logger.warn("Run ID: {} cannot be claimed, current status: {}", 
                               runId, run.getStatus());
                }
            } else {
                logger.warn("Run ID: {} not found", runId);
            }
        } catch (Exception e) {
            logger.error("Error manually processing run ID: {}, Error: {}", runId, e.getMessage(), e);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stop() {
        logger.info("Stopping RunOrchestrator");
        isRunning = false;
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

            return new RunStatistics(pendingCount, runningCount, doneCount, failedCount);
        } catch (Exception e) {
            logger.error("Error getting run statistics: {}", e.getMessage(), e);
            return new RunStatistics(0, 0, 0, 0);
        }
    }

    public static class RunStatistics {
        private final long pending;
        private final long running;
        private final long done;
        private final long failed;

        public RunStatistics(long pending, long running, long done, long failed) {
            this.pending = pending;
            this.running = running;
            this.done = done;
            this.failed = failed;
        }

        public long getPending() { return pending; }
        public long getRunning() { return running; }
        public long getDone() { return done; }
        public long getFailed() { return failed; }
        public long getTotal() { return pending + running + done + failed; }

        @Override
        public String toString() {
            return String.format("RunStatistics{pending=%d, running=%d, done=%d, failed=%d, total=%d}", 
                               pending, running, done, failed, getTotal());
        }
    }
}