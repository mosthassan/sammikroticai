package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.ConnectionTestResult
import com.example.data.network.DeviceStatus
import com.example.data.network.RouterConnectionProfile
import com.example.data.network.RouterConnectionType
import com.example.data.network.RouterProfilesRepository
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCanvas
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

/**
 * Router Login & Multi-Router Connection Portal (Matching MKCards v2.0.1 Screenshot 1 & 2).
 * Allows network engineers to manage multiple routers, test connection probes to port 8728,
 * quick-connect via lightning button, or enter the application in offline mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouterLoginScreen(
    repository: RouterProfilesRepository,
    viewModel: AppViewModel,
    onLoginSuccess: (RouterConnectionProfile) -> Unit,
    onOfflineEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profiles by repository.profiles.collectAsState()
    val activeProfile by repository.activeProfile.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedProfileId by rememberSaveable {
        mutableStateOf(activeProfile?.id ?: profiles.firstOrNull()?.id ?: "")
    }

    // Credentials Form State
    var editingProfileId by rememberSaveable { mutableStateOf<String?>(null) }
    var connectionType by rememberSaveable { mutableStateOf(RouterConnectionType.IP) }
    var hostInput by rememberSaveable { mutableStateOf("10.0.0.1") }
    var portInput by rememberSaveable { mutableStateOf("8728") }
    var usernameInput by rememberSaveable { mutableStateOf("admin") }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberPassword by rememberSaveable { mutableStateOf(true) }
    var syncDbOnLogin by rememberSaveable { mutableStateOf(true) }
    var ignoreSessionsOnSync by rememberSaveable { mutableStateOf(false) }
    var isAdvancedExpanded by rememberSaveable { mutableStateOf(false) }
    var customRouterName by rememberSaveable { mutableStateOf("") }
    var radiusHostInput by rememberSaveable { mutableStateOf("") }

    // Test connection probe feedback
    var isTestingConnection by rememberSaveable { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<ConnectionTestResult?>(null) }

    // Authentication and real connection feedback states
    var isAuthenticatingWithRouter by remember { mutableStateOf(false) }
    var authJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var authErrorTitle by remember { mutableStateOf<String?>(null) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    // Backup restore dialog
    var showRestoreBackupDialog by rememberSaveable { mutableStateOf(false) }
    var restoreBackupText by rememberSaveable { mutableStateOf("") }
    var restoreError by rememberSaveable { mutableStateOf<String?>(null) }

    // Delete confirmation dialog
    var profileToDelete by remember { mutableStateOf<RouterConnectionProfile?>(null) }

    fun loadProfileIntoForm(profile: RouterConnectionProfile) {
        editingProfileId = profile.id
        connectionType = profile.connectionType
        hostInput = profile.host
        portInput = profile.apiPort.toString()
        usernameInput = profile.username
        passwordInput = profile.password
        rememberPassword = profile.isRememberPassword
        syncDbOnLogin = profile.syncDatabaseOnLogin
        ignoreSessionsOnSync = profile.ignoreSessionsOnSync
        customRouterName = profile.name
        radiusHostInput = profile.radiusHost
        selectedTab = 1
    }

    fun resetForm() {
        editingProfileId = null
        connectionType = RouterConnectionType.IP
        hostInput = "10.0.0.1"
        portInput = "8728"
        usernameInput = "admin"
        passwordInput = ""
        rememberPassword = true
        syncDbOnLogin = true
        ignoreSessionsOnSync = false
        isAdvancedExpanded = false
        customRouterName = ""
        radiusHostInput = ""
        connectionTestResult = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // 1. Top Branded Header (Matching MkCards Header v2.0.1)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("router_login_header_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3A47)),
                border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right side (RTL title & version)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black,
                            border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "شعار سام ميكروتك الرسمي",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Column {
                            Text(
                                text = "سام ميكروتك",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF134E5E)
                            ) {
                                Text(
                                    text = "v2.0.1 • إصدار الشبكات",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MikroTikCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Left side toggles: Language & Theme
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "اللغة الحالية: العربية", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF174C5B))
                                .testTag("btn_lang_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = TextPrimaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "الوضع الليلي عالي التباين مفعل", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF174C5B))
                                .testTag("btn_theme_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nightlight,
                                contentDescription = "Theme",
                                tint = SemanticWarningAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Segmented Tab Row (Matching MkCards Tabs: الراوترات المسجلة vs بيانات الدخول)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CyberDarkCardElevated,
                contentColor = MikroTikCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MikroTikCyan,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "الراوترات المسجّلة",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    selectedContentColor = MikroTikCyan,
                    unselectedContentColor = TextMutedDark,
                    modifier = Modifier.testTag("tab_saved_routers")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "بيانات الدخول",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    selectedContentColor = MikroTikCyan,
                    unselectedContentColor = TextMutedDark,
                    modifier = Modifier.testTag("tab_login_credentials")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Tab Body
            Box(modifier = Modifier.weight(1f)) {
                if (selectedTab == 0) {
                    // TAB 0: الراوترات المسجلة
                    SavedRoutersTabContent(
                        profiles = profiles,
                        selectedProfileId = selectedProfileId,
                        onSelectProfile = { selectedProfileId = it },
                        onQuickConnect = { profile ->
                            authJob?.cancel()
                            authJob = scope.launch {
                                try {
                                    isAuthenticatingWithRouter = true
                                    authErrorMessage = null
                                    authErrorTitle = null
                                    val res = viewModel.verifyAndConnectRouter(profile)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "تم الاتصال الحقيقي بالراوتر بنجاح", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess(profile)
                                    } else {
                                        authErrorTitle = "تعذر الاتصال بالراوتر (${profile.name})"
                                        authErrorMessage = res.exceptionOrNull()?.message ?: "فشل الاتصال بالراوتر"
                                    }
                                } finally {
                                    isAuthenticatingWithRouter = false
                                }
                            }
                        },
                        onEditProfile = { profile ->
                            loadProfileIntoForm(profile)
                        },
                        onDeleteProfile = { profile ->
                            profileToDelete = profile
                        },
                        onAddNew = {
                            resetForm()
                            selectedTab = 1
                        },
                        onRefreshAll = {
                            scope.launch {
                                for (p in profiles) {
                                    val res = repository.testConnection(p.host, p.apiPort)
                                    repository.saveProfile(
                                        p.copy(
                                            lastLatencyMs = if (res.success) res.latencyMs else null,
                                            status = if (res.success) DeviceStatus.ONLINE else DeviceStatus.OFFLINE
                                        )
                                    )
                                }
                                Toast.makeText(context, "تم تحديث استجابة الراوترات", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLoginSelected = {
                            val target = profiles.find { it.id == selectedProfileId } ?: profiles.firstOrNull()
                            if (target != null) {
                                authJob?.cancel()
                                authJob = scope.launch {
                                    try {
                                        isAuthenticatingWithRouter = true
                                        authErrorMessage = null
                                        authErrorTitle = null
                                        val res = viewModel.verifyAndConnectRouter(target)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "تم الاتصال الحقيقي بالراوتر بنجاح", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess(target)
                                        } else {
                                            authErrorTitle = "تعذر تسجيل الدخول للراوتر (${target.name})"
                                            authErrorMessage = res.exceptionOrNull()?.message ?: "فشل الاتصال بالراوتر"
                                        }
                                    } finally {
                                        isAuthenticatingWithRouter = false
                                    }
                                }
                            } else {
                                Toast.makeText(context, "يرجى اختيار راوتر أولاً", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOfflineEnter = onOfflineEnter,
                        onRestoreBackup = {
                            restoreBackupText = ""
                            restoreError = null
                            showRestoreBackupDialog = true
                        }
                    )
                } else {
                    // TAB 1: بيانات الدخول
                    LoginCredentialsTabContent(
                        editingProfileId = editingProfileId,
                        connectionType = connectionType,
                        onConnectionTypeChange = { connectionType = it },
                        hostInput = hostInput,
                        onHostChange = { hostInput = it },
                        portInput = portInput,
                        onPortChange = { portInput = it },
                        usernameInput = usernameInput,
                        onUsernameChange = { usernameInput = it },
                        passwordInput = passwordInput,
                        onPasswordChange = { passwordInput = it },
                        isPasswordVisible = isPasswordVisible,
                        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                        rememberPassword = rememberPassword,
                        onRememberPasswordChange = { rememberPassword = it },
                        syncDbOnLogin = syncDbOnLogin,
                        onSyncDbChange = { syncDbOnLogin = it },
                        ignoreSessionsOnSync = ignoreSessionsOnSync,
                        onIgnoreSessionsChange = { ignoreSessionsOnSync = it },
                        isAdvancedExpanded = isAdvancedExpanded,
                        onToggleAdvanced = { isAdvancedExpanded = !isAdvancedExpanded },
                        customRouterName = customRouterName,
                        onCustomRouterNameChange = { customRouterName = it },
                        radiusHostInput = radiusHostInput,
                        onRadiusHostChange = { radiusHostInput = it },
                        isTestingConnection = isTestingConnection,
                        connectionTestResult = connectionTestResult,
                        onTestConnection = {
                            scope.launch {
                                isTestingConnection = true
                                val port = portInput.toIntOrNull() ?: 8728
                                val res = repository.testConnection(hostInput.trim(), port)
                                connectionTestResult = res
                                isTestingConnection = false
                            }
                        },
                        onLoginAndSave = {
                            if (hostInput.isBlank()) {
                                Toast.makeText(context, "يرجى كتابة عنوان الراوتر أو الرابط", Toast.LENGTH_SHORT).show()
                                return@LoginCredentialsTabContent
                            }
                            val port = portInput.toIntOrNull() ?: 8728
                            val nameToUse = if (customRouterName.isNotBlank()) customRouterName.trim() else hostInput.trim()
                            val profile = RouterConnectionProfile(
                                id = editingProfileId ?: java.util.UUID.randomUUID().toString(),
                                name = nameToUse,
                                connectionType = connectionType,
                                host = hostInput.trim(),
                                apiPort = port,
                                username = usernameInput.trim().ifEmpty { "admin" },
                                password = passwordInput,
                                isRememberPassword = rememberPassword,
                                syncDatabaseOnLogin = syncDbOnLogin,
                                ignoreSessionsOnSync = ignoreSessionsOnSync,
                                radiusHost = radiusHostInput.trim(),
                                isActive = false,
                                lastLatencyMs = null,
                                status = DeviceStatus.UNCHECKED,
                                lastConnectedAt = null
                            )
                            authJob?.cancel()
                            authJob = scope.launch {
                                try {
                                    isAuthenticatingWithRouter = true
                                    authErrorMessage = null
                                    authErrorTitle = null
                                    repository.saveProfile(profile)
                                    val res = viewModel.verifyAndConnectRouter(profile)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "تم حفظ بيانات الراوتر والاتصال الحقيقي بنجاح", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess(profile)
                                    } else {
                                        authErrorTitle = "فشل الاتصال بالراوتر (${profile.host}:$port)"
                                        authErrorMessage = res.exceptionOrNull()?.message ?: "فشل الاتصال بمنفذ الراوتر"
                                    }
                                } finally {
                                    isAuthenticatingWithRouter = false
                                }
                            }
                        },
                        onOfflineEnter = onOfflineEnter,
                        onCancel = {
                            resetForm()
                            selectedTab = 0
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    profileToDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = {
                Text(
                    text = "حذف ملف تعريف الراوتر",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف بيانات الراوتر (${profile.name} - ${profile.host}) من هذا الجهاز؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteProfile(profile.id)
                            profileToDelete = null
                            Toast.makeText(context, "تم حذف الراوتر", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticExpenseRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("إلغاء", color = TextSecondaryDark)
                }
            },
            containerColor = CyberDarkCardElevated
        )
    }

    // Restore Backup Dialog
    if (showRestoreBackupDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreBackupDialog = false },
            title = {
                Text(
                    text = "استرجاع نسخة مشفرة للراوترات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "الصق النص المشفر لملف النسخ الاحتياطي لاسترجاع كافة الراوترات المحفوظة:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                    OutlinedTextField(
                        value = restoreBackupText,
                        onValueChange = {
                            restoreBackupText = it
                            restoreError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("input_restore_backup"),
                        placeholder = { Text("الصق كود JSON أو النص المشفر هنا...", color = TextMutedDark) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MikroTikCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )
                    restoreError?.let { err ->
                        Text(text = err, color = SemanticExpenseRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreBackupText.isBlank()) {
                            restoreError = "يرجى لصق بيانات النسخة الاحتياطية"
                            return@Button
                        }
                        scope.launch {
                            val success = repository.restoreEncryptedBackup(restoreBackupText.trim())
                            if (success) {
                                showRestoreBackupDialog = false
                                Toast.makeText(context, "تم استرجاع قائمة الراوترات بنجاح!", Toast.LENGTH_LONG).show()
                            } else {
                                restoreError = "فشل في قراءة أو فك تشفير النسخة. تأكد من صحة النص."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan)
                ) {
                    Text("استرجاع الآن", color = Color(0xFF03252E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreBackupDialog = false }) {
                    Text("إلغاء", color = TextSecondaryDark)
                }
            },
            containerColor = CyberDarkCardElevated
        )
    }

    // Live Authentication Progress Dialog
    if (isAuthenticatingWithRouter) {
        AlertDialog(
            onDismissRequest = {
                authJob?.cancel()
                isAuthenticatingWithRouter = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = MikroTikCyan,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        text = "جاري الاتصال والتحقق من الراوتر...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "يتم الآن فحص منفذ API 8728 الفعلي ومصادقة المستخدم مع الراوتر بمهلة سريعة (أقل من 3 ثوانٍ)...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "💡 إذا كنت تعمل في بيئة اختبار سحابية أو الراوتر غير متاح على نفس شبكتك حالياً، يمكنك النقر على 'دخول بدون اتصال'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MikroTikCyan
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authJob?.cancel()
                        isAuthenticatingWithRouter = false
                        onOfflineEnter()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan)
                ) {
                    Text("دخول بدون اتصال (Offline) ⚡", color = Color(0xFF03252E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        authJob?.cancel()
                        isAuthenticatingWithRouter = false
                    }
                ) {
                    Text("إلغاء", color = TextSecondaryDark)
                }
            },
            containerColor = CyberDarkCardElevated
        )
    }

    // Authentication Failure Alert Dialog
    authErrorMessage?.let { errMsg ->
        AlertDialog(
            onDismissRequest = {
                authErrorMessage = null
                authErrorTitle = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = SemanticExpenseRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = authErrorTitle ?: "فشل الاتصال بالراوتر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = errMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SemanticExpenseRed,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(color = CyberBorder)
                    Text(
                        text = "🔒 سياسة الأمان والبيانات الحقيقية:\n• إذا كان هاتفك متصلاً بالراوتر مباشرة تأكد من تفعيل خدمة API عبر: /ip service enable api\n• أو يمكنك الدخول بوضع عدم الاتصال لاستعراض الشاشات والبيانات وتصميم الكروت بدون عائق.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authErrorMessage = null
                        authErrorTitle = null
                        onOfflineEnter()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan)
                ) {
                    Text("الدخول بوضع عدم الاتصال 🚀", color = Color(0xFF03252E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        authErrorMessage = null
                        authErrorTitle = null
                    }
                ) {
                    Text("إغلاق", color = TextSecondaryDark)
                }
            },
            containerColor = CyberDarkCardElevated
        )
    }
}

/**
 * Tab 0: Saved Routers List & Quick Connect Action
 */
