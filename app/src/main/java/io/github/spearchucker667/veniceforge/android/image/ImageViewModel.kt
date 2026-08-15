package io.github.spearchucker667.veniceforge.android.image

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.spearchucker667.veniceforge.sdk.image.EditImageRequest
import io.github.spearchucker667.veniceforge.sdk.image.GenerateImageRequest
import io.github.spearchucker667.veniceforge.sdk.image.ImageClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImageUiState(
    val prompt: String = "",
    val selectedModelId: String? = null,
    val inputImageUri: Uri? = null,
    val resultImageUri: Uri? = null,
    val isGenerating: Boolean = false,
    val error: String? = null,
)

class ImageViewModel(
    private val imageClient: ImageClient,
    private val apiKeyProvider: () -> String?,
    private val uriToBase64: suspend (Uri) -> String?,
    private val saveBytesToCache: suspend (ByteArray) -> Uri?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageUiState())
    val uiState: StateFlow<ImageUiState> = _uiState.asStateFlow()

    fun updatePrompt(prompt: String) {
        _uiState.update { it.copy(prompt = prompt) }
    }

    fun selectModel(modelId: String) {
        _uiState.update { it.copy(selectedModelId = modelId) }
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(inputImageUri = uri) }
    }
    
    fun setDefaultModelIfUnset(modelId: String) {
        if (_uiState.value.selectedModelId == null) {
            _uiState.update { it.copy(selectedModelId = modelId) }
        }
    }

    fun generateImage() {
        val state = _uiState.value
        val model = state.selectedModelId ?: return
        val prompt = state.prompt.takeIf { it.isNotBlank() } ?: return
        val apiKey = apiKeyProvider()

        if (apiKey.isNullOrBlank()) {
            _uiState.update { it.copy(error = "No API key found. Please set one in Settings.") }
            return
        }

        _uiState.update { it.copy(isGenerating = true, error = null, resultImageUri = null) }

        viewModelScope.launch {
            try {
                val req = GenerateImageRequest(
                    model = model,
                    prompt = prompt,
                    height = 512,
                    width = 512,
                    returnBinary = true
                )
                val bytes = imageClient.generateBinary(apiKey, req)
                val uri = saveBytesToCache(bytes)
                _uiState.update { it.copy(isGenerating = false, resultImageUri = uri) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun editImage() {
        val state = _uiState.value
        val model = state.selectedModelId ?: return
        val prompt = state.prompt.takeIf { it.isNotBlank() } ?: return
        val uri = state.inputImageUri ?: return
        val apiKey = apiKeyProvider()

        if (apiKey.isNullOrBlank()) {
            _uiState.update { it.copy(error = "No API key found. Please set one in Settings.") }
            return
        }

        _uiState.update { it.copy(isGenerating = true, error = null, resultImageUri = null) }

        viewModelScope.launch {
            try {
                val base64Input = uriToBase64(uri)
                if (base64Input == null) {
                    _uiState.update { it.copy(isGenerating = false, error = "Failed to read input image") }
                    return@launch
                }

                val req = EditImageRequest(
                    image = base64Input,
                    model = model,
                    prompt = prompt
                )
                val response = imageClient.edit(apiKey, req)
                val base64 = response.images?.firstOrNull()
                val uri = if (base64 != null) {
                    val decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                    saveBytesToCache(decodedBytes)
                } else null
                _uiState.update { it.copy(isGenerating = false, resultImageUri = uri) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, error = e.message ?: "Unknown error") }
            }
        }
    }
}
