package com.unisales.fila_reportes.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class TaskNotFoundException extends ResponseStatusException {

    public TaskNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Task not found: " + id);
    }
}
