package com.unisales.api_mensageria.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskUpdateDTO(
        @NotBlank String status) {
}