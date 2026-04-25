package com.unisales.api_mensageria.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public record TaskUpdateDTO(
        @NotBlank
        @Pattern(regexp = "pending|processing|done|error", flags = Pattern.Flag.CASE_INSENSITIVE)
        String status,

        @Min(1)
        @Max(10)
        Integer priority,

        @JsonProperty("queue_name")
        String queueName,

        Map<String, Object> result
) {}