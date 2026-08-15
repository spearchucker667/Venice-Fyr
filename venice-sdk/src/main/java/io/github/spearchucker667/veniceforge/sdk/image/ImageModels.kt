package io.github.spearchucker667.veniceforge.sdk.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StyleReference(
    val image: String,
    val strength: Float? = null
)

@Serializable
data class GenerateImageRequest(
    val model: String,
    val prompt: String,
    @SerialName("negative_prompt") val negativePrompt: String? = null,
    @SerialName("style_preset") val stylePreset: String? = null,
    val height: Int? = null,
    val width: Int? = null,
    val steps: Int? = null,
    @SerialName("cfg_scale") val cfgScale: Float? = null,
    val seed: Int? = null,
    @SerialName("safe_mode") val safeMode: Boolean? = null,
    @SerialName("return_binary") val returnBinary: Boolean? = false,
    @SerialName("hide_watermark") val hideWatermark: Boolean? = null,
    val format: String? = null,
    val variants: Int? = null,
    @SerialName("aspect_ratio") val aspectRatio: String? = null,
    val resolution: String? = null,
    val quality: String? = null,
    @SerialName("enable_web_search") val enableWebSearch: Boolean? = null,
    @SerialName("disable_prompt_optimization_thinking") val disablePromptOptimizationThinking: Boolean? = null,
    @SerialName("enhance_prompt") val enhancePrompt: Boolean? = null,
    @SerialName("style_references") val styleReferences: List<StyleReference>? = null
)

@Serializable
data class UpscaleImageRequest(
    val image: String,
    val scale: Int? = null,
    val creativity: Float? = null
)

@Serializable
data class EditImageRequest(
    val image: String,
    val prompt: String,
    val model: String? = null,
    @SerialName("aspect_ratio") val aspectRatio: String? = null,
    val resolution: String? = null,
    @SerialName("output_format") val outputFormat: String? = null,
    @SerialName("disable_prompt_optimization_thinking") val disablePromptOptimizationThinking: Boolean? = null,
    @SerialName("enhance_prompt") val enhancePrompt: Boolean? = null,
    @SerialName("safe_mode") val safeMode: Boolean? = null
)

@Serializable
data class MultiEditImageRequest(
    val images: List<String>,
    val prompt: String,
    val modelId: String? = null,
    @SerialName("aspect_ratio") val aspectRatio: String? = null,
    val resolution: String? = null,
    val quality: String? = null,
    @SerialName("output_format") val outputFormat: String? = null,
    @SerialName("disable_prompt_optimization_thinking") val disablePromptOptimizationThinking: Boolean? = null,
    @SerialName("enhance_prompt") val enhancePrompt: Boolean? = null,
    @SerialName("safe_mode") val safeMode: Boolean? = null
)

@Serializable
data class GenerateImageTiming(
    val inferenceDuration: Double,
    val inferencePreprocessingTime: Double,
    val inferenceQueueTime: Double,
    val total: Double
)

@Serializable
data class GenerateImageResponse(
    val id: String? = null,
    val images: List<String>? = null,
    val timing: GenerateImageTiming? = null
)
