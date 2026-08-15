package io.github.spearchucker667.veniceforge.sdk.audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpeechRequest(
    val model: String,
    val input: String,
    val voice: String? = null,
    @SerialName("response_format") val responseFormat: String? = null,
    val speed: Float? = null,
    @SerialName("safe_mode") val safeMode: Boolean? = null
)
