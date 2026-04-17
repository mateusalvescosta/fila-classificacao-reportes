package com.unisales.api_mensageria.repository;

import com.unisales.api_mensageria.dto.QueueStatsDTO;
import com.unisales.api_mensageria.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Query(value = "SELECT * FROM tasks WHERE queue_name = ?1 " +
                   "AND status = 'pending' " +
                   "ORDER BY (priority IS NULL) DESC, " +
                   "CASE WHEN priority IS NULL THEN created_at END ASC NULLS LAST, " +
                   "priority DESC NULLS LAST, " +
                   "created_at ASC " +
                   "LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<Task> findNextTask(String queueName);

    @Query(value = "SELECT * FROM tasks WHERE queue_name = ?1 " +
                   "AND status = 'pending' AND priority IS NULL " +
                   "ORDER BY created_at ASC " +
                   "LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<Task> findNextUnclassified(String queueName);

    @Query(value = "SELECT * FROM tasks WHERE queue_name = ?1 " +
                   "AND status = 'pending' AND priority IS NOT NULL AND priority >= ?2 " +
                   "ORDER BY priority DESC, created_at ASC " +
                   "LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<Task> findNextHighPriority(String queueName, int minPriority);

    @Query(value = "SELECT * FROM tasks WHERE queue_name = ?1 " +
                   "AND status = 'pending' AND priority IS NOT NULL AND priority < ?2 " +
                   "ORDER BY created_at ASC " +
                   "LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<Task> findNextStandardPriority(String queueName, int belowPriority);

    @Query(value = "SELECT queue_name AS queueName, " +
                   "COUNT(CASE WHEN status = 'pending'    THEN 1 END) AS pending, " +
                   "COUNT(CASE WHEN status = 'processing' THEN 1 END) AS processing, " +
                   "COUNT(CASE WHEN status = 'done'       THEN 1 END) AS done, " +
                   "COUNT(CASE WHEN status = 'error'      THEN 1 END) AS error " +
                   "FROM tasks GROUP BY queue_name ORDER BY queue_name", nativeQuery = true)
    List<QueueStatsDTO> findQueueStats();
}