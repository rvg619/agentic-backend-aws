package com.agentic.backend.repository;

import com.agentic.backend.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    Optional<Agent> findByName(String name);
    List<Agent> findByStatus(Agent.AgentStatus status);
    List<Agent> findByType(Agent.AgentType type);
}
