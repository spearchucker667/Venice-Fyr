package io.github.spearchucker667.veniceforge.sdk.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class QueueVideoRequest(
    val model: String,
    val prompt: String,
    val duration: String,
    @SerialName("negative_prompt") val negativePrompt: String? = null,
    @SerialName("aspect_ratio") val aspectRatio: String? = null,
    val resolution: String? = null,
    val consents: JsonElement? = null,
    @SerialName("upscale_factor") val upscaleFactor: Int? = null,
    val audio: Boolean? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("end_image_url") val endImageUrl: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("reference_image_urls") val referenceImageUrls: List<String>? = null,
    @SerialName("reference_video_urls") val referenceVideoUrls: List<String>? = null,
    @SerialName("reference_audio_urls") val referenceAudioUrls: List<String>? = null,
    val elements: List<JsonElement>? = null,
    @SerialName("scene_image_urls") val sceneImageUrls: List<String>? = null,
    val keyframes: List<JsonElement>? = null,
)

@Serializable
data class VideoQueueResponse(
    val model: String,
    @SerialName("queue_id") val queueId: String,
    @SerialName("download_url") val downloadUrl: String? = null
)

@Serializable
data class RetrieveVideoRequest(
    val model: String,
    @SerialName("queue_id") val queueId: String,
    @SerialName("delete_media_on_completion") val deleteMediaOnCompletion: Boolean? = null
)

@Serializable
data class RetrieveVideoResponseStatus(
    val status: String,
    @SerialName("average_execution_time") val averageExecutionTime: Double,
    @SerialName("execution_duration") val executionDuration: Double
)

@Serializable
data class CompleteVideoRequest(
    val model: String,
    @SerialName("queue_id") val queueId: String
)

@Serializable
data class VideoCompleteResponse(val success: Boolean)

@Serializable
data class QuoteVideoRequest(
    val model: String,
    val duration: String,
    @SerialName("aspect_ratio") val aspectRatio: String? = null,
    val resolution: String? = null,
    @SerialName("upscale_factor") val upscaleFactor: Int? = null,
    val audio: Boolean? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("reference_video_total_duration") val referenceVideoTotalDuration: Double? = null,
)

@Serializable
data class VideoQuoteResponse(val quote: Double)

@Serializable
data class VideoTranscriptionRequest(
    val url: String,
    @SerialName("response_format") val responseFormat: String = "json",
)

sealed class VideoTranscriptionResult {
    data class Json(val transcript: String, val language: String? = null) : VideoTranscriptionResult()
    data class Text(val transcript: String) : VideoTranscriptionResult()
}

@Serializable
internal data class VideoTranscriptionJsonResponse(
    val transcript: String,
    @SerialName("lang") val language: String? = null,
)

sealed class VideoRetrieveResult {
    data class Processing(
        val status: String,
        val averageExecutionTime: Double,
        val executionDuration: Double
    ) : VideoRetrieveResult()
    data class CompletedBinary(
        val binaryVideo: ByteArray,
        val mimeType: String,
        val requestId: String? = null,
    ) : VideoRetrieveResult()
    data class CompletedRemote(
        val status: String,
        val averageExecutionTime: Double,
        val executionDuration: Double,
    ) : VideoRetrieveResult()
    data class UnknownStatus(
        val status: String,
        val averageExecutionTime: Double,
        val executionDuration: Double,
    ) : VideoRetrieveResult()
}
