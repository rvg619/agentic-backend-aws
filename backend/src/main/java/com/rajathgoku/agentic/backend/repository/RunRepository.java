package com.rajathgoku.agentic.backend.repository;

import com.rajathgoku.agentic.backend.entity.Run;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RunRepository extends JpaRepository<Run, Long> {
    
    List<Run> findByTaskId(Long taskId);
    
    List<Run> findByStatus(Run.RunStatus status);
    
    List<Run> findByTaskIdAndStatus(Long taskId, Run.RunStatus status);
}