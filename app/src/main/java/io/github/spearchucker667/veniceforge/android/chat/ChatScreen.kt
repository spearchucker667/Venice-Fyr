package io.github.spearchucker667.veniceforge.android.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.spearchucker667.veniceforge.android.R
import io.github.spearchucker667.veniceforge.core.designsystem.CodexPetState
import io.github.spearchucker667.veniceforge.core.designsystem.VenicePetStatusIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    availableModels: List<String>,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var modelMenuOpen by rememberSaveable { mutableStateOf(false) }
    var pendingMessage by rememberSaveable { mutableStateOf<String?>(null) }

    pendingMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { pendingMessage = null },
            title = { Text(stringResource(R.string.approval_title)) },
            text = { Text(stringResource(R.string.approval_chat_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingMessage = null
                    viewModel.submit(message)
                    input = ""
                }) { Text(stringResource(R.string.approval_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingMessage = null }) {
                    Text(stringResource(R.string.approval_cancel))
                }
            },
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Model picker (text-only for milestone 1; capability-driven grouping deferred).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.chat_model_picker_label))
            OutlinedButton(onClick = { modelMenuOpen = true }) {
                Text(state.modelId ?: stringResource(R.string.chat_select_model))
            }
            DropdownMenu(expanded = modelMenuOpen, onDismissRequest = { modelMenuOpen = false }) {
                if (availableModels.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_no_models)) },
                        onClick = { modelMenuOpen = false },
                    )
                } else {
                    availableModels.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                viewModel.setModel(model)
                                modelMenuOpen = false
                            },
                        )
                    }
                }
            }
            if (state.isStreaming) {
                TextButton(onClick = { viewModel.cancel() }) {
                    Text(stringResource(R.string.chat_cancel))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages, key = { it.id }) { msg ->
                Column {
                    Text(msg.role.name, style = MaterialTheme.typography.labelSmall)
                    Text(msg.text)
                }
            }
            if (state.isStreaming) {
                item {
                    VenicePetStatusIndicator(
                        state = CodexPetState.ActiveTask,
                        message = "Generating response…",
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }

        state.error?.let { err ->
            VenicePetStatusIndicator(
                state = CodexPetState.Failed,
                message = err,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.chat_composer_hint)) },
                enabled = !state.isStreaming,
            )
            TextButton(
                enabled = input.isNotBlank() && !state.isStreaming && !state.modelId.isNullOrBlank(),
                onClick = { pendingMessage = input },
            ) {
                Text(stringResource(R.string.chat_send))
            }
        }
    }
}
