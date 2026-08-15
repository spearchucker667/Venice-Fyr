package io.github.spearchucker667.veniceforge.core.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactorTest {
    @Test fun removesSecretsAndPaths() {
        val source = "Bearer vn-secret123456 /data/user/0/app/files/x"
        val redacted = Redactor.redact(source)
        assertFalse(redacted.contains("vn-secret123456"))
        assertFalse(redacted.contains("/data/user/0"))
        assertTrue(redacted.contains("REDACTED"))
    }
}
