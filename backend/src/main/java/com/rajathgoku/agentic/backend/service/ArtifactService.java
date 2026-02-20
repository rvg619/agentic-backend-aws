package com.rajathgoku.agentic.backend.service;

import com.rajathgoku.agentic.backend.entity.Artifact;
import com.rajathgoku.agentic.backend.entity.Step;
import com.rajathgoku.agentic.backend.repository.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ArtifactService {
    
    private static final Logger logger = LoggerFactory.getLogger(ArtifactService.class);
    private final ArtifactRepository artifactRepository;
    
    @Autowired
    public ArtifactService(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }
    
    /**
     * Create a new artifact for a step with retry logic for concurrency
     */
    @Retryable(
        retryFor = {OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Artifact createArtifact(Step step, String name, String content, String mimeType) {
        try {
            Artifact artifact = new Artifact();
            artifact.setStep(step);
            artifact.setName(name);
            artifact.setContent(content);
            artifact.setMimeType(mimeType);
            artifact.setType(getArtifactType(mimeType));
            artifact.setSize(content != null ? (long) content.length() : 0L);
            
            Artifact savedArtifact = artifactRepository.save(artifact);
            logger.debug("Created artifact ID: {} for step ID: {}", savedArtifact.getId(), step.getId());
            return savedArtifact;
        } catch (Exception e) {
            logger.warn("Failed to create artifact for step ID: {} - {}", step.getId(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * Determine artifact type based on MIME type
     */
    private String getArtifactType(String mimeType) {
        if (mimeType == null) return "unknown";
        if (mimeType.startsWith("text/")) return "text";
        if (mimeType.startsWith("application/json")) return "json";
        if (mimeType.startsWith("application/")) return "application";
        if (mimeType.startsWith("image/")) return "image";
        return "unknown";
    }
    
    /**
     * Create a text artifact with retry logic
     */
    public Artifact createTextArtifact(Step step, String name, String content) {
        return createArtifact(step, name, content, "text/plain");
    }
    
    /**
     * Create a JSON artifact with retry logic
     */
    public Artifact createJsonArtifact(Step step, String name, String content) {
        return createArtifact(step, name, content, "application/json");
    }
    
    /**
     * Get all artifacts for a step
     */
    public List<Artifact> getArtifactsForStep(Step step) {
        return artifactRepository.findByStepOrderByCreatedAt(step);
    }
    
    /**
     * Get all artifacts for a step by step ID
     */
    public List<Artifact> getArtifactsForStep(UUID stepId) {
        return artifactRepository.findByStepIdOrderByCreatedAt(stepId);
    }
    
    /**
     * Find artifact by ID
     */
    public Artifact findById(UUID artifactId) {
        return artifactRepository.findById(artifactId).orElse(null);
    }
    
    /**
     * Get all artifacts for a run (through steps)
     */
    public List<Artifact> getArtifactsForRun(UUID runId) {
        return artifactRepository.findByStepRunIdOrderByCreatedAt(runId);
    }
}