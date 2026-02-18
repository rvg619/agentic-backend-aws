package com.rajathgoku.agentic.backend.service;

import com.rajathgoku.agentic.backend.entity.Task;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.rajathgoku.agentic.backend.repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository repository;

    private final List<Task> tasks = new ArrayList<>();
    private Long nextId = 1L;

    public Task createTask(Task task) {
        task.setId(nextId++);
        tasks.add(task);
        return task;
    }

    public List<Task> getAllTasks() {
        return tasks;
    }

    public Task getTaskById(Long id) {
        return tasks.stream()
                .filter(task -> task.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void deleteTaskById(Long id) {
        tasks.removeIf(task -> task.getId().equals(id));
    }
    public Task create(String title) {
        Task task = new Task();
        task.setTitle(title);
        task.setStatus("PENDING");
        return repository.save(task);
    }
    
}
