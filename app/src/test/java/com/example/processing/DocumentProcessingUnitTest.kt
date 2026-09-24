package com.example.processing

import android.graphics.PointF
import com.example.processing.model.DocumentQuad
import com.example.processing.model.ProcessingMode
import com.example.processing.model.ProcessingOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocumentProcessingUnitTest {

    @Test
    fun testDefaultQuadIsConvex() {
        val defaultQuad = DocumentQuad.defaultQuad()
        assertTrue("Default quad should be convex", defaultQuad.isConvex())
    }

    @Test
    fun testFullImageQuadDimensions() {
        val fullQuad = DocumentQuad.fullImage()
        val (width, height) = fullQuad.calculateDimensions(1000f, 1500f)
        assertEquals(1000, width)
        assertEquals(1500, height)
    }

    @Test
    fun testInvertedQuadIsNonConvex() {
        // Crossed diagonals (hourglass shape)
        val invertedQuad = DocumentQuad(
            topLeft = PointF(0.1f, 0.1f),
            topRight = PointF(0.9f, 0.9f),
            bottomRight = PointF(0.9f, 0.1f),
            bottomLeft = PointF(0.1f, 0.9f)
        )
        assertFalse("Crossed points should not be convex", invertedQuad.isConvex())
    }

    @Test
    fun testClampedQuad() {
        val outOfBounds = DocumentQuad(
            topLeft = PointF(-0.2f, -0.1f),
            topRight = PointF(1.5f, 0.05f),
            bottomRight = PointF(1.2f, 1.1f),
            bottomLeft = PointF(0f, 1.3f)
        )
        val clamped = outOfBounds.clamped()
        assertEquals(0f, clamped.topLeft.x, 0.001f)
        assertEquals(0f, clamped.topLeft.y, 0.001f)
        assertEquals(1f, clamped.topRight.x, 0.001f)
        assertEquals(1f, clamped.bottomRight.y, 0.001f)
    }

    @Test
    fun testProcessingModesAvailability() {
        val modes = ProcessingMode.values()
        assertTrue(modes.contains(ProcessingMode.ORIGINAL))
        assertTrue(modes.contains(ProcessingMode.AUTO))
        assertTrue(modes.contains(ProcessingMode.DOCUMENT))
        assertTrue(modes.contains(ProcessingMode.ENHANCED))
        assertTrue(modes.contains(ProcessingMode.GRAYSCALE))
        assertTrue(modes.contains(ProcessingMode.BLACK_AND_WHITE))
    }

    @Test
    fun testDefaultProcessingOptions() {
        val options = ProcessingOptions()
        assertEquals(0, options.rotationDegrees)
        assertEquals(ProcessingMode.DOCUMENT, options.mode)
        assertEquals(0f, options.brightness, 0.001f)
        assertEquals(1.0f, options.contrast, 0.001f)
    }
}
