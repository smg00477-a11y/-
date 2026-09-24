package com.example.data.model

enum class LibrarySortField(val titleAr: String) {
    DATE_ADDED("تاريخ الإضافة"),
    TITLE("عنوان الكتاب"),
    AUTHOR("المؤلف"),
    PAGE_COUNT("عدد الصفحات"),
    CATEGORY("التصنيف"),
    LAST_UPDATED("آخر تعديل / قراءة")
}

enum class SortDirection(val titleAr: String) {
    ASCENDING("تصاعدي"),
    DESCENDING("تنازلي")
}

data class LibrarySortOption(
    val field: LibrarySortField = LibrarySortField.DATE_ADDED,
    val direction: SortDirection = SortDirection.DESCENDING
)
