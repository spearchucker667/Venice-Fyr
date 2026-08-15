package io.github.spearchucker667.veniceforge.android.image

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.spearchucker667.veniceforge.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageScreen(
    viewModel: ImageViewModel,
    availableModels: List<String>,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingOperation by rememberSaveable { mutableStateOf<String?>(null) }

    pendingOperation?.let { operation ->
        AlertDialog(
            onDismissRequest = { pendingOperation = null },
            title = { Text(stringResource(R.string.approval_title)) },
            text = { Text(stringResource(R.string.approval_image_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingOperation = null
                    if (operation == "edit") viewModel.editImage() else viewModel.generateImage()
                }) { Text(stringResource(R.string.approval_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingOperation = null }) {
                    Text(stringResource(R.string.approval_cancel))
                }
            },
        )
    }

    // Setup SAF Photo Picker for visual media
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            viewModel.onImageSelected(uri)
        }
    )

    // Set a default model if not set
    LaunchedEffect(availableModels) {
        if (state.selectedModelId == null && availableModels.isNotEmpty()) {
            viewModel.setDefaultModelIfUnset(availableModels.first())
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Image Studio", style = MaterialTheme.typography.titleLarge)
        }

        item {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = state.selectedModelId ?: "No model selected",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Model") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableModels.forEach { modelId ->
                        DropdownMenuItem(
                            text = { Text(modelId) },
                            onClick = {
                                viewModel.selectModel(modelId)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.prompt,
                onValueChange = { viewModel.updatePrompt(it) },
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.inputImageUri != null) "Change Image" else "Select Image")
                }
            }
        }

        if (state.inputImageUri != null) {
            item {
                Text("Selected: ${state.inputImageUri?.lastPathSegment}")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { pendingOperation = "generate" },
                    enabled = !state.isGenerating && state.prompt.isNotBlank() && state.selectedModelId != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Generate")
                }

                Button(
                    onClick = { pendingOperation = "edit" },
                    enabled = !state.isGenerating && state.prompt.isNotBlank() && state.selectedModelId != null && state.inputImageUri != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit")
                }
            }
        }

        if (state.isGenerating) {
            item {
                CircularProgressIndicator()
            }
        }

        if (state.error != null) {
            item {
                Text(text = "Error: ${state.error}", color = MaterialTheme.colorScheme.error)
            }
        }

        val resultUri = state.resultImageUri
        if (resultUri != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Result:", style = MaterialTheme.typography.titleMedium)

                val decodedBitmap by produceState<android.graphics.Bitmap?>(null, resultUri) {
                    value = withContext(Dispatchers.IO) {
                        runCatching { resultUri.path?.let(BitmapFactory::decodeFile) }.getOrNull()
                    }
                }

                val bitmap = decodedBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Result Image",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Failed to decode image bitmap.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
