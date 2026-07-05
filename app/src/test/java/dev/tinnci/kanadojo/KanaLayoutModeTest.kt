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

    @Test
    fun shellNavigationHidesDuringImmersiveSessions() {
        assertEquals(true, shouldShowShellTopBar(shellNavigationHidden = false))
        assertEquals(false, shouldShowShellTopBar(shellNavigationHidden = true))

        assertEquals(true, shouldShowBottomNavigation(KanaLayoutMode.Compact, shellNavigationHidden = false))
        assertEquals(false, shouldShowBottomNavigation(KanaLayoutMode.Compact, shellNavigationHidden = true))
        assertEquals(false, shouldShowBottomNavigation(KanaLayoutMode.Medium, shellNavigationHidden = false))
        assertEquals(false, shouldShowBottomNavigation(KanaLayoutMode.Expanded, shellNavigationHidden = false))

        assertEquals(false, shouldShowNavigationRail(KanaLayoutMode.Compact, shellNavigationHidden = false))
        assertEquals(true, shouldShowNavigationRail(KanaLayoutMode.Medium, shellNavigationHidden = false))
        assertEquals(true, shouldShowNavigationRail(KanaLayoutMode.Expanded, shellNavigationHidden = false))
        assertEquals(false, shouldShowNavigationRail(KanaLayoutMode.Expanded, shellNavigationHidden = true))
    }

    @Test
    fun practiceHidesShellNavigationOnlyDuringQuestionFlow() {
        assertEquals(false, shouldHideShellNavigationForPractice(showIntro = true, queueComplete = false, hasCurrentExercise = true))
        assertEquals(true, shouldHideShellNavigationForPractice(showIntro = false, queueComplete = false, hasCurrentExercise = true))
        assertEquals(false, shouldHideShellNavigationForPractice(showIntro = false, queueComplete = true, hasCurrentExercise = false))
        assertEquals(false, shouldHideShellNavigationForPractice(showIntro = false, queueComplete = false, hasCurrentExercise = false))
    }
}
