package com.astrogolem.sidequest.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.CardSurface
import com.astrogolem.sidequest.core.ui.theme.CardStroke
import com.astrogolem.sidequest.core.ui.theme.SidequestSpacing
import com.astrogolem.sidequest.core.ui.theme.TextSecondary

@Composable
fun GlassCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SidequestSpacing.CardCorner),
        color = CardSurface,
        border = BorderStroke(SidequestSpacing.CardBorder, CardStroke.copy(alpha = 0.72f)),
        shadowElevation = SidequestSpacing.CardElevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SidequestSpacing.CardPadding),
        ) {
            Column(modifier = Modifier.padding(bottom = SidequestSpacing.CardContentGap)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = AccentPrimary,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
            content()
        }
    }
}
