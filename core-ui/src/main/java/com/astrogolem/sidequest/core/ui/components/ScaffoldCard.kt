package com.astrogolem.sidequest.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.astrogolem.sidequest.core.ui.theme.AccentCyan
import com.astrogolem.sidequest.core.ui.theme.AccentViolet
import com.astrogolem.sidequest.core.ui.theme.BgGlow
import com.astrogolem.sidequest.core.ui.theme.CardSurface
import com.astrogolem.sidequest.core.ui.theme.CardSurfaceStrong
import com.astrogolem.sidequest.core.ui.theme.CardStroke
import com.astrogolem.sidequest.core.ui.theme.CardStrokeStrong
import com.astrogolem.sidequest.core.ui.theme.TextSecondary

@Composable
fun ScaffoldCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(CardSurfaceStrong, CardSurface),
                ),
            )
            .drawBehind {
                val stroke = 1.dp.toPx()
                val bracket = size.minDimension * 0.08f
                drawRoundRect(
                    color = CardStrokeStrong,
                    style = Stroke(width = stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                )
                val violet = AccentViolet.copy(alpha = 0.35f)
                drawLine(violet, start = androidx.compose.ui.geometry.Offset(0f, bracket), end = androidx.compose.ui.geometry.Offset(0f, 0f), strokeWidth = stroke * 2)
                drawLine(violet, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(bracket, 0f), strokeWidth = stroke * 2)
                drawLine(violet, start = androidx.compose.ui.geometry.Offset(size.width - bracket, 0f), end = androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = stroke * 2)
                drawLine(violet, start = androidx.compose.ui.geometry.Offset(size.width, 0f), end = androidx.compose.ui.geometry.Offset(size.width, bracket), strokeWidth = stroke * 2)
            },
        color = CardSurface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CardStroke),
    ) {
        Box(modifier = Modifier.background(Brush.verticalGradient(listOf(CardSurfaceStrong, CardSurface)))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Surface(
                    color = BgGlow.copy(alpha = 0.55f),
                    shape = CircleShape,
                ) {
                    Text(
                        text = title.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = AccentCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                    )
                }
                content()
            }
        }
    }
}
