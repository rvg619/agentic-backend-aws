package com.rajathgoku.agentic.backend.repository;

import com.rajathgoku.agentic.backend.entity.Step;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StepRepository extends JpaRepository<Step, UUID> {
    
    List<Step> findByRunIdOrderByCreatedAt(UUID runId);
    
    List<Step> findByRunId(UUID runId);
    
    @Query("SELECT s FROM Step s WHERE s.run.id = :runId ORDER BY s.createdAt ASC")
    List<Step> findStepsByRunId(@Param("runId") UUID runId);
}