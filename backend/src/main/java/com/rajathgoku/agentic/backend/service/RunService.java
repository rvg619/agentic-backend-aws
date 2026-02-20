package com.rajathgoku.agentic.backend.service;

import com.rajathgoku.agentic.backend.entity.Run;
import com.rajathgoku.agentic.backend.entity.RunStatus;
import com.rajathgoku.agentic.backend.repository.RunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RunService {

    private static final Logger logger = LoggerFactory.getLogger(RunService.class);

    @Autowired
    private RunRepository runRepository;

    private final String instanceId;

    public RunService() {
        // Generate unique instance identifier for distributed systems
        this.instanceId = generateInstanceId();
    }

    /**
     * Atomically claim one PENDING run and mark it as RUNNING
     * This prevents double processing in distributed systems
     */
    @Transactional
    public Optional<Run> claimNextPendingRun() {
        try {
            // Use pessimistic locking to find and claim the oldest PENDING run
            List<Run> pendingRuns = runRepository.findOldestRunByStatusWithLock(RunStatus.PENDING);
            
            if (pendingRuns.isEmpty()) {
                logger.debug("No PENDING runs available to claim by instance: {}", instanceId);
                return Optional.empty();
            }
            
            // Get the first (oldest) run
            Run runToClaim = pendingRuns.get(0);
            
            // Attempt to update it atomically
            Instant now = Instant.now();
            int updated = runRepository.updateRunStatusById(
                runToClaim.getId(),
                RunStatus.PENDING,
                RunStatus.RUNNING,
                now,
                instanceId
            );
            
            if (updated > 0) {
                // Refresh the entity to get the updated state
                Optional<Run> claimedRun = runRepository.findById(runToClaim.getId());
                if (claimedRun.isPresent()) {
                    logger.info("Successfully claimed run ID: {} by instance: {}", 
                               claimedRun.get().getId(), instanceId);
                    return claimedRun;
                }
            }
            
            logger.debug("Failed to claim run ID: {} (may have been claimed by another instance)", 
                        runToClaim.getId());
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("Error claiming PENDING run by instance: {}, error: {}", 
                        instanceId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Clean up stale RUNNING runs (for failure recovery)
     */
    @Transactional
    public List<Run> cleanupStaleRuns(Duration timeout) {
        Instant threshold = Instant.now().minus(timeout);
        List<Run> staleRuns = runRepository.findStaleRunningRuns(RunStatus.RUNNING, threshold);
        
        for (Run staleRun : staleRuns) {
            logger.warn("Marking stale run ID: {} as FAILED (was running for too long)", 
                       staleRun.getId());
            staleRun.markAsFailed("Run timed out - was running for more than " + timeout.toString());
            runRepository.save(staleRun);
        }
        
        return staleRuns;
    }

    public Run createRun(UUID taskId) {
        Run run = new Run(taskId, RunStatus.PENDING);
        Run saved = runRepository.save(run);
        logger.info("Created new run ID: {} for task ID: {}", saved.getId(), taskId);
        return saved;
    }

    public Run createRun(UUID taskId, RunStatus status) {
        Run run = new Run(taskId, status);
        Run saved = runRepository.save(run);
        logger.info("Created new run ID: {} for task ID: {} with status: {}", 
                   saved.getId(), taskId, status);
        return saved;
    }

    public List<Run> getAllRuns() {
        return runRepository.findAll();
    }

    public Optional<Run> getRunById(UUID id) {
        return runRepository.findById(id);
    }

    public List<Run> getRunsByTaskId(UUID taskId) {
        return runRepository.findByTaskId(taskId);
    }

    public List<Run> getRunsByStatus(RunStatus status) {
        return runRepository.findByStatus(status);
    }

    @Transactional
    public Run updateRunStatus(UUID id, RunStatus status) {
        Optional<Run> runOptional = runRepository.findById(id);
        if (runOptional.isPresent()) {
            Run run = runOptional.get();
            run.setStatus(status);
            Run updated = runRepository.save(run);
            logger.info("Updated run ID: {} to status: {}", id, status);
            return updated;
        }
        logger.warn("Run ID: {} not found for status update", id);
        return null;
    }

    @Transactional
    public void markRunAsCompleted(UUID runId) {
        Optional<Run> runOpt = runRepository.findById(runId);
        if (runOpt.isPresent()) {
            Run run = runOpt.get();
            run.markAsCompleted();
            runRepository.save(run);
            logger.info("Marked run ID: {} as COMPLETED", runId);
        }
    }

    @Transactional
    public void markRunAsFailed(UUID runId, String errorMessage) {
        Optional<Run> runOpt = runRepository.findById(runId);
        if (runOpt.isPresent()) {
            Run run = runOpt.get();
            run.markAsFailed(errorMessage);
            runRepository.save(run);
            logger.info("Marked run ID: {} as FAILED: {}", runId, errorMessage);
        }
    }

    public void deleteRun(UUID id) {
        runRepository.deleteById(id);
        logger.info("Deleted run ID: {}", id);
    }

    public boolean existsById(UUID id) {
        return runRepository.existsById(id);
    }

    public long getRunCountByStatus(RunStatus status) {
        return runRepository.countByStatus(status);
    }

    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Get stale RUNNING runs for crash recovery
     */
    public List<Run> getStaleRunningRuns(Duration timeout) {
        Instant threshold = Instant.now().minus(timeout);
        return runRepository.findStaleRunningRuns(RunStatus.RUNNING, threshold);
    }

    /**
     * Reset a run back to PENDING status for retry
     */
    @Transactional
    public Run resetRunToPending(UUID runId) {
        Optional<Run> runOpt = runRepository.findById(runId);
        if (runOpt.isPresent()) {
            Run run = runOpt.get();
            run.setStatus(RunStatus.PENDING);
            run.setUpdatedAt(Instant.now());
            run.setClaimedBy(null);
            Run updated = runRepository.save(run);
            logger.info("Reset run ID: {} back to PENDING for retry", runId);
            return updated;
        }
        throw new RuntimeException("Run not found: " + runId);
    }

    private String generateInstanceId() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            long timestamp = System.currentTimeMillis();
            return String.format("%s-%d", hostname, timestamp);
        } catch (UnknownHostException e) {
            long timestamp = System.currentTimeMillis();
            return String.format("unknown-%d", timestamp);
        }
    }
}