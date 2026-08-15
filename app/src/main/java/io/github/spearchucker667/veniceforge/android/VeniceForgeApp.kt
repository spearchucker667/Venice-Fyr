package io.github.spearchucker667.veniceforge.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.spearchucker667.veniceforge.android.chat.ChatScreen
import io.github.spearchucker667.veniceforge.android.chat.ChatViewModel
import io.github.spearchucker667.veniceforge.android.feature.AppFeature
import io.github.spearchucker667.veniceforge.android.feature.FeatureCatalog
import io.github.spearchucker667.veniceforge.android.feature.FeatureGroup
import io.github.spearchucker667.veniceforge.android.ui.ConfigScreen
import io.github.spearchucker667.veniceforge.core.data.DataServices
import io.github.spearchucker667.veniceforge.core.security.SecureSecretStore
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.capabilities.CapabilitiesRepository
import io.github.spearchucker667.veniceforge.sdk.capabilities.ModelCapabilities
import io.github.spearchucker667.veniceforge.sdk.chat.ChatClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeniceForgeApp() {
    var selected by remember { mutableStateOf(FeatureCatalog.byId("chat")) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val secureStore = remember { SecureSecretStore(context) }
    val sdk = remember { VeniceForgeSdk() }

    // Chat wiring (service-locator, keyed by profileId so the conversation cannot span profiles).
    val data = remember { DataServices.create(context) }
    val chatRepo = data.chatRepository
    val profileRepo = data.profileRepository
    val chatClient = remember { ChatClient(VeniceForgeSdk()) }
    var profileId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { profileId = profileRepo.ensureDefault() }

    val chatViewModel = remember(profileId) {
        profileId?.let { pid ->
            ChatViewModel(
                chatRepo = chatRepo,
                chatClient = chatClient,
                apiKeyProvider = { secureStore.loadApiKey(pid) },
                profileId = pid,
                initialModelId = "llama-3.3-70b",
            )
        }
    }
    val modelCaps by produceState(initialValue = emptyList<ModelCapabilities>(), profileId) {
        val pid = profileId
        val key = pid?.let(secureStore::loadApiKey)
        if (pid != null && key != null) {
            value = CapabilitiesRepository(sdk).fetchLiveCapabilities(key).models
        }
    }
    val modelIds = remember(modelCaps) { modelCaps.map { it.id } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Venice Forge Android",
                    modifier = Modifier.padding(20.dp),
                    fontWeight = FontWeight.Bold,
                )
                FeatureGroup.entries.forEach { group ->
                    Text(group.label, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    FeatureCatalog.all.filter { it.group == group }.forEach { feature ->
                        NavigationDrawerItem(
                            label = { Text(feature.label) },
                            selected = selected.id == feature.id,
                            onClick = {
                                selected = feature
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selected.label) },
                    navigationIcon = {
                        OutlinedButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.padding(start = 8.dp),
                        ) { Text("Menu") }
                    },
                )
            },
        ) { padding ->
            if (selected.id == "settings") {
                ConfigScreen(
                    secureStore = secureStore,
                    sdk = sdk,
                    modifier = Modifier.padding(padding),
                )
            } else if (selected.id == "chat") {
                val vm = chatViewModel
                if (vm != null) {
                    ChatScreen(
                        viewModel = vm,
                        availableModels = modelIds,
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    Column(modifier = Modifier.padding(padding).padding(20.dp)) {
                        Text(stringResource(R.string.chat_no_api_key))
                    }
                }
            } else {
                FeatureScreen(selected, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun FeatureScreen(feature: AppFeature, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(feature.label, fontWeight = FontWeight.Bold)
            Text("Android status: ${feature.status}")
        }
        item {
            Text("Desktop parity target", fontWeight = FontWeight.SemiBold)
            Text(feature.desktopPurpose)
        }
        item {
            Text("Port contract", fontWeight = FontWeight.SemiBold)
            Text(feature.androidPortNotes)
        }
        item {
            Text(
                "This starter intentionally exposes the complete desktop navigation surface now. " +
                    "Each screen is replaced incrementally without changing its stable feature id.",
            )
        }
    }
}
