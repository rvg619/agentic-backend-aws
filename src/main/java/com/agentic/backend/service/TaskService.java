package com.agentic.backend.service;

import com.agentic.backend.model.Task;
import com.agentic.backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final AIService aiService;

    @Transactional
    public Task createTask(Task task) {
        log.info("Creating new task: {}", task.getTitle());
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksByStatus(Task.TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Task> getPendingTasksByPriority() {
        return taskRepository.findByStatusOrderByPriorityDesc(Task.TaskStatus.PENDING);
    }

    @Transactional
    public Task updateTask(Long id, Task updatedTask) {
        return taskRepository.findById(id)
                .map(task -> {
                    if (updatedTask.getTitle() != null) task.setTitle(updatedTask.getTitle());
                    if (updatedTask.getDescription() != null) task.setDescription(updatedTask.getDescription());
                    if (updatedTask.getStatus() != null) task.setStatus(updatedTask.getStatus());
                    if (updatedTask.getPriority() != null) task.setPriority(updatedTask.getPriority());
                    if (updatedTask.getAssignedAgent() != null) task.setAssignedAgent(updatedTask.getAssignedAgent());
                    return taskRepository.save(task);
                })
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    @Async
    @Transactional
    public void processTaskWithAI(Long taskId) {
        log.info("Processing task {} with AI", taskId);
        
        taskRepository.findById(taskId).ifPresent(task -> {
            try {
                task.setStatus(Task.TaskStatus.PROCESSING_AI);
                taskRepository.save(task);

                // Create AI prompt and get response
                String prompt = String.format("Task: %s\nDescription: %s\nPlease provide a detailed solution.",
                        task.getTitle(), task.getDescription());
                
                String aiResponse = aiService.processWithAI(prompt);
                
                task.setAiPrompt(prompt);
                task.setAiResponse(aiResponse);
                task.setStatus(Task.TaskStatus.COMPLETED);
                taskRepository.save(task);
                
                log.info("Task {} processed successfully with AI", taskId);
            } catch (Exception e) {
                log.error("Error processing task {} with AI: {}", taskId, e.getMessage());
                task.setStatus(Task.TaskStatus.FAILED);
                task.setAiResponse("Error: " + e.getMessage());
                taskRepository.save(task);
            }
        });
    }

    @Transactional
    public Task assignTaskToAgent(Long taskId, String agentName) {
        return taskRepository.findById(taskId)
                .map(task -> {
                    task.setAssignedAgent(agentName);
                    task.setStatus(Task.TaskStatus.IN_PROGRESS);
                    return taskRepository.save(task);
                })
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
    }
}
