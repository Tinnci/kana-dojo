package dev.tinnci.kanadojo

enum class WritingAssessmentEngine {
    GuidedTrace,
    TemplateSimilarity,
    MlKitDigitalInk
}

data class WritingAssessmentCapabilities(
    val teachesStrokeShape: Boolean,
    val comparesTemplateShape: Boolean,
    val recognizesFreeHandwriting: Boolean,
    val requiresDownloadedModel: Boolean,
    val canRunOfflineAfterSetup: Boolean
)

data class WritingAssessmentPlan(
    val engine: WritingAssessmentEngine,
    val learnerFacingClaim: String,
    val capabilities: WritingAssessmentCapabilities
)

fun writingAssessmentPlanFor(engine: WritingAssessmentEngine): WritingAssessmentPlan =
    when (engine) {
        WritingAssessmentEngine.GuidedTrace -> WritingAssessmentPlan(
            engine = engine,
            learnerFacingClaim = "guided trace scoring",
            capabilities = WritingAssessmentCapabilities(
                teachesStrokeShape = true,
                comparesTemplateShape = false,
                recognizesFreeHandwriting = false,
                requiresDownloadedModel = false,
                canRunOfflineAfterSetup = true
            )
        )

        WritingAssessmentEngine.TemplateSimilarity -> WritingAssessmentPlan(
            engine = engine,
            learnerFacingClaim = "shape similarity",
            capabilities = WritingAssessmentCapabilities(
                teachesStrokeShape = true,
                comparesTemplateShape = true,
                recognizesFreeHandwriting = false,
                requiresDownloadedModel = false,
                canRunOfflineAfterSetup = true
            )
        )

        WritingAssessmentEngine.MlKitDigitalInk -> WritingAssessmentPlan(
            engine = engine,
            learnerFacingClaim = "handwriting recognition",
            capabilities = WritingAssessmentCapabilities(
                teachesStrokeShape = false,
                comparesTemplateShape = false,
                recognizesFreeHandwriting = true,
                requiresDownloadedModel = true,
                canRunOfflineAfterSetup = true
            )
        )
    }

fun shouldShowWritingModelManagement(plan: WritingAssessmentPlan): Boolean =
    plan.capabilities.requiresDownloadedModel

fun canClaimFreeHandwritingRecognition(plan: WritingAssessmentPlan): Boolean =
    plan.capabilities.recognizesFreeHandwriting

fun fallbackWritingAssessmentEngineFor(engine: WritingAssessmentEngine): WritingAssessmentEngine =
    when (engine) {
        WritingAssessmentEngine.MlKitDigitalInk -> WritingAssessmentEngine.GuidedTrace
        else -> engine
    }
