package io.github.spearchucker667.veniceforge.sdk.audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpeechRequest(
    val input: String,
    val model: String? = null,
    val voice: String? = null,
    @SerialName("response_format") val responseFormat: String? = null,
    val speed: Float? = null,
    val language: String? = null,
    val prompt: String? = null,
    val streaming: Boolean? = null,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
)

@Serializable
data class QueueAudioRequest(
    val model: String,
    val prompt: String,
    @SerialName("lyrics_prompt") val lyricsPrompt: String? = null,
    /** Positive numeric string; the wire schema also accepts an integer. */
    @SerialName("duration_seconds") val durationSeconds: String? = null,
    @SerialName("force_instrumental") val forceInstrumental: Boolean? = null,
    @SerialName("lyrics_optimizer") val lyricsOptimizer: Boolean? = null,
    val loop: Boolean? = null,
    val voice: String? = null,
    @SerialName("language_code") val languageCode: String? = null,
    val speed: Double? = null,
)

@Serializable
data class AudioQueueResponse(
    val model: String,
    @SerialName("queue_id") val queueId: String,
    val status: String,
)

@Serializable
data class QuoteAudioRequest(
    val model: String,
    @SerialName("duration_seconds") val durationSeconds: String? = null,
    @SerialName("character_count") val characterCount: Int? = null,
)

@Serializable
data class AudioQuoteResponse(val quote: Double)

@Serializable
data class RetrieveAudioRequest(
    val model: String,
    @SerialName("queue_id") val queueId: String,
    @SerialName("delete_media_on_completion") val deleteMediaOnCompletion: Boolean? = null,
)

@Serializable
internal data class RetrieveAudioResponseStatus(
    val status: String,
    @SerialName("average_execution_time") val averageExecutionTime: Double,
    @SerialName("execution_duration") val executionDuration: Double,
)

sealed class AudioRetrieveResult {
    data class Processing(
        val status: String,
        val averageExecutionTime: Double,
        val executionDuration: Double,
    ) : AudioRetrieveResult()

    data class CompletedBinary(
        val audio: ByteArray,
        val mimeType: String,
        val requestId: String? = null,
    ) : AudioRetrieveResult()

    data class UnknownStatus(
        val status: String,
        val averageExecutionTime: Double,
        val executionDuration: Double,
    ) : AudioRetrieveResult()
}

@Serializable
data class CompleteAudioRequest(
    val model: String,
    @SerialName("queue_id") val queueId: String,
)

@Serializable
data class AudioCompleteResponse(val success: Boolean)

data class AudioFileUpload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

data class AudioTranscriptionRequest(
    val file: AudioFileUpload,
    val model: String? = null,
    val responseFormat: String = "json",
    val timestamps: Boolean = false,
    val language: String? = null,
)

sealed class AudioTranscriptionResult {
    data class Json(
        val text: String,
        val duration: Double? = null,
        val timestamps: kotlinx.serialization.json.JsonElement? = null,
    ) : AudioTranscriptionResult()

    data class Text(val text: String) : AudioTranscriptionResult()
}

@Serializable
internal data class AudioTranscriptionJsonResponse(
    val text: String,
    val duration: Double? = null,
    val timestamps: kotlinx.serialization.json.JsonElement? = null,
)

data class CloneVoiceRequest(
    val file: AudioFileUpload,
    val model: String? = null,
)

@Serializable
data class ClonedVoiceResponse(
    val id: String,
    val model: String? = null,
)
