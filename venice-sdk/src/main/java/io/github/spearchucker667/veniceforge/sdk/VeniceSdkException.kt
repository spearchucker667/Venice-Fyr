package io.github.spearchucker667.veniceforge.sdk

/**
 * Structured exception hierarchy for Venice SDK operations.
 *
 * Designed to provide rich, safe diagnostic information (HTTP status codes, Venice error codes,
 * rate limit window metadata, request IDs) without leaking sensitive credentials or prompt contents.
 */
sealed class VeniceSdkException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * HTTP 429 rate limit exceeded, including rate-limit header metadata and retry timestamps.
     */
    data class RateLimit(
        val statusCode: Int = 429,
        val errorCode: String? = "RATE_LIMIT_EXCEEDED",
        val safeMessage: String = "Rate limit exceeded",
        val requestId: String? = null,
        val retryAfterSeconds: Long? = null,
        val rateLimitInfo: RateLimitInfo? = null,
    ) : VeniceSdkException("Venice API rate limit exceeded ($statusCode): $safeMessage [request-id: $requestId]")

    /**
     * HTTP 401 / 403 authentication or authorization failure.
     */
    data class Authentication(
        val statusCode: Int = 401,
        val errorCode: String? = null,
        val safeMessage: String = "Authentication failed",
        val requestId: String? = null,
    ) : VeniceSdkException("Venice API authentication failure ($statusCode): $safeMessage [request-id: $requestId]")

    /**
     * HTTP 400 / 422 request validation error with optional field-level details.
     */
    data class Validation(
        val statusCode: Int = 400,
        val errorCode: String? = null,
        val safeMessage: String = "Invalid request",
        val requestId: String? = null,
        val validationDetails: String? = null,
    ) : VeniceSdkException("Venice API validation error ($statusCode): $safeMessage [request-id: $requestId]")

    /**
     * HTTP 5xx server-side error.
     */
    data class Server(
        val statusCode: Int = 500,
        val errorCode: String? = null,
        val safeMessage: String = "Server error",
        val requestId: String? = null,
    ) : VeniceSdkException("Venice API server error ($statusCode): $safeMessage [request-id: $requestId]")

    /**
     * General HTTP status error when a more specific subclass does not apply.
     */
    open class Http(
        val statusCode: Int,
        val requestId: String? = null,
        val safeMessage: String? = null,
        val errorCode: String? = null,
    ) : VeniceSdkException(
        "Venice API HTTP $statusCode" +
            (if (!safeMessage.isNullOrBlank()) ": $safeMessage" else "") +
            (if (!requestId.isNullOrBlank()) " [request-id: $requestId]" else "")
    )

    /**
     * Network transport failure (e.g. DNS failure, connection reset, socket timeout).
     */
    data class Network(
        override val cause: Throwable,
        val isTimeout: Boolean = false,
    ) : VeniceSdkException(
        if (isTimeout) "Venice API request timed out" else "Venice API network failure",
        cause
    )

    /**
     * Protocol violation (e.g. malformed JSON, invalid SSE framing, unexpected response shape).
     */
    data class Protocol(
        override val message: String,
        override val cause: Throwable? = null,
    ) : VeniceSdkException(message, cause)

    /**
     * Cooperative client-side cancellation.
     */
    data class Cancelled(
        override val message: String = "Venice API request was cancelled",
    ) : VeniceSdkException(message)
}

/**
 * Parsed rate-limit metadata from Venice response headers.
 */
data class RateLimitInfo(
    val limitRequests: Long? = null,
    val remainingRequests: Long? = null,
    val resetRequestsTimestamp: Long? = null,
    val limitTokens: Long? = null,
    val remainingTokens: Long? = null,
    val resetTokensSeconds: Long? = null,
)
