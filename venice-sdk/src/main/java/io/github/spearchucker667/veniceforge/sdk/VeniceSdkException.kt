package io.github.spearchucker667.veniceforge.sdk

sealed class VeniceSdkException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Http(val statusCode: Int, val requestId: String?) : VeniceSdkException("Venice API HTTP $statusCode")
    class Protocol(message: String, cause: Throwable? = null) : VeniceSdkException(message, cause)
    class Network(cause: Throwable) : VeniceSdkException("Venice API network failure", cause)
}
