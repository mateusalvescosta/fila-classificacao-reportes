package com.unisales.api_mensageria.projection;

public interface QueueStatsProjection {
    String getQueueName();
    long getPending();
    long getProcessing();
    long getDone();
    long getError();
}