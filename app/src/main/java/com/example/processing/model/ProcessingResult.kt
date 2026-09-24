package com.example.processing.model

data class ProcessingResult(
    val success: Boolean,
    val processedFilePath: String? = null,
    val thumbnailPath: String? = null,
    val state: ProcessingState = if (success) ProcessingState.COMPLETED else ProcessingState.FAILED,
    val errorMessage: String? = null
)
