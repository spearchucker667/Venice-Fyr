package io.github.spearchucker667.veniceforge.sdk.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QueueVideoRequest(
    val model: String,
    val prompt: String,
    @SerialName("negative_prompt") val negativePrompt: String? = null,
    val seed: Int? = null,
    @SerialName("aspect_ratio") val aspectRatio: String? = null,
    val duration: String? = null,
    val fps: Int? = null,
    val resolution: String? = null,
    @SerialName("hide_watermark") val hideWatermark: Boolean? = null,
    @SerialName("safe_mode") val safeMode: Boolean? = null,
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

sealed class VideoRetrieveResult {
    data class Processing(
        val status: String,
        val averageExecutionTime: Double,
        val executionDuration: Double
    ) : VideoRetrieveResult()
    data class Completed(val binaryVideo: ByteArray) : VideoRetrieveResult()
}
