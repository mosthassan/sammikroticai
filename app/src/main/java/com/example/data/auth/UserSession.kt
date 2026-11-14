package com.example.data.auth

import org.json.JSONObject
import java.security.SecureRandom

/**
 * SaaS Multi-Role Access Control (RBAC) Roles for SamMikrotik.
 *
 * - [NETWORK_OWNER]: Full 100% access to MikroTik router configuration, Card Studio,
 *   Inventory, Financial Reports, Purchases/Assets, Partners/Equity, and Team Management.
 * - [DISTRIBUTOR_AGENT]: Field distributor/agent with customizable permissions granted by the Network Owner.
 * - [POS_MERCHANT]: Point of Sale / Grocery owner (`صاحب نقطة بيع / بقالة`) with strictly Read-Only
 *   access to their own party statement (`PartyLedger`), invoice/receipt history, and restock/support requests.
 */
enum class AppUserRole(
    val titleAr: String,
    val badgeAr: String,
    val subtitleAr: String
) {
    NETWORK_OWNER(
        titleAr = "مالك الشبكة",
        badgeAr = "👑 مالك الشبكة",
        subtitleAr = "صلاحيات كاملة لإدارة الميكروتك والمحاسبة والوكلاء"
    ),
    DISTRIBUTOR_AGENT(
        titleAr = "وكيل / موزع",
        badgeAr = "🚚 وكيل ميداني",
        subtitleAr = "صلاحيات التوزيع الميداني والتحصيل وفق تفويض المالك"
    ),
    POS_MERCHANT(
        titleAr = "نقطة بيع (بقالة)",
        badgeAr = "🏪 نقطة بيع (بقالة)",
        subtitleAr = "بوابة الشفافية الرقمية لمتابعة كشف الحساب والفواتير"
    );

    companion object {
        fun fromNameOrDefault(name: String?): AppUserRole {
            if (name.isNullOrBlank()) return NETWORK_OWNER
            return entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } ?: NETWORK_OWNER
        }
    }
}

/**
 * Granular permissions controlled by the [AppUserRole.NETWORK_OWNER] for [AppUserRole.DISTRIBUTOR_AGENT].
 *
 * Hard Restriction: [AppUserRole.DISTRIBUTOR_AGENT] and [AppUserRole.POS_MERCHANT] can NEVER access
 * RouterOS IP/API settings, WiFi Spectrum, Partner Equity, Capital, Net Profit Reports, or Purchases/Assets.
 */
data class AgentPermissions(
    val canCreateSalesInvoices: Boolean = true,
    val canCollectReceipts: Boolean = true,
    val canCreateDisbursements: Boolean = false,
    val canViewAssignedStock: Boolean = true,
    val canGeneratePosCodes: Boolean = true
) {
    fun toMap(): Map<String, Any> = mapOf(
        "canCreateSalesInvoices" to canCreateSalesInvoices,
        "canCollectReceipts" to canCollectReceipts,
        "canCreateDisbursements" to canCreateDisbursements,
        "canViewAssignedStock" to canViewAssignedStock,
        "canGeneratePosCodes" to canGeneratePosCodes
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("canCreateSalesInvoices", canCreateSalesInvoices)
        put("canCollectReceipts", canCollectReceipts)
        put("canCreateDisbursements", canCreateDisbursements)
        put("canViewAssignedStock", canViewAssignedStock)
        put("canGeneratePosCodes", canGeneratePosCodes)
    }

    companion object {
        val FULL_OWNER = AgentPermissions(
            canCreateSalesInvoices = true,
            canCollectReceipts = true,
            canCreateDisbursements = true,
            canViewAssignedStock = true,
            canGeneratePosCodes = true
        )

        val READ_ONLY_NONE = AgentPermissions(
            canCreateSalesInvoices = false,
            canCollectReceipts = false,
            canCreateDisbursements = false,
            canViewAssignedStock = false,
            canGeneratePosCodes = false
        )

        fun fromMap(map: Map<String, Any?>?): AgentPermissions {
            if (map == null) return AgentPermissions()
            return AgentPermissions(
                canCreateSalesInvoices = (map["canCreateSalesInvoices"] as? Boolean) ?: true,
                canCollectReceipts = (map["canCollectReceipts"] as? Boolean) ?: true,
                canCreateDisbursements = (map["canCreateDisbursements"] as? Boolean) ?: false,
                canViewAssignedStock = (map["canViewAssignedStock"] as? Boolean) ?: true,
                canGeneratePosCodes = (map["canGeneratePosCodes"] as? Boolean) ?: true
            )
        }

        fun fromJson(json: JSONObject?): AgentPermissions {
            if (json == null) return AgentPermissions()
            return AgentPermissions(
                canCreateSalesInvoices = json.optBoolean("canCreateSalesInvoices", true),
                canCollectReceipts = json.optBoolean("canCollectReceipts", true),
                canCreateDisbursements = json.optBoolean("canCreateDisbursements", false),
                canViewAssignedStock = json.optBoolean("canViewAssignedStock", true),
                canGeneratePosCodes = json.optBoolean("canGeneratePosCodes", true)
            )
        }
    }
}

