package io.github.spearchucker667.veniceforge.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard centralized loading indicator for Venice Fyr.
 *
 * Combines the Codex Pet v2 sprite animation with accessible status typography.
 */
@Composable
fun VeniceLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null,
    state: CodexPetState = CodexPetState.ActiveTask,
    petSize: Dp = 72.dp,
    reducedMotion: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CodexPetRenderer(
            state = state,
            modifier = Modifier.size(petSize),
            reducedMotion = reducedMotion,
            contentDescription = message ?: "Loading indicator",
        )
        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Compact inline status indicator for chat streams, task progress, and approval prompts.
 */
@Composable
fun VenicePetStatusIndicator(
    state: CodexPetState,
    modifier: Modifier = Modifier,
    message: String? = null,
    petSize: Dp = 36.dp,
    reducedMotion: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CodexPetRenderer(
            state = state,
            modifier = Modifier.size(petSize),
            reducedMotion = reducedMotion,
            contentDescription = message ?: "Status indicator: ${state.name}",
        )
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
