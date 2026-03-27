package com.astrogolem.sidequest.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.astrogolem.sidequest.core.ui.theme.Danger
import com.astrogolem.sidequest.core.ui.theme.SidequestSpacing

@Composable
fun DestructiveOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val borderColor = if (enabled) Danger.copy(alpha = 0.64f) else Danger.copy(alpha = 0.28f)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        border = BorderStroke(SidequestSpacing.CardBorder, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Danger,
            disabledContentColor = Danger.copy(alpha = 0.38f),
        ),
    ) {
        Text(label)
    }
}
