package com.unisales.fila_reportes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.unisales.fila_reportes.model.Task;
import com.unisales.fila_reportes.projection.QueueStatsProjection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Query(value = "SELECT * FROM tasks WHERE queue_name = ?1 " +
                   "AND status = 'pending' AND priority IS NULL " +
                   "ORDER BY created_at ASC " +
                   "LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<Task> findNextUnclassified(String queueName);

    @Query(value = "SELECT * FROM tasks WHERE queue_name = ?1 " +
                   "AND status = 'pending' " +
                   "ORDER BY created_at ASC " +
                   "LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<Task> findNext(String queueName);

    @Query(value = "SELECT queue_name AS queueName, " +
                   "COUNT(CASE WHEN status = 'pending'    THEN 1 END) AS pending, " +
                   "COUNT(CASE WHEN status = 'processing' THEN 1 END) AS processing, " +
                   "COUNT(CASE WHEN status = 'done'       THEN 1 END) AS done, " +
                   "COUNT(CASE WHEN status = 'error'      THEN 1 END) AS error " +
                   "FROM tasks GROUP BY queue_name ORDER BY queue_name", nativeQuery = true)
    List<QueueStatsProjection> findQueueStats();
}