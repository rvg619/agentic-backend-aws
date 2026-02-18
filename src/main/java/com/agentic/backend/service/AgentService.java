package com.agentic.backend.service;

import com.agentic.backend.model.Agent;
import com.agentic.backend.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;

    @Transactional
    public Agent createAgent(Agent agent) {
        log.info("Creating new agent: {}", agent.getName());
        return agentRepository.save(agent);
    }

    @Transactional(readOnly = true)
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Agent> getAgentById(Long id) {
        return agentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Agent> getAgentByName(String name) {
        return agentRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public List<Agent> getIdleAgents() {
        return agentRepository.findByStatus(Agent.AgentStatus.IDLE);
    }

    @Transactional
    public Agent updateAgentStatus(Long id, Agent.AgentStatus status) {
        return agentRepository.findById(id)
                .map(agent -> {
                    agent.setStatus(status);
                    agent.setLastActiveAt(LocalDateTime.now());
                    return agentRepository.save(agent);
                })
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + id));
    }

    @Transactional
    public void deleteAgent(Long id) {
        agentRepository.deleteById(id);
    }
}
