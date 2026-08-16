package io.github.spearchucker667.veniceforge.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.spearchucker667.veniceforge.android.R
import io.github.spearchucker667.veniceforge.core.designsystem.CodexPetState
import io.github.spearchucker667.veniceforge.core.designsystem.VeniceLoadingIndicator
import io.github.spearchucker667.veniceforge.core.designsystem.VenicePetStatusIndicator
import io.github.spearchucker667.veniceforge.core.security.SecureSecretStore
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceModel
import kotlinx.coroutines.launch

@Composable
fun ConfigScreen(
    secureStore: SecureSecretStore,
    sdk: VeniceForgeSdk,
    profileId: String,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var apiKeyInput by remember { mutableStateOf("") }
    var hasStoredKey by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("No API key loaded") }
    var loading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<VeniceModel>>(emptyList()) }
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        val existing = secureStore.loadApiKey(profileId)
        if (!existing.isNullOrBlank()) {
            hasStoredKey = true
            status = "API key loaded from Android Keystore-backed storage"
            hasError = false
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            ) {
                Image(
                    painter = painterResource(
                        if (isDark) R.drawable.ic_venice_keys_off_white
                        else R.drawable.ic_venice_keys_deep_blue,
                    ),
                    contentDescription = "Official Venice crossed keys",
                    modifier = Modifier.size(36.dp),
                )
                Column {
                    Text("Venice API", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Official Venice.ai API integration & capability discovery",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text("Starter functionality: secure API-key persistence plus live /models capability discovery.")
            Text(
                "Authentication uses a Venice API key. Normal Venice.ai account login is not supported in this app.",
                fontWeight = FontWeight.SemiBold,
            )
        }
        item {
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Venice API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    enabled = apiKeyInput.isNotBlank(),
                    onClick = {
                        secureStore.saveApiKey(profileId, apiKeyInput)
                        apiKeyInput = ""
                        hasStoredKey = true
                        status = "Saved to Keystore-backed storage"
                        hasError = false
                    },
                ) { Text("Save") }
                OutlinedButton(
                    onClick = {
                        secureStore.deleteApiKey(profileId)
                        apiKeyInput = ""
                        hasStoredKey = false
                        models = emptyList()
                        status = "API key removed"
                        hasError = false
                    },
                ) { Text("Remove") }
                Button(
                    enabled = (apiKeyInput.isNotBlank() || hasStoredKey) && !loading,
                    onClick = {
                        loading = true
                        hasError = false
                        status = "Loading /models…"
                        scope.launch {
                            val apiKey = apiKeyInput.takeIf(String::isNotBlank)
                                ?: secureStore.loadApiKey(profileId)
                            runCatching { requireNotNull(apiKey) { "No API key is stored" }; sdk.listModels(apiKey) }
                                .onSuccess {
                                    models = it
                                    status = "Loaded ${it.size} models"
                                    hasError = false
                                }
                                .onFailure {
                                    status = "Model probe failed: ${it.message ?: it::class.simpleName}"
                                    hasError = true
                                }
                            loading = false
                        }
                    },
                ) { Text("Load models") }
            }
        }
        item {
            if (loading) {
                VeniceLoadingIndicator(
                    message = status,
                    state = CodexPetState.ActiveTask,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
            } else {
                val state = if (hasError) CodexPetState.Failed else CodexPetState.Idle
                VenicePetStatusIndicator(
                    state = state,
                    message = status,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
        }
        if (models.isNotEmpty()) {
            item { Text("Model catalog", fontWeight = FontWeight.SemiBold) }
            items(models.take(100), key = { it.id }) { model ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(model.name ?: model.id, fontWeight = FontWeight.Medium)
                    Text(model.id)
                    val description = model.description
                    if (!description.isNullOrBlank()) Text(description)
                }
            }
        }
    }
}
