package com.astrogolem.sidequest.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.astrogolem.sidequest.core.ui.theme.SidequestSpacing

data class StateShellAction(
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun StateShellCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    supportingLines: List<String> = emptyList(),
    actions: (@Composable ColumnScope.() -> Unit)? = null,
) {
    ScaffoldCard(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
    ) {
        if (supportingLines.isNotEmpty() || actions != null) {
            Column(verticalArrangement = Arrangement.spacedBy(SidequestSpacing.ItemGap)) {
                supportingLines.forEach { line ->
                    InlineSupportText(line)
                }
                actions?.invoke(this)
            }
        }
    }
}

@Composable
fun LoadingStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    StateShellCard(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        actions = {
            CircularProgressIndicator(
                strokeWidth = SidequestSpacing.CardBorder,
            )
        },
    )
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    supportingLines: List<String> = emptyList(),
    primaryAction: StateShellAction? = null,
    secondaryAction: StateShellAction? = null,
) {
    StateShellCard(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        supportingLines = supportingLines,
        actions = {
            StateShellActions(
                primaryAction = primaryAction,
                secondaryAction = secondaryAction,
            )
        },
    )
}

@Composable
fun ErrorStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    supportingLines: List<String> = emptyList(),
    primaryAction: StateShellAction? = null,
    secondaryAction: StateShellAction? = null,
) {
    StateShellCard(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        supportingLines = supportingLines,
        actions = {
            StateShellActions(
                primaryAction = primaryAction,
                secondaryAction = secondaryAction,
            )
        },
    )
}

@Composable
private fun ColumnScope.StateShellActions(
    primaryAction: StateShellAction?,
    secondaryAction: StateShellAction?,
) {
    if (primaryAction == null && secondaryAction == null) return
    Column(verticalArrangement = Arrangement.spacedBy(SidequestSpacing.ControlGap)) {
        primaryAction?.let { action ->
            Button(
                onClick = action.onClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                androidx.compose.material3.Text(action.label)
            }
        }
        secondaryAction?.let { action ->
            OutlinedButton(
                onClick = action.onClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                androidx.compose.material3.Text(action.label)
            }
        }
    }
}
