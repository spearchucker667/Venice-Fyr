package io.github.spearchucker667.veniceforge.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Profile-scoped API-key persistence backed by Android Keystore AES-GCM keys.
 * Ciphertext only is stored in SharedPreferences. Plaintext secrets are never logged.
 *
 * This is intentionally credential-protected app storage. Do not move credentials into
 * device-protected/direct-boot storage.
 */
class SecureSecretStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveApiKey(profileId: String, apiKey: String) {
        require(profileId.isNotBlank()) { "profileId must not be blank" }
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }

        val key = getOrCreateKey(profileId)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext = cipher.doFinal(apiKey.toByteArray(StandardCharsets.UTF_8))
        val payload = cipher.iv + ciphertext
        prefs.edit().putString(prefKey(profileId), Base64.encodeToString(payload, Base64.NO_WRAP)).apply()
    }

    fun loadApiKey(profileId: String): String? {
        val encoded = prefs.getString(prefKey(profileId), null) ?: return null
        return runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            require(payload.size > GCM_IV_BYTES) { "Encrypted payload is truncated" }
            val iv = payload.copyOfRange(0, GCM_IV_BYTES)
            val ciphertext = payload.copyOfRange(GCM_IV_BYTES, payload.size)
            val keyStore = openKeyStore()
            val key = keyStore.getKey(alias(profileId), null) as? SecretKey ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
        }.getOrElse {
            // Treat undecryptable/corrupt ciphertext as unavailable and remove it.
            prefs.edit().remove(prefKey(profileId)).apply()
            null
        }
    }

    fun deleteApiKey(profileId: String) {
        prefs.edit().remove(prefKey(profileId)).apply()
        runCatching {
            val keyStore = openKeyStore()
            if (keyStore.containsAlias(alias(profileId))) keyStore.deleteEntry(alias(profileId))
        }
    }

    private fun getOrCreateKey(profileId: String): SecretKey {
        val keyStore = openKeyStore()
        (keyStore.getKey(alias(profileId), null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias(profileId),
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun openKeyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun alias(profileId: String): String = "venice_forge.profile.${digest(profileId)}"
    private fun prefKey(profileId: String): String = "api_key.${digest(profileId)}"

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
        .take(32)

    private companion object {
        const val PREFS_NAME = "venice_forge_secure_secrets"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
