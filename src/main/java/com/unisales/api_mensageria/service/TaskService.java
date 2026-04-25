package com.unisales.api_mensageria.service;

import com.unisales.api_mensageria.projection.QueueStatsProjection;
import com.unisales.api_mensageria.dto.TaskRequestDTO;
import com.unisales.api_mensageria.dto.TaskUpdateDTO;
import com.unisales.api_mensageria.exception.TaskNotFoundException;
import com.unisales.api_mensageria.model.Task;
import com.unisales.api_mensageria.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task enqueue(TaskRequestDTO dto) {
        Task task = new Task();
        task.setQueueName(dto.queueName());
        task.setPayload(dto.payload());
        Task saved = taskRepository.save(task);
        System.out.println("[api] tarefa " + saved.getId() + " recebida na fila '" + saved.getQueueName() + "'");
        return saved;
    }

    @Transactional
    public Optional<Task> dequeueNext(String queueName) {
        return dequeueAndReserve(taskRepository.findNext(queueName));
    }

    @Transactional
    public Optional<Task> dequeueUnclassified(String queueName) {
        return dequeueAndReserve(taskRepository.findNextUnclassified(queueName));
    }

    private Optional<Task> dequeueAndReserve(Optional<Task> found) {
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Task task = found.get();
        task.setStatus("processing");
        task.setAttempts(task.getAttempts() + 1);
        taskRepository.save(task);
        return Optional.of(task);
    }

    @Transactional
    public Task updateStatus(UUID id, TaskUpdateDTO dto) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        String oldStatus = task.getStatus();
        String newStatus = dto.status().toLowerCase();
        Integer oldPriority = task.getPriority();
        task.setStatus(newStatus);

        Integer newPriority = dto.priority();
        if (newPriority == null && dto.result() != null) {
            Object rp = dto.result().get("priority");
            if (rp instanceof Number n) {
                newPriority = n.intValue();
            }
        }
        if (newPriority != null) {
            if (newPriority < 1 || newPriority > 10) {
                throw new ResponseStatusException(BAD_REQUEST, "priority must be between 1 and 10");
            }
            task.setPriority(newPriority);
        }

        if (dto.queueName() != null && !dto.queueName().isBlank()) {
            task.setQueueName(dto.queueName());
        }

        if (dto.result() != null) {
            task.setResult(dto.result());
        }

        boolean classifierBackToQueue =
                "processing".equalsIgnoreCase(oldStatus)
                        && "pending".equalsIgnoreCase(newStatus)
                        && oldPriority == null
                        && newPriority != null;
        if (classifierBackToQueue) {
            task.setAttempts(Math.max(0, task.getAttempts() - 1));
        }

        return taskRepository.save(task);
    }

    public Task getById(UUID id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    public List<Task> listAll() {
        return taskRepository.findAll();
    }

    public List<QueueStatsProjection> getQueueStats() {
        return taskRepository.findQueueStats();
    }
}