package com.unisales.fila_reportes.projection;

public interface QueueStatsProjection {
    String getQueueName();
    long getPending();
    long getProcessing();
    long getDone();
    long getError();
}