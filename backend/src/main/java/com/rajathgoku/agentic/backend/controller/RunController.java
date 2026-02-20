package com.rajathgoku.agentic.backend.controller;

import com.rajathgoku.agentic.backend.dto.CreateRunRequest;
import com.rajathgoku.agentic.backend.dto.RunResponse;
import com.rajathgoku.agentic.backend.dto.StepResponse;
import com.rajathgoku.agentic.backend.dto.ArtifactResponse;
import com.rajathgoku.agentic.backend.entity.Run;
import com.rajathgoku.agentic.backend.entity.Step;
import com.rajathgoku.agentic.backend.entity.Artifact;
import com.rajathgoku.agentic.backend.repository.StepRepository;
import com.rajathgoku.agentic.backend.repository.ArtifactRepository;
import com.rajathgoku.agentic.backend.service.RunService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/runs")
public class RunController {

    @Autowired
    private RunService runService;

    @Autowired
    private StepRepository stepRepository;

    @PostMapping
    public ResponseEntity<RunResponse> createRun(@Valid @RequestBody CreateRunRequest request) {
        Run.RunStatus status = Run.RunStatus.valueOf(request.getStatus().toUpperCase());
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
    public ResponseEntity<RunResponse> getRunById(@PathVariable Long id) {
        Optional<Run> run = runService.getRunById(id);
        if (run.isPresent()) {
            return ResponseEntity.ok(toResponse(run.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<RunResponse>> getRunsByTaskId(@PathVariable Long taskId) {
        List<RunResponse> runs = runService.getRunsByTaskId(taskId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<RunResponse>> getRunsByStatus(@PathVariable String status) {
        try {
            Run.RunStatus runStatus = Run.RunStatus.valueOf(status.toUpperCase());
            List<RunResponse> runs = runService.getRunsByStatus(runStatus)
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(runs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/steps")
    public ResponseEntity<List<StepResponse>> getSteps(@PathVariable Long id) {
        // First verify that the run exists
        Optional<Run> run = runService.getRunById(id);
        if (!run.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        List<Step> steps = stepRepository.findByRunId(id);
        List<StepResponse> stepResponses = steps.stream()
                .map(StepResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(stepResponses);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<RunResponse> updateRunStatus(@PathVariable Long id, @RequestBody String status) {
        try {
            Run.RunStatus runStatus = Run.RunStatus.valueOf(status.toUpperCase());
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
    public ResponseEntity<Void> deleteRun(@PathVariable Long id) {
        if (runService.existsById(id)) {
            runService.deleteRun(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
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

    @GetMapping("/{id}/artifacts")
    public ResponseEntity<List<ArtifactResponse>> getArtifactsByStepId(@PathVariable Long id) {
        // First verify that the step exists
        Optional<Step> step = stepRepository.findById(id);
        if (!step.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        List<Artifact> artifacts = artifactRepository.findByStepIdOrderByCreatedAt(id);
        List<ArtifactResponse> artifactResponses = artifacts.stream()
                .map(ArtifactResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(artifactResponses);
    }
}