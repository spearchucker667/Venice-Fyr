package io.github.spearchucker667.veniceforge.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.spearchucker667.veniceforge.core.security.SecureSecretStore
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceModel
import kotlinx.coroutines.launch

@Composable
fun ConfigScreen(
    secureStore: SecureSecretStore,
    sdk: VeniceForgeSdk,
    modifier: Modifier = Modifier,
) {
    val profileId = "default"
    val scope = rememberCoroutineScope()
    var apiKey by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("No API key loaded") }
    var loading by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<VeniceModel>>(emptyList()) }

    LaunchedEffect(Unit) {
        val existing = secureStore.loadApiKey(profileId)
        if (!existing.isNullOrBlank()) {
            apiKey = existing
            status = "API key loaded from Android Keystore-backed storage"
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Venice API", fontWeight = FontWeight.Bold)
            Text("Starter functionality: secure API-key persistence plus live /models capability discovery.")
        }
        item {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Venice API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    enabled = apiKey.isNotBlank(),
                    onClick = {
                        secureStore.saveApiKey(profileId, apiKey)
                        status = "Saved to Keystore-backed storage"
                    },
                ) { Text("Save") }
                OutlinedButton(
                    onClick = {
                        secureStore.deleteApiKey(profileId)
                        apiKey = ""
                        models = emptyList()
                        status = "API key removed"
                    },
                ) { Text("Remove") }
                Button(
                    enabled = apiKey.isNotBlank() && !loading,
                    onClick = {
                        loading = true
                        status = "Loading /models…"
                        scope.launch {
                            runCatching { sdk.listModels(apiKey) }
                                .onSuccess {
                                    models = it
                                    status = "Loaded ${it.size} models"
                                }
                                .onFailure {
                                    status = "Model probe failed: ${it.message ?: it::class.simpleName}"
                                }
                            loading = false
                        }
                    },
                ) { Text("Load models") }
            }
        }
        item {
            if (loading) CircularProgressIndicator()
            Text(status)
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
