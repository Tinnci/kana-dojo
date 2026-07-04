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
            stage.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
