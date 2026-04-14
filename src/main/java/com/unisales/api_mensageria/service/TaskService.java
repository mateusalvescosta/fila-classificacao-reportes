package com.unisales.api_mensageria.service;

import com.unisales.api_mensageria.dto.TaskRequestDTO;
import com.unisales.api_mensageria.dto.TaskUpdateDTO;
import com.unisales.api_mensageria.model.Task;
import com.unisales.api_mensageria.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
        Optional<Task> task = taskRepository.findNextTask(queueName);

        if (task.isPresent()) {
            task.get().setStatus("processing");
            taskRepository.save(task.get());
        }

        return task;
    }

    @Transactional
    public Task updateStatus(Long id, TaskUpdateDTO dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarefa não encontrada: " + id));

        task.setStatus(dto.getStatus());

        if (dto.getStatus().equals("error")) {
            task.setAttempts(task.getAttempts() + 1);
        }

        return taskRepository.save(task);
    }
}