package com.rajathgoku.agentic.backend.controller;

import com.rajathgoku.agentic.backend.dto.CreateRunRequest;
import com.rajathgoku.agentic.backend.dto.RunResponse;
import com.rajathgoku.agentic.backend.dto.StepResponse;
import com.rajathgoku.agentic.backend.dto.ArtifactResponse;
import com.rajathgoku.agentic.backend.dto.RunMetricsResponse;
import com.rajathgoku.agentic.backend.entity.Run;
import com.rajathgoku.agentic.backend.entity.RunStatus;
import com.rajathgoku.agentic.backend.entity.Step;
import com.rajathgoku.agentic.backend.entity.Artifact;
import com.rajathgoku.agentic.backend.repository.StepRepository;
import com.rajathgoku.agentic.backend.repository.ArtifactRepository;
import com.rajathgoku.agentic.backend.service.RunService;
import com.rajathgoku.agentic.backend.service.StepService;
import com.rajathgoku.agentic.backend.service.ArtifactService;
import com.rajathgoku.agentic.backend.engine.RunOrchestrator;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/runs")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class RunController {

    private static final Logger logger = LoggerFactory.getLogger(RunController.class);

    @Autowired
    private RunService runService;

    @Autowired
    private StepRepository stepRepository;

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private StepService stepService;

    @Autowired
    private ArtifactService artifactService;

    @Autowired
    private RunOrchestrator runOrchestrator;

    @PostMapping
    public ResponseEntity<RunResponse> createRun(@Valid @RequestBody CreateRunRequest request) {
        RunStatus status = RunStatus.valueOf(request.getStatus().toUpperCase());
        Run created = runService.createRun(request.getTaskId(), status);
        return ResponseEntity.ok(toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<RunResponse>> getAllRuns() {
        List<RunResponse> runs = runService.getAllRuns()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RunResponse> getRunById(@PathVariable UUID id) {
        Optional<Run> run = runService.getRunById(id);
        if (run.isPresent()) {
            return ResponseEntity.ok(toResponse(run.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<RunResponse>> getRunsByTaskId(@PathVariable UUID taskId) {
        List<RunResponse> runs = runService.getRunsByTaskId(taskId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<RunResponse>> getRunsByStatus(@PathVariable String status) {
        try {
            RunStatus runStatus = RunStatus.valueOf(status.toUpperCase());
            List<RunResponse> runs = runService.getRunsByStatus(runStatus)
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(runs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Enhanced trace API - Get all steps for a run with their execution timeline
     */
    @GetMapping("/{id}/steps")
    public ResponseEntity<List<StepResponse>> getRunSteps(@PathVariable UUID id) {
        Optional<Run> run = runService.getRunById(id);
        if (!run.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        List<Step> steps = stepRepository.findByRunIdOrderByCreatedAt(id);
        List<StepResponse> stepResponses = steps.stream()
                .map(StepResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(stepResponses);
    }

    /**
     * Get full execution trace - run with all steps and artifacts
     */
    @GetMapping("/{id}/trace")
    public ResponseEntity<Map<String, Object>> getFullTrace(@PathVariable UUID id) {
        Optional<Run> runOpt = runService.getRunById(id);
        if (!runOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Run run = runOpt.get();
        List<Step> steps = stepRepository.findByRunIdOrderByCreatedAt(id);
        
        // Build comprehensive trace
        Map<String, Object> trace = Map.of(
            "run", toResponse(run),
            "steps", steps.stream().map(StepResponse::new).collect(Collectors.toList()),
            "totalSteps", steps.size(),
            "completedSteps", steps.stream().mapToLong(s -> s.getStatus() == Step.StepStatus.DONE ? 1 : 0).sum(),
            "failedSteps", steps.stream().mapToLong(s -> s.getStatus() == Step.StepStatus.FAILED ? 1 : 0).sum(),
            "totalArtifacts", steps.stream().mapToLong(s -> s.getArtifacts().size()).sum()
        );
        
        return ResponseEntity.ok(trace);
    }

    /**
     * Get all artifacts for a run (from all steps)
     */
    @GetMapping("/{id}/artifacts")
    public ResponseEntity<List<ArtifactResponse>> getRunArtifacts(@PathVariable UUID id) {
        Optional<Run> run = runService.getRunById(id);
        if (!run.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // Get all steps for this run
        List<Step> steps = stepRepository.findByRunIdOrderByCreatedAt(id);
        
        // Collect all artifacts from all steps
        List<Artifact> allArtifacts = steps.stream()
                .flatMap(step -> artifactRepository.findByStepIdOrderByCreatedAt(step.getId()).stream())
                .collect(Collectors.toList());
        
        List<ArtifactResponse> artifactResponses = allArtifacts.stream()
                .map(ArtifactResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(artifactResponses);
    }

    /**
     * Force process a specific run (for testing/debugging)
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<Map<String, String>> forceProcessRun(@PathVariable UUID id) {
        Optional<Run> run = runService.getRunById(id);
        if (!run.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        if (!run.get().canBeClaimed()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Run cannot be processed",
                "currentStatus", run.get().getStatus().toString()
            ));
        }

        runOrchestrator.processRun(id);
        return ResponseEntity.ok(Map.of(
            "message", "Run processing initiated",
            "runId", id.toString()
        ));
    }

    /**
     * Get orchestrator statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<RunOrchestrator.RunStatistics> getStats() {
        return ResponseEntity.ok(runOrchestrator.getStatistics());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<RunResponse> updateRunStatus(@PathVariable UUID id, @RequestBody String status) {
        try {
            RunStatus runStatus = RunStatus.valueOf(status.toUpperCase());
            Run updated = runService.updateRunStatus(id, runStatus);
            if (updated != null) {
                return ResponseEntity.ok(toResponse(updated));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRun(@PathVariable UUID id) {
        if (runService.existsById(id)) {
            runService.deleteRun(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{runId}/metrics")
    public ResponseEntity<RunMetricsResponse> getRunMetrics(@PathVariable UUID runId) {
        try {
            Optional<Run> runOpt = runService.getRunById(runId);
            if (runOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Run run = runOpt.get();
            // Use the correct method names from StepService
            List<Step> steps = stepService.getStepsForRun(runId);
            // Use the existing method from ArtifactRepository that queries by runId
            List<Artifact> artifacts = artifactRepository.findByStepRunIdOrderByCreatedAt(runId);
            
            // Calculate real metrics from actual data
            long tokensProcessed = calculateTokensProcessed(steps, artifacts);
            int apiCallsMade = steps.size(); // Each step represents an AI agent API call
            long processingTimeMs = calculateProcessingTime(run);
            double complexityScore = calculateComplexityScore(run, steps, artifacts);
            int stepsCompleted = (int) steps.stream().filter(s -> s.getStatus() == Step.StepStatus.DONE).count();
            int artifactsGenerated = artifacts.size();
            String currentPhase = determineCurrentPhase(run, steps);
            
            RunMetricsResponse metrics = new RunMetricsResponse(
                tokensProcessed, apiCallsMade, processingTimeMs, complexityScore,
                stepsCompleted, artifactsGenerated, currentPhase
            );
            
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Error getting metrics for run {}: {}", runId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private long calculateTokensProcessed(List<Step> steps, List<Artifact> artifacts) {
        // Estimate tokens based on actual content generated
        long tokens = 0;
        
        // Count tokens from step results (approximate: 1 token ≈ 4 characters)
        for (Step step : steps) {
            if (step.getResult() != null) {
                tokens += step.getResult().length() / 4;
            }
        }
        
        // Count tokens from artifacts
        for (Artifact artifact : artifacts) {
            if (artifact.getContent() != null) {
                tokens += artifact.getContent().length() / 4;
            }
        }
        
        return Math.max(tokens, 100); // Minimum 100 tokens
    }
    
    private long calculateProcessingTime(Run run) {
        if (run.getUpdatedAt() != null && !run.getStatus().equals(RunStatus.PENDING)) {
            return Duration.between(run.getCreatedAt(), run.getUpdatedAt()).toMillis();
        } else {
            return Duration.between(run.getCreatedAt(), Instant.now()).toMillis();
        }
    }
    
    private double calculateComplexityScore(Run run, List<Step> steps, List<Artifact> artifacts) {
        double score = 2.0; // Base complexity
        
        // Add complexity based on number of steps
        score += Math.min(steps.size() * 0.5, 3.0);
        
        // Add complexity based on artifacts generated
        score += Math.min(artifacts.size() * 0.3, 2.0);
        
        // Add complexity based on content size
        long totalContentSize = artifacts.stream()
            .mapToLong(a -> a.getContent() != null ? a.getContent().length() : 0)
            .sum();
        score += Math.min(totalContentSize / 1000.0, 2.5);
        
        return Math.min(score, 9.5); // Cap at 9.5
    }
    
    private String determineCurrentPhase(Run run, List<Step> steps) {
        if (run.getStatus() == RunStatus.DONE || run.getStatus() == RunStatus.FAILED) {
            return "complete";
        }
        
        if (run.getStatus() == RunStatus.PENDING) {
            return "idle";
        }
        
        // Find the current running step
        Optional<Step> currentStep = steps.stream()
            .filter(s -> s.getStatus() == Step.StepStatus.RUNNING)
            .findFirst();
            
        if (currentStep.isPresent()) {
            String stepName = currentStep.get().getName().toLowerCase();
            if (stepName.contains("planning")) return "planning";
            if (stepName.contains("execute")) return "executing";
            if (stepName.contains("critique")) return "critiquing";
        }
        
        return "executing"; // Default phase
    }

    private RunResponse toResponse(Run run) {
        RunResponse response = new RunResponse();
        response.setId(run.getId());
        response.setTaskId(run.getTaskId());
        response.setStatus(run.getStatus().name());
        response.setCreatedAt(run.getCreatedAt());
        response.setUpdatedAt(run.getUpdatedAt());
        return response;
    }
}

@RestController
@RequestMapping("/steps")
class StepController {

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private StepRepository stepRepository;

    /**
     * Enhanced artifacts API with filtering
     */
    @GetMapping("/{id}/artifacts")
    public ResponseEntity<List<ArtifactResponse>> getStepArtifacts(
            @PathVariable UUID id,
            @RequestParam(required = false) String type) {
        
        Optional<Step> step = stepRepository.findById(id);
        if (!step.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        List<Artifact> artifacts = artifactRepository.findByStepIdOrderByCreatedAt(id);
        
        // Filter by type if specified
        if (type != null && !type.trim().isEmpty()) {
            artifacts = artifacts.stream()
                    .filter(a -> a.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        
        List<ArtifactResponse> artifactResponses = artifacts.stream()
                .map(ArtifactResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(artifactResponses);
    }

    /**
     * Get step details with artifacts
     */
    @GetMapping("/{id}")
    public ResponseEntity<StepResponse> getStepDetails(@PathVariable UUID id) {
        Optional<Step> step = stepRepository.findById(id);
        if (!step.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new StepResponse(step.get()));
    }
}