package com.example.markpoint.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.markpoint.ui.theme.Wordmark

private const val SPLASH_DURATION_MS = 1300
private const val SPLASH_POINTS = 4

/** 0..1 progress of a sub-phase that runs between [from] and [to] of the overall animation. */
private fun phase(progress: Float, from: Float, to: Float): Float =
    ((progress - from) / (to - from)).coerceIn(0f, 1f)

private fun easeOut(t: Float): Float = FastOutSlowInEasing.transform(t)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SPLASH_DURATION_MS, easing = LinearEasing))
        finish()
    }

    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .semantics { contentDescription = "MarkPoint. Place evenly spaced marks." },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Canvas(Modifier.widthIn(max = 320.dp).fillMaxWidth().padding(horizontal = 40.dp).height(48.dp)) {
                val p = progress.value
                val y = size.height / 2
                val left = 8.dp.toPx()
                val right = size.width - 8.dp.toPx()
                val width = right - left

                // 1. The measurement line draws in.
                val lineT = easeOut(phase(p, 0f, 0.3f))
                drawLine(scheme.outline, Offset(left, y), Offset(left + width * lineT, y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)

                // Start reference point.
                if (lineT > 0f) {
                    drawCircle(scheme.onSurface, radius = 6.dp.toPx(), center = Offset(left, y), style = Stroke(2.dp.toPx()))
                }

                // 2. Points appear at the start, then 3. distribute evenly.
                val appear = phase(p, 0.22f, 0.4f)
                val spread = easeOut(phase(p, 0.35f, 0.75f))
                for (n in 1..SPLASH_POINTS) {
                    val target = n.toFloat() / SPLASH_POINTS
                    val x = left + width * target * spread
                    val isEnd = n == SPLASH_POINTS
                    val tick = 8.dp.toPx()
                    drawLine(
                        scheme.primary.copy(alpha = appear),
                        Offset(x, y - tick), Offset(x, y + tick),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
                    )
                    drawCircle(scheme.primary.copy(alpha = appear), radius = 4.dp.toPx(), center = Offset(x, y))
                    // 4. The final endpoint is highlighted.
                    if (isEnd) {
                        val highlight = phase(p, 0.72f, 0.9f)
                        drawCircle(scheme.primary.copy(alpha = highlight), radius = 6.5f.dp.toPx(), center = Offset(x, y))
                        drawCircle(
                            scheme.primary.copy(alpha = 0.25f * highlight),
                            radius = (6.5f + 6f * highlight).dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
            // 5. Title fades in.
            val titleAlpha = phase(progress.value, 0.6f, 0.95f)
            Column(Modifier.alpha(titleAlpha), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MARKPOINT", style = Wordmark, color = scheme.onBackground)
                Text("Place evenly spaced marks", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
        }
    }
}
