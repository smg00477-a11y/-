package com.example.ocr

import com.example.ocr.postprocessing.ArabicOcrPostProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicOcrPostProcessorTest {

    @Test
    fun testCleanPunctuation() {
        val raw = "هل هذا نص عربي? نعم, إنه رقيم!"
        val cleaned = ArabicOcrPostProcessor.clean(raw)
        // Should replace ? with ؟ and , with ، in Arabic context
        assertTrue(cleaned.contains("؟"))
        assertTrue(cleaned.contains("،"))
    }

    @Test
    fun testCleanWhitespaceAndTatweel() {
        val raw = "رقيم    من   الـ  ـورق"
        val cleaned = ArabicOcrPostProcessor.clean(raw)
        // Multiple spaces collapsed
        assertTrue(!cleaned.contains("   "))
    }

    @Test
    fun testNormalizeSearch() {
        val queryWithVariants = "أحمد وإبراهيم في مكة المكرمة"
        val normalized = ArabicOcrPostProcessor.normalizeForSearch(queryWithVariants)
        
        // Hamzas normalized to bare alef, taa marbuta to haa
        assertTrue(normalized.contains("احمد"))
        assertTrue(normalized.contains("ابراهيم"))
        assertTrue(normalized.contains("مكه المكرمه"))
    }

    @Test
    fun testNormalizeSearchDigits() {
        val textWithHindiDigits = "الصفحة ١٢٣ من ٤٥٦"
        val normalized = ArabicOcrPostProcessor.normalizeForSearch(textWithHindiDigits)
        assertTrue(normalized.contains("123"))
        assertTrue(normalized.contains("456"))
    }

    @Test
    fun testNormalizeSearchTashkeel() {
        val withTashkeel = "رَقِيمٌ مِنَ الوَرَقِ"
        val normalized = ArabicOcrPostProcessor.normalizeForSearch(withTashkeel)
        assertTrue(normalized.contains("رقيم"))
        assertTrue(normalized.contains("الورق"))
    }
}
