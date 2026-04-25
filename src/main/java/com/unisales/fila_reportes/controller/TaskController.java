package com.unisales.fila_reportes.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.unisales.fila_reportes.dto.TaskRequestDTO;
import com.unisales.fila_reportes.dto.TaskUpdateDTO;
import com.unisales.fila_reportes.model.Task;
import com.unisales.fila_reportes.projection.QueueStatsProjection;
import com.unisales.fila_reportes.service.TaskService;

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
    public Task enqueue(@Valid @RequestBody TaskRequestDTO dto) {
        return taskService.enqueue(dto);
    }

    @GetMapping
    public List<Task> listAll() {
        return taskService.listAll();
    }

    @GetMapping("/stats")
    public List<QueueStatsProjection> getStats() {
        return taskService.getQueueStats();
    }

    /** Próxima tarefa pendente da fila (usado pelo standard e high_alert). */
    @GetMapping("/next/{queueName}")
    public ResponseEntity<Task> dequeueNext(@PathVariable String queueName) {
        return taskService.dequeueNext(queueName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /** Próxima tarefa sem prioridade (só o worker classificador deve usar). */
    @GetMapping("/next/{queueName}/unclassified")
    public ResponseEntity<Task> dequeueUnclassified(@PathVariable String queueName) {
        return taskService.dequeueUnclassified(queueName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public Task getById(@PathVariable UUID id) {
        return taskService.getById(id);
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public Task updateStatus(@PathVariable UUID id, @Valid @RequestBody TaskUpdateDTO dto) {
        return taskService.updateStatus(id, dto);
    }
}