/**
 * Strict, deterministic Role Access Policy enforcing hard restrictions and granular agent flags.
 */
object RoleAccessPolicy {
    // Hard-restricted owner-only capabilities (NEVER accessible to DISTRIBUTOR_AGENT or POS_MERCHANT)
    fun canAccessRouterOsSettings(role: AppUserRole): Boolean = role == AppUserRole.NETWORK_OWNER
    fun canAccessWifiSpectrum(role: AppUserRole): Boolean = role == AppUserRole.NETWORK_OWNER
    fun canAccessPartnerEquityAndCapital(role: AppUserRole): Boolean = role == AppUserRole.NETWORK_OWNER
    fun canAccessNetProfitReports(role: AppUserRole): Boolean = role == AppUserRole.NETWORK_OWNER
    fun canAccessPurchasesAndAssets(role: AppUserRole): Boolean = role == AppUserRole.NETWORK_OWNER
    fun canAccessCardStudioGeneration(role: AppUserRole): Boolean = role == AppUserRole.NETWORK_OWNER
    fun canAccessTeamAndCodesManager(role: AppUserRole): Boolean = role == AppUserRole.NETWORK_OWNER
    fun canAccessAuditAndInspection(role: AppUserRole): Boolean = role == AppUserRole.NETWORK_OWNER

    // Granular operational capabilities
    fun canCreateSalesInvoices(role: AppUserRole, permissions: AgentPermissions = AgentPermissions()): Boolean =
        when (role) {
            AppUserRole.NETWORK_OWNER -> true
            AppUserRole.DISTRIBUTOR_AGENT -> permissions.canCreateSalesInvoices
            AppUserRole.POS_MERCHANT -> false
        }

    fun canCollectReceipts(role: AppUserRole, permissions: AgentPermissions = AgentPermissions()): Boolean =
        when (role) {
            AppUserRole.NETWORK_OWNER -> true
            AppUserRole.DISTRIBUTOR_AGENT -> permissions.canCollectReceipts
            AppUserRole.POS_MERCHANT -> false
        }

    fun canCreateDisbursements(role: AppUserRole, permissions: AgentPermissions = AgentPermissions()): Boolean =
        when (role) {
            AppUserRole.NETWORK_OWNER -> true
            AppUserRole.DISTRIBUTOR_AGENT -> permissions.canCreateDisbursements
            AppUserRole.POS_MERCHANT -> false
        }

    fun canViewAssignedStock(role: AppUserRole, permissions: AgentPermissions = AgentPermissions()): Boolean =
        when (role) {
            AppUserRole.NETWORK_OWNER -> true
            AppUserRole.DISTRIBUTOR_AGENT -> permissions.canViewAssignedStock
            AppUserRole.POS_MERCHANT -> false
        }

    fun canGeneratePosCodes(role: AppUserRole, permissions: AgentPermissions = AgentPermissions()): Boolean =
        when (role) {
            AppUserRole.NETWORK_OWNER -> true
            AppUserRole.DISTRIBUTOR_AGENT -> permissions.canGeneratePosCodes
            AppUserRole.POS_MERCHANT -> false
        }

