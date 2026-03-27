package com.astrogolem.sidequest.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.astrogolem.sidequest.core.ui.theme.AccentCyan
import com.astrogolem.sidequest.core.ui.theme.AccentCoral
import com.astrogolem.sidequest.core.ui.theme.AccentGold
import com.astrogolem.sidequest.core.ui.theme.AccentNeutral
import com.astrogolem.sidequest.core.ui.theme.AccentViolet
import com.astrogolem.sidequest.core.ui.theme.BgGlow
import com.astrogolem.sidequest.core.ui.theme.SidequestSpacing

@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    tone: HudTone = HudTone.Cyan,
) {
    val color = tone.color()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SidequestSpacing.PillCorner))
            .background(color.copy(alpha = 0.16f))
            .padding(
                horizontal = SidequestSpacing.StatusPillHorizontal,
                vertical = SidequestSpacing.StatusPillVertical,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = color,
        )
    }
}

@Composable
fun SegmentedMeter(
    progress: Float,
    modifier: Modifier = Modifier,
    segments: Int = 12,
    tone: HudTone = HudTone.Cyan,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SidequestSpacing.Xxxs),
    ) {
        val activeSegments = (segments * progress.coerceIn(0f, 1f)).toInt().coerceAtLeast(if (progress > 0f) 1 else 0)
        repeat(segments) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(SidequestSpacing.MeterHeight)
                    .clip(RoundedCornerShape(SidequestSpacing.FullCorner))
                    .background(
                        if (index < activeSegments) tone.color().copy(alpha = 0.95f) else BgGlow.copy(alpha = 0.45f),
                    ),
            )
        }
    }
}

@Composable
fun HudRing(
    progress: Float,
    modifier: Modifier = Modifier,
    tone: HudTone = HudTone.Cyan,
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.12f
        drawArc(
            color = BgGlow.copy(alpha = 0.5f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = tone.color(),
            startAngle = 135f,
            sweepAngle = 270f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

enum class HudTone {
    Cyan,
    Amber,
    Violet,
    Coral,
    Neutral,
}

private fun HudTone.color(): Color = when (this) {
    HudTone.Cyan -> AccentCyan
    HudTone.Amber -> AccentGold
    HudTone.Violet -> AccentViolet
    HudTone.Coral -> AccentCoral
    HudTone.Neutral -> AccentNeutral
}
