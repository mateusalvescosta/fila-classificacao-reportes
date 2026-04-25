package com.unisales.fila_reportes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record TaskRequestDTO(
        @NotBlank
        @JsonProperty("queue_name")
        String queueName,

        @NotNull
        Map<String, Object> payload
) {}
