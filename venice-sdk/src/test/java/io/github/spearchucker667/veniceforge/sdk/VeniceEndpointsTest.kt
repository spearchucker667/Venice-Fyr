package io.github.spearchucker667.veniceforge.sdk

import org.junit.Assert.assertEquals
import org.junit.Test

class VeniceEndpointsTest {
    @Test fun canonicalMediaPathsRemainStable() {
        assertEquals("image/edit", VeniceEndpoints.IMAGE_EDIT)
        assertEquals("video/queue", VeniceEndpoints.VIDEO_QUEUE)
        assertEquals("audio/queue", VeniceEndpoints.AUDIO_QUEUE)
    }

    @Test fun parameterizedPathsDoNotAddLeadingSlash() {
        assertEquals("characters/demo/reviews", VeniceEndpoints.characterReviews("demo"))
        assertEquals("crypto/rpc/ethereum", VeniceEndpoints.cryptoRpc("ethereum"))
    }
}