    fun canViewPartyStatement(
        role: AppUserRole,
        targetPartyId: String,
        boundPartyId: String?
    ): Boolean = when (role) {
        AppUserRole.NETWORK_OWNER -> true
        AppUserRole.DISTRIBUTOR_AGENT -> true
        AppUserRole.POS_MERCHANT -> !boundPartyId.isNullOrBlank() && boundPartyId == targetPartyId
    }
}

/**
 * Invite & Transparency Access Code model bound to an Agent (`AGT-XXXX`) or POS Merchant (`POS-XXXXXX`).
 * Synced to Firestore under `networks/{networkId}/access_codes/{code}` and cached locally.
 */
data class AccessCodeEntry(
    val code: String,
    val role: AppUserRole,
    val networkId: String,
    val networkName: String,
    val agentId: String? = null,
    val agentName: String = "",
    val agentPin: String = "",
    val agentPhone: String = "",
    val partyId: String? = null,
    val partyName: String = "",
    val partyPhone: String = "",
    val permissions: AgentPermissions = AgentPermissions(),
    val isActive: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "code" to code,
        "role" to role.name,
        "networkId" to networkId,
        "networkName" to networkName,
        "agentId" to agentId,
        "agentName" to agentName,
        "agentPin" to agentPin,
        "agentPhone" to agentPhone,
        "partyId" to partyId,
        "partyName" to partyName,
        "partyPhone" to partyPhone,
        "permissions" to permissions.toMap(),
        "isActive" to isActive,
        "createdAtEpochMs" to createdAtEpochMs
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("code", code)
        put("role", role.name)
        put("networkId", networkId)
        put("networkName", networkName)
        put("agentId", agentId ?: "")
        put("agentName", agentName)
        put("agentPin", agentPin)
        put("agentPhone", agentPhone)
        put("partyId", partyId ?: "")
        put("partyName", partyName)
        put("partyPhone", partyPhone)
        put("permissions", permissions.toJson())
        put("isActive", isActive)
        put("createdAtEpochMs", createdAtEpochMs)
    }

    companion object {
        private const val CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        private val secureRandom = SecureRandom()

        /**
         * Generates a unique Agent Invite Code in `AGT-XXXX` format (or `AGT-XXXX-XXXX` if 8 chars requested).
         */
        fun generateAgentCodeFormat(existingCodes: Set<String> = emptySet(), suffixLength: Int = 4): String {
            val cleanExisting = existingCodes.map { it.trim().uppercase() }.toSet()
            repeat(50) {
                val part1 = buildString(4) {
                    repeat(4) { append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)]) }
                }
                val candidate = if (suffixLength > 4) {
                    val part2 = buildString(4) {
                        repeat(4) { append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)]) }
                    }
                    "AGT-$part1-$part2"
                } else {
                    "AGT-$part1"
                }
                if (candidate !in cleanExisting) return candidate
            }
            return "AGT-${System.nanoTime().toString(36).uppercase().takeLast(4).padStart(4, 'X')}"
        }

        /**
         * Generates a unique 6-character POS Digital Transparency Code (`POS-XXXXXX`).
         */
        fun generatePosCodeFormat(existingCodes: Set<String> = emptySet()): String {
            val cleanExisting = existingCodes.map { it.trim().uppercase() }.toSet()
            repeat(50) {
                val body = buildString(6) {
                    repeat(6) { append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)]) }
                }
                val candidate = "POS-$body"
                if (candidate !in cleanExisting) return candidate
            }
            return "POS-${System.nanoTime().toString(36).uppercase().takeLast(6).padStart(6, 'X')}"
        }

        /**
         * Builds the official POS Digital Transparency WhatsApp message.
         */
        fun buildPosWhatsAppMessage(networkName: String, code: String): String {
            val cleanNet = networkName.trim().ifBlank { "الشبكة الرئيسية" }
            val cleanCode = code.trim().uppercase()
            return "مرحباً بك في بوابة الشفافية الرقمية لشبكة $cleanNet. كود الدخول الخاص بنقطة البيع لمتابعة كشف حسابك وفواتيرك لحظياً هو: $cleanCode"
        }

        /**
         * Builds the official Distributor/Agent Invite WhatsApp message.
         */
        fun buildAgentWhatsAppMessage(
            networkName: String,
            agentName: String,
            code: String,
            pin: String = ""
        ): String {
            val cleanNet = networkName.trim().ifBlank { "الشبكة الرئيسية" }
            val cleanAgent = agentName.trim().ifBlank { "الوكيل الميداني" }
            val cleanCode = code.trim().uppercase()
            val pinSegment = if (pin.isNotBlank()) " | رمز التحقق (PIN): $pin" else ""
            return "مرحباً $cleanAgent، تم تفويضك كوكيل/موزع ميداني في شبكة $cleanNet. كود دعوة الوكيل للدخول إلى التطبيق هو: $cleanCode$pinSegment"
        }

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>?): AccessCodeEntry? {
            if (map == null) return null
            val code = map["code"]?.toString()?.trim()?.uppercase().orEmpty()
            if (code.isBlank()) return null
            val roleStr = map["role"]?.toString()
            val role = AppUserRole.fromNameOrDefault(roleStr)
            val permsMap = map["permissions"] as? Map<String, Any?>
            return AccessCodeEntry(
                code = code,
                role = role,
                networkId = map["networkId"]?.toString().orEmpty().ifBlank { "DEFAULT_ORG" },
                networkName = map["networkName"]?.toString().orEmpty(),
                agentId = map["agentId"]?.toString()?.takeIf { it.isNotBlank() },
                agentName = map["agentName"]?.toString().orEmpty(),
                agentPin = map["agentPin"]?.toString().orEmpty(),
                agentPhone = map["agentPhone"]?.toString().orEmpty(),
                partyId = map["partyId"]?.toString()?.takeIf { it.isNotBlank() },
                partyName = map["partyName"]?.toString().orEmpty(),
                partyPhone = map["partyPhone"]?.toString().orEmpty(),
                permissions = AgentPermissions.fromMap(permsMap),
                isActive = (map["isActive"] as? Boolean) ?: true,
                createdAtEpochMs = (map["createdAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        fun fromJson(json: JSONObject?): AccessCodeEntry? {
            if (json == null) return null
            val code = json.optString("code", "").trim().uppercase()
            if (code.isBlank()) return null
            val role = AppUserRole.fromNameOrDefault(json.optString("role", AppUserRole.DISTRIBUTOR_AGENT.name))
            return AccessCodeEntry(
                code = code,
                role = role,
                networkId = json.optString("networkId", "DEFAULT_ORG").ifBlank { "DEFAULT_ORG" },
                networkName = json.optString("networkName", ""),
                agentId = json.optString("agentId", "").takeIf { it.isNotBlank() },
                agentName = json.optString("agentName", ""),
                agentPin = json.optString("agentPin", ""),
                agentPhone = json.optString("agentPhone", ""),
                partyId = json.optString("partyId", "").takeIf { it.isNotBlank() },
                partyName = json.optString("partyName", ""),
                partyPhone = json.optString("partyPhone", ""),
                permissions = AgentPermissions.fromJson(json.optJSONObject("permissions")),
                isActive = json.optBoolean("isActive", true),
                createdAtEpochMs = json.optLong("createdAtEpochMs", System.currentTimeMillis())
            )
        }
    }
}

data class UserSession(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val uid: String = "",
    val isSignedIn: Boolean = true,
    val lastLoginEpochMs: Long = System.currentTimeMillis(),
    val role: AppUserRole = AppUserRole.NETWORK_OWNER,
    val agentId: String? = null,
    val agentName: String? = null,
    val boundPartyId: String? = null,
    val boundPartyName: String? = null,
    val networkId: String = "DEFAULT_ORG",
    val networkName: String = "",
    val accessCode: String? = null,
    val agentPermissions: AgentPermissions = AgentPermissions.FULL_OWNER
)

/**
 * Digital Handshake: Mutual Invoice & Receipt Verification Status (`التأكيد الرقمي المتبادل`).
 * Strictly a metadata layer — NEVER alters, reverses, or blocks IFRS double-entry ledger postings.
 */
enum class DocumentVerificationStatus(
    val posBadgeAr: String,
    val ownerBadgeAr: String
) {
    PENDING_POS_CONFIRMATION(
        posBadgeAr = "⏳ بانتظار تأكيد البقالة",
        ownerBadgeAr = "⏳ بانتظار تأكيد البقالة"
    ),
    CONFIRMED_BY_POS(
        posBadgeAr = "✅ مؤكد من نقطة البيع",
        ownerBadgeAr = "✅ مؤكد من البقالة"
    ),
    DISPUTED_BY_POS(
        posBadgeAr = "⚠️ يوجد ملاحظة / اعتراض",
        ownerBadgeAr = "⚠️ اعتراض من البقالة"
    );

    companion object {
        fun fromNameOrDefault(name: String?): DocumentVerificationStatus {
            if (name.isNullOrBlank()) return PENDING_POS_CONFIRMATION
            return entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
                ?: PENDING_POS_CONFIRMATION
        }
    }
}

/**
 * Metadata record representing POS Merchant verification or dispute of an issued Sales Invoice or Receipt Voucher.
 * Synced to Firestore under `networks/{networkId}/parties/{partyId}/verifications/{documentId}` with full local fallback.
 */
data class DocumentVerificationRecord(
    val documentId: String,
    val partyId: String,
    val networkId: String = "DEFAULT_ORG",
    val status: DocumentVerificationStatus = DocumentVerificationStatus.PENDING_POS_CONFIRMATION,
    val disputeNote: String = "",
    val verifiedAtEpochMs: Long = System.currentTimeMillis(),
    val verifiedByAccessCode: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "documentId" to documentId,
        "partyId" to partyId,
        "networkId" to networkId,
        "status" to status.name,
        "disputeNote" to disputeNote,
        "verifiedAtEpochMs" to verifiedAtEpochMs,
        "verifiedByAccessCode" to verifiedByAccessCode
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("documentId", documentId)
        put("partyId", partyId)
        put("networkId", networkId)
        put("status", status.name)
        put("disputeNote", disputeNote)
        put("verifiedAtEpochMs", verifiedAtEpochMs)
        put("verifiedByAccessCode", verifiedByAccessCode)
    }

    companion object {
        fun fromMap(map: Map<String, Any?>?): DocumentVerificationRecord? {
            if (map == null) return null
            val docId = map["documentId"]?.toString()?.trim().orEmpty()
            if (docId.isBlank()) return null
            return DocumentVerificationRecord(
                documentId = docId,
                partyId = map["partyId"]?.toString()?.trim().orEmpty(),
                networkId = map["networkId"]?.toString()?.trim().orEmpty().ifBlank { "DEFAULT_ORG" },
                status = DocumentVerificationStatus.fromNameOrDefault(map["status"]?.toString()),
                disputeNote = map["disputeNote"]?.toString()?.trim().orEmpty(),
                verifiedAtEpochMs = (map["verifiedAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                verifiedByAccessCode = map["verifiedByAccessCode"]?.toString()?.trim().orEmpty()
            )
        }

        fun fromJson(json: JSONObject?): DocumentVerificationRecord? {
            if (json == null) return null
            val docId = json.optString("documentId", "").trim()
            if (docId.isBlank()) return null
            return DocumentVerificationRecord(
                documentId = docId,
                partyId = json.optString("partyId", "").trim(),
                networkId = json.optString("networkId", "DEFAULT_ORG").ifBlank { "DEFAULT_ORG" },
                status = DocumentVerificationStatus.fromNameOrDefault(json.optString("status")),
                disputeNote = json.optString("disputeNote", "").trim(),
                verifiedAtEpochMs = json.optLong("verifiedAtEpochMs", System.currentTimeMillis()),
                verifiedByAccessCode = json.optString("verifiedByAccessCode", "").trim()
            )
        }
    }
}

/**
 * Phase 3: POS Restock Order Status (`حالة طلب تغذية الكروت`).
 * Strictly an operational workflow — NEVER mutates LedgerWriter or inventory until a Sales Invoice is explicitly issued.
 */
enum class RestockOrderStatus(
    val labelAr: String,
    val badgeAr: String
) {
    PENDING(labelAr = "قيد الانتظار", badgeAr = "⏳ قيد الانتظار"),
    PREPARING(labelAr = "جاري التجهيز", badgeAr = "📦 جاري التجهيز"),
    FULFILLED(labelAr = "تم التسليم", badgeAr = "✅ تم التسليم"),
    CANCELLED(labelAr = "ملغي", badgeAr = "❌ ملغي");

    companion object {
        fun fromNameOrDefault(name: String?): RestockOrderStatus {
            if (name.isNullOrBlank()) return PENDING
            return entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } ?: PENDING
        }
    }
}

/**
 * Individual package line item within a [PosRestockOrder].
 */
data class PosRestockOrderItem(
    val packageId: String,
    val packageName: String,
    val unitPrice: Long,
    val requestedQuantity: Int
) {
    val lineTotal: Long
        get() = unitPrice * requestedQuantity.toLong()

    fun toMap(): Map<String, Any?> = mapOf(
        "packageId" to packageId,
        "packageName" to packageName,
        "unitPrice" to unitPrice,
        "requestedQuantity" to requestedQuantity
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("packageId", packageId)
        put("packageName", packageName)
        put("unitPrice", unitPrice)
        put("requestedQuantity", requestedQuantity)
    }

    companion object {
        fun fromMap(map: Map<String, Any?>?): PosRestockOrderItem? {
            if (map == null) return null
            val pkgName = map["packageName"]?.toString()?.trim().orEmpty()
            val qty = (map["requestedQuantity"] as? Number)?.toInt() ?: 0
            if (pkgName.isBlank() || qty <= 0) return null
            return PosRestockOrderItem(
                packageId = map["packageId"]?.toString()?.trim().orEmpty(),
                packageName = pkgName,
                unitPrice = (map["unitPrice"] as? Number)?.toLong() ?: 0L,
                requestedQuantity = qty
            )
        }

        fun fromJson(json: JSONObject?): PosRestockOrderItem? {
            if (json == null) return null
            val pkgName = json.optString("packageName", "").trim()
            val qty = json.optInt("requestedQuantity", 0)
            if (pkgName.isBlank() || qty <= 0) return null
            return PosRestockOrderItem(
                packageId = json.optString("packageId", "").trim(),
                packageName = pkgName,
                unitPrice = json.optLong("unitPrice", 0L),
                requestedQuantity = qty
            )
        }
    }
}

/**
 * Structured In-App POS Restock Order (`طلب دفعة كروت جديدة`).
 * Synced under `networks/{networkId}/pos_orders/{orderId}` with full local storage fallback.
 */
data class PosRestockOrder(
    val orderId: String,
    val networkId: String = "DEFAULT_ORG",
    val partyId: String,
    val partyName: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val items: List<PosRestockOrderItem>,
    val note: String? = null,
    val status: RestockOrderStatus = RestockOrderStatus.PENDING,
    val handledBy: String? = null,
    val updatedAtEpochMs: Long = createdAtEpochMs
) {
    val estimatedTotal: Long
        get() = items.sumOf { it.lineTotal }

    val totalRequestedCards: Int
        get() = items.sumOf { it.requestedQuantity }

    fun buildWhatsAppReminderMessage(networkName: String = ""): String {
        val itemsSummary = items.joinToString(" + ") { "${it.requestedQuantity} × ${it.packageName}" }
        val notePart = if (!note.isNullOrBlank()) "\nملاحظة: $note" else ""
        val netPart = if (networkName.isNotBlank()) " ($networkName)" else ""
        return "تذكير بطلب دفعة كروت رقم #$orderId من نقطة البيع: $partyName$netPart\n" +
            "الحالة الحالية: ${status.badgeAr}\n" +
            "الأصناف المطلوبة: $itemsSummary\n" +
            "الإجمالي التقديري: $estimatedTotal ر.ي$notePart"
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "orderId" to orderId,
        "networkId" to networkId,
        "partyId" to partyId,
        "partyName" to partyName,
        "createdAtEpochMs" to createdAtEpochMs,
        "items" to items.map { it.toMap() },
        "note" to (note ?: ""),
        "status" to status.name,
        "handledBy" to (handledBy ?: ""),
        "updatedAtEpochMs" to updatedAtEpochMs
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("orderId", orderId)
        put("networkId", networkId)
        put("partyId", partyId)
        put("partyName", partyName)
        put("createdAtEpochMs", createdAtEpochMs)
        val itemsArr = org.json.JSONArray()
        items.forEach { itemsArr.put(it.toJson()) }
        put("items", itemsArr)
        put("note", note ?: "")
        put("status", status.name)
        put("handledBy", handledBy ?: "")
        put("updatedAtEpochMs", updatedAtEpochMs)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>?): PosRestockOrder? {
            if (map == null) return null
            val id = map["orderId"]?.toString()?.trim().orEmpty()
            if (id.isBlank()) return null
            val rawItems = map["items"] as? List<Map<String, Any?>> ?: emptyList()
            val parsedItems = rawItems.mapNotNull { PosRestockOrderItem.fromMap(it) }
            val created = (map["createdAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
            return PosRestockOrder(
                orderId = id,
                networkId = map["networkId"]?.toString()?.trim().orEmpty().ifBlank { "DEFAULT_ORG" },
                partyId = map["partyId"]?.toString()?.trim().orEmpty(),
                partyName = map["partyName"]?.toString()?.trim().orEmpty().ifBlank { "نقطة بيع" },
                createdAtEpochMs = created,
                items = parsedItems,
                note = map["note"]?.toString()?.trim()?.takeIf { it.isNotBlank() },
                status = RestockOrderStatus.fromNameOrDefault(map["status"]?.toString()),
                handledBy = map["handledBy"]?.toString()?.trim()?.takeIf { it.isNotBlank() },
                updatedAtEpochMs = (map["updatedAtEpochMs"] as? Number)?.toLong() ?: created
            )
        }

        fun fromJson(json: JSONObject?): PosRestockOrder? {
            if (json == null) return null
            val id = json.optString("orderId", "").trim()
            if (id.isBlank()) return null
            val arr = json.optJSONArray("items")
            val parsedItems = mutableListOf<PosRestockOrderItem>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    PosRestockOrderItem.fromJson(arr.optJSONObject(i))?.let { parsedItems.add(it) }
                }
            }
            val created = json.optLong("createdAtEpochMs", System.currentTimeMillis())
            return PosRestockOrder(
                orderId = id,
                networkId = json.optString("networkId", "DEFAULT_ORG").ifBlank { "DEFAULT_ORG" },
                partyId = json.optString("partyId", "").trim(),
                partyName = json.optString("partyName", "نقطة بيع").trim().ifBlank { "نقطة بيع" },
                createdAtEpochMs = created,
                items = parsedItems,
                note = json.optString("note", "").trim().takeIf { it.isNotBlank() },
                status = RestockOrderStatus.fromNameOrDefault(json.optString("status")),
                handledBy = json.optString("handledBy", "").trim().takeIf { it.isNotBlank() },
                updatedAtEpochMs = json.optLong("updatedAtEpochMs", created)
            )
        }
    }
}

