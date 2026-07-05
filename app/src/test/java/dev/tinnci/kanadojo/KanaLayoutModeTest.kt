package dev.tinnci.kanadojo

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class KanaLayoutModeTest {
    @Test
    fun widthClassesFollowMaterialBreakpoints() {
        assertEquals(KanaLayoutMode.Compact, kanaLayoutModeFor(599.dp))
        assertEquals(KanaLayoutMode.Medium, kanaLayoutModeFor(600.dp))
        assertEquals(KanaLayoutMode.Medium, kanaLayoutModeFor(839.dp))
        assertEquals(KanaLayoutMode.Expanded, kanaLayoutModeFor(840.dp))
    }

    @Test
    fun compactHeightKeepsLayoutSinglePane() {
        assertEquals(KanaLayoutMode.Compact, kanaLayoutModeFor(840.dp, 479.dp))
        assertEquals(KanaLayoutMode.Expanded, kanaLayoutModeFor(840.dp, 480.dp))
    }

    @Test
    fun compactTopBarKeepsOnlyPrimaryBrandText() {
        assertEquals(false, shouldShowTopBarSubtitle(KanaLayoutMode.Compact))
        assertEquals(true, shouldShowTopBarSubtitle(KanaLayoutMode.Medium))
        assertEquals(true, shouldShowTopBarSubtitle(KanaLayoutMode.Expanded))
    }
}
