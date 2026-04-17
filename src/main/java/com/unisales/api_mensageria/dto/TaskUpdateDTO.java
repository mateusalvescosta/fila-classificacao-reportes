package com.unisales.api_mensageria.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class TaskUpdateDTO {

    @NotBlank
    @Pattern(regexp = "pending|processing|done|error", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String status;

    /** Opcional: persiste na coluna {@code priority} (ex.: worker após IA). */
    @Min(1)
    @Max(10)
    private Integer priority;

    private Map<String, Object> result;
}