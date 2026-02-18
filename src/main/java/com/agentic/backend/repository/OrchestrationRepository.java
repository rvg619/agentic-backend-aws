package com.agentic.backend.repository;

import com.agentic.backend.model.Orchestration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrchestrationRepository extends JpaRepository<Orchestration, Long> {
    List<Orchestration> findByStatus(Orchestration.OrchestrationStatus status);
}
