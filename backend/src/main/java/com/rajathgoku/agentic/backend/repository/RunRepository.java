package com.rajathgoku.agentic.backend.repository;

import com.rajathgoku.agentic.backend.entity.Run;
import com.rajathgoku.agentic.backend.entity.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RunRepository extends JpaRepository<Run, UUID> {
    
    List<Run> findByTaskId(UUID taskId);
    
    List<Run> findByStatus(RunStatus status);
    
    List<Run> findByTaskIdAndStatus(UUID taskId, RunStatus status);
    
    /**
     * Find the oldest PENDING run with pessimistic locking
     * This prevents concurrent access to the same run
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Run r WHERE r.status = :status ORDER BY r.createdAt ASC")
    List<Run> findOldestRunByStatusWithLock(@Param("status") RunStatus status);
    
    /**
     * Simple update method for a specific run
     */
    @Modifying
    @Query("UPDATE Run r SET r.status = :newStatus, r.updatedAt = :timestamp, r.claimedBy = :claimedBy " +
           "WHERE r.id = :runId AND r.status = :oldStatus")
    int updateRunStatusById(@Param("runId") UUID runId,
                            @Param("oldStatus") RunStatus oldStatus, 
                            @Param("newStatus") RunStatus newStatus,
                            @Param("timestamp") Instant timestamp,
                            @Param("claimedBy") String claimedBy);
    
    /**
     * Find the next available PENDING run (without claiming)
     */
    @Query("SELECT r FROM Run r WHERE r.status = :status ORDER BY r.createdAt ASC")
    Optional<Run> findOldestRunByStatus(@Param("status") RunStatus status);
    
    /**
     * Get runs that have been running for too long (for cleanup/retry)
     */
    @Query("SELECT r FROM Run r WHERE r.status = :status AND r.updatedAt < :threshold")
    List<Run> findStaleRunningRuns(@Param("status") RunStatus status, 
                                   @Param("threshold") Instant threshold);
    
    /**
     * Count runs by status for monitoring
     */
    @Query("SELECT COUNT(r) FROM Run r WHERE r.status = :status")
    long countByStatus(@Param("status") RunStatus status);
}