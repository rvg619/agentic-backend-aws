package com.rajathgoku.agentic.backend.engine;

import com.rajathgoku.agentic.backend.entity.Run;
import com.rajathgoku.agentic.backend.entity.Step;
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

import java.time.Instant;
import java.util.List;
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
     * Scheduled method that polls for PENDING runs every 5 seconds
     * and processes them asynchronously
     */
    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    public void pollAndProcessRuns() {
        if (!isRunning) {
            return;
        }

        try {
            List<Run> pendingRuns = runService.getRunsByStatus(Run.RunStatus.PENDING);
            
            if (!pendingRuns.isEmpty()) {
                logger.info("Found {} PENDING runs to process", pendingRuns.size());
                
                for (Run run : pendingRuns) {
                    processRunAsync(run);
                }
            } else {
                logger.debug("No PENDING runs found");
            }
        } catch (Exception e) {
            logger.error("Error while polling for runs: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes a single run asynchronously
     */
    @Async
    public CompletableFuture<Void> processRunAsync(Run run) {
        logger.info("Starting async processing of run ID: {}", run.getId());
        
        Step currentStep = null;
        
        try {
            // Mark the run as RUNNING
            run.setStatus(Run.RunStatus.RUNNING);
            runRepository.save(run);
            logger.info("Run ID: {} marked as RUNNING", run.getId());

            // Create a Step entity for this execution
            currentStep = new Step();
            currentStep.setName("Main execution step");
            currentStep.setDescription("Processing run ID: " + run.getId());
            currentStep.setRun(run);
            currentStep.setStatus(Step.StepStatus.RUNNING);
            currentStep.setStartedAt(Instant.now());
            currentStep = stepRepository.save(currentStep);
            logger.info("Created step ID: {} for run ID: {}", currentStep.getId(), run.getId());

            // Simulate work - sleep for 2-3 seconds
            Thread.sleep(2000);
            logger.info("Run ID: {} - work simulation completed", run.getId());

            // Finish Step
            currentStep.setStatus(Step.StepStatus.DONE);
            currentStep.setFinishedAt(Instant.now());
            stepRepository.save(currentStep);
            logger.info("Step ID: {} marked as DONE", currentStep.getId());

            // Create an Artifact with the result
            Artifact resultArtifact = new Artifact();
            resultArtifact.setName("execution_result");
            resultArtifact.setType("TEXT");
            resultArtifact.setContent("Hello from step execution! Run " + run.getId() + 
                                    " executed successfully at " + Instant.now().toString() + 
                                    ". Task ID: " + run.getTaskId());
            resultArtifact.setStep(currentStep);
            resultArtifact.setCreatedAt(Instant.now());
            resultArtifact.setSize((long) resultArtifact.getContent().length());
            artifactRepository.save(resultArtifact);
            logger.info("Created artifact ID: {} for step ID: {}", resultArtifact.getId(), currentStep.getId());

            // Mark the run as DONE
            run.setStatus(Run.RunStatus.DONE);
            runRepository.save(run);
            logger.info("Run ID: {} marked as DONE", run.getId());

        } catch (InterruptedException e) {
            logger.warn("Run ID: {} was interrupted, marking as FAILED", run.getId());
            
            // Mark step as FAILED if it exists
            if (currentStep != null) {
                currentStep.setStatus(Step.StepStatus.FAILED);
                currentStep.setFinishedAt(Instant.now());
                stepRepository.save(currentStep);
                
                // Create error artifact
                Artifact errorArtifact = new Artifact();
                errorArtifact.setName("error_result");
                errorArtifact.setType("TEXT");
                errorArtifact.setContent("Run " + run.getId() + " was interrupted: " + e.getMessage());
                errorArtifact.setStep(currentStep);
                errorArtifact.setCreatedAt(Instant.now());
                errorArtifact.setSize((long) errorArtifact.getContent().length());
                artifactRepository.save(errorArtifact);
            }
            
            run.setStatus(Run.RunStatus.FAILED);
            runRepository.save(run);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Error processing run ID: {}, marking as FAILED. Error: {}", 
                        run.getId(), e.getMessage(), e);
            
            // Mark step as FAILED if it exists
            if (currentStep != null) {
                currentStep.setStatus(Step.StepStatus.FAILED);
                currentStep.setFinishedAt(Instant.now());
                stepRepository.save(currentStep);
                
                // Create error artifact
                Artifact errorArtifact = new Artifact();
                errorArtifact.setName("error_result");
                errorArtifact.setType("TEXT");
                errorArtifact.setContent("Run " + run.getId() + " failed with error: " + e.getMessage());
                errorArtifact.setStep(currentStep);
                errorArtifact.setCreatedAt(Instant.now());
                errorArtifact.setSize((long) errorArtifact.getContent().length());
                artifactRepository.save(errorArtifact);
            }
            
            run.setStatus(Run.RunStatus.FAILED);
            runRepository.save(run);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Manually process a specific run (can be called from external services)
     */
    public void processRun(Long runId) {
        try {
            Run run = runService.getRunById(runId).orElse(null);
            if (run != null && run.getStatus() == Run.RunStatus.PENDING) {
                processRunAsync(run);
            } else if (run == null) {
                logger.warn("Run ID: {} not found", runId);
            } else {
                logger.warn("Run ID: {} is not in PENDING status, current status: {}", 
                           runId, run.getStatus());
            }
        } catch (Exception e) {
            logger.error("Error manually processing run ID: {}, Error: {}", runId, e.getMessage(), e);
        }
    }

    /**
     * Get orchestrator status
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Stop the orchestrator (for graceful shutdown)
     */
    public void stop() {
        logger.info("Stopping RunOrchestrator");
        isRunning = false;
    }

    /**
     * Start the orchestrator
     */
    public void start() {
        logger.info("Starting RunOrchestrator");
        isRunning = true;
    }

    /**
     * Get statistics about runs
     */
    public RunStatistics getStatistics() {
        try {
            long pendingCount = runService.getRunsByStatus(Run.RunStatus.PENDING).size();
            long runningCount = runService.getRunsByStatus(Run.RunStatus.RUNNING).size();
            long doneCount = runService.getRunsByStatus(Run.RunStatus.DONE).size();
            long failedCount = runService.getRunsByStatus(Run.RunStatus.FAILED).size();

            return new RunStatistics(pendingCount, runningCount, doneCount, failedCount);
        } catch (Exception e) {
            logger.error("Error getting run statistics: {}", e.getMessage(), e);
            return new RunStatistics(0, 0, 0, 0);
        }
    }

    /**
     * Inner class to hold run statistics
     */
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