package com.agentic.backend.controller;

import com.agentic.backend.model.Orchestration;
import com.agentic.backend.service.OrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orchestrations")
@RequiredArgsConstructor
@Slf4j
public class OrchestrationController {

    private final OrchestrationService orchestrationService;

    @PostMapping
    public ResponseEntity<Orchestration> createOrchestration(@RequestBody Orchestration orchestration) {
        log.info("Creating orchestration: {}", orchestration.getName());
        Orchestration created = orchestrationService.createOrchestration(orchestration);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Orchestration>> getAllOrchestrations() {
        List<Orchestration> orchestrations = orchestrationService.getAllOrchestrations();
        return ResponseEntity.ok(orchestrations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Orchestration> getOrchestrationById(@PathVariable Long id) {
        return orchestrationService.getOrchestrationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<String> executeOrchestration(@PathVariable Long id) {
        orchestrationService.executeOrchestration(id);
        return ResponseEntity.accepted().body("Orchestration execution started");
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Orchestration> pauseOrchestration(@PathVariable Long id) {
        try {
            Orchestration orchestration = orchestrationService.pauseOrchestration(id);
            return ResponseEntity.ok(orchestration);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Orchestration> resumeOrchestration(@PathVariable Long id) {
        try {
            Orchestration orchestration = orchestrationService.resumeOrchestration(id);
            return ResponseEntity.ok(orchestration);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
