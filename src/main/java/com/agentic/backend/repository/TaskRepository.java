package com.agentic.backend.repository;

import com.agentic.backend.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(Task.TaskStatus status);
    List<Task> findByPriority(Task.TaskPriority priority);
    List<Task> findByAssignedAgent(String agentName);
    List<Task> findByStatusOrderByPriorityDesc(Task.TaskStatus status);
}
