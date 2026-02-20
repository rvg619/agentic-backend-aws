package com.rajathgoku.agentic.backend.repository;

import com.rajathgoku.agentic.backend.entity.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, Long> {
    
    List<Artifact> findByStepIdOrderByCreatedAt(Long stepId);
    
    @Query("SELECT a FROM Artifact a WHERE a.step.id = :stepId ORDER BY a.createdAt ASC")
    List<Artifact> findArtifactsByStepId(@Param("stepId") Long stepId);
    
    List<Artifact> findByType(String type);
}