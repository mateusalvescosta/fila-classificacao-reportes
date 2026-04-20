package com.unisales.api_mensageria.controller;

import com.unisales.api_mensageria.dto.TaskRequestDTO;
import com.unisales.api_mensageria.dto.TaskUpdateDTO;
import com.unisales.api_mensageria.model.Task;
import com.unisales.api_mensageria.projection.QueueStatsProjection;
import com.unisales.api_mensageria.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public Task enqueue(@RequestBody @Valid TaskRequestDTO dto) {
        return taskService.enqueue(dto);
    }

    @GetMapping
    public List<Task> listAll() {
        return taskService.listAll();
    }

    @GetMapping("/next/{queueName}")
    public ResponseEntity<Task> dequeue(@PathVariable String queueName) {
        Task task = taskService.dequeue(queueName);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @PatchMapping("/{id}")
    public Task updateStatus(@PathVariable UUID id, @RequestBody @Valid TaskUpdateDTO dto) {
        return taskService.updateStatus(id, dto);
    }

    @GetMapping("/stats")
    public List<QueueStatsProjection> getStats() {
        return taskService.getQueueStats();
    }
}