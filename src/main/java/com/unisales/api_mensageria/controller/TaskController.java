package com.unisales.api_mensageria.controller;

import com.unisales.api_mensageria.dto.QueueStatsDTO;
import com.unisales.api_mensageria.dto.TaskRequestDTO;
import com.unisales.api_mensageria.dto.TaskUpdateDTO;
import com.unisales.api_mensageria.model.Task;
import com.unisales.api_mensageria.service.TaskService;
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
    public Task enqueue(@RequestBody TaskRequestDTO dto) {
        return taskService.enqueue(dto);
    }

    @GetMapping
    public List<Task> listAll() {
        return taskService.listAll();
    }

    @GetMapping("/next/{queueName}")
    public Task dequeue(@PathVariable String queueName) {
        return taskService.dequeue(queueName);
    }

    @PatchMapping("/{id}")
    public Task updateStatus(@PathVariable UUID id, @RequestBody TaskUpdateDTO dto) {
        return taskService.updateStatus(id, dto);
    }

    @GetMapping("/stats")
    public List<QueueStatsDTO> getStats() {
        return taskService.getQueueStats();
    }
}