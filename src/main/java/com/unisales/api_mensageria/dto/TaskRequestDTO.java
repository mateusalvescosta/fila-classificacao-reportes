package com.unisales.api_mensageria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class TaskRequestDTO {

    @NotBlank
    private String queueName;

    @NotNull
    private Map<String, Object> payload;
}