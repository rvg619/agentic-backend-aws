package com.rajathgoku.agentic.backend.service;

import com.rajathgoku.agentic.backend.entity.Step;
import com.rajathgoku.agentic.backend.entity.Run;
import com.rajathgoku.agentic.backend.repository.StepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class StepService {
    
    private static final Logger logger = LoggerFactory.getLogger(StepService.class);
    
    private final StepRepository stepRepository;
    
    // Use ConcurrentHashMap instead of ThreadLocal for better thread safety
    private final ConcurrentHashMap<String, Step> workerStepMap = new ConcurrentHashMap<>();
    
    // Add locks per step to prevent concurrent modifications
    private final ConcurrentHashMap<UUID, ReentrantLock> stepLocks = new ConcurrentHashMap<>();
    
    public StepService(StepRepository stepRepository) {
        this.stepRepository = stepRepository;
    }
    
    /**
     * Get or create lock for a specific step
     */
    private ReentrantLock getStepLock(UUID stepId) {
        return stepLocks.computeIfAbsent(stepId, k -> new ReentrantLock());
    }
    
    /**
     * Create a new step for a run
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Step createStep(Run run, String description, int stepNumber) {
        Step step = new Step();
        step.setRun(run);
        step.setDescription(description);
        step.setStepNumber(stepNumber);
        step.setName(description.length() > 50 ? description.substring(0, 47) + "..." : description);
        step.updateStatus(Step.StepStatus.PENDING);
        
        Step savedStep = stepRepository.save(step);
        logger.debug("Created step ID: {} for run ID: {}", savedStep.getId(), run.getId());
        return savedStep;
    }
    
    /**
     * Start execution of a step with retry logic for concurrency
     */
    @Retryable(
        retryFor = {OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Step startStep(Step step) {
        ReentrantLock lock = getStepLock(step.getId());
        lock.lock();
        try {
            // Refresh step from database to get latest version
            Optional<Step> freshStep = stepRepository.findById(step.getId());
            if (freshStep.isEmpty()) {
                throw new RuntimeException("Step not found: " + step.getId());
            }
            
            Step currentStep = freshStep.get();
            
            // Only update if step is in PENDING status
            if (currentStep.getStatus() == Step.StepStatus.PENDING) {
                currentStep.updateStatus(Step.StepStatus.RUNNING);
                Step savedStep = stepRepository.save(currentStep);
                logger.debug("Started step ID: {}", savedStep.getId());
                return savedStep;
            } else {
                logger.warn("Step ID: {} is not in PENDING status, current status: {}", 
                           currentStep.getId(), currentStep.getStatus());
                return currentStep;
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Complete a step with result and retry logic
     */
    @Retryable(
        retryFor = {OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Step completeStep(Step step, String result) {
        ReentrantLock lock = getStepLock(step.getId());
        lock.lock();
        try {
            // Refresh step from database to get latest version
            Optional<Step> freshStep = stepRepository.findById(step.getId());
            if (freshStep.isEmpty()) {
                throw new RuntimeException("Step not found: " + step.getId());
            }
            
            Step currentStep = freshStep.get();
            
            // Only complete if step is in RUNNING status
            if (currentStep.getStatus() == Step.StepStatus.RUNNING) {
                currentStep.updateStatus(Step.StepStatus.DONE);  // Changed from COMPLETED to DONE
                currentStep.updateResult(result);
                Step savedStep = stepRepository.save(currentStep);
                logger.debug("Completed step ID: {}", savedStep.getId());
                return savedStep;
            } else {
                logger.warn("Step ID: {} is not in RUNNING status, current status: {}", 
                           currentStep.getId(), currentStep.getStatus());
                return currentStep;
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Mark a step as failed with retry logic
     */
    @Retryable(
        retryFor = {OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Step markStepAsFailed(Step step, String errorMessage) {
        ReentrantLock lock = getStepLock(step.getId());
        lock.lock();
        try {
            // Refresh step from database to get latest version
            Optional<Step> freshStep = stepRepository.findById(step.getId());
            if (freshStep.isEmpty()) {
                throw new RuntimeException("Step not found: " + step.getId());
            }
            
            Step currentStep = freshStep.get();
            currentStep.updateStatus(Step.StepStatus.FAILED);
            currentStep.updateErrorMessage(errorMessage);
            
            Step savedStep = stepRepository.save(currentStep);
            logger.debug("Failed step ID: {} with error: {}", savedStep.getId(), errorMessage);
            return savedStep;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Reset a step back to PENDING status for retry with concurrency protection
     */
    @Retryable(
        retryFor = {OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Step resetStepToPending(UUID stepId) {
        ReentrantLock lock = getStepLock(stepId);
        lock.lock();
        try {
            Optional<Step> stepOpt = stepRepository.findById(stepId);
            if (stepOpt.isEmpty()) {
                throw new RuntimeException("Step not found: " + stepId);
            }
            
            Step step = stepOpt.get();
            step.updateStatus(Step.StepStatus.PENDING);
            step.setStartedAt(null);
            step.setCompletedAt(null);
            step.setFinishedAt(null);
            step.updateErrorMessage(null);
            step.updateResult(null);
            
            Step savedStep = stepRepository.save(step);
            logger.info("Reset step ID: {} back to PENDING for retry", stepId);
            return savedStep;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Get all steps for a run, ordered by step number
     */
    public List<Step> getStepsForRun(Run run) {
        return stepRepository.findByRunOrderByStepNumber(run);
    }
    
    /**
     * Get all steps for a run by run ID
     */
    public List<Step> getStepsForRun(UUID runId) {
        return stepRepository.findByRunIdOrderByStepNumber(runId);
    }
    
    /**
     * Find step by ID
     */
    public Step findById(UUID stepId) {
        return stepRepository.findById(stepId).orElse(null);
    }
    
    /**
     * Get running steps for a specific run (for crash recovery)
     */
    public List<Step> getRunningStepsForRun(UUID runId) {
        return stepRepository.findByRunIdAndStatus(runId, Step.StepStatus.RUNNING);
    }
    
    /**
     * Thread-safe worker step management using thread ID as key
     */
    public void setCurrentStepForWorker(Step step) {
        String threadKey = Thread.currentThread().getName() + "-" + Thread.currentThread().getId();
        workerStepMap.put(threadKey, step);
        logger.debug("Set current step {} for worker thread {}", step.getId(), threadKey);
    }
    
    public Step getCurrentStepForWorker() {
        String threadKey = Thread.currentThread().getName() + "-" + Thread.currentThread().getId();
        Step step = workerStepMap.get(threadKey);
        logger.debug("Retrieved current step {} for worker thread {}", 
                    step != null ? step.getId() : "null", threadKey);
        return step;
    }
    
    public void clearCurrentStepForWorker() {
        String threadKey = Thread.currentThread().getName() + "-" + Thread.currentThread().getId();
        Step removed = workerStepMap.remove(threadKey);
        logger.debug("Cleared current step {} for worker thread {}", 
                    removed != null ? removed.getId() : "null", threadKey);
    }
    
    /**
     * Cleanup method to remove old locks (should be called periodically)
     */
    public void cleanupOldLocks() {
        // Remove locks that are not currently being used
        stepLocks.entrySet().removeIf(entry -> !entry.getValue().isLocked());
        logger.debug("Cleaned up unused step locks, remaining: {}", stepLocks.size());
    }
}
