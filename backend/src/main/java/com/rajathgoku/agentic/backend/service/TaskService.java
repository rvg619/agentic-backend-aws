package com.rajathgoku.agentic.backend.service;

import com.rajathgoku.agentic.backend.entity.Task;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.rajathgoku.agentic.backend.repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    @Autowired
    private TaskRepository repository;

    private final List<Task> tasks = new ArrayList<>();

    public Task createTask(Task task) {
        tasks.add(task);
        return task;
    }

    public List<Task> getAllTasks() {
        return repository.findAll();
    }

    public Task getTaskById(UUID id) {
        return repository.findById(id).orElse(null);
    }

    public void deleteTaskById(UUID id) {
        repository.deleteById(id);
    }
    
    public Task create(String title) {
        Task task = new Task();
        task.setTitle(title);
        task.setStatus("PENDING");
        return repository.save(task);
    }
}
