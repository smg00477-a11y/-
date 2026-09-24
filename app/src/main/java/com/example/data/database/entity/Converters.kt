package com.example.data.database.entity

import androidx.room.TypeConverter
import com.example.data.model.DocumentType

class Converters {
    @TypeConverter
    fun fromDocumentType(type: DocumentType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toDocumentType(name: String?): DocumentType {
        return try {
            if (name != null) DocumentType.valueOf(name) else DocumentType.DOCUMENT
        } catch (e: Exception) {
            DocumentType.DOCUMENT
        }
    }
}
