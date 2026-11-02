package com.example.data.sync

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupChunk(
    val index: Int,
    val ivBase64: String,
    val cipherText: String,
    val bytes: Int,
    val sha256: String
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "index" to index,
        "ivBase64" to ivBase64,
        "cipherText" to cipherText,
        "bytes" to bytes,
        "sha256" to sha256
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): BackupChunk {
            return BackupChunk(
                index = (map["index"] as? Number)?.toInt() ?: 0,
                ivBase64 = map["ivBase64"]?.toString() ?: "",
                cipherText = map["cipherText"]?.toString() ?: "",
                bytes = (map["bytes"] as? Number)?.toInt() ?: 0,
                sha256 = map["sha256"]?.toString() ?: ""
            )
        }
    }
}

data class BackupManifest(
    val schemaVersion: Int = 2,
    val backupId: String,
    val createdAtEpochMs: Long,
    val uid: String,
    val appVersionCode: Int,
    val kdf: Map<String, Any?>,
    val cipher: Map<String, Any?>,
    val chunkSize: Int,
    val chunkCount: Int,
    val totalPlainBytes: Long,
    val totalCipherBytes: Long,
    val preEncryptionPayloadSha256: String,
    val tableRowCounts: Map<String, Int>,
    val userEmail: String = "",
    val organizationName: String = "",
    val networkName: String = "",
    val totalRecords: Int = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "schemaVersion" to schemaVersion,
        "backupId" to backupId,
        "createdAtEpochMs" to createdAtEpochMs,
        "uid" to uid,
        "appVersionCode" to appVersionCode,
        "kdf" to kdf,
        "cipher" to cipher,
        "chunkSize" to chunkSize,
        "chunkCount" to chunkCount,
        "totalPlainBytes" to totalPlainBytes,
        "totalCipherBytes" to totalCipherBytes,
        "preEncryptionPayloadSha256" to preEncryptionPayloadSha256,
        "tableRowCounts" to tableRowCounts,
        "userEmail" to userEmail,
        "organizationName" to organizationName,
        "networkName" to networkName,
        "totalRecords" to totalRecords
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): BackupManifest {
            val rawCounts = (map["tableRowCounts"] as? Map<String, Any?>) ?: emptyMap()
            val tableCounts = rawCounts.mapNotNull { (k, v) ->
                (v as? Number)?.toInt()?.let { k to it }
            }.toMap()

            return BackupManifest(
                schemaVersion = (map["schemaVersion"] as? Number)?.toInt() ?: 2,
                backupId = map["backupId"]?.toString() ?: "",
                createdAtEpochMs = (map["createdAtEpochMs"] as? Number)?.toLong() ?: 0L,
                uid = map["uid"]?.toString() ?: "",
                appVersionCode = (map["appVersionCode"] as? Number)?.toInt() ?: 1,
                kdf = (map["kdf"] as? Map<String, Any?>) ?: emptyMap(),
                cipher = (map["cipher"] as? Map<String, Any?>) ?: emptyMap(),
                chunkSize = (map["chunkSize"] as? Number)?.toInt() ?: (400 * 1024),
                chunkCount = (map["chunkCount"] as? Number)?.toInt() ?: 0,
                totalPlainBytes = (map["totalPlainBytes"] as? Number)?.toLong() ?: 0L,
                totalCipherBytes = (map["totalCipherBytes"] as? Number)?.toLong() ?: 0L,
                preEncryptionPayloadSha256 = map["preEncryptionPayloadSha256"]?.toString() ?: "",
                tableRowCounts = tableCounts,
                userEmail = map["userEmail"]?.toString() ?: "",
                organizationName = map["organizationName"]?.toString() ?: "",
                networkName = map["networkName"]?.toString() ?: "",
                totalRecords = (map["totalRecords"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

data class BackupPackage(
    val manifest: BackupManifest,
    val chunks: List<BackupChunk>
)

/**
 * Enterprise-grade encryption, compression, and chunking pipeline for SamMikrotik cloud backups.
 * Exact Sequential Order:
 * JSON Payload -> GZIP Compression -> AES-256-GCM Encryption -> Base64 Encoding -> Chunking (<= 400 KB per chunk)
 */
object BackupCryptoEngine {

    const val KDF_ITERATIONS = 210_000
    const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
    const val GCM_TAG_LENGTH_BITS = 128
    const val GCM_IV_LENGTH_BYTES = 12
    const val SALT_LENGTH_BYTES = 16
    const val MAX_ALLOWED_CHUNKS = 200
    const val MAX_CHUNK_BYTES = 400 * 1024 // 400 KB hard limit per chunk payload
    const val DEFAULT_RAW_CHUNK_SLICE_BYTES = 260_000 // In Base64 ~346 KB, strictly <= 400 KB

    private val secureRandom = SecureRandom()

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun generateIv(): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)
        return iv
    }

    fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int = KDF_ITERATIONS): SecretKey {
        require(passphrase.length >= 8) { "يجب ألا تقل عبارة المرور عن 8 أحرف" }
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun compressGzip(input: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream(input.size / 2 + 1024)
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(input)
        }
        return bos.toByteArray()
    }

    fun decompressGzip(compressed: ByteArray): ByteArray {
        val bis = ByteArrayInputStream(compressed)
        val bos = ByteArrayOutputStream(compressed.size * 2 + 1024)
        GZIPInputStream(bis).use { gzip ->
            val buffer = ByteArray(8192)
            var read: Int
            while (gzip.read(buffer).also { read = it } != -1) {
                bos.write(buffer, 0, read)
            }
        }
        return bos.toByteArray()
    }

    fun encryptChunk(plainBytes: ByteArray, secretKey: SecretKey, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(plainBytes)
    }

    fun decryptChunk(cipherBytes: ByteArray, secretKey: SecretKey, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(cipherBytes)
    }

    fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun sha256Hex(text: String): String {
        return sha256Hex(text.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Splits arbitrary byte array into chunks not exceeding maxChunkBytes.
     * Payloads < maxChunkBytes produce exactly 1 chunk.
     * Payloads > maxChunkBytes split into correct chunk counts with zero empty chunks.
     */
    fun splitIntoChunks(data: ByteArray, maxChunkBytes: Int = MAX_CHUNK_BYTES): List<ByteArray> {
        require(data.isNotEmpty()) { "البيانات فارغة، لا يمكن تقسيمها" }
        require(maxChunkBytes > 0) { "حجم الجزء يجب أن يكون أكبر من الصفر" }
        val chunks = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < data.size) {
            val len = minOf(maxChunkBytes, data.size - offset)
            chunks.add(data.copyOfRange(offset, offset + len))
            offset += len
        }
        return chunks
    }

    /**
     * Executes the complete backup creation pipeline:
     * JSON Payload -> GZIP Compression -> AES-256-GCM Encryption -> Base64 Encoding -> Chunking (<= 400 KB per chunk)
     */
    fun createBackupPackage(
        jsonString: String,
        passphrase: String,
        uid: String,
        userEmail: String,
        appVersionCode: Int = 10,
        rawSliceSize: Int = DEFAULT_RAW_CHUNK_SLICE_BYTES,
        onProgress: ((String, Float) -> Unit)? = null
    ): BackupPackage {
        require(jsonString.isNotBlank()) { "ملف البيانات الاحتياطية فارغ" }
        require(passphrase.length >= 8) { "يجب ألا تقل عبارة المرور عن 8 أحرف" }

        onProgress?.invoke("تحضير", 0.1f)

        val plainBytes = jsonString.toByteArray(StandardCharsets.UTF_8)
        val preEncryptionSha256 = sha256Hex(plainBytes)

        // Parse table counts and metadata from the unencrypted JSON
        val root = JSONObject(jsonString)
        val orgObj = root.optJSONObject("organization")
        val orgName = orgObj?.optString("name", "") ?: ""
        val profileObj = root.optJSONObject("network_profile") ?: root.optJSONObject("networkProfile")
        val netName = profileObj?.optString("networkName", "") ?: ""

        val docsArr = root.optJSONArray("documents")
        val linesArr = root.optJSONArray("journal_lines")
        val partiesArr = root.optJSONArray("parties")
        val pkgsArr = root.optJSONArray("packages")
        val treasuriesArr = root.optJSONArray("treasuries")
        val entriesArr = root.optJSONArray("journal_entries")
        val itemsArr = root.optJSONArray("document_items")
        val allocArr = root.optJSONArray("allocations")
        val assetsArr = root.optJSONArray("assets")
        val ratesArr = root.optJSONArray("currency_rates")
        val devicesArr = root.optJSONArray("network_devices") ?: root.optJSONArray("networkDevices")

        val tableRowCounts = mapOf(
            "documents" to (docsArr?.length() ?: 0),
            "journal_entries" to (entriesArr?.length() ?: 0),
            "journal_lines" to (linesArr?.length() ?: 0),
            "parties" to (partiesArr?.length() ?: 0),
            "packages" to (pkgsArr?.length() ?: 0),
            "treasuries" to (treasuriesArr?.length() ?: 0),
            "document_items" to (itemsArr?.length() ?: 0),
            "allocations" to (allocArr?.length() ?: 0),
            "assets" to (assetsArr?.length() ?: 0),
            "currency_rates" to (ratesArr?.length() ?: 0),
            "network_devices" to (devicesArr?.length() ?: 0)
        )

        val totalRecords = tableRowCounts.values.sum() + (if (orgObj != null) 1 else 0) + (if (profileObj != null) 1 else 0)

        // 1. GZIP Compression
        onProgress?.invoke("ضغط", 0.25f)
        val compressedBytes = compressGzip(plainBytes)

        // 2. Key Derivation
        onProgress?.invoke("تشفير", 0.35f)
        val salt = generateSalt()
        val secretKey = deriveKey(passphrase, salt, KDF_ITERATIONS)

        // 3. Chunk raw compressed stream
        val slices = splitIntoChunks(compressedBytes, rawSliceSize)
        if (slices.size > MAX_ALLOWED_CHUNKS) {
            throw IllegalStateException("حجم النسخة الاحتياطية يتجاوز الحد الأقصى المسموح به (${slices.size} جزء > $MAX_ALLOWED_CHUNKS جزء). يرجى أرشفة الفترات المالية السابقة.")
        }

        // 4. Encrypt each chunk with fresh IV per chunk, encode Base64, and compute SHA-256
        val chunks = mutableListOf<BackupChunk>()
        var totalCipherBytes = 0L

        for ((index, slice) in slices.withIndex()) {
            val iv = generateIv()
            val cipherBytes = encryptChunk(slice, secretKey, iv)
            val ivBase64 = Base64.getEncoder().encodeToString(iv)
            val cipherText = Base64.getEncoder().encodeToString(cipherBytes)
            val chunkBytes = cipherText.toByteArray(StandardCharsets.UTF_8).size

            if (chunkBytes > MAX_CHUNK_BYTES) {
                throw IllegalStateException("تجاوز جزء النسخة رقم $index الحد الأقصى المسموح به ($chunkBytes > $MAX_CHUNK_BYTES بايت)")
            }

            val chunkSha256 = sha256Hex(cipherText)
            totalCipherBytes += chunkBytes

            chunks.add(
                BackupChunk(
                    index = index,
                    ivBase64 = ivBase64,
                    cipherText = cipherText,
                    bytes = chunkBytes,
                    sha256 = chunkSha256
                )
            )
        }

        val backupId = "backup_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val manifest = BackupManifest(
            schemaVersion = 2,
            backupId = backupId,
            createdAtEpochMs = System.currentTimeMillis(),
            uid = uid,
            appVersionCode = appVersionCode,
            kdf = mapOf(
                "algorithm" to KDF_ALGORITHM,
                "iterations" to KDF_ITERATIONS,
                "saltBase64" to Base64.getEncoder().encodeToString(salt)
            ),
            cipher = mapOf(
                "algorithm" to CIPHER_ALGORITHM,
                "tagLengthBits" to GCM_TAG_LENGTH_BITS,
                "ivLengthBytes" to GCM_IV_LENGTH_BYTES
            ),
            chunkSize = MAX_CHUNK_BYTES,
            chunkCount = chunks.size,
            totalPlainBytes = plainBytes.size.toLong(),
            totalCipherBytes = totalCipherBytes,
            preEncryptionPayloadSha256 = preEncryptionSha256,
            tableRowCounts = tableRowCounts,
            userEmail = userEmail,
            organizationName = orgName,
            networkName = netName,
            totalRecords = totalRecords
        )

        return BackupPackage(manifest, chunks)
    }

    /**
     * Assembles, decrypts, decompresses, and validates the restored backup package.
     * Enforces fail-fast authentication and tamper detection.
     */
    fun decryptAndAssembleBackupPackage(
        manifest: BackupManifest,
        chunks: List<BackupChunk>,
        passphrase: String
    ): String {
        require(manifest.schemaVersion in 1..2) { "إصدار هيكل النسخة الاحتياطية (${manifest.schemaVersion}) غير مدعوم" }
        require(chunks.size == manifest.chunkCount) {
            "عدد الأجزاء المستلمة (${chunks.size}) لا يطابق المسجل في بيان النسخة (${manifest.chunkCount})"
        }

        // 1. Sort chunks by index
        val sortedChunks = chunks.sortedBy { it.index }
        for (i in sortedChunks.indices) {
            require(sortedChunks[i].index == i) { "فهرس أجزاء النسخة غير متسلسل أو مفقود عند الجزء رقم $i" }
        }

        // 2. Validate per-chunk SHA-256 (Tamper Detection)
        for (chunk in sortedChunks) {
            val computedHash = sha256Hex(chunk.cipherText)
            require(computedHash.equals(chunk.sha256, ignoreCase = true)) {
                "فشل التحقق من سلامة الجزء رقم ${chunk.index}، تم اكتشاف تلاعب أو تلف في البيانات (SHA-256 Tamper Detected)"
            }
        }

        // 3. Extract KDF parameters and derive Key
        val saltBase64 = manifest.kdf["saltBase64"]?.toString()
            ?: throw IllegalArgumentException("معلمات التشفير KDF مفقودة في بيان النسخة")
        val iterations = (manifest.kdf["iterations"] as? Number)?.toInt() ?: KDF_ITERATIONS
        val salt = Base64.getDecoder().decode(saltBase64)
        val secretKey = deriveKey(passphrase, salt, iterations)

        // 4. Decrypt each chunk with its per-chunk IV
        val compressedBos = ByteArrayOutputStream()
        for (chunk in sortedChunks) {
            val iv = try {
                Base64.getDecoder().decode(chunk.ivBase64)
            } catch (e: Exception) {
                throw IllegalArgumentException("رمز IV تالف في الجزء ${chunk.index}")
            }
            val cipherBytes = try {
                Base64.getDecoder().decode(chunk.cipherText)
            } catch (e: Exception) {
                throw IllegalArgumentException("النص المشفر تالف في الجزء ${chunk.index}")
            }

            val decrypted = try {
                decryptChunk(cipherBytes, secretKey, iv)
            } catch (e: Exception) {
                // Cryptographic auth tag failure (GCM authentication tag mismatch)
                throw IllegalArgumentException("عبارة مرور خاطئة أو ملف تالف")
            }
            compressedBos.write(decrypted)
        }

        // 5. Decompress GZIP payload
        val decompressedBytes = try {
            decompressGzip(compressedBos.toByteArray())
        } catch (e: Exception) {
            throw IllegalArgumentException("فشل فك ضغط البيانات المحفوظة (عبارة مرور خاطئة أو ملف تالف)")
        }

        val jsonString = String(decompressedBytes, StandardCharsets.UTF_8)

        // 6. Verify pre-encryption SHA-256
        val computedPreHash = sha256Hex(jsonString)
        require(computedPreHash.equals(manifest.preEncryptionPayloadSha256, ignoreCase = true)) {
            "فشل التحقق من مطابقة البصمة الرقمية للبيانات الأصلية بعد فك التشفير (SHA-256 Mismatch)"
        }

        // 7. Verify table row counts against manifest
        val root = JSONObject(jsonString)
        val docsCount = root.optJSONArray("documents")?.length() ?: 0
        val linesCount = root.optJSONArray("journal_lines")?.length() ?: 0
        val manifestDocs = manifest.tableRowCounts["documents"]
        val manifestLines = manifest.tableRowCounts["journal_lines"]

        if (manifestDocs != null && docsCount != manifestDocs) {
            throw IllegalStateException("عدم تطابق عدد سجلات المستندات بعد فك التشفير ($docsCount != $manifestDocs)")
        }
        if (manifestLines != null && linesCount != manifestLines) {
            throw IllegalStateException("عدم تطابق عدد قيود اليومية بعد فك التشفير ($linesCount != $manifestLines)")
        }

        return jsonString
    }
}
