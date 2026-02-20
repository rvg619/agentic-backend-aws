package com.rajathgoku.agentic.backend.service;

import com.rajathgoku.agentic.backend.entity.Run;
import com.rajathgoku.agentic.backend.repository.RunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RunService {

    @Autowired
    private RunRepository runRepository;

    public Run createRun(Long taskId) {
        Run run = new Run(taskId, Run.RunStatus.PENDING);
        return runRepository.save(run);
    }

    public Run createRun(Long taskId, Run.RunStatus status) {
        Run run = new Run(taskId, status);
        return runRepository.save(run);
    }

    public List<Run> getAllRuns() {
        return runRepository.findAll();
    }

    public Optional<Run> getRunById(Long id) {
        return runRepository.findById(id);
    }

    public List<Run> getRunsByTaskId(Long taskId) {
        return runRepository.findByTaskId(taskId);
    }

    public List<Run> getRunsByStatus(Run.RunStatus status) {
        return runRepository.findByStatus(status);
    }

    public Run updateRunStatus(Long id, Run.RunStatus status) {
        Optional<Run> runOptional = runRepository.findById(id);
        if (runOptional.isPresent()) {
            Run run = runOptional.get();
            run.setStatus(status);
            return runRepository.save(run);
        }
        return null;
    }

    public void deleteRun(Long id) {
        runRepository.deleteById(id);
    }

    public boolean existsById(Long id) {
        return runRepository.existsById(id);
    }
}