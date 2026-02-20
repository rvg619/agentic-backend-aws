package com.rajathgoku.agentic.backend.repository;

import com.rajathgoku.agentic.backend.entity.Step;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StepRepository extends JpaRepository<Step, Long> {
    
    List<Step> findByRunIdOrderByCreatedAt(Long runId);
    
    List<Step> findByRunId(Long runId);
    
    @Query("SELECT s FROM Step s WHERE s.run.id = :runId ORDER BY s.createdAt ASC")
    List<Step> findStepsByRunId(@Param("runId") Long runId);
}