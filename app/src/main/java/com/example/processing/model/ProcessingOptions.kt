package com.example.processing.model

data class ProcessingOptions(
    val mode: ProcessingMode = ProcessingMode.DOCUMENT,
    val rotationDegrees: Int = 0, // 0, 90, 180, 270
    val cropQuad: DocumentQuad? = null,
    val brightness: Float = 0f, // -50f..+50f
    val contrast: Float = 1.0f, // 0.5f..2.0f
    val noiseReduction: Boolean = false,
    val sharpening: Boolean = false,
    val deskew: Boolean = false
)
