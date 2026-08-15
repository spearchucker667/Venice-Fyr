package io.github.spearchucker667.veniceforge.sdk

/**
 * Valid model category types defined by the official Venice API specification (GET /models?type=...).
 */
enum class ModelType(val wireName: String) {
    ALL("all"),
    TEXT("text"),
    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    TTS("tts"),
    ASR("asr"),
    EMBEDDING("embedding"),
    MUSIC("music"),
    UPSCALE("upscale"),
    INPAINT("inpaint"),
    CODE("code");

    companion object {
        fun fromWireName(wireName: String?): ModelType? {
            if (wireName == null) return null
            return entries.firstOrNull { it.wireName.equals(wireName, ignoreCase = true) }
        }
    }
}
