package io.github.spearchucker667.veniceforge.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import io.github.spearchucker667.veniceforge.android.chat.ChatScreen
import io.github.spearchucker667.veniceforge.android.chat.ChatViewModel
import io.github.spearchucker667.veniceforge.android.feature.AppFeature
import io.github.spearchucker667.veniceforge.android.feature.FeatureCatalog
import io.github.spearchucker667.veniceforge.android.feature.FeatureGroup
import io.github.spearchucker667.veniceforge.android.ui.ConfigScreen
import io.github.spearchucker667.veniceforge.android.image.ImageScreen
import io.github.spearchucker667.veniceforge.android.image.ImageViewModel
import io.github.spearchucker667.veniceforge.core.data.DataServices
import io.github.spearchucker667.veniceforge.core.security.SecureSecretStore
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.capabilities.CapabilitiesRepository
import io.github.spearchucker667.veniceforge.sdk.capabilities.ModelCatalog
import io.github.spearchucker667.veniceforge.sdk.chat.ChatClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeniceForgeApp() {
    var selectedId by rememberSaveable { mutableStateOf("chat") }
    val selected = remember(selectedId) { FeatureCatalog.byId(selectedId) ?: FeatureCatalog.byId("chat") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val secureStore = remember { SecureSecretStore(context) }
    val sdk = remember { VeniceForgeSdk() }

    // Shared networking & chat wiring
    val data = remember { DataServices.create(context) }
    val chatRepo = data.chatRepository
    val profileRepo = data.profileRepository
    val chatClient = remember(sdk) { ChatClient(sdk) }
    val capabilitiesRepo = remember(sdk) { CapabilitiesRepository(sdk) }

    var profileId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { profileId = profileRepo.ensureDefault() }

    val chatViewModel = remember(profileId) {
        profileId?.let { pid ->
            ChatViewModel(
                chatRepo = chatRepo,
                chatClient = chatClient,
                apiKeyProvider = { secureStore.loadApiKey(pid) },
                profileId = pid,
                initialModelId = null,
            )
        }
    }

    val imageViewModel = remember(profileId) {
        profileId?.let { pid ->
            ImageViewModel(
                imageClient = sdk.imageClient(),
                apiKeyProvider = { secureStore.loadApiKey(pid) },
                uriToBase64 = { uri ->
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bytes = stream.readBytes()
                            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        }
                    }
                },
                saveBytesToCache = { bytes ->
                    withContext(Dispatchers.IO) {
                        val file = java.io.File(context.cacheDir, "venice_image_${System.currentTimeMillis()}.png")
                        file.writeBytes(bytes)
                        android.net.Uri.fromFile(file)
                    }
                }
            )
        }
    }

    var modelCatalog by remember { mutableStateOf<ModelCatalog?>(null) }
    LaunchedEffect(profileId) {
        val pid = profileId
        val key = pid?.let(secureStore::loadApiKey)
        if (pid != null && !key.isNullOrBlank()) {
            runCatching { capabilitiesRepo.fetchLiveCapabilities(key) }
                .onSuccess { catalog ->
                    modelCatalog = catalog
                    catalog.defaultTextModelId?.let { defaultModel ->
                        chatViewModel?.setDefaultModelIfUnset(defaultModel)
                    }
                }
        }
    }

    val modelIds = remember(modelCatalog) {
        modelCatalog?.models?.filter { it.supportsTextChat }?.map { it.id } ?: emptyList()
    }

    val imageModelIds = remember(modelCatalog) {
        modelCatalog?.models?.filter { it.supportsImageGeneration }?.map { it.id } ?: emptyList()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                val isDark = isSystemInDarkTheme()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Image(
                        painter = painterResource(
                            if (isDark) R.drawable.ic_venice_keys_off_white
                            else R.drawable.ic_venice_keys_deep_blue,
                        ),
                        contentDescription = "Official Venice crossed keys",
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Venice Forge Android",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                HorizontalDivider()
                FeatureGroup.entries.forEach { group ->
                    Text(group.label, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    FeatureCatalog.all.filter { it.group == group }.forEach { feature ->
                        NavigationDrawerItem(
                            label = { Text(feature.label) },
                            selected = selected.id == feature.id,
                            onClick = {
                                selectedId = feature.id
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
            } else if (selected.id == "image") {
                val vm = imageViewModel
                if (vm != null) {
                    ImageScreen(
                        viewModel = vm,
                        availableModels = imageModelIds,
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
