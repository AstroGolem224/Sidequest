package com.astrogolem.sidequest.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.astrogolem.sidequest.core.ui.theme.CardSurface
import com.astrogolem.sidequest.core.ui.theme.CardStroke
import com.astrogolem.sidequest.core.ui.theme.SidequestSpacing
import com.astrogolem.sidequest.core.ui.theme.TextPrimary
import com.astrogolem.sidequest.core.ui.theme.TextSecondary

@Composable
fun ScaffoldCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CardSurface,
        shape = RoundedCornerShape(SidequestSpacing.CardCorner),
        border = BorderStroke(SidequestSpacing.CardBorder, CardStroke.copy(alpha = 0.72f)),
        shadowElevation = SidequestSpacing.CardElevation,
    ) {
        Column(modifier = Modifier.padding(SidequestSpacing.CardPadding)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(
                        top = SidequestSpacing.Xxs,
                        bottom = SidequestSpacing.CardContentGap,
                    ),
                )
            }
            content()
        }
    }
}