/**
 * Phase 3: Support & Complaint Ticket Category (`تصنيف البلاغ أو الشكوى`).
 */
enum class TicketCategory(val labelAr: String) {
    NETWORK_SIGNAL("ضعف أو انقطاع الشبكة"),
    VOUCHER_ISSUE("مشكلة في كرت"),
    BILLING_INQUIRY("مراجعة مالية"),
    OTHER("أخرى");

    companion object {
        fun fromNameOrDefault(name: String?): TicketCategory {
            if (name.isNullOrBlank()) return OTHER
            return entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } ?: OTHER
        }
    }
}

/**
 * Phase 3: Support & Complaint Ticket Status (`حالة البلاغ`).
 */
enum class TicketStatus(
    val labelAr: String,
    val badgeAr: String
) {
    OPEN(labelAr = "مفتوح", badgeAr = "🔴 مفتوح"),
    IN_PROGRESS(labelAr = "قيد المعالجة", badgeAr = "⏳ قيد المعالجة"),
    RESOLVED(labelAr = "تم الحل", badgeAr = "✅ تم الحل");

    companion object {
        fun fromNameOrDefault(name: String?): TicketStatus {
            if (name.isNullOrBlank()) return OPEN
            return entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } ?: OPEN
        }
    }
}

/**
 * Direct Support & Complaint Ticket (`بلاغ أو شكوى من نقطة البيع`).
 * Synced under `networks/{networkId}/pos_tickets/{ticketId}` with full local storage fallback.
 */
