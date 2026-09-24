package com.example.ai.model

enum class AiProcessingState {
    NOT_PROCESSED,
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    STALE,
    CANCELLED
}
