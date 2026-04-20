package com.unisales.api_mensageria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record TaskRequestDTO(
        @NotBlank String queueName,
        @NotNull Map<String, Object> payload) {
}