data class PosSupportTicket(
    val ticketId: String,
    val networkId: String = "DEFAULT_ORG",
    val partyId: String,
    val partyName: String,
    val category: TicketCategory = TicketCategory.OTHER,
    val voucherCode: String? = null,
    val message: String,
    val status: TicketStatus = TicketStatus.OPEN,
    val adminReply: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val resolvedAtEpochMs: Long? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "ticketId" to ticketId,
        "networkId" to networkId,
        "partyId" to partyId,
        "partyName" to partyName,
        "category" to category.name,
        "voucherCode" to (voucherCode ?: ""),
        "message" to message,
        "status" to status.name,
        "adminReply" to (adminReply ?: ""),
        "createdAtEpochMs" to createdAtEpochMs,
        "resolvedAtEpochMs" to (resolvedAtEpochMs ?: -1L)
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("ticketId", ticketId)
        put("networkId", networkId)
        put("partyId", partyId)
        put("partyName", partyName)
        put("category", category.name)
        put("voucherCode", voucherCode ?: "")
        put("message", message)
        put("status", status.name)
        put("adminReply", adminReply ?: "")
        put("createdAtEpochMs", createdAtEpochMs)
        put("resolvedAtEpochMs", resolvedAtEpochMs ?: -1L)
    }

    companion object {
        fun fromMap(map: Map<String, Any?>?): PosSupportTicket? {
            if (map == null) return null
            val id = map["ticketId"]?.toString()?.trim().orEmpty()
            if (id.isBlank()) return null
            val resolvedRaw = (map["resolvedAtEpochMs"] as? Number)?.toLong() ?: -1L
            return PosSupportTicket(
                ticketId = id,
                networkId = map["networkId"]?.toString()?.trim().orEmpty().ifBlank { "DEFAULT_ORG" },
                partyId = map["partyId"]?.toString()?.trim().orEmpty(),
                partyName = map["partyName"]?.toString()?.trim().orEmpty().ifBlank { "نقطة بيع" },
                category = TicketCategory.fromNameOrDefault(map["category"]?.toString()),
                voucherCode = map["voucherCode"]?.toString()?.trim()?.takeIf { it.isNotBlank() },
                message = map["message"]?.toString()?.trim().orEmpty(),
                status = TicketStatus.fromNameOrDefault(map["status"]?.toString()),
                adminReply = map["adminReply"]?.toString()?.trim()?.takeIf { it.isNotBlank() },
                createdAtEpochMs = (map["createdAtEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                resolvedAtEpochMs = resolvedRaw.takeIf { it > 0L }
            )
        }

        fun fromJson(json: JSONObject?): PosSupportTicket? {
            if (json == null) return null
            val id = json.optString("ticketId", "").trim()
            if (id.isBlank()) return null
            val resolvedRaw = json.optLong("resolvedAtEpochMs", -1L)
            return PosSupportTicket(
                ticketId = id,
                networkId = json.optString("networkId", "DEFAULT_ORG").ifBlank { "DEFAULT_ORG" },
                partyId = json.optString("partyId", "").trim(),
                partyName = json.optString("partyName", "نقطة بيع").trim().ifBlank { "نقطة بيع" },
                category = TicketCategory.fromNameOrDefault(json.optString("category")),
                voucherCode = json.optString("voucherCode", "").trim().takeIf { it.isNotBlank() },
                message = json.optString("message", "").trim(),
                status = TicketStatus.fromNameOrDefault(json.optString("status")),
                adminReply = json.optString("adminReply", "").trim().takeIf { it.isNotBlank() },
                createdAtEpochMs = json.optLong("createdAtEpochMs", System.currentTimeMillis()),
                resolvedAtEpochMs = resolvedRaw.takeIf { it > 0L }
            )
        }
    }
}


