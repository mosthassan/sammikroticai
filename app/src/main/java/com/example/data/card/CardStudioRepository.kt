package com.example.data.card

import android.content.Context
import android.util.AtomicFile
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.core.model.UuidUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class BatchStockInResult(
    val packageId: String,
    val packageName: String,
    val quantity: Int,
    val alreadyAdded: Boolean = false
)

/**
 * Offline-first repository for Hotspot Card Templates and Batches.
 * Stores templates and batches with rich presets matching SamMikrotik Web Engine.
 */
class CardStudioRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val templatesFile = File(context.filesDir, "card_templates.json")
    private val batchesFile = File(context.filesDir, "card_batches.json")
    private val injectionJournalFile = File(context.filesDir, "card_batch_injection_state.log")
    private val batchMutex = Mutex()
    private val injectedIdsByBatch = mutableMapOf<String, MutableSet<String>>()
    private val database = AppDatabase.getInstance(context)

    private val _templates = MutableStateFlow<List<CardTemplate>>(emptyList())
    val templates: Flow<List<CardTemplate>> = _templates.asStateFlow()

    private val _batches = MutableStateFlow<List<CardBatch>>(emptyList())
    val batches: Flow<List<CardBatch>> = _batches.asStateFlow()

    private val secureRandom = SecureRandom()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            if (templatesFile.exists()) {
                val json = templatesFile.readText()
                val type = Types.newParameterizedType(List::class.java, CardTemplate::class.java)
                val adapter = moshi.adapter<List<CardTemplate>>(type)
                val loaded = adapter.fromJson(json) ?: getInitialTemplates()
                _templates.value = if (loaded.isEmpty()) getInitialTemplates() else loaded
            } else {
                _templates.value = getInitialTemplates()
                saveTemplatesSync(_templates.value)
            }

            if (batchesFile.exists()) {
                val json = AtomicFile(batchesFile).openRead().bufferedReader().use { it.readText() }
                val type = Types.newParameterizedType(List::class.java, CardBatch::class.java)
                val adapter = moshi.adapter<List<CardBatch>>(type)
                _batches.value = adapter.fromJson(json) ?: emptyList()
            }
            if (injectionJournalFile.exists()) {
                val injectedByBatch = injectionJournalFile.useLines { lines ->
                    lines.mapNotNull { line ->
                        val separator = line.indexOf('\t')
                        if (separator <= 0 || separator == line.lastIndex) null
                        else line.substring(0, separator) to line.substring(separator + 1)
                    }.groupBy({ it.first }, { it.second })
                }
                injectedByBatch.forEach { (batchId, ids) ->
                    injectedIdsByBatch.getOrPut(batchId) { linkedSetOf() }.addAll(ids)
                }
                _batches.value = _batches.value.map { batch ->
                    val injectedIds = (batch.injectedCardIds + injectedIdsByBatch[batch.id].orEmpty()).distinct()
                    batch.copy(
                        injectedCardIds = injectedIds,
                        injectionStatus = when {
                            injectedIds.size >= batch.cardCount -> CardInjectionStatus.INJECTED
                            injectedIds.isNotEmpty() -> CardInjectionStatus.PARTIAL
                            else -> batch.injectionStatus
                        }
                    )
                }
            }
        } catch (_: Exception) {
            _templates.value = getInitialTemplates()
        }
    }

    suspend fun saveTemplate(template: CardTemplate) = withContext(ioDispatcher) {
        val current = _templates.value.toMutableList()
        val index = current.indexOfFirst { it.id == template.id }
        if (index >= 0) {
            current[index] = template
        } else {
            current.add(0, template)
        }
        _templates.value = current
        saveTemplatesSync(current)
    }

    suspend fun resetToDefaultTemplates() = withContext(ioDispatcher) {
        val defaults = getInitialTemplates()
        _templates.value = defaults
        saveTemplatesSync(defaults)
    }

    suspend fun deleteTemplate(templateId: String) = withContext(ioDispatcher) {
        val current = _templates.value.filterNot { it.id == templateId }
        _templates.value = current
        saveTemplatesSync(current)
    }

    suspend fun generateBatch(
        template: CardTemplate,
        profileName: String,
        count: Int,
        prefix: String,
        usernameLength: Int,
        passwordPattern: PasswordPattern,
        characterSet: CardCharacterSet = CardCharacterSet.NUMBERS_ONLY,
        targetMikrotikHost: String = "",
        internalNote: String = "",
        injectionTarget: CardInjectionTarget = CardInjectionTarget.HOTSPOT
    ): CardBatch = withContext(ioDispatcher) {
        batchMutex.withLock {
        val nextBatchNum = (_batches.value.maxOfOrNull { it.batchNumber } ?: 0L) + 1L
        val batchId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val profile = profileName.ifBlank { "default" }
        val packageTitle = template.targetPackageName.ifBlank { template.name.ifBlank { profile } }
        val date = java.text.SimpleDateFormat("dd/MM", Locale.US).format(java.util.Date(now))
        val displayTitle = "$packageTitle (${template.quotaText}) - $date #$nextBatchNum"

        val cards = mutableListOf<GeneratedCard>()
        val existingUsernames = mutableSetOf<String>()

        for (i in 1..count) {
            val username = generateUniqueUsername(prefix, usernameLength, existingUsernames, characterSet)
            existingUsernames.add(username)

            val password = when (passwordPattern) {
                PasswordPattern.SAME_AS_USERNAME -> username
                PasswordPattern.NUMBERS_ONLY -> generateRandomNumeric(6)
                PasswordPattern.NO_PASSWORD -> ""
                PasswordPattern.ALPHANUMERIC -> generateRandomAlphanumeric(6)
            }

            val serialNumber = String.format(java.util.Locale.US, "%06d", i)

            // Construct captive portal instant login URL
            val baseUrl = template.hotspotLoginUrl.trimEnd('/')
            val loginUrl = "$baseUrl?username=$username&password=$password"

            cards.add(
                GeneratedCard(
                    id = UUID.randomUUID().toString(),
                    serialNumber = serialNumber,
                    username = username,
                    password = password,
                    priceText = template.priceText,
                    quotaText = template.quotaText,
                    validityText = template.validityText,
                    networkName = template.networkName,
                    supportPhone = template.supportPhone,
                    loginUrlWithCredentials = loginUrl,
                    batchId = batchId
                )
            )
        }

        val batch = CardBatch(
            id = batchId,
            batchNumber = nextBatchNum,
            batchTitle = displayTitle,
            displayTitle = displayTitle,
            mikrotikComment = createMikrotikBatchComment(profile, now, nextBatchNum),
            internalNote = internalNote,
            injectionTarget = injectionTarget,
            templateId = template.id,
            templateSnapshot = template,
            profileName = profile,
            cardCount = count,
            prefix = prefix,
            usernameLength = usernameLength,
            passwordPattern = passwordPattern,
            cards = cards,
            createdAt = now,
            isSyncedToMikrotik = false,
            targetMikrotikHost = targetMikrotikHost
        )

        val updatedBatches = listOf(batch) + _batches.value
        _batches.value = updatedBatches
        saveBatchesSync(updatedBatches)

        batch
        }
    }

    suspend fun deleteBatch(batchId: String) = withContext(ioDispatcher) {
        batchMutex.withLock {
            val updated = _batches.value.filterNot { it.id == batchId }
            _batches.value = updated
            saveBatchesSync(updated)
            compactInjectionJournal()
        }
    }

    suspend fun markBatchSynced(batchId: String, host: String) = withContext(ioDispatcher) {
        batchMutex.withLock {
            val updated = _batches.value.map {
                if (it.id == batchId) {
                    it.copy(
                        isSyncedToMikrotik = true,
                        targetMikrotikHost = host,
                        injectionStatus = CardInjectionStatus.INJECTED,
                        injectionCompletedAt = System.currentTimeMillis()
                    )
                } else it
            }
            _batches.value = updated
            saveBatchesSync(updated)
            compactInjectionJournal()
        }
    }

    suspend fun markBatchInjectionStarted(batchId: String, host: String) = withContext(ioDispatcher) {
        batchMutex.withLock {
            updateBatch(batchId) {
                it.copy(injectionStatus = CardInjectionStatus.INJECTING, targetMikrotikHost = host)
            }
        }
    }

    suspend fun markBatchCardsInjected(batchId: String, cardIds: List<String>) = withContext(ioDispatcher) {
        if (cardIds.isEmpty()) return@withContext
        batchMutex.withLock {
            val batch = _batches.value.firstOrNull { it.id == batchId } ?: error("دفعة الكروت غير موجودة")
            val knownIds = injectedIdsByBatch.getOrPut(batchId) { batch.injectedCardIds.toMutableSet() }
            val newIds = cardIds.filterNot { it in knownIds }.distinct()
            if (newIds.isEmpty()) return@withLock
            FileOutputStream(injectionJournalFile, true).use { output ->
                newIds.forEach { id ->
                    output.write("$batchId\t$id\n".toByteArray(Charsets.UTF_8))
                }
                output.fd.sync()
            }
            knownIds.addAll(newIds)
        }
    }

    suspend fun finishBatchInjection(batchId: String, host: String, failed: Boolean) = withContext(ioDispatcher) {
        batchMutex.withLock {
            val updated = _batches.value.map { batch ->
                if (batch.id != batchId) return@map batch
                val injectedIds = (batch.injectedCardIds + injectedIdsByBatch[batchId].orEmpty()).distinct()
                val injected = injectedIds.size.coerceAtMost(batch.cardCount)
                val complete = injected == batch.cardCount
                batch.copy(
                    injectedCardIds = injectedIds,
                    injectionStatus = when {
                        complete -> CardInjectionStatus.INJECTED
                        injected > 0 -> CardInjectionStatus.PARTIAL
                        failed -> CardInjectionStatus.PENDING
                        else -> CardInjectionStatus.PENDING
                    },
                    isSyncedToMikrotik = complete,
                    targetMikrotikHost = host,
                    injectionCompletedAt = if (complete) System.currentTimeMillis() else null
                )
            }
            _batches.value = updated
            saveBatchesSync(updated)
            compactInjectionJournal()
        }
    }

    suspend fun addBatchStockToInventory(batchId: String): BatchStockInResult = withContext(ioDispatcher) {
        batchMutex.withLock {
            val batch = _batches.value.firstOrNull { it.id == batchId } ?: error("دفعة الكروت غير موجودة")
            if (batch.isStockAdded) {
                return@withLock BatchStockInResult(
                    batch.inventoryPackageId.orEmpty(),
                    batch.templateSnapshot.targetPackageName.ifBlank { batch.profileName },
                    batch.injectedCardIds.size,
                    alreadyAdded = true
                )
            }
            require(batch.injectionStatus == CardInjectionStatus.INJECTED) {
                "لا يمكن إضافة مخزون قبل اكتمال حقن الدفعة"
            }
            val quantity = batch.injectedCardIds.size.coerceAtMost(batch.cardCount)
            require(quantity > 0) { "لا توجد كروت مؤكدة الحقن لإضافتها إلى المخزون" }

            val stockDocId = "CARD_BATCH_INJECTION:$batchId"
            val inventory = database.withTransaction {
                val dao = database.cardPackageDao()
                val existingMovement = dao.getStockMovementByDocId(stockDocId)
                if (existingMovement != null) {
                    require(existingMovement.quantity == quantity) {
                        "كمية حركة المخزون المسجلة لا تطابق عدد كروت الدفعة"
                    }
                    val existingPackage = dao.getPackageById(existingMovement.packageId)
                        ?: error("تعذر العثور على الباقة المرتبطة بحركة مخزون الدفعة")
                    BatchStockInResult(
                        existingPackage.id,
                        existingPackage.name,
                        existingMovement.quantity,
                        alreadyAdded = true
                    )
                } else {
                    val allPackages = dao.getAllPackagesSync()
                    val packageName = batch.templateSnapshot.targetPackageName
                        .ifBlank { batch.templateSnapshot.name.ifBlank { batch.profileName } }
                    val inventoryPackage = allPackages.firstOrNull {
                        it.profileName.equals(batch.profileName, ignoreCase = true)
                    } ?: allPackages.firstOrNull { it.name.equals(packageName, ignoreCase = true) }
                        ?: CardPackageEntity(
                            id = UuidUtils.newTimeOrderedId(),
                            name = packageName,
                            durationOrQuota = formatDurationOrQuota(
                                batch.getEffectivePlan().dataQuotaMb,
                                batch.getEffectivePlan().uptimeHours,
                                batch.getEffectivePlan().validityDays,
                                batch.profileName
                            ),
                            wholesalePriceMinor = batch.getEffectivePlan().wholesalePriceMinor,
                            retailPriceMinor = batch.getEffectivePlan().retailPriceMinor
                                .takeIf { it > 0L } ?: parsePriceMinor(batch.templateSnapshot.priceText)
                        ).also { dao.insertPackage(it) }

                    dao.insertStockMovement(
                        StockMovementEntity(
                            id = UuidUtils.newTimeOrderedId(),
                            packageId = inventoryPackage.id,
                            docId = stockDocId,
                            type = "RECEIVE",
                            quantity = quantity,
                            movementDateEpochDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis()),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    BatchStockInResult(inventoryPackage.id, inventoryPackage.name, quantity)
                }
            }

            val updated = _batches.value.map {
                if (it.id == batchId) it.copy(isStockAdded = true, inventoryPackageId = inventory.packageId) else it
            }
            _batches.value = updated
            saveBatchesSync(updated)
            inventory
        }
    }

    suspend fun markBatchPrinted(batchId: String) = withContext(ioDispatcher) {
        batchMutex.withLock {
            updateBatch(batchId) {
                if (it.printStatus == CardPrintStatus.PRINTED) it
                else it.copy(printStatus = CardPrintStatus.PRINTED, printedAt = System.currentTimeMillis())
            }
        }
    }

    private suspend fun updateBatch(batchId: String, transform: (CardBatch) -> CardBatch) {
        val current = _batches.value.map { batch ->
            val journalIds = injectedIdsByBatch[batch.id].orEmpty()
            if (journalIds.isEmpty()) batch
            else {
                val injectedIds = (batch.injectedCardIds + journalIds).distinct()
                batch.copy(
                    injectedCardIds = injectedIds,
                    injectionStatus = when {
                        injectedIds.size >= batch.cardCount -> CardInjectionStatus.INJECTED
                        batch.injectionStatus == CardInjectionStatus.INJECTING -> CardInjectionStatus.PARTIAL
                        else -> batch.injectionStatus
                    }
                )
            }
        }
        require(current.any { it.id == batchId }) { "دفعة الكروت غير موجودة" }
        val updated = current.map { if (it.id == batchId) transform(it) else it }
        _batches.value = updated
        saveBatchesSync(updated)
        compactInjectionJournal()
    }

    private fun compactInjectionJournal() {
        if (injectionJournalFile.exists() && !injectionJournalFile.delete()) {
            error("تعذر تنظيف سجل تقدم حقن الدفعات")
        }
        injectedIdsByBatch.clear()
    }

    private fun parsePriceMinor(priceText: String): Long {
        val wholeCurrency = priceText.filter { it in '0'..'9' }.toLongOrNull() ?: 0L
        return Math.multiplyExact(wholeCurrency, 100L)
    }

    private fun generateUniqueUsername(
        prefix: String,
        length: Int,
        existing: Set<String>,
        characterSet: CardCharacterSet = CardCharacterSet.NUMBERS_ONLY
    ): String {
        val cleanPrefix = prefix.trim()
        val randomLen = (length - cleanPrefix.length).coerceAtLeast(3)
        var candidate: String
        do {
            val randomPart = if (characterSet == CardCharacterSet.NUMBERS_ONLY) {
                generateRandomNumeric(randomLen)
            } else {
                generateRandomAlphanumeric(randomLen)
            }
            candidate = "$cleanPrefix$randomPart"
        } while (existing.contains(candidate))
        return candidate
    }

    private fun generateRandomNumeric(length: Int): String {
        val chars = "0123456789"
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(chars[secureRandom.nextInt(chars.length)])
        }
        return sb.toString()
    }

    private fun generateRandomAlphanumeric(length: Int): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(chars[secureRandom.nextInt(chars.length)])
        }
        return sb.toString()
    }

    private fun saveTemplatesSync(list: List<CardTemplate>) {
        try {
            val type = Types.newParameterizedType(List::class.java, CardTemplate::class.java)
            val adapter = moshi.adapter<List<CardTemplate>>(type)
            templatesFile.writeText(adapter.toJson(list))
        } catch (_: Exception) {}
    }

    private fun saveBatchesSync(list: List<CardBatch>) {
        val type = Types.newParameterizedType(List::class.java, CardBatch::class.java)
        val adapter = moshi.adapter<List<CardBatch>>(type)
        val atomicFile = AtomicFile(batchesFile)
        val output = atomicFile.startWrite()
        try {
            output.write(adapter.toJson(list).toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(output)
        } catch (error: Exception) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    fun getInitialTemplates(): List<CardTemplate> {
        return listOf(
            CardTemplate(
                id = "tpl_cyber_neon_2026",
                name = "سايبر نيون 2026 (Cyber Neon)",
                networkName = "شبكة سام ميكروتك",
                priceText = "500 ر.ي",
                quotaText = "2 جيجابايت",
                validityText = "صلاحية 7 أيام",
                headline = "إنترنت فائق السرعة",
                footerNote = "امسح الرمز أو ادخل الكود للاتصال الفوري",
                supportPhone = "770446040",
                hotspotLoginUrl = "http://10.50.7.1/login",
                primaryColorHex = 0xFF00F0FF,
                backgroundColorHex = 0xFF080318,
                gradientEndColorHex = 0xFF12062B,
                textColorHex = 0xFFFFFFFF,
                accentColorHex = 0xFFFF007F,
                badgeBgColorHex = 0xFF00F0FF,
                badgeTextColorHex = 0xFF000000,
                codeBoxStyle = CodeBoxStyle.PILL_BADGE,
                themePreset = CardThemePreset.CYBER_NEON
            ),
            CardTemplate(
                id = "tpl_royal_gold_vip",
                name = "ذهب ملكي VIP (Royal Gold)",
                networkName = "الشبكة الذهبية VIP",
                priceText = "1,000 ر.ي",
                quotaText = "5 جيجابايت",
                validityText = "صلاحية 15 يوم",
                headline = "باقة كبار الشخصيات",
                footerNote = "شكراً لاختياركم شبكتنا المتميزة",
                supportPhone = "770446040",
                hotspotLoginUrl = "http://wifi.net/login",
                primaryColorHex = 0xFFEAB308,
                backgroundColorHex = 0xFF0C0A09,
                gradientEndColorHex = 0xFF1C1917,
                textColorHex = 0xFFFEF08A,
                accentColorHex = 0xFFCA8A04,
                badgeBgColorHex = 0xFFEAB308,
                badgeTextColorHex = 0xFF000000,
                codeBoxStyle = CodeBoxStyle.MODERN_BOX,
                themePreset = CardThemePreset.ROYAL_GOLD
            ),
            CardTemplate(
                id = "tpl_corporate_blue",
                name = "أزرق فايبر كلاسيك (Fiber Blue)",
                networkName = "سام نت للإنترنت اللاسلكي",
                priceText = "300 ر.ي",
                quotaText = "1.5 جيجابايت",
                validityText = "صلاحية 3 أيام",
                headline = "اتصال مستقر بلا انقطاع",
                footerNote = "خدمة العملاء متاحة 24/7",
                supportPhone = "770446040",
                hotspotLoginUrl = "http://10.50.7.1/login",
                primaryColorHex = 0xFF38BDF8,
                backgroundColorHex = 0xFF071322,
                gradientEndColorHex = 0xFF0F2744,
                textColorHex = 0xFFFFFFFF,
                accentColorHex = 0xFF0284C7,
                badgeBgColorHex = 0xFF0284C7,
                badgeTextColorHex = 0xFFFFFFFF,
                codeBoxStyle = CodeBoxStyle.TICKET_DASHED,
                themePreset = CardThemePreset.CORPORATE_BLUE
            ),
            CardTemplate(
                id = "tpl_clean_print_economy",
                name = "أبيض ناصع 300DPI (موفر للحبر)",
                networkName = "شبكة الواي فاي العامة",
                priceText = "200 ر.ي",
                quotaText = "1 جيجابايت",
                validityText = "صلاحية 24 ساعة",
                headline = "كرت إنترنت يومي اقتصادي",
                footerNote = "امسح الرمز بكاميرا الهاتف",
                supportPhone = "770446040",
                hotspotLoginUrl = "http://10.50.7.1/login",
                primaryColorHex = 0xFF0284C7,
                backgroundColorHex = 0xFFFFFFFF,
                gradientEndColorHex = 0xFFF8FAFC,
                textColorHex = 0xFF0F172A,
                accentColorHex = 0xFF64748B,
                badgeBgColorHex = 0xFF0F172A,
                badgeTextColorHex = 0xFFFFFFFF,
                codeBoxStyle = CodeBoxStyle.MODERN_BOX,
                themePreset = CardThemePreset.CLEAN_WHITE
            ),
            CardTemplate(
                id = "tpl_scratch_card",
                name = "كرت كشط أمني (Scratch Card)",
                networkName = "بطاقة شحن الرصيد الفوري",
                priceText = "500 ر.ي",
                quotaText = "2 جيجابايت",
                validityText = "صلاحية 7 أيام",
                headline = "امسح الشريط برفق للكشف عن الكود",
                footerNote = "لا تقبل البطاقة إذا كان الشريط مكشوطاً",
                supportPhone = "770446040",
                hotspotLoginUrl = "http://10.50.7.1/login",
                primaryColorHex = 0xFFF59E0B,
                backgroundColorHex = 0xFF1E293B,
                gradientEndColorHex = 0xFF0F172A,
                textColorHex = 0xFFFFFFFF,
                accentColorHex = 0xFFD97706,
                badgeBgColorHex = 0xFFF59E0B,
                badgeTextColorHex = 0xFF000000,
                codeBoxStyle = CodeBoxStyle.SCRATCH_FOIL,
                themePreset = CardThemePreset.SCRATCH_CARD
            )
        )
    }

    companion object {
        @Volatile
        private var instance: CardStudioRepository? = null

        fun getInstance(context: Context): CardStudioRepository {
            return instance ?: synchronized(this) {
                instance ?: CardStudioRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
