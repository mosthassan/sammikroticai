package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.util.GoogleServicesConfigHelper
import com.example.util.findActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Production Native Google Credential Manager, Firebase Authentication & Multi-Role SaaS RBAC Engine.
 * Features automatic fallback to legacy GoogleSignInClient when CredentialManager
 * returns NoCredentialException or GetCredentialException, plus full support for:
 * - [AppUserRole.NETWORK_OWNER] (Default for existing users and direct MikroTik router logins)
 * - [AppUserRole.DISTRIBUTOR_AGENT] (Invite code `AGT-XXXX` + customizable [AgentPermissions])
 * - [AppUserRole.POS_MERCHANT] (Digital Transparency code `POS-XXXXXX` bound to `partyId`)
 */
class AuthManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sammikrotik_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserSession?>(null)
    val currentUser: StateFlow<UserSession?> = _currentUser.asStateFlow()

    private val _activeRole = MutableStateFlow(AppUserRole.NETWORK_OWNER)
    val activeRole: StateFlow<AppUserRole> = _activeRole.asStateFlow()

    private val _activeAgentPermissions = MutableStateFlow(AgentPermissions.FULL_OWNER)
    val activeAgentPermissions: StateFlow<AgentPermissions> = _activeAgentPermissions.asStateFlow()

    private val _accessCodes = MutableStateFlow<List<AccessCodeEntry>>(emptyList())
    val accessCodes: StateFlow<List<AccessCodeEntry>> = _accessCodes.asStateFlow()

    private val _documentVerifications = MutableStateFlow<Map<String, DocumentVerificationRecord>>(emptyMap())
    val documentVerifications: StateFlow<Map<String, DocumentVerificationRecord>> = _documentVerifications.asStateFlow()

    private val _posRestockOrders = MutableStateFlow<List<PosRestockOrder>>(emptyList())
    val posRestockOrders: StateFlow<List<PosRestockOrder>> = _posRestockOrders.asStateFlow()

    private val _posSupportTickets = MutableStateFlow<List<PosSupportTicket>>(emptyList())
    val posSupportTickets: StateFlow<List<PosSupportTicket>> = _posSupportTickets.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    init {
        ensureFirebaseInitialized()
        loadPersistedAccessCodes()
        loadPersistedDocumentVerifications()
        loadPersistedPosRestockOrders()
        loadPersistedPosSupportTickets()
        loadPersistedSession()
    }

    private fun ensureFirebaseInitialized() {
        com.example.core.firebase.FirebaseInitializer.ensureInitialized(context)
    }

    private fun loadPersistedAccessCodes() {
        val raw = prefs.getString(KEY_ACCESS_CODES_JSON, null)
        if (raw.isNullOrBlank()) {
            _accessCodes.value = emptyList()
            return
        }
        try {
            val arr = JSONArray(raw)
            val parsed = mutableListOf<AccessCodeEntry>()
            for (i in 0 until arr.length()) {
                AccessCodeEntry.fromJson(arr.optJSONObject(i))?.let { parsed.add(it) }
            }
            _accessCodes.value = parsed.sortedByDescending { it.createdAtEpochMs }
        } catch (e: Exception) {
            Log.w("AuthManager", "Failed to parse persisted access codes: ${e.message}")
            _accessCodes.value = emptyList()
        }
    }

    private fun saveAccessCodesToPrefs(list: List<AccessCodeEntry>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_ACCESS_CODES_JSON, arr.toString()).apply()
        _accessCodes.value = list.sortedByDescending { it.createdAtEpochMs }
    }

    private fun loadPersistedDocumentVerifications() {
        val raw = prefs.getString(KEY_DOC_VERIFICATIONS_JSON, null)
        if (raw.isNullOrBlank()) {
            _documentVerifications.value = emptyMap()
            return
        }
        try {
            val arr = JSONArray(raw)
            val parsed = mutableMapOf<String, DocumentVerificationRecord>()
            for (i in 0 until arr.length()) {
                DocumentVerificationRecord.fromJson(arr.optJSONObject(i))?.let {
                    parsed[it.documentId] = it
                }
            }
            _documentVerifications.value = parsed
        } catch (e: Exception) {
            Log.w("AuthManager", "Failed to parse persisted document verifications: ${e.message}")
            _documentVerifications.value = emptyMap()
        }
    }

    private fun saveDocumentVerificationsToPrefs(map: Map<String, DocumentVerificationRecord>) {
        val arr = JSONArray()
        map.values.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_DOC_VERIFICATIONS_JSON, arr.toString()).apply()
        _documentVerifications.value = map.toMap()
    }

    private fun loadPersistedPosRestockOrders() {
        val raw = prefs.getString(KEY_POS_RESTOCK_ORDERS_JSON, null)
        if (raw.isNullOrBlank()) {
            _posRestockOrders.value = emptyList()
            return
        }
        try {
            val arr = JSONArray(raw)
            val parsed = mutableListOf<PosRestockOrder>()
            for (i in 0 until arr.length()) {
                PosRestockOrder.fromJson(arr.optJSONObject(i))?.let { parsed.add(it) }
            }
            _posRestockOrders.value = parsed.sortedByDescending { it.createdAtEpochMs }
        } catch (e: Exception) {
            Log.w("AuthManager", "Failed to parse persisted POS restock orders: ${e.message}")
            _posRestockOrders.value = emptyList()
        }
    }

    private fun savePosRestockOrdersToPrefs(list: List<PosRestockOrder>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_POS_RESTOCK_ORDERS_JSON, arr.toString()).apply()
        _posRestockOrders.value = list.sortedByDescending { it.createdAtEpochMs }
    }

    private fun loadPersistedPosSupportTickets() {
        val raw = prefs.getString(KEY_POS_SUPPORT_TICKETS_JSON, null)
        if (raw.isNullOrBlank()) {
            _posSupportTickets.value = emptyList()
            return
        }
        try {
            val arr = JSONArray(raw)
            val parsed = mutableListOf<PosSupportTicket>()
            for (i in 0 until arr.length()) {
                PosSupportTicket.fromJson(arr.optJSONObject(i))?.let { parsed.add(it) }
            }
            _posSupportTickets.value = parsed.sortedByDescending { it.createdAtEpochMs }
        } catch (e: Exception) {
            Log.w("AuthManager", "Failed to parse persisted POS support tickets: ${e.message}")
            _posSupportTickets.value = emptyList()
        }
    }

    private fun savePosSupportTicketsToPrefs(list: List<PosSupportTicket>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_POS_SUPPORT_TICKETS_JSON, arr.toString()).apply()
        _posSupportTickets.value = list.sortedByDescending { it.createdAtEpochMs }
    }

    private fun readPersistedPermissions(role: AppUserRole): AgentPermissions {
        if (role == AppUserRole.NETWORK_OWNER) return AgentPermissions.FULL_OWNER
        if (role == AppUserRole.POS_MERCHANT) return AgentPermissions.READ_ONLY_NONE
        val raw = prefs.getString("user_agent_permissions_json", null)
        return if (!raw.isNullOrBlank()) {
            runCatching { AgentPermissions.fromJson(JSONObject(raw)) }.getOrDefault(AgentPermissions())
        } else {
            AgentPermissions()
        }
    }

    private fun loadPersistedSession() {
        val persistedRole = AppUserRole.fromNameOrDefault(
            prefs.getString("user_role", AppUserRole.NETWORK_OWNER.name)
        )
        val persistedPermissions = readPersistedPermissions(persistedRole)
        val agentId = prefs.getString("user_agent_id", null)?.takeIf { it.isNotBlank() }
        val agentName = prefs.getString("user_agent_name", null)?.takeIf { it.isNotBlank() }
        val boundPartyId = prefs.getString("user_bound_party_id", null)?.takeIf { it.isNotBlank() }
        val boundPartyName = prefs.getString("user_bound_party_name", null)?.takeIf { it.isNotBlank() }
        val networkId = prefs.getString("user_network_id", "DEFAULT_ORG") ?: "DEFAULT_ORG"
        val networkName = prefs.getString("user_network_name", "") ?: ""
        val accessCode = prefs.getString("user_access_code", null)?.takeIf { it.isNotBlank() }

        _activeRole.value = persistedRole
        _activeAgentPermissions.value = persistedPermissions

        // If user is in DISTRIBUTOR_AGENT or POS_MERCHANT role, restore that role session first
        if (persistedRole != AppUserRole.NETWORK_OWNER) {
            val savedEmail = prefs.getString("user_email", null).orEmpty()
            val savedUid = prefs.getString("user_uid", null).orEmpty()
            val displayName = prefs.getString("user_name", null)
                ?.takeIf { it.isNotBlank() }
                ?: agentName
                ?: boundPartyName
                ?: persistedRole.titleAr
            val session = UserSession(
                email = savedEmail.ifBlank { "${persistedRole.name.lowercase()}@sammikrotik.local" },
                displayName = displayName,
                photoUrl = prefs.getString("user_photo", null),
                uid = savedUid.ifBlank { accessCode ?: persistedRole.name },
                isSignedIn = true,
                role = persistedRole,
                agentId = agentId,
                agentName = agentName,
                boundPartyId = boundPartyId,
                boundPartyName = boundPartyName,
                networkId = networkId,
                networkName = networkName,
                accessCode = accessCode,
                agentPermissions = persistedPermissions
            )
            _currentUser.value = session
            return
        }

        val fbUser = try {
            if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseAuth.getInstance().currentUser else null
        } catch (e: Exception) { null }
        if (fbUser != null && !fbUser.email.isNullOrBlank()) {
            val email = fbUser.email ?: ""
            val session = UserSession(
                email = email,
                displayName = fbUser.displayName ?: email.substringBefore("@"),
                photoUrl = fbUser.photoUrl?.toString(),
                uid = fbUser.uid,
                isSignedIn = true,
                role = AppUserRole.NETWORK_OWNER,
                networkId = networkId.ifBlank { fbUser.uid },
                networkName = networkName,
                agentPermissions = AgentPermissions.FULL_OWNER
            )
            _currentUser.value = session
            _activeRole.value = AppUserRole.NETWORK_OWNER
            _activeAgentPermissions.value = AgentPermissions.FULL_OWNER
            saveSession(session)
            return
        }

        val savedEmail = prefs.getString("user_email", null)
        val savedUid = prefs.getString("user_uid", null)
        if (!savedEmail.isNullOrBlank() && !savedUid.isNullOrBlank()) {
            val name = prefs.getString("user_name", "") ?: ""
            val photo = prefs.getString("user_photo", null)
            val session = UserSession(
                email = savedEmail,
                displayName = if (name.isNotBlank()) name else savedEmail.substringBefore("@"),
                photoUrl = photo,
                uid = savedUid,
                isSignedIn = true,
                role = persistedRole,
                agentId = agentId,
                agentName = agentName,
                boundPartyId = boundPartyId,
                boundPartyName = boundPartyName,
                networkId = networkId,
                networkName = networkName,
                accessCode = accessCode,
                agentPermissions = persistedPermissions
            )
            _currentUser.value = session
            _activeRole.value = session.role
            _activeAgentPermissions.value = session.agentPermissions
        } else {
            _currentUser.value = null
            _activeRole.value = AppUserRole.NETWORK_OWNER
            _activeAgentPermissions.value = AgentPermissions.FULL_OWNER
        }
    }

    /**
     * One-Tap Sign In via Android Credential Manager & Google ID.
     * Defaults role to [AppUserRole.NETWORK_OWNER] unless explicitly switched.
     */
    suspend fun signInWithGoogleCredentialManager(
        activityOrContext: Context,
        serverClientId: String? = null
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _authError.value = null

        val webClientId = serverClientId?.takeIf { it.isNotBlank() }
            ?: GoogleServicesConfigHelper.getWebClientId(context)

        val activity = activityOrContext.findActivity() ?: (activityOrContext as? Activity)
        val targetContext = activity ?: activityOrContext

        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                context = targetContext,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                val idToken = googleIdTokenCredential.idToken

                ensureFirebaseInitialized()
                val firebaseUser = runCatching {
                    if (FirebaseApp.getApps(context).isNotEmpty()) {
                        val firebaseAuth = FirebaseAuth.getInstance()
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                        authResult.user
                    } else null
                }.getOrNull()
                val uid = firebaseUser?.uid ?: idToken.hashCode().toString()

                val session = UserSession(
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    uid = uid,
                    isSignedIn = true,
                    role = AppUserRole.NETWORK_OWNER,
                    networkId = uid,
                    agentPermissions = AgentPermissions.FULL_OWNER
                )
                saveOwnerBackupIdentity(session)
                saveSession(session)
                _currentUser.value = session
                _activeRole.value = AppUserRole.NETWORK_OWNER
                _activeAgentPermissions.value = AgentPermissions.FULL_OWNER
                _isLoading.value = false
                Result.success(session)
            } else {
                val err = "نوع الاعتماد غير مدعوم من Google"
                _authError.value = err
                _isLoading.value = false
                Result.failure(IllegalStateException(err))
            }
        } catch (e: GetCredentialCancellationException) {
            _isLoading.value = false
            _authError.value = "تم إلغاء اختيار الحساب"
            Result.failure(e)
        } catch (e: NoCredentialException) {
            Log.w("AuthManager", "NoCredentialException: ${e.message}. Launching standard GoogleSignIn fallback.")
            return@withContext launchGoogleSignInFallback(activity, webClientId, e)
        } catch (e: GetCredentialException) {
            Log.w("AuthManager", "GetCredentialException: ${e.message}. Launching standard GoogleSignIn fallback.")
            return@withContext launchGoogleSignInFallback(activity, webClientId, e)
        } catch (e: Exception) {
            _isLoading.value = false
            val msg = e.localizedMessage ?: "فشل تسجيل الدخول باستخدام حساب Google"
            _authError.value = msg
            Result.failure(e)
        }
    }

    private suspend fun launchGoogleSignInFallback(
        activity: Activity?,
        webClientId: String,
        originalException: Exception
    ): Result<UserSession> {
        if (activity != null) {
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(activity, gso)
                withContext(Dispatchers.Main) {
                    activity.startActivityForResult(client.signInIntent, RC_GOOGLE_SIGN_IN)
                }
                _isLoading.value = false
                val notice = "جاري فتح نافذة اختيار حساب Google البديلة..."
                _authError.value = notice
                return Result.failure(Exception(notice))
            } catch (fallbackEx: Exception) {
                Log.e("AuthManager", "GoogleSignIn fallback failed: ${fallbackEx.message}")
            }
        }
        _isLoading.value = false
        val msg = "لا تتوفر حسابات Google متوافقة (تأكد من مطابقة SHA-1 في Firebase Console أو استخدم زر الدخول بالبريد أدناه)"
        _authError.value = msg
        return Result.failure(Exception(msg))
    }

    fun saveSessionDirectly(session: UserSession) {
        if (session.role == AppUserRole.NETWORK_OWNER) {
            saveOwnerBackupIdentity(session)
        }
        saveSession(session)
        _currentUser.value = session
        _activeRole.value = session.role
        _activeAgentPermissions.value = session.agentPermissions
        _authError.value = null
    }

    /**
     * Direct Sign-in method for explicit email/manual entry. Defaults to [AppUserRole.NETWORK_OWNER].
     */
    fun signInDirectly(
        email: String,
        displayName: String = "",
        photoUrl: String? = null
    ): UserSession {
        val cleanEmail = email.trim().lowercase()
        val name = if (displayName.isNotBlank()) displayName else cleanEmail.substringBefore("@")
        val uid = cleanEmail.replace(".", "_").replace("@", "_at_")
        val session = UserSession(
            email = cleanEmail,
            displayName = name,
            photoUrl = photoUrl,
            uid = uid,
            isSignedIn = true,
            role = AppUserRole.NETWORK_OWNER,
            networkId = uid.ifBlank { "DEFAULT_ORG" },
            agentPermissions = AgentPermissions.FULL_OWNER
        )
        saveOwnerBackupIdentity(session)
        saveSession(session)
        _currentUser.value = session
        _activeRole.value = AppUserRole.NETWORK_OWNER
        _activeAgentPermissions.value = AgentPermissions.FULL_OWNER
        _authError.value = null
        return session
    }

    // =========================================================================
    // SaaS Multi-Role Invite & Transparency Code Management
    // =========================================================================

    /**
     * Creates or updates a Field Distributor/Agent (`DISTRIBUTOR_AGENT`) profile and generates a unique
     * invite code (`AGT-XXXX`).
     */
    fun createOrUpdateAgentInviteCode(
        agentName: String,
        agentPin: String = "",
        agentPhone: String = "",
        permissions: AgentPermissions = AgentPermissions(),
        networkId: String = resolveActiveNetworkId(),
        networkName: String = "الشبكة الرئيسية",
        existingCode: String? = null
    ): AccessCodeEntry {
        val cleanName = agentName.trim().ifBlank { "وكيل ميداني" }
        val currentList = _accessCodes.value.toMutableList()
        val existingSet = currentList.map { it.code }.toSet()

        val targetCode = existingCode?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
            ?: AccessCodeEntry.generateAgentCodeFormat(existingSet)

        val existingIndex = currentList.indexOfFirst { it.code.equals(targetCode, ignoreCase = true) }
        val agentId = if (existingIndex >= 0) {
            currentList[existingIndex].agentId ?: "AGT_${UUID.randomUUID().toString().take(8).uppercase()}"
        } else {
            "AGT_${UUID.randomUUID().toString().take(8).uppercase()}"
        }

        val entry = AccessCodeEntry(
            code = targetCode,
            role = AppUserRole.DISTRIBUTOR_AGENT,
            networkId = networkId.ifBlank { "DEFAULT_ORG" },
            networkName = networkName.ifBlank { "الشبكة الرئيسية" },
            agentId = agentId,
            agentName = cleanName,
            agentPin = agentPin.trim(),
            agentPhone = agentPhone.trim(),
            permissions = permissions,
            isActive = true,
            createdAtEpochMs = if (existingIndex >= 0) currentList[existingIndex].createdAtEpochMs else System.currentTimeMillis()
        )

        if (existingIndex >= 0) {
            currentList[existingIndex] = entry
        } else {
            currentList.add(0, entry)
        }
        saveAccessCodesToPrefs(currentList)

        // If the current session is actively running as this agent, hot-update permissions immediately
        val currentSession = _currentUser.value
        if (currentSession?.role == AppUserRole.DISTRIBUTOR_AGENT &&
            currentSession.accessCode.equals(entry.code, ignoreCase = true)
        ) {
            val updatedSession = currentSession.copy(
                displayName = entry.agentName,
                agentName = entry.agentName,
                agentPermissions = entry.permissions
            )
            saveSession(updatedSession)
            _currentUser.value = updatedSession
            _activeAgentPermissions.value = entry.permissions
        }

        return entry
    }

    /**
     * Updates the [AgentPermissions] of an existing `AGT-XXXX` agent code.
     */
    fun updateAgentPermissions(code: String, permissions: AgentPermissions): AccessCodeEntry? {
        val cleanCode = code.trim().uppercase()
        val currentList = _accessCodes.value.toMutableList()
        val index = currentList.indexOfFirst { it.code.equals(cleanCode, ignoreCase = true) }
        if (index < 0) return null
        val updated = currentList[index].copy(permissions = permissions)
        currentList[index] = updated
        saveAccessCodesToPrefs(currentList)

        val currentSession = _currentUser.value
        if (currentSession?.role == AppUserRole.DISTRIBUTOR_AGENT &&
            currentSession.accessCode.equals(cleanCode, ignoreCase = true)
        ) {
            val updatedSession = currentSession.copy(agentPermissions = permissions)
            saveSession(updatedSession)
            _currentUser.value = updatedSession
            _activeAgentPermissions.value = permissions
        }
        return updated
    }

    /**
     * Generates (or retrieves existing active) 6-character POS Digital Transparency Code (`POS-XXXXXX`)
     * bound to a specific Customer/POS [partyId] and [networkId].
     */
    fun generateOrGetPosCode(
        partyId: String,
        partyName: String,
        partyPhone: String = "",
        networkId: String = resolveActiveNetworkId(),
        networkName: String = "الشبكة الرئيسية",
        forceNew: Boolean = false
    ): AccessCodeEntry {
        val currentList = _accessCodes.value.toMutableList()
        if (!forceNew) {
            val existingForParty = currentList.firstOrNull {
                it.role == AppUserRole.POS_MERCHANT &&
                    it.partyId == partyId &&
                    it.isActive
            }
            if (existingForParty != null) {
                val refreshed = existingForParty.copy(
                    partyName = partyName.ifBlank { existingForParty.partyName },
                    partyPhone = partyPhone.ifBlank { existingForParty.partyPhone },
                    networkName = networkName.ifBlank { existingForParty.networkName }
                )
                val idx = currentList.indexOfFirst { it.code == existingForParty.code }
                if (idx >= 0) {
                    currentList[idx] = refreshed
                    saveAccessCodesToPrefs(currentList)
                }
                return refreshed
            }
        }

        // Deactivate old codes for this party if forceNew is requested
        if (forceNew) {
            for (i in currentList.indices) {
                val item = currentList[i]
                if (item.role == AppUserRole.POS_MERCHANT && item.partyId == partyId) {
                    currentList[i] = item.copy(isActive = false)
                }
            }
        }

        val existingCodes = currentList.map { it.code }.toSet()
        val newCode = AccessCodeEntry.generatePosCodeFormat(existingCodes)
        val entry = AccessCodeEntry(
            code = newCode,
            role = AppUserRole.POS_MERCHANT,
            networkId = networkId.ifBlank { "DEFAULT_ORG" },
            networkName = networkName.ifBlank { "الشبكة الرئيسية" },
            partyId = partyId,
            partyName = partyName.trim().ifBlank { "نقطة بيع" },
            partyPhone = partyPhone.trim(),
            permissions = AgentPermissions.READ_ONLY_NONE,
            isActive = true,
            createdAtEpochMs = System.currentTimeMillis()
        )
        currentList.add(0, entry)
        saveAccessCodesToPrefs(currentList)
        return entry
    }

    /**
     * Revokes or reactivates an access code.
     */
    fun setAccessCodeActive(code: String, isActive: Boolean): AccessCodeEntry? {
        val cleanCode = code.trim().uppercase()
        val currentList = _accessCodes.value.toMutableList()
        val idx = currentList.indexOfFirst { it.code.equals(cleanCode, ignoreCase = true) }
        if (idx < 0) return null
        val updated = currentList[idx].copy(isActive = isActive)
        currentList[idx] = updated
        saveAccessCodesToPrefs(currentList)
        return updated
    }

    /**
     * Merges access codes pulled from Firestore (`networks/{networkId}/access_codes`) into the local store.
     */
    fun mergeRemoteAccessCodes(remoteCodes: List<AccessCodeEntry>) {
        if (remoteCodes.isEmpty()) return
        val byCode = _accessCodes.value.associateBy { it.code.uppercase() }.toMutableMap()
        remoteCodes.forEach { remote ->
            byCode[remote.code.uppercase()] = remote
        }
        saveAccessCodesToPrefs(byCode.values.toList())
    }

    /**
     * Authenticates a Field Distributor/Agent using their `AGT-XXXX` invite code and optional Name/PIN.
     */
    suspend fun loginWithAgentInviteCode(
        codeInput: String,
        nameOrPinInput: String = "",
        cloudLookup: (suspend (String) -> AccessCodeEntry?)? = null
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        val cleanCode = codeInput.trim().uppercase()
        if (cleanCode.isBlank()) {
            val err = "يرجى إدخال كود دعوة الوكيل (مثال: AGT-XXXX)"
            _authError.value = err
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        var matched = _accessCodes.value.firstOrNull {
            it.code.equals(cleanCode, ignoreCase = true) && it.role == AppUserRole.DISTRIBUTOR_AGENT
        }

        if (matched == null && cloudLookup != null) {
            matched = runCatching { cloudLookup(cleanCode) }.getOrNull()
            if (matched != null && matched.role == AppUserRole.DISTRIBUTOR_AGENT) {
                mergeRemoteAccessCodes(listOf(matched))
            }
        }

        if (matched == null) {
            val err = "كود الوكيل ($cleanCode) غير مسجل. تأكد من الكود الصادر من مالك الشبكة."
            _authError.value = err
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        if (!matched.isActive) {
            val err = "كود الوكيل ($cleanCode) موقوف حالياً من قبل مالك الشبكة."
            _authError.value = err
            return@withContext Result.failure(IllegalStateException(err))
        }

        val cleanPinInput = nameOrPinInput.trim()
        if (matched.agentPin.isNotBlank() &&
            cleanPinInput != matched.agentPin &&
            !cleanPinInput.equals(matched.agentName, ignoreCase = true)
        ) {
            val err = "رمز التحقق (PIN) أو اسم الوكيل غير مطابق للكود المدخل."
            _authError.value = err
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        val session = activateRoleSessionFromEntry(
            entry = matched,
            overrideDisplayName = matched.agentName.ifBlank { cleanPinInput.ifBlank { "وكيل ميداني" } }
        )
        Result.success(session)
    }

    /**
     * Authenticates a Point of Sale / Grocery owner (`POS_MERCHANT`) using their Digital Transparency Code (`POS-XXXXXX`).
     */
    suspend fun loginWithPosTransparencyCode(
        codeInput: String,
        cloudLookup: (suspend (String) -> AccessCodeEntry?)? = null
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        val cleanCode = codeInput.trim().uppercase()
        if (cleanCode.isBlank()) {
            val err = "يرجى إدخال كود الشفافية الرقمية لنقطة البيع (مثال: POS-XXXXXX)"
            _authError.value = err
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        var matched = _accessCodes.value.firstOrNull {
            it.code.equals(cleanCode, ignoreCase = true) && it.role == AppUserRole.POS_MERCHANT
        }

        if (matched == null && cloudLookup != null) {
            matched = runCatching { cloudLookup(cleanCode) }.getOrNull()
            if (matched != null && matched.role == AppUserRole.POS_MERCHANT) {
                mergeRemoteAccessCodes(listOf(matched))
            }
        }

        if (matched == null) {
            val err = "كود الشفافية الرقمية ($cleanCode) غير صالح أو غير مسجل."
            _authError.value = err
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        if (!matched.isActive) {
            val err = "كود نقطة البيع ($cleanCode) غير نشط حالياً. يرجى التواصل مع إدارة الشبكة."
            _authError.value = err
            return@withContext Result.failure(IllegalStateException(err))
        }

        if (matched.partyId.isNullOrBlank()) {
            val err = "هذا الكود غير مرتبط بحساب نقطة بيع محدد."
            _authError.value = err
            return@withContext Result.failure(IllegalStateException(err))
        }

        val session = activateRoleSessionFromEntry(
            entry = matched,
            overrideDisplayName = matched.partyName.ifBlank { "نقطة بيع" }
        )
        Result.success(session)
    }

    /**
     * Activates an Agent or POS session from an [AccessCodeEntry] (used both for code login and Owner role preview).
     */
    fun activateRoleSessionFromEntry(
        entry: AccessCodeEntry,
        overrideDisplayName: String? = null
    ): UserSession {
        val current = _currentUser.value
        if (current != null && current.role == AppUserRole.NETWORK_OWNER) {
            saveOwnerBackupIdentity(current)
        }

        val effectivePermissions = when (entry.role) {
            AppUserRole.NETWORK_OWNER -> AgentPermissions.FULL_OWNER
            AppUserRole.DISTRIBUTOR_AGENT -> entry.permissions
            AppUserRole.POS_MERCHANT -> AgentPermissions.READ_ONLY_NONE
        }

        val displayName = overrideDisplayName?.takeIf { it.isNotBlank() }
            ?: when (entry.role) {
                AppUserRole.DISTRIBUTOR_AGENT -> entry.agentName.ifBlank { "وكيل ميداني" }
                AppUserRole.POS_MERCHANT -> entry.partyName.ifBlank { "نقطة بيع" }
                AppUserRole.NETWORK_OWNER -> "مالك الشبكة"
            }

        val session = UserSession(
            email = current?.email?.takeIf { it.isNotBlank() } ?: "${entry.code.lowercase()}@sammikrotik.local",
            displayName = displayName,
            photoUrl = current?.photoUrl,
            uid = current?.uid?.takeIf { it.isNotBlank() } ?: entry.code,
            isSignedIn = true,
            role = entry.role,
            agentId = entry.agentId,
            agentName = entry.agentName.ifBlank { displayName },
            boundPartyId = entry.partyId,
            boundPartyName = entry.partyName.ifBlank { displayName },
            networkId = entry.networkId.ifBlank { resolveActiveNetworkId() },
            networkName = entry.networkName,
            accessCode = entry.code,
            agentPermissions = effectivePermissions
        )
        saveSession(session)
        _currentUser.value = session
        _activeRole.value = session.role
        _activeAgentPermissions.value = effectivePermissions
        _authError.value = null
        return session
    }

    /**
     * Switches back to [AppUserRole.NETWORK_OWNER] role, restoring the owner's saved session if available.
     * Ensures existing users or direct MikroTik router logins always default to NETWORK_OWNER.
     */
    fun switchToNetworkOwnerRole() {
        val ownerEmail = prefs.getString("owner_backup_email", null)
        val ownerName = prefs.getString("owner_backup_name", null)
        val ownerPhoto = prefs.getString("owner_backup_photo", null)
        val ownerUid = prefs.getString("owner_backup_uid", null)
        val ownerNetworkId = prefs.getString("owner_backup_network_id", "DEFAULT_ORG") ?: "DEFAULT_ORG"

        if (!ownerEmail.isNullOrBlank() && !ownerUid.isNullOrBlank()) {
            val restored = UserSession(
                email = ownerEmail,
                displayName = ownerName ?: ownerEmail.substringBefore("@"),
                photoUrl = ownerPhoto,
                uid = ownerUid,
                isSignedIn = true,
                role = AppUserRole.NETWORK_OWNER,
                networkId = ownerNetworkId,
                agentPermissions = AgentPermissions.FULL_OWNER
            )
            saveSession(restored)
            _currentUser.value = restored
        } else {
            // Clear role overrides in prefs so direct MikroTik router login is in pure NETWORK_OWNER mode
            prefs.edit()
                .putString("user_role", AppUserRole.NETWORK_OWNER.name)
                .remove("user_agent_id")
                .remove("user_agent_name")
                .remove("user_bound_party_id")
                .remove("user_bound_party_name")
                .remove("user_access_code")
                .putString("user_agent_permissions_json", AgentPermissions.FULL_OWNER.toJson().toString())
                .apply()
            val cur = _currentUser.value
            if (cur != null && !cur.email.endsWith("@sammikrotik.local")) {
                val updated = cur.copy(
                    role = AppUserRole.NETWORK_OWNER,
                    agentId = null,
                    agentName = null,
                    boundPartyId = null,
                    boundPartyName = null,
                    accessCode = null,
                    agentPermissions = AgentPermissions.FULL_OWNER
                )
                saveSession(updated)
                _currentUser.value = updated
            } else {
                _currentUser.value = null
            }
        }
        _activeRole.value = AppUserRole.NETWORK_OWNER
        _activeAgentPermissions.value = AgentPermissions.FULL_OWNER
        _authError.value = null
    }

    fun resolveActiveNetworkId(): String {
        val cur = _currentUser.value
        if (cur != null && cur.networkId.isNotBlank() && cur.networkId != "DEFAULT_ORG") {
            return cur.networkId
        }
        val ownerUid = prefs.getString("owner_backup_uid", null)
            ?: prefs.getString("user_uid", null)
        return ownerUid?.takeIf { it.isNotBlank() } ?: "DEFAULT_ORG"
    }

    private fun saveOwnerBackupIdentity(session: UserSession) {
        if (session.email.endsWith("@sammikrotik.local")) return
        prefs.edit()
            .putString("owner_backup_email", session.email)
            .putString("owner_backup_name", session.displayName)
            .putString("owner_backup_photo", session.photoUrl)
            .putString("owner_backup_uid", session.uid)
            .putString("owner_backup_network_id", session.networkId)
            .apply()
    }

    /**
     * Digital Handshake: Returns the current [DocumentVerificationRecord] for [documentId],
     * defaulting to [DocumentVerificationStatus.PENDING_POS_CONFIRMATION].
     */
    fun getDocumentVerification(
        documentId: String,
        partyId: String = "",
        networkId: String = resolveActiveNetworkId()
    ): DocumentVerificationRecord {
        return _documentVerifications.value[documentId] ?: DocumentVerificationRecord(
            documentId = documentId,
            partyId = partyId,
            networkId = networkId.ifBlank { "DEFAULT_ORG" },
            status = DocumentVerificationStatus.PENDING_POS_CONFIRMATION
        )
    }

    /**
     * Marks a document as [DocumentVerificationStatus.CONFIRMED_BY_POS] with current timestamp.
     * Metadata-only operation — zero ledger mutations.
     */
    fun confirmDocumentByPos(
        documentId: String,
        partyId: String,
        networkId: String = resolveActiveNetworkId(),
        accessCode: String = _currentUser.value?.accessCode.orEmpty(),
        verifiedAtEpochMs: Long = System.currentTimeMillis()
    ): DocumentVerificationRecord {
        val cleanDocId = documentId.trim()
        require(cleanDocId.isNotBlank()) { "معرف المستند مطلوب لتأكيد الاستلام" }
        val record = DocumentVerificationRecord(
            documentId = cleanDocId,
            partyId = partyId.trim(),
            networkId = networkId.trim().ifBlank { "DEFAULT_ORG" },
            status = DocumentVerificationStatus.CONFIRMED_BY_POS,
            disputeNote = "",
            verifiedAtEpochMs = verifiedAtEpochMs,
            verifiedByAccessCode = accessCode.trim()
        )
        val updated = _documentVerifications.value.toMutableMap()
        updated[cleanDocId] = record
        saveDocumentVerificationsToPrefs(updated)
        return record
    }

    /**
     * Marks a document as [DocumentVerificationStatus.DISPUTED_BY_POS] with the POS merchant's dispute note.
     * Metadata-only operation — zero ledger mutations.
     */
    fun disputeDocumentByPos(
        documentId: String,
        partyId: String,
        disputeNote: String,
        networkId: String = resolveActiveNetworkId(),
        accessCode: String = _currentUser.value?.accessCode.orEmpty(),
        verifiedAtEpochMs: Long = System.currentTimeMillis()
    ): DocumentVerificationRecord {
        val cleanDocId = documentId.trim()
        require(cleanDocId.isNotBlank()) { "معرف المستند مطلوب لتسجيل الملاحظة" }
        val cleanNote = disputeNote.trim().ifBlank { "يوجد ملاحظة من نقطة البيع" }
        val record = DocumentVerificationRecord(
            documentId = cleanDocId,
            partyId = partyId.trim(),
            networkId = networkId.trim().ifBlank { "DEFAULT_ORG" },
            status = DocumentVerificationStatus.DISPUTED_BY_POS,
            disputeNote = cleanNote,
            verifiedAtEpochMs = verifiedAtEpochMs,
            verifiedByAccessCode = accessCode.trim()
        )
        val updated = _documentVerifications.value.toMutableMap()
        updated[cleanDocId] = record
        saveDocumentVerificationsToPrefs(updated)
        return record
    }

    /**
     * Merges remote [DocumentVerificationRecord] items pulled from Firestore into local storage.
     */
    fun mergeRemoteDocumentVerifications(remoteRecords: List<DocumentVerificationRecord>) {
        if (remoteRecords.isEmpty()) return
        val updated = _documentVerifications.value.toMutableMap()
        remoteRecords.forEach { remote ->
            val existing = updated[remote.documentId]
            if (existing == null || remote.verifiedAtEpochMs >= existing.verifiedAtEpochMs) {
                updated[remote.documentId] = remote
            }
        }
        saveDocumentVerificationsToPrefs(updated)
    }

    // =========================================================================
    // Phase 3: In-App POS Restock Orders & Support/Complaint Tickets
    // =========================================================================

    /**
     * Submits a new [PosRestockOrder] in `PENDING` status.
     * Operational workflow only — zero ledger or stock mutations until a Sales Invoice is explicitly issued.
     */
    fun createPosRestockOrder(
        partyId: String,
        partyName: String,
        items: List<PosRestockOrderItem>,
        note: String? = null,
        networkId: String = resolveActiveNetworkId(),
        orderId: String? = null,
        createdAtEpochMs: Long = System.currentTimeMillis()
    ): PosRestockOrder {
        val validItems = items.filter { it.requestedQuantity > 0 && it.packageName.isNotBlank() }
        require(validItems.isNotEmpty()) { "يرجى تحديد باقة واحدة على الأقل بكمية أكبر من صفر" }

        val cleanOrderId = orderId?.trim()?.takeIf { it.isNotBlank() }
            ?: "ORD-${UUID.randomUUID().toString().take(6).uppercase()}"
        val order = PosRestockOrder(
            orderId = cleanOrderId,
            networkId = networkId.trim().ifBlank { "DEFAULT_ORG" },
            partyId = partyId.trim(),
            partyName = partyName.trim().ifBlank { "نقطة بيع" },
            createdAtEpochMs = createdAtEpochMs,
            items = validItems,
            note = note?.trim()?.takeIf { it.isNotBlank() },
            status = RestockOrderStatus.PENDING,
            handledBy = null,
            updatedAtEpochMs = createdAtEpochMs
        )
        val currentList = _posRestockOrders.value.toMutableList()
        val existingIdx = currentList.indexOfFirst { it.orderId == cleanOrderId }
        if (existingIdx >= 0) {
            currentList[existingIdx] = order
        } else {
            currentList.add(0, order)
        }
        savePosRestockOrdersToPrefs(currentList)
        return order
    }

    /**
     * Transitions a [PosRestockOrder] to a new [RestockOrderStatus] (`PENDING` -> `PREPARING` -> `FULFILLED` or `CANCELLED`).
     * Operational workflow only — zero ledger or stock mutations.
     */
    fun updatePosRestockOrderStatus(
        orderId: String,
        newStatus: RestockOrderStatus,
        handledBy: String? = _currentUser.value?.displayName,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    ): PosRestockOrder? {
        val cleanId = orderId.trim()
        val currentList = _posRestockOrders.value.toMutableList()
        val idx = currentList.indexOfFirst { it.orderId == cleanId }
        if (idx < 0) return null
        val updated = currentList[idx].copy(
            status = newStatus,
            handledBy = handledBy?.trim()?.takeIf { it.isNotBlank() } ?: currentList[idx].handledBy,
            updatedAtEpochMs = updatedAtEpochMs
        )
        currentList[idx] = updated
        savePosRestockOrdersToPrefs(currentList)
        return updated
    }

    /**
     * Merges remote [PosRestockOrder] records pulled from Firestore into local storage.
     */
    fun mergeRemotePosRestockOrders(remoteOrders: List<PosRestockOrder>) {
        if (remoteOrders.isEmpty()) return
        val byId = _posRestockOrders.value.associateBy { it.orderId }.toMutableMap()
        remoteOrders.forEach { remote ->
            val existing = byId[remote.orderId]
            if (existing == null || remote.updatedAtEpochMs >= existing.updatedAtEpochMs) {
                byId[remote.orderId] = remote
            }
        }
        savePosRestockOrdersToPrefs(byId.values.toList())
    }

    /**
     * Submits a new [PosSupportTicket] in `OPEN` status.
     */
    fun createPosSupportTicket(
        partyId: String,
        partyName: String,
        category: TicketCategory,
        message: String,
        voucherCode: String? = null,
        networkId: String = resolveActiveNetworkId(),
        ticketId: String? = null,
        createdAtEpochMs: Long = System.currentTimeMillis()
    ): PosSupportTicket {
        val cleanMsg = message.trim()
        require(cleanMsg.isNotBlank()) { "يرجى كتابة تفاصيل البلاغ أو المشكلة" }

        val cleanTicketId = ticketId?.trim()?.takeIf { it.isNotBlank() }
            ?: "TKT-${UUID.randomUUID().toString().take(6).uppercase()}"
        val ticket = PosSupportTicket(
            ticketId = cleanTicketId,
            networkId = networkId.trim().ifBlank { "DEFAULT_ORG" },
            partyId = partyId.trim(),
            partyName = partyName.trim().ifBlank { "نقطة بيع" },
            category = category,
            voucherCode = voucherCode?.trim()?.takeIf { it.isNotBlank() },
            message = cleanMsg,
            status = TicketStatus.OPEN,
            adminReply = null,
            createdAtEpochMs = createdAtEpochMs,
            resolvedAtEpochMs = null
        )
        val currentList = _posSupportTickets.value.toMutableList()
        val existingIdx = currentList.indexOfFirst { it.ticketId == cleanTicketId }
        if (existingIdx >= 0) {
            currentList[existingIdx] = ticket
        } else {
            currentList.add(0, ticket)
        }
        savePosSupportTicketsToPrefs(currentList)
        return ticket
    }

    /**
     * Updates a [PosSupportTicket] status (`OPEN` -> `IN_PROGRESS` -> `RESOLVED`) and/or `adminReply`.
     */
    fun updatePosSupportTicketStatus(
        ticketId: String,
        newStatus: TicketStatus,
        adminReply: String? = null,
        resolvedAtEpochMs: Long? = null
    ): PosSupportTicket? {
        val cleanId = ticketId.trim()
        val currentList = _posSupportTickets.value.toMutableList()
        val idx = currentList.indexOfFirst { it.ticketId == cleanId }
        if (idx < 0) return null
        val current = currentList[idx]
        val cleanReply = adminReply?.trim()?.takeIf { it.isNotBlank() } ?: current.adminReply
        val effectiveResolvedAt = when (newStatus) {
            TicketStatus.RESOLVED -> resolvedAtEpochMs ?: current.resolvedAtEpochMs ?: System.currentTimeMillis()
            else -> null
        }
        val updated = current.copy(
            status = newStatus,
            adminReply = cleanReply,
            resolvedAtEpochMs = effectiveResolvedAt
        )
        currentList[idx] = updated
        savePosSupportTicketsToPrefs(currentList)
        return updated
    }

    /**
     * Merges remote [PosSupportTicket] records pulled from Firestore into local storage.
     */
    fun mergeRemotePosSupportTickets(remoteTickets: List<PosSupportTicket>) {
        if (remoteTickets.isEmpty()) return
        val byId = _posSupportTickets.value.associateBy { it.ticketId }.toMutableMap()
        remoteTickets.forEach { remote ->
            val existing = byId[remote.ticketId]
            val remoteTs = remote.resolvedAtEpochMs ?: remote.createdAtEpochMs
            val existingTs = existing?.let { it.resolvedAtEpochMs ?: it.createdAtEpochMs } ?: 0L
            if (existing == null || remoteTs >= existingTs || remote.adminReply != null) {
                byId[remote.ticketId] = remote
            }
        }
        savePosSupportTicketsToPrefs(byId.values.toList())
    }

    /**
     * Sign out current user and clear credential manager state, while preserving generated access codes,
     * verifications, restock orders, and support tickets.
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        val preservedCodesJson = prefs.getString(KEY_ACCESS_CODES_JSON, null)
        val preservedVerificationsJson = prefs.getString(KEY_DOC_VERIFICATIONS_JSON, null)
        val preservedOrdersJson = prefs.getString(KEY_POS_RESTOCK_ORDERS_JSON, null)
        val preservedTicketsJson = prefs.getString(KEY_POS_SUPPORT_TICKETS_JSON, null)
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance().signOut()
            }
        } catch (e: Exception) {
            Log.w("AuthManager", "Error clearing credentials: ${e.message}")
        }
        prefs.edit().clear().apply()
        if (!preservedCodesJson.isNullOrBlank()) {
            prefs.edit().putString(KEY_ACCESS_CODES_JSON, preservedCodesJson).apply()
        }
        if (!preservedVerificationsJson.isNullOrBlank()) {
            prefs.edit().putString(KEY_DOC_VERIFICATIONS_JSON, preservedVerificationsJson).apply()
        }
        if (!preservedOrdersJson.isNullOrBlank()) {
            prefs.edit().putString(KEY_POS_RESTOCK_ORDERS_JSON, preservedOrdersJson).apply()
        }
        if (!preservedTicketsJson.isNullOrBlank()) {
            prefs.edit().putString(KEY_POS_SUPPORT_TICKETS_JSON, preservedTicketsJson).apply()
        }
        _currentUser.value = null
        _activeRole.value = AppUserRole.NETWORK_OWNER
        _activeAgentPermissions.value = AgentPermissions.FULL_OWNER
    }

    private fun saveSession(session: UserSession) {
        prefs.edit()
            .putString("user_email", session.email)
            .putString("user_name", session.displayName)
            .putString("user_photo", session.photoUrl)
            .putString("user_uid", session.uid)
            .putLong("user_last_login", session.lastLoginEpochMs)
            .putString("user_role", session.role.name)
            .putString("user_agent_id", session.agentId)
            .putString("user_agent_name", session.agentName)
            .putString("user_bound_party_id", session.boundPartyId)
            .putString("user_bound_party_name", session.boundPartyName)
            .putString("user_network_id", session.networkId)
            .putString("user_network_name", session.networkName)
            .putString("user_access_code", session.accessCode)
            .putString("user_agent_permissions_json", session.agentPermissions.toJson().toString())
            .apply()
    }

    companion object {
        const val RC_GOOGLE_SIGN_IN = 9001
        private const val KEY_ACCESS_CODES_JSON = "access_codes_json"
        private const val KEY_DOC_VERIFICATIONS_JSON = "doc_verifications_json"
        private const val KEY_POS_RESTOCK_ORDERS_JSON = "pos_restock_orders_json"
        private const val KEY_POS_SUPPORT_TICKETS_JSON = "pos_support_tickets_json"
    }
}
