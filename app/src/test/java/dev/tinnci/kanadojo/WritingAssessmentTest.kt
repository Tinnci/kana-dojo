package dev.tinnci.kanadojo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingAssessmentTest {
    @Test
    fun guidedTraceDoesNotClaimFreeHandwritingRecognition() {
        val plan = writingAssessmentPlanFor(WritingAssessmentEngine.GuidedTrace)

        assertEquals("guided trace scoring", plan.learnerFacingClaim)
        assertTrue(plan.capabilities.teachesStrokeShape)
        assertFalse(plan.capabilities.comparesTemplateShape)
        assertFalse(canClaimFreeHandwritingRecognition(plan))
        assertFalse(shouldShowWritingModelManagement(plan))
    }

    @Test
    fun templateSimilarityStaysShapeComparisonNotRecognition() {
        val plan = writingAssessmentPlanFor(WritingAssessmentEngine.TemplateSimilarity)

        assertEquals("shape similarity", plan.learnerFacingClaim)
        assertTrue(plan.capabilities.teachesStrokeShape)
        assertTrue(plan.capabilities.comparesTemplateShape)
        assertFalse(canClaimFreeHandwritingRecognition(plan))
        assertFalse(shouldShowWritingModelManagement(plan))
    }

    @Test
    fun mlKitDigitalInkRequiresModelManagementBeforeRecognitionUi() {
        val plan = writingAssessmentPlanFor(WritingAssessmentEngine.MlKitDigitalInk)

        assertEquals("handwriting recognition", plan.learnerFacingClaim)
        assertTrue(canClaimFreeHandwritingRecognition(plan))
        assertTrue(shouldShowWritingModelManagement(plan))
        assertTrue(plan.capabilities.canRunOfflineAfterSetup)
    }

    @Test
    fun unavailableMlRecognitionFallsBackToGuidedTrace() {
        assertEquals(
            WritingAssessmentEngine.GuidedTrace,
            fallbackWritingAssessmentEngineFor(WritingAssessmentEngine.MlKitDigitalInk)
        )
        assertEquals(
            WritingAssessmentEngine.TemplateSimilarity,
            fallbackWritingAssessmentEngineFor(WritingAssessmentEngine.TemplateSimilarity)
        )
    }
}
