package io.github.spearchucker667.veniceforge.core.data.repo

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import io.github.spearchucker667.veniceforge.core.data.entity.GeneratedMediaEntity
import io.github.spearchucker667.veniceforge.core.data.entity.GeneratedMediaKind
import io.github.spearchucker667.veniceforge.core.data.entity.GeneratedMediaOperation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

/** Owns app-private generated-media files and their profile-scoped Room metadata. */
class GeneratedMediaRepository(
    context: Context,
    private val db: AppDatabase,
) {
    private val root = File(context.applicationContext.filesDir, DIRECTORY_NAME)
    private val dao = db.generatedMediaDao()

    suspend fun persistImage(
        profileId: String,
        bytes: ByteArray,
        operation: GeneratedMediaOperation,
        modelId: String,
        prompt: String,
        declaredMimeType: String? = null,
    ): GeneratedMediaEntity {
        require(profileId.isNotBlank()) { "profileId must not be blank" }
        require(modelId.isNotBlank()) { "modelId must not be blank" }
        require(bytes.isNotEmpty()) { "generated image is empty" }
        require(bytes.size <= MAX_IMAGE_BYTES) { "generated image exceeds the $MAX_IMAGE_BYTES byte limit" }
        require(db.profileDao().findById(profileId) != null) { "Unknown profileId: $profileId" }

        val detected = detectImage(bytes)
            ?: throw IllegalArgumentException("generated image has an unsupported or invalid signature")
        if (declaredMimeType != null) {
            require(declaredMimeType.substringBefore(';').trim().equals(detected.mimeType, ignoreCase = true)) {
                "generated image MIME type does not match its signature"
            }
        }

        val digest = sha256(bytes)
        val profileDirectory = File(root, sha256(profileId.toByteArray(Charsets.UTF_8)))
        check(profileDirectory.mkdirs() || profileDirectory.isDirectory) { "could not create generated-media directory" }
        val target = File(profileDirectory, "$digest.${detected.extension}")
        val createdFile = if (target.exists()) {
            require(target.length() == bytes.size.toLong() && sha256(target) == digest) {
                "existing generated-media file failed integrity validation"
            }
            false
        } else {
            writeAtomically(target, bytes)
            true
        }

        val entity = GeneratedMediaEntity(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            kind = GeneratedMediaKind.IMAGE,
            operation = operation,
            mimeType = detected.mimeType,
            sha256 = digest,
            relativePath = target.relativeTo(root).invariantSeparatorsPath,
            byteSize = bytes.size.toLong(),
            modelId = modelId.trim(),
            prompt = prompt,
            createdAt = System.currentTimeMillis(),
        )
        try {
            dao.insert(entity)
        } catch (failure: Throwable) {
            if (createdFile) target.delete()
            throw failure
        }
        return entity
    }

    fun observeLatestImageUri(profileId: String): Flow<Uri?> =
        dao.observeLatest(profileId).map { media ->
            media?.takeIf { it.kind == GeneratedMediaKind.IMAGE }
                ?.let(::resolveExistingFile)
                ?.let(Uri::fromFile)
        }

    fun uriFor(media: GeneratedMediaEntity): Uri? = resolveExistingFile(media)?.let(Uri::fromFile)

    suspend fun page(profileId: String, limit: Int, offset: Int): List<GeneratedMediaEntity> {
        require(limit in 1..MAX_PAGE_SIZE) { "limit must be between 1 and $MAX_PAGE_SIZE" }
        require(offset >= 0) { "offset must not be negative" }
        return dao.page(profileId, limit, offset)
    }

    suspend fun deleteForProfile(profileId: String): Int {
        val media = mutableListOf<GeneratedMediaEntity>()
        var offset = 0
        do {
            val page = dao.page(profileId, MAX_PAGE_SIZE, offset)
            media += page
            offset += page.size
        } while (page.size == MAX_PAGE_SIZE)

        val deleted = db.withTransaction { dao.deleteForProfile(profileId) }
        media.mapNotNull(::resolveExistingFile).distinct().forEach(File::delete)
        val profileDirectory = File(root, sha256(profileId.toByteArray(Charsets.UTF_8)))
        profileDirectory.delete()
        return deleted
    }

    private fun resolveExistingFile(media: GeneratedMediaEntity): File? {
        val candidate = File(root, media.relativePath)
        val canonicalRoot = root.canonicalFile
        val canonicalFile = candidate.canonicalFile
        val withinRoot = canonicalFile.path.startsWith(canonicalRoot.path + File.separator)
        return canonicalFile.takeIf { withinRoot && it.isFile && it.length() == media.byteSize }
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val temporary = File(target.parentFile, ".${target.name}.${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            check(temporary.renameTo(target) || target.exists()) { "could not finalize generated-media file" }
        } finally {
            temporary.delete()
        }
    }

    private data class ImageFormat(val mimeType: String, val extension: String)

    private fun detectImage(bytes: ByteArray): ImageFormat? = when {
        bytes.size >= 8 && bytes.sliceArray(0..7).contentEquals(PNG_SIGNATURE) -> ImageFormat("image/png", "png")
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> ImageFormat("image/jpeg", "jpg")
        bytes.size >= 12 && bytes.copyOfRange(0, 4).decodeToString() == "RIFF" && bytes.copyOfRange(8, 12).decodeToString() == "WEBP" -> ImageFormat("image/webp", "webp")
        else -> null
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val DIRECTORY_NAME = "generated-media"
        private const val MAX_IMAGE_BYTES = 32 * 1024 * 1024
        private const val MAX_PAGE_SIZE = 200
        private val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        )
    }
}
