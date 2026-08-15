package io.github.spearchucker667.veniceforge.android.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.spearchucker667.veniceforge.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    availableModels: List<String>,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    var input by remember { mutableStateOf("") }
    var modelMenuOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Model picker (text-only for milestone 1; capability-driven grouping deferred).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.chat_model_picker_label))
            OutlinedButton(onClick = { modelMenuOpen = true }) {
                Text(state.modelId)
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
                    Text(msg.role.name, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    Text(msg.text)
                }
            }
            if (state.isStreaming) {
                item { Text("…") }
            }
        }

        state.error?.let { Text(it) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.chat_composer_hint)) },
                enabled = !state.isStreaming,
            )
            TextButton(
                enabled = input.isNotBlank() && !state.isStreaming,
                onClick = {
                    viewModel.submit(input)
                    input = ""
                },
            ) {
                Text(stringResource(R.string.chat_send))
            }
        }
    }
}