@Composable
private fun SavedRoutersTabContent(
    profiles: List<RouterConnectionProfile>,
    selectedProfileId: String,
    onSelectProfile: (String) -> Unit,
    onQuickConnect: (RouterConnectionProfile) -> Unit,
    onEditProfile: (RouterConnectionProfile) -> Unit,
    onDeleteProfile: (RouterConnectionProfile) -> Unit,
    onAddNew: () -> Unit,
    onRefreshAll: () -> Unit,
    onLoginSelected: () -> Unit,
    onOfflineEnter: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Toolbar with Add and Refresh icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الراوترات المسجّلة (${profiles.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onAddNew,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberDarkCardElevated)
                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                        .testTag("btn_add_router")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Router",
                        tint = MikroTikCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onRefreshAll,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberDarkCardElevated)
                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                        .testTag("btn_refresh_routers")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Probe",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Router Cards List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (profiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "لا يوجد راوترات مسجلة حالياً",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                            Button(
                                onClick = onAddNew,
                                colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan)
                            ) {
                                Text("إضافة أول راوتر الآن", color = Color(0xFF03252E), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(profiles, key = { it.id }) { profile ->
                    val isSelected = profile.id == selectedProfileId
                    RouterCardItem(
                        profile = profile,
                        isSelected = isSelected,
                        onCardClick = { onSelectProfile(profile.id) },
                        onQuickConnect = { onQuickConnect(profile) },
                        onEdit = { onEditProfile(profile) },
                        onDelete = { onDeleteProfile(profile) }
                    )
                }
            }
        }

        // Bottom Action Buttons (Matching Screenshot 1)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary: تسجيل الدخول
                Button(
                    onClick = onLoginSelected,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_login_connect"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D5E72))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "تسجيل الدخول",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Secondary: الدخول للبرنامج (Offline)
                OutlinedButton(
                    onClick = onOfflineEnter,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_offline_enter"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CyberDarkCardElevated,
                        contentColor = TextPrimaryDark
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "الدخول للبرنامج",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Restore Backup Button
            OutlinedButton(
                onClick = onRestoreBackup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("btn_restore_encrypted_backup"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyberBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CyberDarkSurface,
                    contentColor = MikroTikCyan
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = MikroTikCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "استرجاع نسخة مشفرة",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MikroTikCyan
                    )
                }
            }
        }
    }
}

