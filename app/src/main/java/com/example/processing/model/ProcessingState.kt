package com.example.processing.model

enum class ProcessingState(val labelAr: String) {
    NOT_PROCESSED("غير معالج"),
    PROCESSING("قيد المعالجة"),
    COMPLETED("مكتمل"),
    FAILED("فشلت المعالجة")
}
