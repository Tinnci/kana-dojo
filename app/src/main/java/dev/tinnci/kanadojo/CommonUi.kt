package dev.tinnci.kanadojo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FocusMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ProgressStat(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = color, modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun StageChip(stage: LearningStage) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when (stage) {
            LearningStage.Anchor -> Color(0xFFDCEBDD)
            LearningStage.RegularRows -> Color(0xFFFFF1BC)
            LearningStage.ShapeHeavy -> Color(0xFFFFDFD6)
            LearningStage.TailRows -> Color(0xFFE7DEFF)
            LearningStage.Voiced -> Color(0xFFE2EEF8)
            LearningStage.Combination -> Color(0xFFFFF1BC)
            LearningStage.Special -> Color(0xFFFFDFD6)
            LearningStage.Confusable -> Color(0xFFE2EEF8)
        }
    ) {
        Text(
            localizedLearningStageLabel(stage),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun localizedLearningStageLabel(stage: LearningStage): String =
    when (stage) {
        LearningStage.Anchor -> stringResource(R.string.path_stage_anchor)
        LearningStage.RegularRows -> stringResource(R.string.path_stage_rows)
        LearningStage.ShapeHeavy -> stringResource(R.string.path_stage_shapes)
        LearningStage.TailRows -> stringResource(R.string.path_stage_tail)
        LearningStage.Voiced -> stringResource(R.string.path_stage_marks)
        LearningStage.Combination -> stringResource(R.string.path_stage_blend)
        LearningStage.Special -> stringResource(R.string.path_stage_special)
        LearningStage.Confusable -> stringResource(R.string.path_stage_contrast)
    }

@Composable
fun localizedLearningStageLabelText(label: String): String =
    when (label) {
        "Anchor" -> stringResource(R.string.path_stage_anchor)
        "Rows" -> stringResource(R.string.path_stage_rows)
        "Shapes" -> stringResource(R.string.path_stage_shapes)
        "Tail" -> stringResource(R.string.path_stage_tail)
        "Marks" -> stringResource(R.string.path_stage_marks)
        "Blend" -> stringResource(R.string.path_stage_blend)
        "Special" -> stringResource(R.string.path_stage_special)
        "Contrast" -> stringResource(R.string.path_stage_contrast)
        else -> label
    }

@Composable
fun localizedLearningStageLabelLower(label: String): String =
    when (label) {
        "Anchor", "anchor" -> stringResource(R.string.path_stage_anchor_lower)
        "Rows", "rows" -> stringResource(R.string.path_stage_rows_lower)
        "Shapes", "shapes" -> stringResource(R.string.path_stage_shapes_lower)
        "Tail", "tail" -> stringResource(R.string.path_stage_tail_lower)
        "Marks", "marks" -> stringResource(R.string.path_stage_marks_lower)
        "Blend", "blend" -> stringResource(R.string.path_stage_blend_lower)
        "Special", "special" -> stringResource(R.string.path_stage_special_lower)
        "Contrast", "contrast" -> stringResource(R.string.path_stage_contrast_lower)
        else -> label
    }
