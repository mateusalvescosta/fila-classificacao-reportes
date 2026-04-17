package com.unisales.api_mensageria.service;

import com.unisales.api_mensageria.dto.QueueStatsDTO;
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
        task.setQueueName(dto.getQueueName());
        task.setPayload(dto.getPayload());
        return taskRepository.save(task);
    }

    @Transactional
    public Optional<Task> dequeue(String queueName) {
        return dequeueAndReserve(taskRepository.findNextTask(queueName));
    }

    @Transactional
    public Optional<Task> dequeueUnclassified(String queueName) {
        return dequeueAndReserve(taskRepository.findNextUnclassified(queueName));
    }

    @Transactional
    public Optional<Task> dequeueHighPriority(String queueName, int minPriority) {
        if (minPriority < 1 || minPriority > 10) {
            throw new ResponseStatusException(BAD_REQUEST, "minPriority must be between 1 and 10");
        }
        return dequeueAndReserve(taskRepository.findNextHighPriority(queueName, minPriority));
    }

    @Transactional
    public Optional<Task> dequeueStandardPriority(String queueName, int belowPriority) {
        if (belowPriority < 2 || belowPriority > 10) {
            throw new ResponseStatusException(BAD_REQUEST, "belowPriority must be between 2 and 10");
        }
        return dequeueAndReserve(taskRepository.findNextStandardPriority(queueName, belowPriority));
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
        String newStatus = dto.getStatus().toLowerCase();
        task.setStatus(newStatus);

        Integer newPriority = dto.getPriority();
        if (newPriority == null && dto.getResult() != null) {
            Object rp = dto.getResult().get("priority");
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

        if (dto.getResult() != null) {
            task.setResult(dto.getResult());
        }

        boolean classifiedBackToQueue =
                "processing".equalsIgnoreCase(oldStatus)
                        && "pending".equalsIgnoreCase(newStatus)
                        && task.getPriority() != null;
        if (classifiedBackToQueue) {
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

    public List<QueueStatsDTO> getQueueStats() {
        return taskRepository.findQueueStats();
    }
}