/**
 * Single Router Card Item with ⚡ Lightning Bolt quick connect
 */
@Composable
private fun RouterCardItem(
    profile: RouterConnectionProfile,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onQuickConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("router_profile_card_${profile.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF0C2B34) else CyberDarkCardElevated
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MikroTikCyan else CyberBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row: Router Name and IP/Port
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                // Status chip
                val statusColor = when (profile.status) {
                    DeviceStatus.ONLINE -> StatusOnline
                    DeviceStatus.WARNING -> SemanticWarningAmber
                    DeviceStatus.OFFLINE -> SemanticExpenseRed
                    DeviceStatus.UNCHECKED -> Color(0xFF94A3B8)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (profile.lastLatencyMs != null) "${profile.status.labelArabic} • ${profile.lastLatencyMs}ms" else profile.status.labelArabic,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Sub-tags: IP:Port and Username
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tag 1: Host and Port
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberDarkSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lan,
                            contentDescription = null,
                            tint = MikroTikCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${profile.host}:${profile.apiPort}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Tag 2: Username
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberDarkSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = profile.username,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom action row: Delete, Edit, and Quick Connect ⚡
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = SemanticExpenseRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MikroTikCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Golden Quick Connect Button ⚡ (Matching MkCards)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E3A44),
                    border = BorderStroke(2.dp, SemanticWarningAmber),
                    modifier = Modifier
                        .size(46.dp)
                        .clickable { onQuickConnect() }
                        .testTag("btn_quick_connect_${profile.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Quick Connect",
                            tint = SemanticWarningAmber,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Login Credentials Form (Matching MkCards Screenshot 2)
 */
@Composable
private fun LoginCredentialsTabContent(
    editingProfileId: String?,
    connectionType: RouterConnectionType,
    onConnectionTypeChange: (RouterConnectionType) -> Unit,
    hostInput: String,
    onHostChange: (String) -> Unit,
    portInput: String,
    onPortChange: (String) -> Unit,
    usernameInput: String,
    onUsernameChange: (String) -> Unit,
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    rememberPassword: Boolean,
    onRememberPasswordChange: (Boolean) -> Unit,
    syncDbOnLogin: Boolean,
    onSyncDbChange: (Boolean) -> Unit,
    ignoreSessionsOnSync: Boolean,
    onIgnoreSessionsChange: (Boolean) -> Unit,
    isAdvancedExpanded: Boolean,
    onToggleAdvanced: () -> Unit,
    customRouterName: String,
    onCustomRouterNameChange: (String) -> Unit,
    radiusHostInput: String,
    onRadiusHostChange: (String) -> Unit,
    isTestingConnection: Boolean,
    connectionTestResult: ConnectionTestResult?,
    onTestConnection: () -> Unit,
    onLoginAndSave: () -> Unit,
    onOfflineEnter: () -> Unit,
    onCancel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Connection Mode Selector Chips (IP, Domain, Cloud)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RouterConnectionType.values().forEach { type ->
                    val isChipSelected = type == connectionType
                    val chipBg = if (isChipSelected) Color(0xFF0E5B6E) else CyberDarkCardElevated
                    val chipText = if (isChipSelected) TextPrimaryDark else TextSecondaryDark
                    val chipBorder = if (isChipSelected) MikroTikCyan else CyberBorder

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onConnectionTypeChange(type) }
                            .testTag("chip_conn_type_${type.name}"),
                        shape = RoundedCornerShape(12.dp),
                        color = chipBg,
                        border = BorderStroke(1.dp, chipBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (type) {
                                    RouterConnectionType.IP -> Icons.Default.Lan
                                    RouterConnectionType.DOMAIN -> Icons.Default.Language
                                    RouterConnectionType.CLOUD -> Icons.Default.Cloud
                                },
                                contentDescription = null,
                                tint = if (isChipSelected) MikroTikCyan else TextMutedDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = type.labelArabic,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Normal,
                                color = chipText,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 2. Host and Port Fields
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Port field (e.g. 8728)
                OutlinedTextField(
                    value = portInput,
                    onValueChange = onPortChange,
                    modifier = Modifier
                        .width(95.dp)
                        .testTag("input_port"),
                    label = { Text("بورت", color = TextSecondaryDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikroTikCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Host field (IP, Domain, or Cloud ID)
                OutlinedTextField(
                    value = hostInput,
                    onValueChange = onHostChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_host"),
                    label = {
                        Text(
                            text = when (connectionType) {
                                RouterConnectionType.IP -> "عنوان IP للراوتر"
                                RouterConnectionType.DOMAIN -> "رابط Domain أو DDNS"
                                RouterConnectionType.CLOUD -> "رقم السيريال أو الكلاود"
                            },
                            color = TextSecondaryDark
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (connectionType) {
                                RouterConnectionType.IP -> Icons.Default.Lan
                                RouterConnectionType.DOMAIN -> Icons.Default.Language
                                RouterConnectionType.CLOUD -> Icons.Default.Cloud
                            },
                            contentDescription = null,
                            tint = MikroTikCyan
                        )
                    },
                    trailingIcon = {
                        if (hostInput.isNotEmpty()) {
                            IconButton(onClick = { onHostChange("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMutedDark)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikroTikCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        // Quick IP Presets for fast entry
        if (connectionType == RouterConnectionType.IP) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("10.0.0.1", "192.168.88.1", "10.10.1.1", "192.168.1.1").forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .clickable { onHostChange(preset) }
                                .padding(vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = CyberDarkSurface,
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                color = MikroTikCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Username Field
        item {
            OutlinedTextField(
                value = usernameInput,
                onValueChange = onUsernameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_username"),
                label = { Text("اسم المستخدم", color = TextSecondaryDark) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA)
                    )
                },
                trailingIcon = {
                    if (usernameInput.isNotEmpty()) {
                        IconButton(onClick = { onUsernameChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMutedDark)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MikroTikCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // 4. Password Field with Visibility Toggle
        item {
            OutlinedTextField(
                value = passwordInput,
                onValueChange = onPasswordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_password"),
                label = { Text("كلمة المرور", color = TextSecondaryDark) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SemanticWarningAmber
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = TextSecondaryDark
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MikroTikCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // 5. Primary Login & Connect Button
        item {
            Button(
                onClick = onLoginAndSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_submit_login"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E5B6E))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (editingProfileId != null) "حفظ التعديلات والاتصال" else "تسجيل الدخول والاتصال بالراوتر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        tint = TextPrimaryDark
                    )
                }
            }
        }

        // 6. Action Row: اختبار الاتصال & معرف حسابي
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // اختبار الاتصال
                OutlinedButton(
                    onClick = onTestConnection,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_test_connection"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CyberDarkCardElevated,
                        contentColor = MikroTikCyan
                    )
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            color = MikroTikCyan,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(text = "اختبار الاتصال", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // معرف حسابي
                OutlinedButton(
                    onClick = { /* Display account ID */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_account_id"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CyberDarkCardElevated,
                        contentColor = TextSecondaryDark
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(text = "معرف حسابي")
                    }
                }
            }
        }

        // Probe Test Result Banner
        connectionTestResult?.let { res ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (res.success) SemanticIncomeGreen.copy(alpha = 0.15f) else SemanticExpenseRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (res.success) SemanticIncomeGreen else SemanticExpenseRed)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (res.success) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (res.success) SemanticIncomeGreen else SemanticExpenseRed
                        )
                        Text(
                            text = res.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (res.success) SemanticIncomeGreen else SemanticExpenseRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 7. Checkboxes: تذكر كلمة المرور & المزامنة
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Checkbox 1: تذكر كلمة المرور
                OptionCheckCard(
                    title = "تذكر كلمة المرور (سيتم حفظها مشفرة)",
                    icon = Icons.Default.Lock,
                    iconTint = SemanticWarningAmber,
                    checked = rememberPassword,
                    onCheckedChange = onRememberPasswordChange,
                    testTag = "check_remember_password"
                )

                // Checkbox 2: دخول بعمل مزامنة لقاعدة البيانات
                OptionCheckCard(
                    title = "دخول بعمل مزامنه لقاعدة البيانات",
                    icon = Icons.Default.Sync,
                    iconTint = MikroTikCyan,
                    checked = syncDbOnLogin,
                    onCheckedChange = onSyncDbChange,
                    testTag = "check_sync_db"
                )

                // Checkbox 3: مزامنة بـ (تجاهل الجلسات والمبيعات)
                OptionCheckCard(
                    title = "مزامنة بـ (تجاهل الجلسات والمبيعات لتسريع الفتح)",
                    icon = Icons.Default.Bolt,
                    iconTint = Color(0xFFF59E0B),
                    checked = ignoreSessionsOnSync,
                    onCheckedChange = onIgnoreSessionsChange,
                    testTag = "check_ignore_sessions"
                )

                // Switch: متقدم
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleAdvanced() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                    border = BorderStroke(1.dp, CyberBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MikroTikCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "خيارات متقدمة",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Switch(
                            checked = isAdvancedExpanded,
                            onCheckedChange = { onToggleAdvanced() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF03252E),
                                checkedTrackColor = MikroTikCyan
                            )
                        )
                    }
                }

                // Advanced Fields (Expanded)
                AnimatedVisibility(
                    visible = isAdvancedExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customRouterName,
                            onValueChange = onCustomRouterNameChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_router_name"),
                            label = { Text("اسم الراوتر التوضيحي (اختياري)", color = TextSecondaryDark) },
                            placeholder = { Text("مثال: راوتر البرج الرئيسي", color = TextMutedDark) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MikroTikCyan,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = radiusHostInput,
                            onValueChange = onRadiusHostChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_radius_host"),
                            label = { Text("عنوان RADIUS Server (اختياري)", color = TextSecondaryDark) },
                            placeholder = { Text("مثال: 10.0.0.254", color = TextMutedDark) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MikroTikCyan,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // 8. Offline Mode and Cancel Bottom Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOfflineEnter,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CyberDarkSurface,
                        contentColor = TextPrimaryDark
                    )
                ) {
                    Text("الدخول بدون اتصال")
                }

                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("العودة للقائمة", color = TextSecondaryDark)
                }
            }
        }
    }
}

/**
 * Reusable Card with Checkbox for connection options
 */
@Composable
private fun OptionCheckCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        border = BorderStroke(1.dp, if (checked) MikroTikCyan.copy(alpha = 0.5f) else CyberBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Medium
                )
            }
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MikroTikCyan,
                    checkmarkColor = Color(0xFF03252E),
                    uncheckedColor = TextMutedDark
                )
            )
        }
    }
}
