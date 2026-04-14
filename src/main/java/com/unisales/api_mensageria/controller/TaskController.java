package com.unisales.api_mensageria.controller;

import com.unisales.api_mensageria.dto.TaskRequestDTO;
import com.unisales.api_mensageria.dto.TaskUpdateDTO;
import com.unisales.api_mensageria.model.Task;
import com.unisales.api_mensageria.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<Task> enqueue(@Valid @RequestBody TaskRequestDTO dto) {
        Task task = taskService.enqueue(dto);
        return ResponseEntity.ok(task);
    }

    @GetMapping("/next/{queueName}")
    public ResponseEntity<Task> dequeue(@PathVariable String queueName) {
        Optional<Task> task = taskService.dequeue(queueName);

        if (task.isPresent()) {
            return ResponseEntity.ok(task.get());
        }

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Task> updateStatus(@PathVariable Long id, @Valid @RequestBody TaskUpdateDTO dto) {
        Task task = taskService.updateStatus(id, dto);
        return ResponseEntity.ok(task);
    }
}