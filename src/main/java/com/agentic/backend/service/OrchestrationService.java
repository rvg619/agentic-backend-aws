package com.agentic.backend.service;

import com.agentic.backend.model.Orchestration;
import com.agentic.backend.model.Task;
import com.agentic.backend.repository.OrchestrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrchestrationService {

    private final OrchestrationRepository orchestrationRepository;
    private final TaskService taskService;

    @Transactional
    public Orchestration createOrchestration(Orchestration orchestration) {
        log.info("Creating new orchestration: {}", orchestration.getName());
        return orchestrationRepository.save(orchestration);
    }

    @Transactional(readOnly = true)
    public List<Orchestration> getAllOrchestrations() {
        return orchestrationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Orchestration> getOrchestrationById(Long id) {
        return orchestrationRepository.findById(id);
    }

    @Async
    @Transactional
    public void executeOrchestration(Long orchestrationId) {
        log.info("Executing orchestration {}", orchestrationId);
        
        orchestrationRepository.findById(orchestrationId).ifPresent(orchestration -> {
            try {
                orchestration.setStatus(Orchestration.OrchestrationStatus.RUNNING);
                orchestrationRepository.save(orchestration);

                // Get pending tasks and process them
                List<Task> pendingTasks = taskService.getPendingTasksByPriority();
                orchestration.setTotalTasks(pendingTasks.size());
                orchestrationRepository.save(orchestration);

                int completed = 0;
                int failed = 0;
                int updateInterval = Math.max(1, pendingTasks.size() / 10); // Update every 10% of tasks

                for (int i = 0; i < pendingTasks.size(); i++) {
                    Task task = pendingTasks.get(i);
                    try {
                        taskService.processTaskWithAI(task.getId());
                        completed++;
                    } catch (Exception e) {
                        log.error("Failed to process task {}: {}", task.getId(), e.getMessage());
                        failed++;
                    }
                    
                    // Batch updates to reduce database writes
                    if (i % updateInterval == 0 || i == pendingTasks.size() - 1) {
                        orchestration.setCompletedTasks(completed);
                        orchestration.setFailedTasks(failed);
                        orchestrationRepository.save(orchestration);
                    }
                }

                orchestration.setStatus(Orchestration.OrchestrationStatus.COMPLETED);
                orchestration.setCompletedAt(LocalDateTime.now());
                orchestrationRepository.save(orchestration);
                
                log.info("Orchestration {} completed. Tasks: {}, Completed: {}, Failed: {}",
                        orchestrationId, pendingTasks.size(), completed, failed);
                        
            } catch (Exception e) {
                log.error("Error executing orchestration {}: {}", orchestrationId, e.getMessage());
                orchestration.setStatus(Orchestration.OrchestrationStatus.FAILED);
                orchestrationRepository.save(orchestration);
            }
        });
    }

    @Transactional
    public Orchestration pauseOrchestration(Long id) {
        return orchestrationRepository.findById(id)
                .map(orchestration -> {
                    orchestration.setStatus(Orchestration.OrchestrationStatus.PAUSED);
                    return orchestrationRepository.save(orchestration);
                })
                .orElseThrow(() -> new RuntimeException("Orchestration not found with id: " + id));
    }

    @Transactional
    public Orchestration resumeOrchestration(Long id) {
        return orchestrationRepository.findById(id)
                .map(orchestration -> {
                    orchestration.setStatus(Orchestration.OrchestrationStatus.RUNNING);
                    orchestrationRepository.save(orchestration);
                    executeOrchestration(id);
                    return orchestration;
                })
                .orElseThrow(() -> new RuntimeException("Orchestration not found with id: " + id));
    }
}
