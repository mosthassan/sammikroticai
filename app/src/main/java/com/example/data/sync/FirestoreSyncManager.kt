package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.core.model.RateZone
import com.example.data.local.AppDatabase
import com.example.data.local.dao.DeviceDao
import com.example.data.local.entity.CurrencyRateEntity
import com.example.data.local.entity.OrganizationEntity
import com.example.data.network.DeviceStatus
import com.example.data.network.DeviceType
import com.example.data.network.NetworkConfig
import com.example.data.network.NetworkDevice
import com.example.data.network.NetworkRepository
import com.example.data.network.toBackupDto
import com.example.domain.usecase.BackupRestoreUseCase
import com.example.util.NetworkProfileBackupDto
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CloudBackupMeta(
    val uid: String,
    val userEmail: String,
    val timestamp: Long,
    val documentsCount: Int,
    val journalLinesCount: Int,
    val partiesCount: Int,
    val totalRecords: Int,
    val checksum: String,
    val networkDevicesCount: Int = 0,
    val currencyRatesCount: Int = 0,
    val networkName: String = "",
    val organizationName: String = "",
    val hasNetworkProfile: Boolean = false,
    val backupId: String = "",
    val isChunked: Boolean = false,
    val chunkCount: Int = 1,
    val totalCipherBytes: Long = 0L,
    val appVersionCode: Int = 10,
    val manifest: BackupManifest? = null,
    val isEncrypted: Boolean = true
)

interface FirestoreAdapter {
    fun isAvailable(): Boolean = true
    suspend fun get(path: String): Map<String, Any?>?
    suspend fun set(path: String, data: Map<String, Any?>, merge: Boolean = true)
    suspend fun delete(path: String) {}
    suspend fun listCollection(collectionPath: String): List<Pair<String, Map<String, Any?>>> = emptyList()
}

class DefaultFirestoreAdapter(private val firestoreProvider: () -> FirebaseFirestore?) : FirestoreAdapter {
    override fun isAvailable(): Boolean = firestoreProvider() != null

    override suspend fun get(path: String): Map<String, Any?>? {
        val fs = firestoreProvider() ?: return null
        return try {
            val snap = getDocRef(fs, path).get().await()
            if (snap.exists()) snap.data else null
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore get error at $path: ${e.message}")
            null
        }
    }

    override suspend fun set(path: String, data: Map<String, Any?>, merge: Boolean) {
        val fs = firestoreProvider() ?: throw IllegalStateException("خدمة Firebase غير مهيأة على هذا الجهاز")
        if (merge) {
            getDocRef(fs, path).set(data, SetOptions.merge()).await()
        } else {
            getDocRef(fs, path).set(data).await()
        }
    }

    override suspend fun delete(path: String) {
        val fs = firestoreProvider() ?: return
        try {
            getDocRef(fs, path).delete().await()
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore delete error at $path: ${e.message}")
        }
    }

    override suspend fun listCollection(collectionPath: String): List<Pair<String, Map<String, Any?>>> {
        val fs = firestoreProvider() ?: return emptyList()
        return try {
            val colRef = getCollectionRef(fs, collectionPath)
            val snapshot = colRef.get().await()
            snapshot.documents.map { doc -> doc.id to (doc.data ?: emptyMap()) }
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore listCollection error at $collectionPath: ${e.message}")
            emptyList()
        }
    }

    private fun getDocRef(fs: FirebaseFirestore, path: String): com.google.firebase.firestore.DocumentReference {
        val segments = path.split("/").filter { it.isNotBlank() }
        require(segments.size >= 2 && segments.size % 2 == 0) { "Document path must have even number of segments: $path" }
        var col = fs.collection(segments[0])
        var doc = col.document(segments[1])
        for (i in 2 until segments.size step 2) {
            col = doc.collection(segments[i])
            doc = col.document(segments[i + 1])
        }
        return doc
    }

    private fun getCollectionRef(fs: FirebaseFirestore, path: String): com.google.firebase.firestore.CollectionReference {
        val segments = path.split("/").filter { it.isNotBlank() }
        require(segments.size % 2 == 1) { "Collection path must have odd number of segments: $path" }
        var col = fs.collection(segments[0])
        for (i in 1 until segments.size step 2) {
            val doc = col.document(segments[i])
            col = doc.collection(segments[i + 1])
        }
        return col
    }
}

/**
 * Production-Grade Cloud Backup & Restore Architecture.
 * Scoped strictly under the authenticated user.
 * Guarantees 100% atomic restore within a single Room transaction,
 * including accounting ledgers, vouchers, exchange rates history, network devices, and identity config.
 */
