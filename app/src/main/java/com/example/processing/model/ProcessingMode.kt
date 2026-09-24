package com.example.processing.model

enum class ProcessingMode(val titleAr: String, val descriptionAr: String) {
    ORIGINAL("أصلي", "الصورة الأصلية دون أي تعديلات"),
    AUTO("تلقائي", "موازنة الإضاءة والتباين تلقائياً"),
    DOCUMENT("مستند", "تحسين النصوص وتبييض الخلفية للمستندات والكتب"),
    ENHANCED("محسّن", "شحذ الحواف وإبراز تفاصيل الخط الدقيق"),
    GRAYSCALE("رمادي", "تحويل نقي إلى التدرج الرمادي"),
    BLACK_AND_WHITE("أبيض وأسود", "عزل النص بدقة عالية دون تشويش")
}
