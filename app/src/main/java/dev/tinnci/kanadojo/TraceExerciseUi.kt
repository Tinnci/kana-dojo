package dev.tinnci.kanadojo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TraceKanaExercise(
    item: KanaItem,
    answered: Boolean,
    reduceMotion: Boolean,
    onSpeak: (String) -> Unit,
    onAnswer: (Boolean) -> Unit
) {
    var points by remember(item.id) { mutableStateOf<List<Offset>>(emptyList()) }
    var showComparison by remember(item.id) { mutableStateOf(false) }
    var replayNonce by remember(item.id) { mutableIntStateOf(0) }
    val replayProgress = remember(item.id) { Animatable(1f) }
    val traceScore = remember(points) { traceScoreFor(points.map { TracePoint(it.x, it.y) }) }
    val animatedScore by animateFloatAsState(targetValue = traceScore.progress, label = "traceScore")
    val guideAlpha by animateFloatAsState(
        targetValue = if (traceScore.ready || answered) 0.28f else 0.14f,
        label = "traceGuideAlpha"
    )
    val guideLineAlpha by animateFloatAsState(
        targetValue = if (traceScore.ready || answered) 0.24f else 0.13f,
        label = "traceGuideLineAlpha"
    )
    val padBorderColor by animateColorAsState(
        targetValue = when {
            answered && !traceScore.ready -> Color(0xFF9B2D20)
            traceScore.ready -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        label = "tracePadBorder"
    )
    val padBackgroundColor by animateColorAsState(
        targetValue = when {
            answered && !traceScore.ready -> Color(0xFFFFF4F0)
            traceScore.ready -> Color(0xFFF2FAF1)
            else -> Color(0xFFFFFBF3)
        },
        label = "tracePadBackground"
    )
    val strokeColor by animateColorAsState(
        targetValue = if (answered && !traceScore.ready) Color(0xFF9B2D20) else Color(0xFF2F5D50),
        label = "traceStroke"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (traceScore.ready || answered) 3.dp else 2.dp,
        label = "traceBorderWidth"
    )

    LaunchedEffect(points.size) {
        replayProgress.snapTo(1f)
    }

    LaunchedEffect(replayNonce, reduceMotion) {
        if (replayNonce > 0 && points.size > 1) {
            replayProgress.snapTo(0f)
            if (reduceMotion) {
                replayProgress.snapTo(1f)
            } else {
                replayProgress.animateTo(1f, animationSpec = tween(durationMillis = 900))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.romaji, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onSpeak(item.kana) }) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = "Hear")
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(padBackgroundColor, RoundedCornerShape(26.dp))
                .border(
                    width = borderWidth,
                    color = padBorderColor,
                    shape = RoundedCornerShape(26.dp)
                )
                .pointerInput(item.id) {
                    detectDragGestures(
                        onDragStart = { points = points + it },
                        onDrag = { change, _ -> points = points + change.position }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(item.kana, fontSize = 190.sp, color = Color(0xFF2F5D50).copy(alpha = guideAlpha), fontWeight = FontWeight.Black)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color(0xFFA66A5A).copy(alpha = guideLineAlpha),
                    start = Offset(size.width * 0.18f, size.height * 0.5f),
                    end = Offset(size.width * 0.82f, size.height * 0.5f),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color(0xFFA66A5A).copy(alpha = guideLineAlpha),
                    start = Offset(size.width * 0.5f, size.height * 0.18f),
                    end = Offset(size.width * 0.5f, size.height * 0.82f),
                    strokeWidth = 2.dp.toPx()
                )
                drawPath(
                    path = replayedTracePath(points, replayProgress.value),
                    color = strokeColor,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        TraceScorePanel(score = animatedScore, ready = traceScore.ready, message = traceScore.message)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    points = emptyList()
                    showComparison = false
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
            OutlinedButton(
                onClick = {
                    val next = !showComparison
                    showComparison = next
                    if (next && points.size > 1) replayNonce += 1
                },
                enabled = points.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (showComparison) "Hide" else "Compare")
            }
            Button(onClick = { onAnswer(traceScore.ready) }, enabled = !answered && points.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Text("Check")
            }
        }
        AnimatedVisibility(
            visible = showComparison && points.isNotEmpty(),
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            TraceComparisonPanel(
                item = item,
                points = points,
                replayProgress = if (reduceMotion) 1f else replayProgress.value
            )
        }
    }
}

private fun replayedTracePath(points: List<Offset>, progress: Float): Path {
    val path = Path()
    val visibleCount = (points.size * progress).toInt().coerceIn(0, points.size)
    points.take(visibleCount).firstOrNull()?.let { path.moveTo(it.x, it.y) }
    points.take(visibleCount).drop(1).forEach { path.lineTo(it.x, it.y) }
    return path
}

@Composable
private fun TraceComparisonPanel(item: KanaItem, points: List<Offset>, replayProgress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TraceComparisonTile(label = "Model", modifier = Modifier.weight(1f)) {
                Text(item.kana, fontSize = 64.sp, color = Color(0x882F5D50), fontWeight = FontWeight.Black)
            }
            TraceComparisonTile(label = "Yours", modifier = Modifier.weight(1f)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = replayedTracePath(normalizedTracePoints(points, size.width, size.height), replayProgress)
                    drawPath(
                        path = path,
                        color = Color(0xFF2F5D50),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
        traceGuidanceFor(item)?.let { guidance ->
            TraceGuidancePanel(guidance = guidance)
        }
    }
}

private fun normalizedTracePoints(points: List<Offset>, width: Float, height: Float): List<Offset> {
    if (points.isEmpty()) return emptyList()
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val sourceWidth = (maxX - minX).coerceAtLeast(1f)
    val sourceHeight = (maxY - minY).coerceAtLeast(1f)
    val scale = minOf(width * 0.72f / sourceWidth, height * 0.72f / sourceHeight)
    val offsetX = (width - sourceWidth * scale) / 2f
    val offsetY = (height - sourceHeight * scale) / 2f
    return points.map { point ->
        Offset(
            x = offsetX + (point.x - minX) * scale,
            y = offsetY + (point.y - minY) * scale
        )
    }
}

@Composable
private fun TraceGuidancePanel(guidance: TraceGuidance) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE7DEFF),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(guidance.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            guidance.cues.forEach { cue ->
                Text(cue, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TraceComparisonTile(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.height(104.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

@Composable
private fun TraceScorePanel(score: Float, ready: Boolean, message: String) {
    val panelColor by animateColorAsState(
        targetValue = if (ready) Color(0xFFDCEBDD) else MaterialTheme.colorScheme.surfaceVariant,
        label = "tracePanelColor"
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = panelColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (ready) Color(0xFF2F5D50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text("Trace score", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${(score * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(progress = { score }, modifier = Modifier.fillMaxWidth())
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
