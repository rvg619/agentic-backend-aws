package com.agentic.backend.controller;

import com.agentic.backend.model.Agent;
import com.agentic.backend.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    public ResponseEntity<Agent> createAgent(@RequestBody Agent agent) {
        log.info("Creating agent: {}", agent.getName());
        Agent createdAgent = agentService.createAgent(agent);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAgent);
    }

    @GetMapping
    public ResponseEntity<List<Agent>> getAllAgents() {
        List<Agent> agents = agentService.getAllAgents();
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Agent> getAgentById(@PathVariable Long id) {
        return agentService.getAgentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Agent> getAgentByName(@PathVariable String name) {
        return agentService.getAgentByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/idle")
    public ResponseEntity<List<Agent>> getIdleAgents() {
        List<Agent> agents = agentService.getIdleAgents();
        return ResponseEntity.ok(agents);
    }

    @PatchMapping("/{id}/status/{status}")
    public ResponseEntity<Agent> updateAgentStatus(@PathVariable Long id, @PathVariable Agent.AgentStatus status) {
        try {
            Agent agent = agentService.updateAgentStatus(id, status);
            return ResponseEntity.ok(agent);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgent(@PathVariable Long id) {
        agentService.deleteAgent(id);
        return ResponseEntity.noContent().build();
    }
}
