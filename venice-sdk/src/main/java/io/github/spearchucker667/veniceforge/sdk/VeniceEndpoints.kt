package io.github.spearchucker667.veniceforge.sdk

/**
 * Complete endpoint-path inventory from Venice Forge's tracked 2026-08-14
 * OpenAPI snapshot (schema 20260814.153445).
 *
 * Runtime model metadata remains authoritative for active models, constraints,
 * capabilities, and pricing. Presence here does not imply app-UI exposure.
 */
object VeniceEndpoints {
    const val CHAT_COMPLETIONS = "chat/completions"
    const val RESPONSES = "responses"

    const val IMAGE_GENERATE = "image/generate"
    const val IMAGE_GENERATIONS_COMPAT = "images/generations"
    const val IMAGE_STYLES = "image/styles"
    const val IMAGE_UPSCALE = "image/upscale"
    const val IMAGE_EDIT = "image/edit"
    const val IMAGE_MULTI_EDIT = "image/multi-edit"
    const val IMAGE_BACKGROUND_REMOVE = "image/background-remove"

    const val MODELS = "models"
    const val MODEL_TRAITS = "models/traits"
    const val MODEL_COMPATIBILITY = "models/compatibility_mapping"

    const val API_KEYS = "api_keys"
    const val API_KEY_RATE_LIMITS = "api_keys/rate_limits"
    const val API_KEY_RATE_LIMIT_LOG = "api_keys/rate_limits/log"
    const val API_KEY_GENERATE_WEB3_KEY = "api_keys/generate_web3_key"
    fun apiKey(id: String) = "api_keys/$id"

    const val CHARACTERS = "characters"
    fun character(slug: String) = "characters/$slug"
    fun characterReviews(slug: String) = "characters/$slug/reviews"

    const val EMBEDDINGS = "embeddings"

    const val AUDIO_SPEECH = "audio/speech"
    const val AUDIO_TRANSCRIPTIONS = "audio/transcriptions"
    const val AUDIO_VOICES = "audio/voices"
    const val AUDIO_COMPLETE = "audio/complete"
    const val AUDIO_QUEUE = "audio/queue"
    const val AUDIO_QUOTE = "audio/quote"
    const val AUDIO_RETRIEVE = "audio/retrieve"

    const val VIDEO_COMPLETE = "video/complete"
    const val VIDEO_QUEUE = "video/queue"
    const val VIDEO_QUOTE = "video/quote"
    const val VIDEO_RETRIEVE = "video/retrieve"
    const val VIDEO_TRANSCRIPTIONS = "video/transcriptions"

    const val AUGMENT_TEXT_PARSER = "augment/text-parser"
    const val AUGMENT_SCRAPE = "augment/scrape"
    const val AUGMENT_SEARCH = "augment/search"

    const val BILLING_BALANCE = "billing/balance"
    const val BILLING_USAGE = "billing/usage"
    const val BILLING_USAGE_ANALYTICS = "billing/usage-analytics"
    const val BILLING_USAGE_HISTORY = "billing/usage-history"

    const val CRYPTO_RPC_NETWORKS = "crypto/rpc/networks"
    fun cryptoRpc(network: String) = "crypto/rpc/$network"

    fun x402Balance(walletAddress: String) = "x402/balance/$walletAddress"
    const val X402_TOP_UP = "x402/top-up"
    fun x402Transactions(walletAddress: String) = "x402/transactions/$walletAddress"
}
