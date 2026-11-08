package com.antest1.gotobrowser.Browser

import org.junit.Assert.assertEquals
import org.junit.Test

class GameGeometryTest {
    private val eps = 0.01f

    @Test
    fun portraitCenterLeavesEqualMargins() {
        val r = GameGeometry.fit(1000f, 2000f, GameGeometry.BIAS_CENTER)
        assertEquals(0f, r.left, eps)
        assertEquals(1000f, r.width, eps)
        assertEquals(600f, r.height, eps)
        assertEquals(700f, r.top, eps)
        assertEquals(2000f - r.bottom, r.top, eps)
    }

    @Test
    fun portraitTopStartsBelowTopInset() {
        val r = GameGeometry.fit(1000f, 2000f, GameGeometry.BIAS_TOP, insetTop = 80f)
        assertEquals(80f, r.top, eps)
        assertEquals(600f, r.height, eps)
    }

    @Test
    fun widerThanGameIgnoresBias() {
        val center = GameGeometry.fit(2400f, 1000f, GameGeometry.BIAS_CENTER, insetLeft = 100f)
        val top = GameGeometry.fit(2400f, 1000f, GameGeometry.BIAS_TOP, insetLeft = 100f)
        assertEquals(center, top)
        assertEquals(0f, top.top, eps)
        assertEquals(1000f, top.height, eps)
        // Centered horizontally in the inset-free width (2300).
        assertEquals(100f + (2300f - 1000f * GameGeometry.RATIO) / 2f, top.left, eps)
    }

    @Test
    fun exactRatioHasNoMarginEitherWay() {
        val center = GameGeometry.fit(1200f, 720f, GameGeometry.BIAS_CENTER)
        val top = GameGeometry.fit(1200f, 720f, GameGeometry.BIAS_TOP)
        assertEquals(center, top)
    }
}
