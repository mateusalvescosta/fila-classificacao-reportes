package com.unisales.fila_reportes.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tasks")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String queueName;
    /**
     * Preenchido pelo worker após classificação (1–10). Null = ainda não classificada na fila.
     */
    private Integer priority;
    private String status = "pending";
    private Integer attempts = 0;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> result;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Texto para o front mostrar alerta quando a prioridade numérica é alta.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getPriorityMessage() {
        if (priority == null) {
            return null;
        }
        if (priority >= 8) {
            return "Prioridade alta";
        }
        return null;
    }
}