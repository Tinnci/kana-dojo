package dev.tinnci.kanadojo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TraceKanaExercise(
    item: KanaItem,
    answered: Boolean,
    onEarcon: (KanaEarcon) -> Unit,
    onTaptic: (KanaTaptic) -> Unit,
    reduceMotion: Boolean,
    onSpeak: (String) -> Unit,
    onAnswer: (Boolean) -> Unit
) {
    var pointTokens by rememberSaveable(item.id) { mutableStateOf(emptyList<String>()) }
    var showComparison by rememberSaveable(item.id) { mutableStateOf(false) }
    var showRemediation by rememberSaveable(item.id) { mutableStateOf(false) }
    var replayNonce by rememberSaveable(item.id) { mutableIntStateOf(0) }
    val replayProgress = remember(item.id) { Animatable(1f) }
    val tracePoints = remember(pointTokens) { tracePointsFromSnapshotTokens(pointTokens) }
    val points = remember(tracePoints) { tracePoints.map { Offset(it.x, it.y) } }
    val traceScore = remember(tracePoints) { traceScoreFor(tracePoints) }
    val traceCues = remember(tracePoints, traceScore) { traceFeedbackCuesFor(tracePoints, traceScore) }
    val remediation = remember(traceScore) { traceRemediationFor(traceScore) }
    val animatedScore by animateFloatAsState(
        targetValue = traceScore.progress,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "traceScore"
    )
    val guideAlpha by animateFloatAsState(
        targetValue = if (traceScore.ready || answered) 0.28f else 0.14f,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "traceGuideAlpha"
    )
    val guideLineAlpha by animateFloatAsState(
        targetValue = if (traceScore.ready || answered) 0.24f else 0.13f,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "traceGuideLineAlpha"
    )
    val padBorderColor by animateColorAsState(
        targetValue = when {
            answered && !traceScore.ready -> Color(0xFF9B2D20)
            traceScore.ready -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "tracePadBorder"
    )
    val padBackgroundColor by animateColorAsState(
        targetValue = when {
            answered && !traceScore.ready -> Color(0xFFFFF4F0)
            traceScore.ready -> Color(0xFFF2FAF1)
            else -> Color(0xFFFFFBF3)
        },
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "tracePadBackground"
    )
    val strokeColor by animateColorAsState(
        targetValue = if (answered && !traceScore.ready) Color(0xFF9B2D20) else Color(0xFF2F5D50),
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "traceStroke"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (traceScore.ready || answered) 3.dp else 2.dp,
        animationSpec = kanaMotionSpec(reduceMotion),
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
            IconButton(onClick = {
                onTaptic(KanaTaptic.Speak)
                onSpeak(item.kana)
            }) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = stringResource(R.string.trace_hear_content_description))
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
                        onDragStart = {
                            pointTokens = pointTokens + tracePointSnapshotToken(TracePoint(it.x, it.y))
                        },
                        onDrag = { change, _ ->
                            pointTokens = pointTokens + tracePointSnapshotToken(TracePoint(change.position.x, change.position.y))
                        }
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
                points.firstOrNull()?.let { start ->
                    drawCircle(
                        color = Color(0xFF2F5D50),
                        radius = 8.dp.toPx(),
                        center = start
                    )
                }
                if (points.size > 1) {
                    drawCircle(
                        color = Color(0xFFFFB84D),
                        radius = 7.dp.toPx(),
                        center = points.last()
                    )
                }
            }
        }
        TraceScorePanel(score = animatedScore, ready = traceScore.ready, message = traceScore.message, reduceMotion = reduceMotion)
        TraceCuePanel(cues = traceCues)
        AnimatedVisibility(
            visible = showRemediation && remediation != null,
            enter = if (reduceMotion) EnterTransition.None else fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = if (reduceMotion) ExitTransition.None else fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            remediation?.let { copy ->
                TraceRemediationPanel(
                    remediation = copy,
                    onRetry = {
                        onEarcon(KanaEarcon.Reset)
                        onTaptic(KanaTaptic.Reset)
                        pointTokens = emptyList()
                        showComparison = false
                        showRemediation = false
                    }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    onEarcon(KanaEarcon.Reset)
                    onTaptic(KanaTaptic.Reset)
                    pointTokens = emptyList()
                    showComparison = false
                    showRemediation = false
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.trace_action_clear))
            }
            OutlinedButton(
                onClick = {
                    onEarcon(KanaEarcon.Select)
                    onTaptic(KanaTaptic.Select)
                    val next = !showComparison
                    showComparison = next
                    if (next && points.size > 1) replayNonce += 1
                },
                enabled = points.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (showComparison) {
                        stringResource(R.string.trace_action_hide_comparison)
                    } else {
                        stringResource(R.string.trace_action_compare)
                    }
                )
            }
            Button(
                onClick = {
                    if (traceScore.ready) {
                        onAnswer(true)
                    } else {
                        onEarcon(KanaEarcon.Incorrect)
                        onTaptic(KanaTaptic.Incorrect)
                        showRemediation = true
                        showComparison = true
                        if (points.size > 1) replayNonce += 1
                    }
                },
                enabled = !answered && points.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.trace_action_check))
            }
        }
        AnimatedVisibility(
            visible = showComparison && points.isNotEmpty(),
            enter = if (reduceMotion) EnterTransition.None else fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = if (reduceMotion) ExitTransition.None else fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
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
private fun TraceCuePanel(cues: List<TraceFeedbackCue>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        cues.forEach { cue ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(localizedTraceCueLabel(cue.label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    Text(localizedTraceCueMessage(cue.message), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TraceRemediationPanel(remediation: TraceRemediationCopy, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFDFD6),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = Color(0xFF9B2D20))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(localizedTraceRemediationTitle(remediation.title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(localizedTraceRemediationMessage(remediation.message), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onRetry) {
                Text(localizedTraceRemediationAction(remediation.actionLabel))
            }
        }
    }
}

@Composable
private fun TraceComparisonPanel(item: KanaItem, points: List<Offset>, replayProgress: Float) {
    val guidance = traceGuidanceFor(item)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TraceComparisonTile(label = stringResource(R.string.trace_comparison_model), modifier = Modifier.weight(1f)) {
                TraceModelGlyph(item = item, guidance = guidance)
            }
            TraceComparisonTile(label = stringResource(R.string.trace_comparison_yours), modifier = Modifier.weight(1f)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val normalizedPoints = normalizedTracePoints(points, size.width, size.height)
                    val path = replayedTracePath(normalizedPoints, replayProgress)
                    drawPath(
                        path = path,
                        color = Color(0xFF2F5D50),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    normalizedPoints.firstOrNull()?.let { start ->
                        drawCircle(
                            color = Color(0xFF2F5D50),
                            radius = 4.dp.toPx(),
                            center = start
                        )
                    }
                    if (normalizedPoints.size > 1) {
                        drawCircle(
                            color = Color(0xFFFFB84D),
                            radius = 4.dp.toPx(),
                            center = normalizedPoints.last()
                        )
                    }
                }
            }
        }
        guidance?.let { guidance ->
            TraceGuidancePanel(guidance = guidance)
        }
    }
}

@Composable
private fun TraceModelGlyph(item: KanaItem, guidance: TraceGuidance?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(item.kana, fontSize = 64.sp, color = Color(0x882F5D50), fontWeight = FontWeight.Black)
        Canvas(modifier = Modifier.fillMaxSize()) {
            guidance?.overlays.orEmpty().forEach { overlay ->
                val start = Offset(size.width * overlay.start.x, size.height * overlay.start.y)
                val end = Offset(size.width * overlay.end.x, size.height * overlay.end.y)
                drawLine(
                    color = Color(0xFFFFB84D),
                    start = start,
                    end = end,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = Color(0xFF2F5D50), radius = 4.dp.toPx(), center = start)
                drawCircle(color = Color(0xFFFFB84D), radius = 4.dp.toPx(), center = end)
            }
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
            Text(localizedTraceGuidanceTitle(guidance.title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            guidance.cues.forEach { cue ->
                Text(localizedTraceGuidanceCue(cue), style = MaterialTheme.typography.bodyMedium)
            }
            guidance.overlays.forEach { overlay ->
                Text(
                    "${localizedTraceOverlayLabel(overlay.label)}: ${localizedTraceGuidanceCue(overlay.cue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
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
private fun TraceScorePanel(score: Float, ready: Boolean, message: String, reduceMotion: Boolean) {
    val panelColor by animateColorAsState(
        targetValue = if (ready) Color(0xFFDCEBDD) else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = kanaMotionSpec(reduceMotion),
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
                Text(stringResource(R.string.trace_score_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${(score * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(progress = { score }, modifier = Modifier.fillMaxWidth())
            Text(localizedTraceScoreMessage(message), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun localizedTraceScoreMessage(message: String): String =
    when (message) {
        "Trace over the ghost kana." -> stringResource(R.string.trace_score_empty)
        "Looks ready to check." -> stringResource(R.string.trace_score_ready)
        "Use more of the ghost shape." -> stringResource(R.string.trace_score_more_shape)
        "Add a little more stroke length." -> stringResource(R.string.trace_score_more_length)
        "Keep tracing until the score fills." -> stringResource(R.string.trace_score_keep_tracing)
        else -> message
    }

@Composable
private fun localizedTraceCueLabel(label: String): String =
    when (label) {
        "Start" -> stringResource(R.string.trace_cue_start)
        "Direction" -> stringResource(R.string.trace_cue_direction)
        "Coverage" -> stringResource(R.string.trace_cue_coverage)
        else -> label
    }

@Composable
private fun localizedTraceCueMessage(message: String): String =
    when (message) {
        "Place the first stroke on the ghost kana." -> stringResource(R.string.trace_cue_empty_start)
        "Move slowly enough that the stroke path is visible." -> stringResource(R.string.trace_cue_empty_direction)
        "Start marker is shown on your first touch." -> stringResource(R.string.trace_cue_start_marker)
        "Coverage is broad enough to check." -> stringResource(R.string.trace_coverage_ready)
        "Use more of the ghost shape before checking." -> stringResource(R.string.trace_coverage_more_shape)
        "Coverage is close; add the missing edges." -> stringResource(R.string.trace_coverage_close)
        else -> localizedTraceDirectionCueMessage(message)
    }

@Composable
private fun localizedTraceDirectionCueMessage(message: String): String {
    val prefix = "Your main movement trends "
    if (!message.startsWith(prefix)) return message
    val direction = message.removePrefix(prefix).removeSuffix(".")
    return stringResource(R.string.trace_cue_direction_message, localizedTraceDirection(direction))
}

@Composable
private fun localizedTraceDirection(direction: String): String =
    when (direction) {
        "in a small area" -> stringResource(R.string.trace_direction_small_area)
        "to the right" -> stringResource(R.string.trace_direction_right)
        "to the left" -> stringResource(R.string.trace_direction_left)
        "downward" -> stringResource(R.string.trace_direction_downward)
        "upward" -> stringResource(R.string.trace_direction_upward)
        else -> direction
    }

@Composable
private fun localizedTraceRemediationTitle(title: String): String =
    when (title) {
        "Shape needs one more pass" -> stringResource(R.string.trace_remediation_title)
        else -> title
    }

@Composable
private fun localizedTraceRemediationMessage(message: String): String {
    val suffix = " Compare the model, then retry the stroke."
    val scoreMessage = message.removeSuffix(suffix)
    if (scoreMessage == message) return message
    return stringResource(R.string.trace_remediation_message, localizedTraceScoreMessage(scoreMessage))
}

@Composable
private fun localizedTraceRemediationAction(action: String): String =
    when (action) {
        "Retry trace" -> stringResource(R.string.trace_remediation_action)
        else -> action
    }

@Composable
private fun localizedTraceGuidanceTitle(title: String): String =
    when (title) {
        "Separate from ツ / ン" -> stringResource(R.string.trace_guidance_separate_tsu_n)
        "Separate from シ" -> stringResource(R.string.trace_guidance_separate_shi)
        "Separate from ン" -> stringResource(R.string.trace_guidance_separate_n)
        "Separate from ソ / シ" -> stringResource(R.string.trace_guidance_separate_so_shi)
        "Separate from ち" -> stringResource(R.string.trace_guidance_separate_chi)
        "Separate from さ" -> stringResource(R.string.trace_guidance_separate_sa)
        "Separate from め" -> stringResource(R.string.trace_guidance_separate_me)
        "Separate from ぬ" -> stringResource(R.string.trace_guidance_separate_nu)
        else -> title
    }

@Composable
private fun localizedTraceGuidanceCue(cue: String): String =
    when (cue) {
        "Short strokes sit flatter." -> stringResource(R.string.trace_guidance_short_flatter)
        "Long stroke rises from lower-left to upper-right." -> stringResource(R.string.trace_guidance_long_rises_lower_left)
        "Rise toward upper-right." -> stringResource(R.string.trace_guidance_rise_upper_right)
        "Short strokes stand more upright." -> stringResource(R.string.trace_guidance_short_upright)
        "Long stroke falls from upper-left to lower-right." -> stringResource(R.string.trace_guidance_long_falls_upper_left)
        "Fall toward lower-right." -> stringResource(R.string.trace_guidance_fall_lower_right)
        "Starts with a short upper-left stroke." -> stringResource(R.string.trace_guidance_short_upper_left)
        "Long stroke falls downward." -> stringResource(R.string.trace_guidance_long_falls_downward)
        "Drop down-left." -> stringResource(R.string.trace_guidance_drop_down_left)
        "First stroke is small and high." -> stringResource(R.string.trace_guidance_first_small_high)
        "Long stroke rises toward the upper-right." -> stringResource(R.string.trace_guidance_long_rises_upper_right)
        "Top stroke is separate." -> stringResource(R.string.trace_guidance_top_separate)
        "Lower curve stays compact." -> stringResource(R.string.trace_guidance_lower_curve_compact)
        "Starts with a top stroke." -> stringResource(R.string.trace_guidance_starts_top_stroke)
        "Main stroke drops then curves wide." -> stringResource(R.string.trace_guidance_main_drops_curves_wide)
        "Look for the loop and finishing tail." -> stringResource(R.string.trace_guidance_loop_tail)
        "Keep the crossing clear." -> stringResource(R.string.trace_guidance_crossing_clear)
        "No final loop tail." -> stringResource(R.string.trace_guidance_no_final_loop)
        "Keep the curve simple and open." -> stringResource(R.string.trace_guidance_curve_open)
        else -> cue
    }

@Composable
private fun localizedTraceOverlayLabel(label: String): String =
    when (label) {
        "Main" -> stringResource(R.string.trace_guidance_overlay_main)
        else -> label
    }
