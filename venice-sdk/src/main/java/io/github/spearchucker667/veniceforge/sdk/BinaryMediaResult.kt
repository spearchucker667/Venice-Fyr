package io.github.spearchucker667.veniceforge.sdk

/** Binary media plus response metadata needed for persistence and diagnostics. */
data class BinaryMediaResult(
    val bytes: ByteArray,
    val mimeType: String,
    val requestId: String? = null,
    val balanceRemaining: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is BinaryMediaResult &&
            bytes.contentEquals(other.bytes) &&
            mimeType == other.mimeType &&
            requestId == other.requestId &&
            balanceRemaining == other.balanceRemaining

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (requestId?.hashCode() ?: 0)
        result = 31 * result + (balanceRemaining?.hashCode() ?: 0)
        return result
    }
}
