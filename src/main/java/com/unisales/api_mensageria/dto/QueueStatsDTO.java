package com.unisales.api_mensageria.dto;

public interface QueueStatsDTO {
    String getQueueName();
    long getPending();
    long getProcessing();
    long getDone();
    long getError();
}