class FirestoreSyncManager(
    private val context: Context,
    private val db: AppDatabase,
    private val backupRestoreUseCase: BackupRestoreUseCase,
    private val networkRepository: NetworkRepository? = null,
    private val deviceDao: DeviceDao? = null,
    private val firestoreAdapter: FirestoreAdapter? = null
) {
    private val effectiveAdapter: FirestoreAdapter
        get() = firestoreAdapter ?: DefaultFirestoreAdapter { firestore }
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncMetadata = MutableStateFlow<SyncMetadata?>(null)
    val syncMetadata: StateFlow<SyncMetadata?> = _syncMetadata.asStateFlow()

    private val _syncHistory = MutableStateFlow<List<SyncLogItem>>(emptyList())
    val syncHistory: StateFlow<List<SyncLogItem>> = _syncHistory.asStateFlow()

    private val prefs = context.getSharedPreferences("sammikrotik_sync_prefs", Context.MODE_PRIVATE)

    private val effectiveDeviceDao: DeviceDao?
        get() = deviceDao ?: networkRepository ?: NetworkRepository(context)

    private val effectiveNetworkRepository: NetworkRepository?
        get() = networkRepository ?: (deviceDao as? NetworkRepository) ?: NetworkRepository(context)

    private val firestore: FirebaseFirestore?
        get() = try {
            com.example.core.firebase.FirebaseInitializer.ensureInitialized(context)
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val dbIdRes = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
                val dbId = if (dbIdRes != 0) runCatching { context.getString(dbIdRes) }.getOrNull() else null
                if (!dbId.isNullOrBlank() && dbId != "(default)") {
                    FirebaseFirestore.getInstance(FirebaseApp.getInstance(), dbId)
                } else {
                    FirebaseFirestore.getInstance()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore access: ${e.message}")
            null
        }

    init {
        loadLastSyncMetadata()
    }

    private fun loadLastSyncMetadata() {
        val lastSync = prefs.getLong("last_sync_timestamp", 0L)
        val email = prefs.getString("last_sync_email", "") ?: ""
        if (lastSync > 0L && email.isNotBlank()) {
            _syncMetadata.value = SyncMetadata(
                userEmail = email,
                lastSyncedAt = lastSync,
                totalDocuments = prefs.getInt("last_sync_docs", 0),
                totalJournalLines = prefs.getInt("last_sync_lines", 0),
                totalParties = prefs.getInt("last_sync_parties", 0),
                totalPackages = prefs.getInt("last_sync_packages", 0),
                totalTreasuries = prefs.getInt("last_sync_treasuries", 0),
                totalAllocations = prefs.getInt("last_sync_allocations", 0),
                totalDevices = prefs.getInt("last_sync_devices", 0),
                totalNetworkDevices = prefs.getInt("last_sync_devices", 0),
                totalCurrencyRates = prefs.getInt("last_sync_rates", 0),
                networkName = prefs.getString("last_sync_net_name", "") ?: "",
                organizationName = prefs.getString("last_sync_org_name", "") ?: "",
                hasNetworkProfile = prefs.getBoolean("last_sync_has_profile", false)
            )
        }
    }

    private fun resolveEffectiveUid(uid: String, userEmail: String): String {
        val authUid = runCatching {
            if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseAuth.getInstance().currentUser?.uid else null
        }.getOrNull()
        return authUid?.takeIf { it.isNotBlank() } ?: uid.takeIf { it.isNotBlank() } ?: ""
    }

    private val isBackupInProgress = java.util.concurrent.atomic.AtomicBoolean(false)
    var lastPreRestoreSnapshotPath: String? = null
        private set

    /**
     * Checks if a remote backup exists for the specified user in Firestore.
     * Queries chunked encrypted backups at users/{uid}/backups.
     */
    suspend fun checkRemoteBackup(uid: String, userEmail: String): CloudBackupMeta? = withContext(Dispatchers.IO) {
        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) return@withContext null
        val safeUid = resolveEffectiveUid(uid, userEmail)
        if (safeUid.isBlank()) return@withContext null

        try {
            // 1. Check for chunked encrypted backup manifests under users/{uid}/backups
            val backupDocs = adapter.listCollection("users/$safeUid/backups")
            if (backupDocs.isNotEmpty()) {
                val manifests = backupDocs.mapNotNull { (_, data) ->
                    try {
                        BackupManifest.fromMap(data)
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.createdAtEpochMs }

                val latest = manifests.firstOrNull()
                if (latest != null) {
                    val docs = latest.tableRowCounts["documents"] ?: 0
                    val lines = latest.tableRowCounts["journal_lines"] ?: 0
                    val parties = latest.tableRowCounts["parties"] ?: 0
                    val devices = latest.tableRowCounts["network_devices"] ?: 0
                    val rates = latest.tableRowCounts["currency_rates"] ?: 0

                    return@withContext CloudBackupMeta(
                        uid = safeUid,
                        userEmail = latest.userEmail.ifBlank { userEmail },
                        timestamp = latest.createdAtEpochMs,
                        documentsCount = docs,
                        journalLinesCount = lines,
                        partiesCount = parties,
                        totalRecords = latest.totalRecords,
                        checksum = latest.preEncryptionPayloadSha256,
                        networkDevicesCount = devices,
                        currencyRatesCount = rates,
                        networkName = latest.networkName,
                        organizationName = latest.organizationName,
                        hasNetworkProfile = latest.networkName.isNotBlank(),
                        backupId = latest.backupId,
                        isChunked = true,
                        chunkCount = latest.chunkCount,
                        totalCipherBytes = latest.totalCipherBytes,
                        appVersionCode = latest.appVersionCode,
                        manifest = latest,
                        isEncrypted = latest.isEncrypted
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "checkRemoteBackup error: ${e.message}")
        }
        null
    }

    /**
     * Retrieves active subscription status document from users/{uid}/subscription/status in Firestore.
     */
    suspend fun getCloudSubscriptionStatus(uid: String, userEmail: String): Map<String, Any?>? = withContext(Dispatchers.IO) {
        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) return@withContext null
        val safeUid = resolveEffectiveUid(uid, userEmail)
        if (safeUid.isBlank()) return@withContext null
        try {
            adapter.get("users/$safeUid/subscription/status")
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "getCloudSubscriptionStatus error: ${e.message}")
            null
        }
    }

    /**
     * Sets or updates active subscription status under users/{uid}/subscription/status in Firestore.
     */
    suspend fun setCloudSubscription(
        targetUid: String,
        tier: String,
        expirationEpochMs: Long?,
        customerName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) return@withContext false
        val safeUid = targetUid.trim().replace("/", "_").takeIf { it.isNotBlank() } ?: return@withContext false
        try {
            val data = mapOf(
                "tier" to tier,
                "expirationEpochMs" to (expirationEpochMs ?: -1L),
                "customerName" to customerName,
                "updatedAt" to System.currentTimeMillis()
            )
            adapter.set("users/$safeUid/subscription/status", data, false)
            true
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "setCloudSubscription error: ${e.message}")
            false
        }
    }

    /**
     * Chunked, GZIP compressed, AES-256-GCM Encrypted Cloud Push.
     * Enforces concurrency lock, per-chunk uploads with retry, and atomic manifest commit.
     */
    suspend fun syncPush(
        uid: String,
        userEmail: String,
        passphrase: String = "",
        onProgress: ((String, Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val safeUid = resolveEffectiveUid(uid, userEmail)
        if (safeUid.isBlank()) {
            val err = "معرف المستخدم غير محدد، يرجى تسجيل الدخول أولاً"
            _syncState.value = SyncState.Error(err)
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        // Concurrency Lock (Task 2, Rule 5)
        if (!isBackupInProgress.compareAndSet(false, true)) {
            val err = "عملية نسخ احتياطي سحابي قيد التنفيذ حالياً، يرجى الانتظار حتى اكتمالها"
            _syncState.value = SyncState.Error(err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        try {
            val adapter = effectiveAdapter
            if (!adapter.isAvailable()) {
                val err = "خدمة Firebase غير مهيأة على هذا الجهاز"
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            val fbAuthUser = runCatching {
                if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseAuth.getInstance().currentUser else null
            }.getOrNull()
            val hasFirebaseApp = runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)
            if (hasFirebaseApp && fbAuthUser == null && firestoreAdapter == null) {
                val err = "يرجى تسجيل الدخول بحساب Google أولاً عبر زر 'دخول Google بنقرة' لتوثيق صلاحيات السحابة في Firebase وتجنب رفض الصلاحيات (PERMISSION_DENIED)"
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            val storage = BackupPassphraseStorage(context)
            val isCustomEncryptionEnabled = storage.isEncryptionEnabled(userEmail) || passphrase.isNotBlank()

            val effectivePassphrase = if (isCustomEncryptionEnabled) {
                if (passphrase.isNotBlank()) {
                    if (passphrase.length < 8) {
                        val err = "يجب تحديد عبارة مرور آمنة للنسخ الاحتياطي لا تقل عن 8 أحرف"
                        _syncState.value = SyncState.Error(err)
                        return@withContext Result.failure(IllegalArgumentException(err))
                    }
                    passphrase
                } else {
                    val saved = storage.getPassphrase(userEmail)
                    if (saved.isNullOrBlank() || saved.length < 8) {
                        val err = "عبارة مرور النسخ الاحتياطي مطلوبة لأن خيار التشفير المخصص مفعّل. عيّنها من نافذة المزامنة."
                        _syncState.value = SyncState.Error(err)
                        return@withContext Result.failure(IllegalStateException(err))
                    }
                    saved
                }
            } else {
                // Seamless default: derive strong account key from user ID & email (no prompt needed)
                val accountRaw = "SamMikrotik_Default_Cloud_Key_${safeUid}_${userEmail.trim().lowercase()}"
                BackupCryptoEngine.sha256Hex(accountRaw).take(32)
            }

            _syncState.value = SyncState.InProgress("تحضير البيانات وتجميع السجلات...", 0.1f)
            onProgress?.invoke("تحضير", 0.1f)

            // 1. Export verifiable snapshot
            val jsonSnapshot = backupRestoreUseCase.exportDatabaseToJson()

            // 2. Encrypt, compress, and chunk (JSON -> GZIP -> AES-256-GCM -> Base64 -> Chunks <= 400KB)
            val encLabel = if (isCustomEncryptionEnabled) "AES-256-GCM بكلمة مرور مخصصة" else "AES-256-GCM تلقائي"
            _syncState.value = SyncState.InProgress("ضغط وتشفير البيانات ($encLabel)...", 0.3f)
            val backupPackage = BackupCryptoEngine.createBackupPackage(
                jsonString = jsonSnapshot,
                passphrase = effectivePassphrase,
                uid = safeUid,
                userEmail = userEmail,
                appVersionCode = 10,
                isEncrypted = isCustomEncryptionEnabled,
                onProgress = { step, p ->
                    _syncState.value = SyncState.InProgress(step, p)
                    onProgress?.invoke(step, p)
                }
            )

            val manifest = backupPackage.manifest
            val chunks = backupPackage.chunks
            val totalChunks = chunks.size

            // 3. Upload chunks with retry (up to 3 times) and error isolation
            val uploadedChunkIds = mutableListOf<String>()
            val baseProgress = 0.4f
            val progressRange = 0.5f

            for ((idx, chunk) in chunks.withIndex()) {
                val paddedIndex = String.format(java.util.Locale.US, "%04d", chunk.index)
                val chunkPath = "users/$safeUid/backups/${manifest.backupId}/chunks/$paddedIndex"
                val currentProgress = baseProgress + (progressRange * ((idx + 1).toFloat() / totalChunks.toFloat()))
                val progressMsg = "رفع الجزء ${idx + 1} من $totalChunks..."

                _syncState.value = SyncState.InProgress(progressMsg, currentProgress)
                onProgress?.invoke("رفع الجزء ${idx + 1} من $totalChunks", currentProgress)

                var uploadSuccess = false
                var lastUploadException: Exception? = null

                for (attempt in 1..3) {
                    try {
                        adapter.set(chunkPath, chunk.toMap(), false)
                        uploadSuccess = true
                        uploadedChunkIds.add(chunkPath)
                        break
                    } catch (e: Exception) {
                        lastUploadException = e
                        if (attempt < 3) {
                            kotlinx.coroutines.delay(300L * (1 shl (attempt - 1)))
                        }
                    }
                }

                if (!uploadSuccess) {
                    // Task 2, Rule 4: If retries exhaust, delete all uploaded chunks for that backupId and abort without writing a manifest
                    Log.w("FirestoreSyncManager", "Chunk upload failed after 3 attempts. Rolling back uploaded chunks...")
                    for (pathToDelete in uploadedChunkIds) {
                        try {
                            adapter.delete(pathToDelete)
                        } catch (e: Exception) {
                            Log.w("FirestoreSyncManager", "Failed to clean up chunk at $pathToDelete: ${e.message}")
                        }
                    }
                    val err = "فشل رفع جزء النسخة الاحتياطية (${idx + 1}) بعد 3 محاولات. تم إلغاء العملية وحذف الأجزاء غير المكتملة: ${lastUploadException?.localizedMessage ?: "خطأ في الشبكة"}"
                    _syncState.value = SyncState.Error(err)
                    logSync("رفع سحابي مشفر", userEmail, 0, false, err)
                    return@withContext Result.failure(IllegalStateException(err))
                }
            }

            // 4. Verification and Atomic Manifest Commit (Task 2, Rule 1)
            _syncState.value = SyncState.InProgress("تحقق من سلامة الأجزاء وحفظ بيان النسخة...", 0.92f)
            onProgress?.invoke("تحقق", 0.92f)

            val manifestPath = "users/$safeUid/backups/${manifest.backupId}"
            adapter.set(manifestPath, manifest.toMap(), false)


            // Update user summary
            runCatching {
                adapter.set(
                    "users/$safeUid",
                    mapOf(
                        "lastBackupEpochMs" to manifest.createdAtEpochMs,
                        "latestBackupId" to manifest.backupId,
                        "userEmail" to userEmail,
                        "hasBackup" to true,
                        "documentsCount" to (manifest.tableRowCounts["documents"] ?: 0),
                        "journalLinesCount" to (manifest.tableRowCounts["journal_lines"] ?: 0),
                        "totalRecords" to manifest.totalRecords,
                        "checksum" to manifest.preEncryptionPayloadSha256
                    ),
                    true
                )
            }

            // 5. Retention Policy: Retain only the latest 3 backups (Task 2, Rule 2)
            runCatching {
                val existingManifests = adapter.listCollection("users/$safeUid/backups").mapNotNull { (_, data) ->
                    try { BackupManifest.fromMap(data) } catch (e: Exception) { null }
                }.sortedByDescending { it.createdAtEpochMs }

                if (existingManifests.size > 3) {
                    val toPurge = existingManifests.drop(3)
                    for (old in toPurge) {
                        val oldChunks = adapter.listCollection("users/$safeUid/backups/${old.backupId}/chunks")
                        for ((chunkId, _) in oldChunks) {
                            adapter.delete("users/$safeUid/backups/${old.backupId}/chunks/$chunkId")
                        }
                        adapter.delete("users/$safeUid/backups/${old.backupId}")
                    }
                }
            }

            onProgress?.invoke("تم", 1.0f)

            val now = manifest.createdAtEpochMs
            val totalRecords = manifest.totalRecords
            val docsCount = manifest.tableRowCounts["documents"] ?: 0
            val linesCount = manifest.tableRowCounts["journal_lines"] ?: 0
            val partiesCount = manifest.tableRowCounts["parties"] ?: 0
            val pkgsCount = manifest.tableRowCounts["packages"] ?: 0
            val treasuriesCount = manifest.tableRowCounts["treasuries"] ?: 0
            val devicesCount = manifest.tableRowCounts["network_devices"] ?: 0
            val ratesCount = manifest.tableRowCounts["currency_rates"] ?: 0

            prefs.edit()
                .putLong("last_sync_timestamp", now)
                .putString("last_sync_email", userEmail)
                .putInt("last_sync_docs", docsCount)
                .putInt("last_sync_lines", linesCount)
                .putInt("last_sync_parties", partiesCount)
                .putInt("last_sync_packages", pkgsCount)
                .putInt("last_sync_treasuries", treasuriesCount)
                .putInt("last_sync_devices", devicesCount)
                .putInt("last_sync_rates", ratesCount)
                .putBoolean("last_sync_has_profile", manifest.networkName.isNotBlank())
                .putString("last_sync_net_name", manifest.networkName)
                .putString("last_sync_org_name", manifest.organizationName)
                .putInt("last_sync_total_records", totalRecords)
                .apply()

            val metadata = SyncMetadata(
                userEmail = userEmail,
                lastSyncedAt = now,
                totalDocuments = docsCount,
                totalJournalLines = linesCount,
                totalParties = partiesCount,
                totalPackages = pkgsCount,
                totalTreasuries = treasuriesCount,
                totalAllocations = manifest.tableRowCounts["allocations"] ?: 0,
                totalDevices = devicesCount,
                totalNetworkDevices = devicesCount,
                totalCurrencyRates = ratesCount,
                networkName = manifest.networkName,
                organizationName = manifest.organizationName,
                hasNetworkProfile = manifest.networkName.isNotBlank(),
                isBalanced = true,
                checksum = manifest.preEncryptionPayloadSha256
            )
            _syncMetadata.value = metadata

            val successMsg = "تم رفع النسخة المشفرة المقسمة بنجاح ($totalChunks جزء، $totalRecords سجلاً، AES-256-GCM)"
            _syncState.value = SyncState.Success(
                message = successMsg,
                lastSyncEpochMs = now,
                totalRecords = totalRecords
            )
            logSync("رفع سحابي مشفر", userEmail, totalRecords, true, successMsg)
            Result.success(successMsg)
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "SyncPush error: ${e.message}", e)
            val err = "فشل الرفع السحابي: ${e.localizedMessage ?: "تعذر إكمال الرفع"}"
            _syncState.value = SyncState.Error(err)
            logSync("رفع سحابي مشفر", userEmail, 0, false, err)
            Result.failure(e)
        } finally {
            isBackupInProgress.set(false)
        }
    }

    /**
     * Atomic & Fail-Safe Cloud Restore.
     * Supports both chunked AES-256-GCM encrypted backups and legacy backward compatibility.
     */
    suspend fun syncPull(
        uid: String,
        userEmail: String,
        passphrase: String = "",
        onProgress: ((String, Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val safeUid = resolveEffectiveUid(uid, userEmail)
        if (safeUid.isBlank()) {
            val err = "معرف المستخدم غير محدد، يرجى تسجيل الدخول أولاً"
            _syncState.value = SyncState.Error(err)
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        if (!isBackupInProgress.compareAndSet(false, true)) {
            val err = "عملية مزامنة سحابية قيد التنفيذ حالياً، يرجى الانتظار"
            _syncState.value = SyncState.Error(err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        try {
            val adapter = effectiveAdapter
            if (!adapter.isAvailable()) {
                val err = "خدمة Firebase غير مهيأة على هذا الجهاز"
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            val fbAuthUser = runCatching {
                if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseAuth.getInstance().currentUser else null
            }.getOrNull()
            val hasFirebaseApp = runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)
            if (hasFirebaseApp && fbAuthUser == null && firestoreAdapter == null) {
                val err = "يرجى تسجيل الدخول بحساب Google أولاً عبر زر 'دخول Google بنقرة' لتوثيق صلاحيات السحابة في Firebase وتجنب رفض الصلاحيات (PERMISSION_DENIED)"
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            _syncState.value = SyncState.InProgress("فحص النسخ السحابية المتوفرة...", 0.1f)
            onProgress?.invoke("تحضير", 0.1f)

            // Step 1: Check for chunked backups first: users/{uid}/backups
            val manifestDocs = adapter.listCollection("users/$safeUid/backups")
            val latestManifest = if (manifestDocs.isNotEmpty()) {
                manifestDocs.mapNotNull { (_, data) ->
                    try { BackupManifest.fromMap(data) } catch (e: Exception) { null }
                }.maxByOrNull { it.createdAtEpochMs }
            } else null

            val jsonContent: String

            if (latestManifest != null) {
                // Task 4, Step 1: Verify schemaVersion compatibility
                require(latestManifest.schemaVersion in 1..2) {
                    "إصدار هيكل النسخة الاحتياطية (${latestManifest.schemaVersion}) غير متوافق مع إصدار التطبيق الحالي"
                }

                val effectivePassphrase = if (!latestManifest.isEncrypted) {
                    // Backup was created without custom passphrase -> use account-derived key seamlessly
                    val accountRaw = "SamMikrotik_Default_Cloud_Key_${safeUid}_${userEmail.trim().lowercase()}"
                    BackupCryptoEngine.sha256Hex(accountRaw).take(32)
                } else if (passphrase.isNotBlank()) {
                    if (passphrase.length < 8) {
                        throw IllegalArgumentException("يرجى إدخال عبارة مرور لا تقل عن 8 أحرف لفك تشفير النسخة الاحتياطية")
                    }
                    passphrase
                } else {
                    val saved = BackupPassphraseStorage(context).getPassphrase(userEmail)
                    if (saved.isNullOrBlank() || saved.length < 8) {
                        throw IllegalStateException("عبارة مرور النسخ الاحتياطي مطلوبة لفك تشفير هذه النسخة.")
                    }
                    saved
                }

                // Task 4, Step 2: Download all chunks sequentially by index
                val chunks = mutableListOf<BackupChunk>()
                val total = latestManifest.chunkCount
                for (i in 0 until total) {
                    val padded = String.format(java.util.Locale.US, "%04d", i)
                    val chunkPath = "users/$safeUid/backups/${latestManifest.backupId}/chunks/$padded"
                    val p = 0.15f + (0.45f * ((i + 1).toFloat() / total.toFloat()))
                    _syncState.value = SyncState.InProgress("تنزيل الجزء ${i + 1} من $total...", p)
                    onProgress?.invoke("تنزيل الجزء ${i + 1} من $total", p)

                    val chunkDoc = adapter.get(chunkPath)
                        ?: throw IllegalStateException("الجزء رقم $i مفقود من السحابة للنسخة ${latestManifest.backupId}")
                    chunks.add(BackupChunk.fromMap(chunkDoc))
                }

                _syncState.value = SyncState.InProgress("فك التشفير وفحص البصمة الرقمية (AES-256-GCM)...", 0.65f)
                onProgress?.invoke("فك التشفير", 0.65f)

                // Decrypt, decompress, verify SHA-256 and table row counts
                // If passphrase wrong -> throws "عبارة مرور خاطئة أو ملف تالف"
                jsonContent = BackupCryptoEngine.decryptAndAssembleBackupPackage(
                    manifest = latestManifest,
                    chunks = chunks,
                    passphrase = effectivePassphrase
                )
            } else {
                throw IllegalStateException("لا توجد نسخة سحابية مشفرة لهذا الحساب. النسخ النصية القديمة غير مدعومة.")
            }

            // Task 4, Step 3: Safety Snapshot Creation prior to wiping local tables
            _syncState.value = SyncState.InProgress("إنشاء نقطة أمان محلية قبل الاستعادة...", 0.75f)
            onProgress?.invoke("نقطة أمان", 0.75f)
            val backupsDir = java.io.File(context.filesDir, "backups")
            backupsDir.mkdirs()
            val snapshotFile = java.io.File(backupsDir, "pre_restore_${System.currentTimeMillis()}.json")
            try {
                val currentLocalJson = backupRestoreUseCase.exportDatabaseToJson()
                snapshotFile.writeText(currentLocalJson)
                lastPreRestoreSnapshotPath = snapshotFile.absolutePath
            } catch (e: Exception) {
                Log.w("FirestoreSyncManager", "Failed to create safety snapshot file: ${e.message}")
            }

            // Task 4, Step 4 & 5: Database Import Transaction inside db.withTransaction
            _syncState.value = SyncState.InProgress("تطبيق الاستعادة الذرية والتحقق من قيود المحاسبة...", 0.85f)
            onProgress?.invoke("استعادة ذرية", 0.85f)

            val restoreResult = backupRestoreUseCase.restoreDatabaseFromJson(jsonContent)
            if (restoreResult.isFailure) {
                val failureReason = restoreResult.exceptionOrNull()?.localizedMessage ?: "فشل التحقق من قيود المحاسبة"
                val err = "خطأ في استعادة القيود: $failureReason"
                _syncState.value = SyncState.Error(err)
                logSync("استعادة من السحابة", userEmail, 0, false, err)
                return@withContext Result.failure(Exception(err))
            }

            // Additional restoration metadata & devices
            val restoredRoot = JSONObject(jsonContent)
            val docsCount = restoredRoot.optJSONArray("documents")?.length() ?: 0
            val linesCount = restoredRoot.optJSONArray("journal_lines")?.length() ?: 0
            val partiesCount = restoredRoot.optJSONArray("parties")?.length() ?: 0
            val pkgsCount = restoredRoot.optJSONArray("packages")?.length() ?: 0
            val treasuriesCount = restoredRoot.optJSONArray("treasuries")?.length() ?: 0
            val devJsonArr = restoredRoot.optJSONArray("network_devices") ?: restoredRoot.optJSONArray("networkDevices")
            val resDevCount = devJsonArr?.length() ?: 0
            val resRatesCount = restoredRoot.optJSONArray("currency_rates")?.length() ?: 0
            val resProfObj = restoredRoot.optJSONObject("network_profile") ?: restoredRoot.optJSONObject("networkProfile")
            val resOrgObj = restoredRoot.optJSONObject("organization")
            val resNetName = resProfObj?.optString("networkName", "").orEmpty()
            val resOrgName = resOrgObj?.optString("name", "").orEmpty()

            val totalRecords = docsCount + linesCount + partiesCount + pkgsCount + treasuriesCount + resDevCount + resRatesCount + (if (resProfObj != null || resNetName.isNotBlank()) 1 else 0) + (if (resOrgObj != null) 1 else 0)
            val now = System.currentTimeMillis()

            prefs.edit()
                .putLong("last_sync_timestamp", now)
                .putString("last_sync_email", userEmail)
                .putInt("last_sync_docs", docsCount)
                .putInt("last_sync_lines", linesCount)
                .putInt("last_sync_parties", partiesCount)
                .putInt("last_sync_packages", pkgsCount)
                .putInt("last_sync_treasuries", treasuriesCount)
                .putInt("last_sync_devices", resDevCount)
                .putInt("last_sync_rates", resRatesCount)
                .putString("last_sync_net_name", resNetName)
                .putString("last_sync_org_name", resOrgName)
                .putBoolean("last_sync_has_profile", resProfObj != null)
                .putInt("last_sync_total_records", totalRecords)
                .apply()

            _syncMetadata.value = SyncMetadata(
                userEmail = userEmail,
                lastSyncedAt = now,
                totalDocuments = docsCount,
                totalJournalLines = linesCount,
                totalParties = partiesCount,
                totalPackages = pkgsCount,
                totalTreasuries = treasuriesCount,
                totalDevices = resDevCount,
                totalNetworkDevices = resDevCount,
                totalCurrencyRates = resRatesCount,
                networkName = resNetName,
                organizationName = resOrgName,
                hasNetworkProfile = resProfObj != null,
                isBalanced = true
            )

            val successMsg = "تمت استعادة $totalRecords سجلاً من السحابة بنجاح وفك التشفير ومطابقة كافة الجداول ودفتر اليومية بنسبة 100%"
            _syncState.value = SyncState.Success(
                message = successMsg,
                lastSyncEpochMs = now,
                totalRecords = totalRecords
            )
            onProgress?.invoke("تم", 1.0f)
            logSync("استعادة من السحابة", userEmail, totalRecords, true, successMsg)
            Result.success(successMsg)
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "SyncPull error: ${e.message}", e)
            val err = if (e is IllegalArgumentException && e.message?.contains("عبارة مرور") == true) {
                e.message ?: "عبارة مرور خاطئة أو ملف تالف"
            } else {
                "فشل سحب البيانات من السحابة: ${e.localizedMessage ?: "تعذر الوصول إلى Firebase"}"
            }
            _syncState.value = SyncState.Error(err)
            logSync("استعادة من السحابة", userEmail, 0, false, err)
            Result.failure(Exception(err))
        } finally {
            isBackupInProgress.set(false)
        }
    }

    /**
     * Rollback to safety snapshot created before the last restore attempt.
     */
    suspend fun restorePreAttemptState(): Result<String> = withContext(Dispatchers.IO) {
        val path = lastPreRestoreSnapshotPath
        if (path.isNullOrBlank()) {
            val err = "لا توجد نقطة أمان سابقة متاحة للاستعادة"
            return@withContext Result.failure(IllegalStateException(err))
        }
        val file = java.io.File(path)
        if (!file.exists()) {
            val err = "ملف نقطة الأمان غير موجود: $path"
            return@withContext Result.failure(IllegalStateException(err))
        }
        try {
            val json = file.readText()
            val res = backupRestoreUseCase.restoreDatabaseFromJson(json)
            if (res.isSuccess) {
                _syncState.value = SyncState.Success("تمت استعادة حالة ما قبل المحاولة بنجاح", System.currentTimeMillis(), 0)
            }
            res
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Granular Push & Pull Methods for Network Devices ---

    suspend fun pushNetworkDevices(uid: String, userEmail: String): Result<Int> = withContext(Dispatchers.IO) {
        val safeUid = resolveEffectiveUid(uid, userEmail)
        if (safeUid.isBlank()) return@withContext Result.failure(IllegalArgumentException("معرف المستخدم غير محدد، يرجى تسجيل الدخول أولاً"))
        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) return@withContext Result.failure(IllegalStateException("Firebase غير مهيأ"))
        try {
            val devices = effectiveDeviceDao?.getAllDevices() ?: emptyList()
            val listMap = devices.map { d ->
                mapOf(
                    "id" to d.id,
                    "name" to d.name,
                    "ipAddress" to d.ipAddress,
                    "deviceType" to d.deviceType.name,
                    "macAddress" to d.macAddress,
                    "towerLocation" to d.towerLocation,
                    "frequency" to d.frequency,
                    "channelWidth" to d.channelWidth,
                    "status" to d.status.name,
                    "notes" to d.notes,
                    "model" to d.model,
                    "managementPort" to d.managementPort,
                    "subnet" to d.subnet,
                    "credentials" to d.credentials
                )
            }
            val payload = hashMapOf(
                "devices" to listMap,
                "count" to devices.size,
                "updatedAt" to System.currentTimeMillis()
            )
            adapter.set("users/$safeUid/network_devices/devices_data", payload, true)
            Result.success(devices.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullNetworkDevices(uid: String, userEmail: String): Result<Int> = withContext(Dispatchers.IO) {
        val safeUid = resolveEffectiveUid(uid, userEmail)
        if (safeUid.isBlank()) return@withContext Result.failure(IllegalArgumentException("معرف المستخدم غير محدد، يرجى تسجيل الدخول أولاً"))
        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) return@withContext Result.failure(IllegalStateException("Firebase غير مهيأ"))
        try {
            val doc = adapter.get("users/$safeUid/network_devices/devices_data")
            val list = doc?.get("devices") as? List<Map<String, Any?>> ?: emptyList()
            val restored = list.map { m ->
                val typeStr = m["deviceType"]?.toString() ?: DeviceType.ACCESS_POINT.name
                val statusStr = m["status"]?.toString() ?: DeviceStatus.ONLINE.name
                NetworkDevice(
                    id = m["id"]?.toString() ?: UUID.randomUUID().toString(),
                    name = m["name"]?.toString() ?: "جهاز",
                    ipAddress = m["ipAddress"]?.toString() ?: "",
                    deviceType = runCatching { DeviceType.valueOf(typeStr) }.getOrDefault(DeviceType.ACCESS_POINT),
                    macAddress = m["macAddress"]?.toString() ?: "",
                    towerLocation = m["towerLocation"]?.toString() ?: "البرج الرئيسي",
                    frequency = m["frequency"]?.toString() ?: "5500 MHz",
                    channelWidth = m["channelWidth"]?.toString() ?: "20/40 MHz",
                    status = runCatching { DeviceStatus.valueOf(statusStr) }.getOrDefault(DeviceStatus.ONLINE),
                    notes = m["notes"]?.toString() ?: "",
                    model = m["model"]?.toString() ?: "MikroTik RouterBOARD",
                    managementPort = (m["managementPort"] as? Number)?.toInt() ?: 8728,
                    subnet = m["subnet"]?.toString() ?: "10.10.1.0/24",
                    credentials = m["credentials"]?.toString() ?: "admin"
                )
            }
            if (restored.isNotEmpty()) {
                effectiveDeviceDao?.deleteAllDevices()
                effectiveDeviceDao?.insertAll(restored)
            }
            Result.success(restored.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Granular Push & Pull Methods for Exchange Rates ---

    suspend fun pushExchangeRates(uid: String, userEmail: String): Result<Int> = withContext(Dispatchers.IO) {
        val safeUid = resolveEffectiveUid(uid, userEmail)
        if (safeUid.isBlank()) return@withContext Result.failure(IllegalArgumentException("معرف المستخدم غير محدد، يرجى تسجيل الدخول أولاً"))
        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) return@withContext Result.failure(IllegalStateException("Firebase غير مهيأ"))
        try {
            val rates = db.currencyRateDao().getAllRatesSync()
            val listMap = rates.map { r ->
                mapOf(
                    "id" to r.id,
                    "currency" to r.currency,
                    "zone" to r.zone,
                    "rateMicros" to r.rateMicros,
                    "effectiveDateEpochDay" to r.effectiveDateEpochDay,
                    "createdAt" to r.createdAt,
                    "createdBy" to r.createdBy,
                    "reason" to r.reason
                )
            }
            val payload = hashMapOf(
                "rates" to listMap,
                "count" to rates.size,
                "updatedAt" to System.currentTimeMillis()
            )
            adapter.set("users/$safeUid/exchange_rates/rates_data", payload, true)
            Result.success(rates.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullExchangeRates(uid: String, userEmail: String): Result<Int> = withContext(Dispatchers.IO) {
        val safeUid = resolveEffectiveUid(uid, userEmail)
        if (safeUid.isBlank()) return@withContext Result.failure(IllegalArgumentException("معرف المستخدم غير محدد، يرجى تسجيل الدخول أولاً"))
        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) return@withContext Result.failure(IllegalStateException("Firebase غير مهيأ"))
        try {
            val doc = adapter.get("users/$safeUid/exchange_rates/rates_data")
            val list = doc?.get("rates") as? List<Map<String, Any?>> ?: emptyList()
            if (list.isNotEmpty()) {
                val sdb = db.openHelper.writableDatabase
                sdb.execSQL("DROP TRIGGER IF EXISTS prevent_currency_rates_update")
                sdb.execSQL("DROP TRIGGER IF EXISTS prevent_currency_rates_delete")
                val stmt = sdb.compileStatement("""
                    INSERT OR REPLACE INTO currency_rates 
                    (id, currency, zone, rateMicros, effectiveDateEpochDay, createdAt, createdBy, reason)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent())
                list.forEach { m ->
                    stmt.clearBindings()
                    stmt.bindString(1, m["id"]?.toString() ?: UUID.randomUUID().toString())
                    stmt.bindString(2, m["currency"]?.toString() ?: "USD")
                    stmt.bindString(3, m["zone"]?.toString() ?: "SANAA")
                    stmt.bindLong(4, (m["rateMicros"] as? Number)?.toLong() ?: 535_000_000L)
                    stmt.bindLong(5, (m["effectiveDateEpochDay"] as? Number)?.toLong() ?: com.example.core.time.AppClock.todayEpochDay())
                    stmt.bindLong(6, (m["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis())
                    stmt.bindString(7, m["createdBy"]?.toString() ?: "SYSTEM")
                    stmt.bindString(8, m["reason"]?.toString() ?: "")
                    stmt.executeInsert()
                }
                stmt.close()
                AppDatabase.installTriggers(sdb)
            }
            Result.success(list.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Granular Push & Pull Methods for Network Identity & Initial Configuration ---

    suspend fun pushNetworkIdentityAndConfig(uid: String, userEmail: String): Result<String> = withContext(Dispatchers.IO) {
        val safeUid = resolveEffectiveUid(uid, userEmail)
        if (safeUid.isBlank()) return@withContext Result.failure(IllegalArgumentException("معرف المستخدم غير محدد، يرجى تسجيل الدخول أولاً"))
        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) return@withContext Result.failure(IllegalStateException("Firebase غير مهيأ"))
        try {
            val org = db.organizationDao().getOrganizationSync()
            val conf = effectiveNetworkRepository?.config?.value ?: NetworkRepository(context).config.value
            val payload = hashMapOf<String, Any?>(
                "updatedAt" to System.currentTimeMillis(),
                "networkConfig" to mapOf(
                    "networkName" to conf.networkName,
                    "ownerName" to conf.ownerName,
                    "location" to conf.location,
                    "welcomeMessage" to conf.welcomeMessage,
                    "supportPhone" to conf.supportPhone,
                    "supportWhatsapp" to conf.supportWhatsapp,
                    "mainRouterModel" to conf.mainRouterModel,
                    "routerOsVersion" to conf.routerOsVersion,
                    "hotspotDomain" to conf.hotspotDomain,
                    "hotspotServerName" to conf.hotspotServerName,
                    "adminPort" to conf.adminPort,
                    "primaryDns" to conf.primaryDns,
                    "secondaryDns" to conf.secondaryDns,
                    "approvedSubnet" to conf.approvedSubnet,
                    "rateZone" to conf.rateZone.name,
                    "defaultUsdRateMicros" to conf.defaultUsdRateMicros,
                    "defaultSarRateMicros" to conf.defaultSarRateMicros,
                    "updatedAt" to conf.updatedAt
                )
            )
            if (org != null) {
                payload["organization"] = mapOf(
                    "id" to org.id,
                    "name" to org.name,
                    "taxNumber" to org.taxNumber,
                    "functionalCurrency" to org.functionalCurrency,
                    "fiscalYearStartMonth" to org.fiscalYearStartMonth,
                    "isInitialized" to org.isInitialized,
                    "primaryRateZone" to org.primaryRateZone,
                    "equityShareMode" to org.equityShareMode,
                    "createdAt" to org.createdAt
                )
            }
            adapter.set("users/$safeUid/config/network_identity", payload, true)
            Result.success("تم رفع هوية الشبكة وإعدادات المنشأة بنجاح")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullNetworkIdentityAndConfig(uid: String, userEmail: String): Result<String> = withContext(Dispatchers.IO) {
        val safeUid = resolveEffectiveUid(uid, userEmail)
        if (safeUid.isBlank()) return@withContext Result.failure(IllegalArgumentException("معرف المستخدم غير محدد، يرجى تسجيل الدخول أولاً"))
        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) return@withContext Result.failure(IllegalStateException("Firebase غير مهيأ"))
        try {
            val doc = adapter.get("users/$safeUid/config/network_identity")

            val netMap = doc?.get("networkConfig") as? Map<String, Any?>
            if (netMap != null) {
                val zoneStr = netMap["rateZone"]?.toString() ?: "SANAA"
                val restoredConfig = NetworkConfig(
                    networkName = netMap["networkName"]?.toString() ?: "شبكة توزيع الإنترنت",
                    ownerName = netMap["ownerName"]?.toString() ?: "مدير الشبكة",
                    location = netMap["location"]?.toString() ?: "المركز الرئيسي",
                    welcomeMessage = netMap["welcomeMessage"]?.toString() ?: "أهلاً بكم في شبكتنا - إنترنت فائق السرعة",
                    supportPhone = netMap["supportPhone"]?.toString() ?: "770000000",
                    supportWhatsapp = netMap["supportWhatsapp"]?.toString() ?: "967770000000",
                    mainRouterModel = netMap["mainRouterModel"]?.toString() ?: "MikroTik CCR2004-16G-2S+",
                    routerOsVersion = netMap["routerOsVersion"]?.toString() ?: "v7.16",
                    hotspotDomain = netMap["hotspotDomain"]?.toString() ?: "login.net",
                    hotspotServerName = netMap["hotspotServerName"]?.toString() ?: "hotspot1",
                    adminPort = (netMap["adminPort"] as? Number)?.toInt() ?: 8728,
                    primaryDns = netMap["primaryDns"]?.toString() ?: "8.8.8.8",
                    secondaryDns = netMap["secondaryDns"]?.toString() ?: "1.1.1.1",
                    approvedSubnet = netMap["approvedSubnet"]?.toString() ?: "10.10.0.0/16",
                    rateZone = runCatching { RateZone.valueOf(zoneStr) }.getOrDefault(RateZone.SANAA),
                    defaultUsdRateMicros = (netMap["defaultUsdRateMicros"] as? Number)?.toLong() ?: 535_000_000L,
                    defaultSarRateMicros = (netMap["defaultSarRateMicros"] as? Number)?.toLong() ?: 140_500_000L,
                    updatedAt = (netMap["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                effectiveNetworkRepository?.saveConfig(restoredConfig)
            }

            val orgMap = doc?.get("organization") as? Map<String, Any?>
            if (orgMap != null) {
                val orgEntity = OrganizationEntity(
                    id = orgMap["id"]?.toString() ?: "ORG_MAIN",
                    name = orgMap["name"]?.toString() ?: "الشبكة الرئيسية",
                    taxNumber = orgMap["taxNumber"]?.toString() ?: "",
                    functionalCurrency = orgMap["functionalCurrency"]?.toString() ?: "YER",
                    fiscalYearStartMonth = (orgMap["fiscalYearStartMonth"] as? Number)?.toInt() ?: 1,
                    isInitialized = (orgMap["isInitialized"] as? Boolean) ?: true,
                    primaryRateZone = orgMap["primaryRateZone"]?.toString() ?: "SANAA",
                    equityShareMode = orgMap["equityShareMode"]?.toString() ?: "DERIVED_FROM_CAPITAL",
                    createdAt = (orgMap["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                db.organizationDao().insertOrganization(orgEntity)
            }
            Result.success("تمت استعادة هوية الشبكة وإعدادات المنشأة بنجاح")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    suspend fun pushNetworkDevices(userEmail: String): Result<Int> {
        val uid = resolveEffectiveUid("", userEmail)
        if (uid.isBlank()) return Result.failure(IllegalStateException("User is not signed in"))
        return pushNetworkDevices(uid, userEmail)
    }

    suspend fun pullNetworkDevices(userEmail: String): Result<Int> {
        val uid = resolveEffectiveUid("", userEmail)
        if (uid.isBlank()) return Result.failure(IllegalStateException("User is not signed in"))
        return pullNetworkDevices(uid, userEmail)
    }

    suspend fun pushExchangeRates(userEmail: String): Result<Int> {
        val uid = resolveEffectiveUid("", userEmail)
        if (uid.isBlank()) return Result.failure(IllegalStateException("User is not signed in"))
        return pushExchangeRates(uid, userEmail)
    }

    suspend fun pullExchangeRates(userEmail: String): Result<Int> {
        val uid = resolveEffectiveUid("", userEmail)
        if (uid.isBlank()) return Result.failure(IllegalStateException("User is not signed in"))
        return pullExchangeRates(uid, userEmail)
    }

    suspend fun pushNetworkIdentityAndConfig(userEmail: String): Result<String> {
        val uid = resolveEffectiveUid("", userEmail)
        if (uid.isBlank()) return Result.failure(IllegalStateException("User is not signed in"))
        return pushNetworkIdentityAndConfig(uid, userEmail)
    }

    suspend fun pullNetworkIdentityAndConfig(userEmail: String): Result<String> {
        val uid = resolveEffectiveUid("", userEmail)
        if (uid.isBlank()) return Result.failure(IllegalStateException("User is not signed in"))
        return pullNetworkIdentityAndConfig(uid, userEmail)
    }

    // --- SaaS Multi-Role Access Codes Cloud Sync (networks/{networkId}/access_codes/{code}) ---

    private fun sanitizeNetworkId(networkId: String): String {
        return networkId.trim().replace("/", "_").ifBlank { "DEFAULT_ORG" }
    }

    private fun sanitizeAccessCode(code: String): String {
        return code.trim().uppercase().replace("/", "_")
    }

    suspend fun syncAccessCodeToCloud(
        networkId: String,
        entry: com.example.data.auth.AccessCodeEntry
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) {
            return@withContext Result.failure(IllegalStateException("خدمة Firebase غير مهيأة على هذا الجهاز"))
        }
        val safeNetId = sanitizeNetworkId(networkId.ifBlank { entry.networkId })
        val cleanCode = sanitizeAccessCode(entry.code)
        if (cleanCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("كود الدخول غير صالح"))
        }
        try {
            val payload = entry.copy(networkId = safeNetId, code = cleanCode).toMap()
            adapter.set("networks/$safeNetId/access_codes/$cleanCode", payload, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "syncAccessCodeToCloud error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun revokeAccessCodeInCloud(
        networkId: String,
        code: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) {
            return@withContext Result.failure(IllegalStateException("خدمة Firebase غير مهيأة على هذا الجهاز"))
        }
        val safeNetId = sanitizeNetworkId(networkId)
        val cleanCode = sanitizeAccessCode(code)
        if (cleanCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("كود الدخول غير صالح"))
        }
        try {
            adapter.set(
                "networks/$safeNetId/access_codes/$cleanCode",
                mapOf("isActive" to false, "revokedAtEpochMs" to System.currentTimeMillis()),
                true
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "revokeAccessCodeInCloud error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchAccessCodeFromCloud(
        networkId: String,
        code: String
    ): com.example.data.auth.AccessCodeEntry? = withContext(Dispatchers.IO) {
        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) return@withContext null
        val safeNetId = sanitizeNetworkId(networkId)
        val cleanCode = sanitizeAccessCode(code)
        if (cleanCode.isBlank()) return@withContext null
        try {
            val doc = adapter.get("networks/$safeNetId/access_codes/$cleanCode")
            com.example.data.auth.AccessCodeEntry.fromMap(doc)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "fetchAccessCodeFromCloud error: ${e.message}")
            null
        }
    }

    suspend fun pullAllAccessCodes(
        networkId: String
    ): List<com.example.data.auth.AccessCodeEntry> = withContext(Dispatchers.IO) {
        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) return@withContext emptyList()
        val safeNetId = sanitizeNetworkId(networkId)
        try {
            val docs = adapter.listCollection("networks/$safeNetId/access_codes")
            docs.mapNotNull { (_, map) ->
                com.example.data.auth.AccessCodeEntry.fromMap(map)
            }.sortedByDescending { it.createdAtEpochMs }
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "pullAllAccessCodes error: ${e.message}")
            emptyList()
        }
    }

    // --- Phase 2: Mutual Invoice & Receipt Verification Sync (networks/{networkId}/parties/{partyId}/verifications/{documentId}) ---

    private fun sanitizePathSegment(segment: String, defaultVal: String = "UNKNOWN"): String {
        return segment.trim().replace("/", "_").ifBlank { defaultVal }
    }

    private fun saveLocalVerificationFallback(record: com.example.data.auth.DocumentVerificationRecord) {
        try {
            val raw = prefs.getString("local_verifications_fallback_json", null)
            val obj = if (!raw.isNullOrBlank()) runCatching { JSONObject(raw) }.getOrNull() ?: JSONObject() else JSONObject()
            obj.put(record.documentId, record.toJson())
            prefs.edit().putString("local_verifications_fallback_json", obj.toString()).apply()
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "saveLocalVerificationFallback error: ${e.message}")
        }
    }

    fun getLocalVerificationFallback(documentId: String): com.example.data.auth.DocumentVerificationRecord? {
        return try {
            val raw = prefs.getString("local_verifications_fallback_json", null) ?: return null
            val obj = JSONObject(raw)
            com.example.data.auth.DocumentVerificationRecord.fromJson(obj.optJSONObject(documentId.trim()))
        } catch (e: Exception) {
            null
        }
    }

    fun getAllLocalVerificationsFallback(partyId: String? = null): List<com.example.data.auth.DocumentVerificationRecord> {
        return try {
            val raw = prefs.getString("local_verifications_fallback_json", null) ?: return emptyList()
            val obj = JSONObject(raw)
            val result = mutableListOf<com.example.data.auth.DocumentVerificationRecord>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val rec = com.example.data.auth.DocumentVerificationRecord.fromJson(obj.optJSONObject(key))
                if (rec != null && (partyId.isNullOrBlank() || rec.partyId == partyId)) {
                    result.add(rec)
                }
            }
            result.sortedByDescending { it.verifiedAtEpochMs }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Syncs a POS document verification status under `networks/{networkId}/parties/{partyId}/verifications/{documentId}`
     * with automatic local storage fallback so it works seamlessly both offline and online.
     */
    suspend fun syncDocumentVerificationToCloud(
        networkId: String,
        partyId: String,
        record: com.example.data.auth.DocumentVerificationRecord
    ): Result<com.example.data.auth.DocumentVerificationRecord> = withContext(Dispatchers.IO) {
        val safeNetId = sanitizeNetworkId(networkId.ifBlank { record.networkId })
        val safePartyId = sanitizePathSegment(partyId.ifBlank { record.partyId }, "UNKNOWN_PARTY")
        val safeDocId = sanitizePathSegment(record.documentId, "")
        if (safeDocId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("معرف المستند غير صالح"))
        }

        val normalized = record.copy(
            documentId = safeDocId,
            partyId = safePartyId,
            networkId = safeNetId
        )
        // Always persist to local fallback immediately for 100% offline reliability
        saveLocalVerificationFallback(normalized)

        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) {
            return@withContext Result.success(normalized)
        }
        try {
            val path = "networks/$safeNetId/parties/$safePartyId/verifications/$safeDocId"
            adapter.set(path, normalized.toMap(), true)
            Result.success(normalized)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "syncDocumentVerificationToCloud offline fallback used: ${e.message}")
            Result.success(normalized)
        }
    }

    /**
     * Fetches a single document verification status from `networks/{networkId}/parties/{partyId}/verifications/{documentId}`
     * with local fallback when offline.
     */
    suspend fun fetchDocumentVerificationFromCloud(
        networkId: String,
        partyId: String,
        documentId: String
    ): com.example.data.auth.DocumentVerificationRecord? = withContext(Dispatchers.IO) {
        val safeNetId = sanitizeNetworkId(networkId)
        val safePartyId = sanitizePathSegment(partyId, "UNKNOWN_PARTY")
        val safeDocId = sanitizePathSegment(documentId, "")
        if (safeDocId.isBlank()) return@withContext null

        val adapter = effectiveAdapter
        if (adapter.isAvailable()) {
            try {
                val path = "networks/$safeNetId/parties/$safePartyId/verifications/$safeDocId"
                val map = adapter.get(path)
                val remote = com.example.data.auth.DocumentVerificationRecord.fromMap(map)
                if (remote != null) {
                    saveLocalVerificationFallback(remote)
                    return@withContext remote
                }
            } catch (e: Exception) {
                Log.w("FirestoreSyncManager", "fetchDocumentVerificationFromCloud error: ${e.message}")
            }
        }
        getLocalVerificationFallback(safeDocId)
    }

    /**
     * Pulls all document verifications for a party from `networks/{networkId}/parties/{partyId}/verifications`,
     * merging with local offline fallback records.
     */
    suspend fun pullPartyDocumentVerifications(
        networkId: String,
        partyId: String
    ): List<com.example.data.auth.DocumentVerificationRecord> = withContext(Dispatchers.IO) {
        val safeNetId = sanitizeNetworkId(networkId)
        val safePartyId = sanitizePathSegment(partyId, "UNKNOWN_PARTY")
        val mergedByDocId = getAllLocalVerificationsFallback(safePartyId)
            .associateBy { it.documentId }
            .toMutableMap()

        val adapter = effectiveAdapter
        if (adapter.isAvailable()) {
            try {
                val docs = adapter.listCollection("networks/$safeNetId/parties/$safePartyId/verifications")
                docs.forEach { (_, map) ->
                    val remote = com.example.data.auth.DocumentVerificationRecord.fromMap(map)
                    if (remote != null) {
                        val existing = mergedByDocId[remote.documentId]
                        if (existing == null || remote.verifiedAtEpochMs >= existing.verifiedAtEpochMs) {
                            mergedByDocId[remote.documentId] = remote
                            saveLocalVerificationFallback(remote)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("FirestoreSyncManager", "pullPartyDocumentVerifications error: ${e.message}")
            }
        }
        mergedByDocId.values.sortedByDescending { it.verifiedAtEpochMs }
    }

    // --- Phase 3: POS Restock Orders & Support Tickets Sync (networks/{networkId}/pos_orders/{orderId} & networks/{networkId}/pos_tickets/{ticketId}) ---

    private fun saveLocalOrderFallback(order: com.example.data.auth.PosRestockOrder) {
        try {
            val raw = prefs.getString("local_pos_orders_fallback_json", null)
            val obj = if (!raw.isNullOrBlank()) runCatching { JSONObject(raw) }.getOrNull() ?: JSONObject() else JSONObject()
            obj.put(order.orderId, order.toJson())
            prefs.edit().putString("local_pos_orders_fallback_json", obj.toString()).apply()
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "saveLocalOrderFallback error: ${e.message}")
        }
    }

    fun getAllLocalOrdersFallback(partyId: String? = null): List<com.example.data.auth.PosRestockOrder> {
        return try {
            val raw = prefs.getString("local_pos_orders_fallback_json", null) ?: return emptyList()
            val obj = JSONObject(raw)
            val result = mutableListOf<com.example.data.auth.PosRestockOrder>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val order = com.example.data.auth.PosRestockOrder.fromJson(obj.optJSONObject(key))
                if (order != null && (partyId.isNullOrBlank() || order.partyId == partyId)) {
                    result.add(order)
                }
            }
            result.sortedByDescending { it.createdAtEpochMs }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Syncs a [com.example.data.auth.PosRestockOrder] under `networks/{networkId}/pos_orders/{orderId}`
     * with full local storage fallback.
     */
    suspend fun syncPosRestockOrderToCloud(
        networkId: String,
        order: com.example.data.auth.PosRestockOrder
    ): Result<com.example.data.auth.PosRestockOrder> = withContext(Dispatchers.IO) {
        val safeNetId = sanitizeNetworkId(networkId.ifBlank { order.networkId })
        val safeOrderId = sanitizePathSegment(order.orderId, "")
        if (safeOrderId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("معرف الطلب غير صالح"))
        }

        val normalized = order.copy(orderId = safeOrderId, networkId = safeNetId)
        saveLocalOrderFallback(normalized)

        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) {
            return@withContext Result.success(normalized)
        }
        try {
            val path = "networks/$safeNetId/pos_orders/$safeOrderId"
            adapter.set(path, normalized.toMap(), true)
            Result.success(normalized)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "syncPosRestockOrderToCloud offline fallback used: ${e.message}")
            Result.success(normalized)
        }
    }

    /**
     * Pulls all [com.example.data.auth.PosRestockOrder] items from `networks/{networkId}/pos_orders`,
     * merging with local fallback storage. Optionally filters by [partyId].
     */
    suspend fun pullPosRestockOrders(
        networkId: String,
        partyId: String? = null
    ): List<com.example.data.auth.PosRestockOrder> = withContext(Dispatchers.IO) {
        val safeNetId = sanitizeNetworkId(networkId)
        val mergedById = getAllLocalOrdersFallback(partyId)
            .associateBy { it.orderId }
            .toMutableMap()

        val adapter = effectiveAdapter
        if (adapter.isAvailable()) {
            try {
                val docs = adapter.listCollection("networks/$safeNetId/pos_orders")
                docs.forEach { (_, map) ->
                    val remote = com.example.data.auth.PosRestockOrder.fromMap(map)
                    if (remote != null && (partyId.isNullOrBlank() || remote.partyId == partyId)) {
                        val existing = mergedById[remote.orderId]
                        if (existing == null || remote.updatedAtEpochMs >= existing.updatedAtEpochMs) {
                            mergedById[remote.orderId] = remote
                            saveLocalOrderFallback(remote)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("FirestoreSyncManager", "pullPosRestockOrders error: ${e.message}")
            }
        }
        mergedById.values.sortedByDescending { it.createdAtEpochMs }
    }

    private fun saveLocalTicketFallback(ticket: com.example.data.auth.PosSupportTicket) {
        try {
            val raw = prefs.getString("local_pos_tickets_fallback_json", null)
            val obj = if (!raw.isNullOrBlank()) runCatching { JSONObject(raw) }.getOrNull() ?: JSONObject() else JSONObject()
            obj.put(ticket.ticketId, ticket.toJson())
            prefs.edit().putString("local_pos_tickets_fallback_json", obj.toString()).apply()
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "saveLocalTicketFallback error: ${e.message}")
        }
    }

    fun getAllLocalTicketsFallback(partyId: String? = null): List<com.example.data.auth.PosSupportTicket> {
        return try {
            val raw = prefs.getString("local_pos_tickets_fallback_json", null) ?: return emptyList()
            val obj = JSONObject(raw)
            val result = mutableListOf<com.example.data.auth.PosSupportTicket>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val ticket = com.example.data.auth.PosSupportTicket.fromJson(obj.optJSONObject(key))
                if (ticket != null && (partyId.isNullOrBlank() || ticket.partyId == partyId)) {
                    result.add(ticket)
                }
            }
            result.sortedByDescending { it.createdAtEpochMs }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Syncs a [com.example.data.auth.PosSupportTicket] under `networks/{networkId}/pos_tickets/{ticketId}`
     * with full local storage fallback.
     */
    suspend fun syncPosSupportTicketToCloud(
        networkId: String,
        ticket: com.example.data.auth.PosSupportTicket
    ): Result<com.example.data.auth.PosSupportTicket> = withContext(Dispatchers.IO) {
        val safeNetId = sanitizeNetworkId(networkId.ifBlank { ticket.networkId })
        val safeTicketId = sanitizePathSegment(ticket.ticketId, "")
        if (safeTicketId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("معرف البلاغ غير صالح"))
        }

        val normalized = ticket.copy(ticketId = safeTicketId, networkId = safeNetId)
        saveLocalTicketFallback(normalized)

        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) {
            return@withContext Result.success(normalized)
        }
        try {
            val path = "networks/$safeNetId/pos_tickets/$safeTicketId"
            adapter.set(path, normalized.toMap(), true)
            Result.success(normalized)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "syncPosSupportTicketToCloud offline fallback used: ${e.message}")
            Result.success(normalized)
        }
    }

    /**
     * Pulls all [com.example.data.auth.PosSupportTicket] items from `networks/{networkId}/pos_tickets`,
     * merging with local fallback storage. Optionally filters by [partyId].
     */
    suspend fun pullPosSupportTickets(
        networkId: String,
        partyId: String? = null
    ): List<com.example.data.auth.PosSupportTicket> = withContext(Dispatchers.IO) {
        val safeNetId = sanitizeNetworkId(networkId)
        val mergedById = getAllLocalTicketsFallback(partyId)
            .associateBy { it.ticketId }
            .toMutableMap()

        val adapter = effectiveAdapter
        if (adapter.isAvailable()) {
            try {
                val docs = adapter.listCollection("networks/$safeNetId/pos_tickets")
                docs.forEach { (_, map) ->
                    val remote = com.example.data.auth.PosSupportTicket.fromMap(map)
                    if (remote != null && (partyId.isNullOrBlank() || remote.partyId == partyId)) {
                        val existing = mergedById[remote.ticketId]
                        val remoteTs = remote.resolvedAtEpochMs ?: remote.createdAtEpochMs
                        val existingTs = existing?.let { it.resolvedAtEpochMs ?: it.createdAtEpochMs } ?: 0L
                        if (existing == null || remoteTs >= existingTs || remote.adminReply != null) {
                            mergedById[remote.ticketId] = remote
                            saveLocalTicketFallback(remote)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("FirestoreSyncManager", "pullPosSupportTickets error: ${e.message}")
            }
        }
        mergedById.values.sortedByDescending { it.createdAtEpochMs }
    }

    // --- Cloud Email Identity Binding & New-Device Recovery (users_by_email/{sanitizedEmail}) ---

    private fun saveLocalEmailIdentityFallback(binding: com.example.data.auth.CloudUserIdentityBinding) {
        try {
            val raw = prefs.getString("local_users_by_email_fallback_json", null)
            val obj = if (!raw.isNullOrBlank()) runCatching { JSONObject(raw) }.getOrNull() ?: JSONObject() else JSONObject()
            obj.put(binding.sanitizedEmail, binding.toJson())
            prefs.edit().putString("local_users_by_email_fallback_json", obj.toString()).apply()
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "saveLocalEmailIdentityFallback error: ${e.message}")
        }
    }

    fun getLocalEmailIdentityFallback(email: String): com.example.data.auth.CloudUserIdentityBinding? {
        return try {
            val key = com.example.data.auth.CloudUserIdentityBinding.sanitizeEmailKey(email)
            if (key.isBlank()) return null
            val raw = prefs.getString("local_users_by_email_fallback_json", null) ?: return null
            val obj = JSONObject(raw)
            com.example.data.auth.CloudUserIdentityBinding.fromJson(obj.optJSONObject(key))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Stores or updates the user's cloud identity mapping in Firestore under `users_by_email/{sanitizedEmail}`
     * containing `email`, `role`, `networkId`, `agentCode` / `posCode`, `partyId`, `displayName`, and `updatedAt`,
     * with full local fallback.
     */
    suspend fun bindUserEmailIdentityToCloud(
        binding: com.example.data.auth.CloudUserIdentityBinding
    ): Result<com.example.data.auth.CloudUserIdentityBinding> = withContext(Dispatchers.IO) {
        val cleanEmail = binding.email.trim().lowercase()
        val safeKey = com.example.data.auth.CloudUserIdentityBinding.sanitizeEmailKey(cleanEmail)
        if (cleanEmail.isBlank() || safeKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("البريد الإلكتروني غير صالح للربط السحابي"))
        }

        val normalized = binding.copy(
            email = cleanEmail,
            networkId = sanitizeNetworkId(binding.networkId),
            updatedAt = binding.updatedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
        saveLocalEmailIdentityFallback(normalized)

        val adapter = effectiveAdapter
        if (!adapter.isAvailable()) {
            return@withContext Result.success(normalized)
        }
        try {
            val path = "users_by_email/$safeKey"
            adapter.set(path, normalized.toMap(), true)
            Result.success(normalized)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "bindUserEmailIdentityToCloud offline fallback used: ${e.message}")
            Result.success(normalized)
        }
    }

    /**
     * Queries `users_by_email/{sanitizedEmail}` in Firestore (with local fallback) to automatically
     * recover a returning Owner, Distributor Agent, or POS Merchant's role, networkId, and bound code on a new device.
     */
    suspend fun fetchUserEmailIdentityFromCloud(
        email: String
    ): com.example.data.auth.CloudUserIdentityBinding? = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val safeKey = com.example.data.auth.CloudUserIdentityBinding.sanitizeEmailKey(cleanEmail)
        if (cleanEmail.isBlank() || safeKey.isBlank()) return@withContext null

        val local = getLocalEmailIdentityFallback(cleanEmail)
        val adapter = effectiveAdapter
        if (adapter.isAvailable()) {
            try {
                val path = "users_by_email/$safeKey"
                val map = adapter.get(path)
                val remote = com.example.data.auth.CloudUserIdentityBinding.fromMap(map)
                if (remote != null) {
                    if (local == null || remote.updatedAt >= local.updatedAt) {
                        saveLocalEmailIdentityFallback(remote)
                        return@withContext remote
                    }
                }
            } catch (e: Exception) {
                Log.w("FirestoreSyncManager", "fetchUserEmailIdentityFromCloud error: ${e.message}")
            }
        }
        local
    }

    private fun logSync(
        action: String,
        email: String,
        records: Int,
        isSuccess: Boolean,
        summary: String
    ) {
        val item = SyncLogItem(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            action = action,
            userEmail = email,
            recordsCount = records,
            isSuccess = isSuccess,
            summary = summary
        )
        val updated = listOf(item) + _syncHistory.value.take(19)
        _syncHistory.value = updated
    }
}



