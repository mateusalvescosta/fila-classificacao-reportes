package com.unisales.api_mensageria.service;

import com.unisales.api_mensageria.dto.TaskRequestDTO;
import com.unisales.api_mensageria.dto.TaskUpdateDTO;
import com.unisales.api_mensageria.model.Task;
import com.unisales.api_mensageria.projection.QueueStatsProjection;
import com.unisales.api_mensageria.repository.TaskRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    @Value("${task.max-attempts:3}")
    private int maxAttempts;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task enqueue(TaskRequestDTO dto) {
        Task task = new Task();
        task.setQueueName(dto.queueName());
        task.setPayload(dto.payload());
        return taskRepository.save(task);
    }

    @Transactional
    public Task dequeue(String queueName) {
        Task task = taskRepository.findNextTask(queueName).orElse(null);
        if (task != null) {
            task.setStatus("processing");
            task.setAttempts(task.getAttempts() + 1);
            taskRepository.save(task);
        }
        return task;
    }

    @Transactional
    public Task updateStatus(UUID id, TaskUpdateDTO dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));

        if (dto.status().equals("error") && task.getAttempts() < maxAttempts) {
            task.setStatus("pending");
        } else {
            task.setStatus(dto.status());
        }

        return taskRepository.save(task);
    }

    public List<Task> listAll() {
        return taskRepository.findAll();
    }

    public List<QueueStatsProjection> getQueueStats() {
        return taskRepository.findQueueStats();
    }
}