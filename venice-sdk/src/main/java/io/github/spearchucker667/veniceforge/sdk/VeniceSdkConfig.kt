package io.github.spearchucker667.veniceforge.sdk

data class VeniceSdkConfig(
    val baseUrl: String = "https://api.venice.ai/api/v1/",
    val userAgent: String = "VeniceForgeAndroid/0.1.0",
) {
    init {
        require(baseUrl.startsWith("https://")) { "Venice SDK requires an HTTPS base URL" }
        require(baseUrl.endsWith("/")) { "baseUrl must end with '/'" }
    }
}
