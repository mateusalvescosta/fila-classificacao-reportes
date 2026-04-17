package com.unisales.api_mensageria.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class TaskRequestDTO {

    @NotBlank
    @JsonProperty("queue_name")
    private String queueName;

    @NotNull
    private Map<String, Object> payload;
}