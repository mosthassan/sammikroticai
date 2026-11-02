package com.example.ui.screens

import android.app.Activity
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sync.SyncLogItem
import com.example.data.sync.SyncState
import com.example.ui.theme.BrandCyanPrimary
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.viewmodel.AppViewModel
import com.example.util.findActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncSheet(
    viewModel: AppViewModel,
    sheetState: SheetState,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val currentUser by viewModel.currentUser.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val syncMeta by viewModel.syncMetadata.collectAsState()
    val syncHistory by viewModel.syncHistory.collectAsState()

    var showEmailEditDialog by remember { mutableStateOf(false) }
    var customEmailInput by remember { mutableStateOf("") }

    val lastLocalBackupTime by viewModel.lastLocalBackupTime.collectAsState()
    val isBackupExporting by viewModel.isBackupExporting.collectAsState()
    val clipboard = LocalClipboardManager.current
    var showLocalRestoreConfirmDialog by remember { mutableStateOf<Uri?>(null) }
    var localBackupMessage by remember { mutableStateOf<String?>(null) }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val passphraseStorage = remember { com.example.data.sync.BackupPassphraseStorage(context) }

    var showBackupPassphraseDialog by remember { mutableStateOf(false) }
    var backupPassphrase by remember { mutableStateOf("") }
    var backupConfirmPassphrase by remember { mutableStateOf("") }
    var savePassphraseInKeystore by remember { mutableStateOf(false) }
    var passphraseError by remember { mutableStateOf<String?>(null) }

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var restoreRemoteMeta by remember { mutableStateOf<com.example.data.sync.CloudBackupMeta?>(null) }
    var restorePassphrase by remember { mutableStateOf("") }
    var restorePassphraseError by remember { mutableStateOf<String?>(null) }
    var isCheckingRemoteBackup by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(currentUser?.email) {
        val email = currentUser?.email ?: ""
        if (email.isNotBlank()) {
            val saved = passphraseStorage.getPassphrase(email)
            if (!saved.isNullOrBlank()) {
                backupPassphrase = saved
                backupConfirmPassphrase = saved
                restorePassphrase = saved
                savePassphraseInKeystore = true
            }
        }
    }

    val restoreFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            showLocalRestoreConfirmDialog = uri
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("cloud_sync_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BrandCyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = BrandCyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "المزامنة السحابية ودخول Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Firebase Firestore • عزل متعدد المستأجرين",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.syncPushToFirebase() },
                    modifier = Modifier.testTag("btn_quick_sync_header")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تحديث المزامنة",
                        tint = BrandCyanPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Google Account Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_account_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentUser?.displayName?.ifBlank { "مستخدم Google" } ?: "غير متصل بحساب",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = currentUser?.email ?: "لم يتم تحديد بريد",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BrandCyanPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Connection Status Tag
                        val fbAuthUser = runCatching {
                            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) com.google.firebase.auth.FirebaseAuth.getInstance().currentUser else null
                        }.getOrNull()
                        val isOnline = fbAuthUser != null
                        val hasLocalEmail = currentUser != null
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isOnline) SemanticIncomeGreen.copy(alpha = 0.15f)
                                    else if (hasLocalEmail) Color(0xFFFFB300).copy(alpha = 0.18f)
                                    else SemanticExpenseRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isOnline) "متصل وموثق (Google)"
                                       else if (hasLocalEmail) "توثيق Google مطلوب"
                                       else "غير متصل",
                                color = if (isOnline) SemanticIncomeGreen
                                        else if (hasLocalEmail) Color(0xFFFFB300)
                                        else SemanticExpenseRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons for Google One-Tap Sign In or Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.signInWithGoogle(activity ?: context)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_google_one_tap"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "دخول Google بنقرة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                customEmailInput = currentUser?.email ?: ""
                                showEmailEditDialog = true
                            },
                            modifier = Modifier.testTag("btn_switch_account"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "تغيير البريد",
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Safe fallback button for test/debug environments without matching SHA-1
                    OutlinedButton(
                        onClick = {
                            customEmailInput = currentUser?.email ?: ""
                            showEmailEditDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_dev_fallback_login"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = BrandCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "دخول بديل بالبريد (بيئة التطوير / بدون SHA-1)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!authError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = SemanticExpenseRed.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = SemanticExpenseRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$authError\n(إذا كنت في بيئة تطوير بدون تطابق SHA-1، يمكنك استخدام زر 'دخول بديل بالبريد')",
                                    fontSize = 10.sp,
                                    color = SemanticExpenseRed,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Multi-Tenant Isolation Guarantee
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = BrandCyanPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "البريد هو المعرف الحصري للمزامنة والبيانات (عزل سحابي آمن 100%)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Sync Progress / State Banner
            when (val state = syncState) {
                is SyncState.InProgress -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_progress_card"),
                        colors = CardDefaults.cardColors(containerColor = BrandCyanPrimary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = BrandCyanPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BrandCyanPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Localized stages pipeline indicator
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "مراحل المعالجة: تحضير ➔ ضغط ➔ تشفير ➔ رفع الأجزاء ➔ تحقق ➔ تم",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                is SyncState.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SemanticIncomeGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SemanticIncomeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.message,
                                color = SemanticIncomeGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                is SyncState.Error -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_error_surface"),
                        color = SemanticExpenseRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SemanticExpenseRed.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = SemanticExpenseRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.message,
                                    color = SemanticExpenseRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Fail-safe rollback recovery button
                            OutlinedButton(
                                onClick = { viewModel.restorePreAttemptState() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticExpenseRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SemanticExpenseRed.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_restore_pre_attempt_state")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استعادة حالة ما قبل المحاولة (Rollback Safety Snapshot)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                SyncState.Idle -> { /* Nothing */ }
            }

            // 3. Sync Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val signedIn = currentUser?.isSignedIn == true
                        if (!signedIn) {
                            viewModel.signInWithGoogle(activity ?: context)
                        } else {
                            passphraseError = null
                            showBackupPassphraseDialog = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_sync_push_cloud"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("رفع ومزامنة الآن", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isCheckingRemoteBackup = true
                            val meta = viewModel.checkRemoteCloudBackup()
                            isCheckingRemoteBackup = false
                            restoreRemoteMeta = meta
                            restorePassphraseError = null
                            showRestoreConfirmDialog = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_restore_from_cloud"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isCheckingRemoteBackup
                ) {
                    if (isCheckingRemoteBackup) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BrandCyanPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فحص السحابة...", fontSize = 11.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استعادة من السحابة", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3.5. Local Full Backup & Instant Share Section (تأمين شامل وبديل السحابة عند انقطاعها)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("local_backup_share_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    BrandCyanPrimary.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BrandCyanPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = BrandCyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "النسخ الاحتياطي المحلي والمشاركة",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "تأمين شامل بدون إنترنت (بديل آمن عند تعذر السحابة)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Status badge showing last local backup
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (lastLocalBackupTime != null) SemanticIncomeGreen.copy(alpha = 0.15f) else Color(0xFFFFB300).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (lastLocalBackupTime != null) "مؤمّن محلياً ✓" else "يوصى بعمل نسخة",
                                color = if (lastLocalBackupTime != null) SemanticIncomeGreen else Color(0xFFFFB300),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "ملف نسخة احتياطية محلي كامل (.json) يحتوي على كامل القيود المحاسبية، السندات، الفواتير، أرصدة العملاء، وأجهزة الشبكة مع التوقيع الرقمي SHA-256.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    lastLocalBackupTime?.let { epoch ->
                        val dateFormatted = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(epoch))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SemanticIncomeGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "آخر تصدير محلي: $dateFormatted",
                                fontSize = 11.sp,
                                color = SemanticIncomeGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 1-Tap Share Full Backup Button
                    Button(
                        onClick = {
                            viewModel.exportAndShareLocalBackup(context) { success, msg ->
                                localBackupMessage = msg
                            }
                        },
                        enabled = !isBackupExporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_share_local_backup"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isBackupExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري استخراج وتجهيز الملف...", fontSize = 12.sp, color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "عمل نسخة كاملة ومشاركتها (Share Backup)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Secondary Actions: Restore from file & Copy JSON Text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                restoreFilePickerLauncher.launch("application/json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_restore_local_backup_file"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استعادة من ملف", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.exportBackup { json ->
                                    clipboard.setText(AnnotatedString(json))
                                    localBackupMessage = "تم نسخ نص النسخة الاحتياطية إلى الحافظة بنجاح (${json.length} حرف)"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_copy_local_backup_json"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ كود النسخة", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    localBackupMessage?.let { msg ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = SemanticIncomeGreen.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelSmall,
                                color = SemanticIncomeGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Live Sync Telemetry & Record Counters
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "إحصائيات السجلات السحابية",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val lastTime = syncMeta?.lastSyncedAt ?: 0L
                        val timeStr = if (lastTime > 0L) {
                            com.example.util.TransactionDateUtils.formatDateTime(lastTime)
                        } else {
                            "لم تتم بعد"
                        }
                        Text(
                            text = "آخر مزامنة: $timeStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SyncMetricItem(title = "المستندات والسندات", count = syncMeta?.totalDocuments ?: 0)
                        SyncMetricItem(title = "قيود اليومية", count = syncMeta?.totalJournalLines ?: 0)
                        SyncMetricItem(title = "الأطراف والوكلاء", count = syncMeta?.totalParties ?: 0)
                        SyncMetricItem(title = "باقات الكروت", count = syncMeta?.totalPackages ?: 0)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SyncMetricItem(title = "أجهزة الشبكة", count = syncMeta?.totalDevices ?: syncMeta?.totalNetworkDevices ?: 0)
                        SyncMetricItem(title = "أسعار الصرف", count = syncMeta?.totalCurrencyRates ?: 0)
                        SyncMetricItem(title = "الخزائن", count = syncMeta?.totalTreasuries ?: 0)
                        SyncMetricItem(title = "هوية الشبكة", count = if (syncMeta?.hasNetworkProfile == true || !syncMeta?.networkName.isNullOrBlank()) 1 else 0)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Recent Sync Log
            Text(
                text = "سجل عمليات المزامنة الأخيرة",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (syncHistory.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد عمليات مزامنة سابقة بعد. اضغط 'رفع ومزامنة الآن' للمزامنة الأولى.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    syncHistory.take(4).forEach { item ->
                        SyncHistoryRow(item)
                    }
                }
            }
        }
    }

    // Dialog for changing / setting custom email
    if (showEmailEditDialog) {
        AlertDialog(
            onDismissRequest = { showEmailEditDialog = false },
            title = { Text("تعيين بريد المزامنة السحابية", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "أدخل بريد حساب Google ليكون المعرف الحصري للمزامنة والبيانات الخاصة بشبكتك:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customEmailInput,
                        onValueChange = { customEmailInput = it },
                        label = { Text("بريد حساب Google") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_email")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customEmailInput.isNotBlank()) {
                            viewModel.signInDirectly(customEmailInput)
                        }
                        showEmailEditDialog = false
                    },
                    modifier = Modifier.testTag("btn_confirm_custom_email")
                ) {
                    Text("حفظ وتفعيل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailEditDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Confirmation dialog for restoring local backup from picked file
    showLocalRestoreConfirmDialog?.let { fileUri ->
        AlertDialog(
            onDismissRequest = { showLocalRestoreConfirmDialog = null },
            title = { Text("تأكيد استعادة النسخة المحلية", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل أنت متأكد من استعادة البيانات من هذا الملف؟\n\n" +
                    "سيتم فحص الملف واستيراد جميع القيود المحاسبية، السندات، الفواتير، وأجهزة الشبكة، مع التحقق من سلامة التوازن المحاسبي IFRS."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = fileUri
                        showLocalRestoreConfirmDialog = null
                        viewModel.restoreBackupFromUri(context, uri) { success, msg ->
                            localBackupMessage = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary)
                ) {
                    Text("تأكيد الاستعادة والتحقق", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocalRestoreConfirmDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Pre-Backup Passphrase Dialog (تأمين وتشفير النسخة السحابية)
    if (showBackupPassphraseDialog) {
        AlertDialog(
            onDismissRequest = { showBackupPassphraseDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = BrandCyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأمين وتشفير النسخة السحابية (AES-256)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = SemanticExpenseRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SemanticExpenseRed.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "تنبيه أمني هام:\nبدون عبارة المرور لا يمكن استعادة بياناتك — احتفظ بها في مكان آمن.",
                            color = SemanticExpenseRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = {
                            backupPassphrase = it
                            passphraseError = null
                        },
                        label = { Text("عبارة مرور التشفير (8 أحرف فأكثر)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_backup_passphrase")
                    )

                    OutlinedTextField(
                        value = backupConfirmPassphrase,
                        onValueChange = {
                            backupConfirmPassphrase = it
                            passphraseError = null
                        },
                        label = { Text("تأكيد عبارة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_confirm_backup_passphrase")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = savePassphraseInKeystore,
                            onCheckedChange = { savePassphraseInKeystore = it },
                            modifier = Modifier.testTag("checkbox_save_keystore")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حفظ عبارة المرور في أمان الجهاز (Android Keystore)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    passphraseError?.let { err ->
                        Text(
                            text = err,
                            color = SemanticExpenseRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pass = backupPassphrase.trim()
                        val confirm = backupConfirmPassphrase.trim()
                        if (pass.length < 8) {
                            passphraseError = "عبارة المرور يجب أن لا تقل عن 8 أحرف"
                            return@Button
                        }
                        if (pass != confirm) {
                            passphraseError = "عبارتا المرور غير متطابقتين"
                            return@Button
                        }
                        val userEmail = currentUser?.email ?: ""
                        if (userEmail.isNotBlank()) {
                            if (savePassphraseInKeystore) {
                                passphraseStorage.savePassphrase(userEmail, pass)
                            } else {
                                passphraseStorage.clearPassphrase(userEmail)
                            }
                        }
                        showBackupPassphraseDialog = false
                        viewModel.syncPushToFirebase(pass)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                    modifier = Modifier.testTag("btn_confirm_backup_passphrase")
                ) {
                    Text("بدء التشفير والرفع الذري", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackupPassphraseDialog = false },
                    modifier = Modifier.testTag("btn_cancel_backup_passphrase")
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Pre-Restore Confirmation & Passphrase Dialog
    if (showRestoreConfirmDialog) {
        val meta = restoreRemoteMeta
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = BrandCyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد فك التشفير واستعادة البيانات السحابية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (meta == null) {
                        Text(
                            text = "لم يتم العثور على أي نسخة سحابية مسجلة لحسابك الحالي.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val dateStr = if (meta.timestamp > 0L) {
                            com.example.util.TransactionDateUtils.formatDateTime(meta.timestamp)
                        } else "غير محدد"

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("تاريخ النسخة: $dateStr", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("إصدار التطبيق: ${meta.appVersionCode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("المستندات: ${meta.documentsCount} | القيود: ${meta.journalLinesCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("الأجهزة: ${meta.networkDevicesCount} | أسعار الصرف: ${meta.currencyRatesCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (meta.isChunked) {
                                    val sizeKb = meta.totalCipherBytes / 1024
                                    Text("الهيكلية: مشفرة ومجزأة (${meta.chunkCount} أجزاء • $sizeKb ك.ب)", fontSize = 11.sp, color = BrandCyanPrimary, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("الهيكلية: نسخة سحابية سابقة (Legacy)", fontSize = 11.sp, color = SemanticIncomeGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (meta.isChunked) {
                            Text(
                                text = "أدخل عبارة المرور لفك التشفير والمطابقة الذرية:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            OutlinedTextField(
                                value = restorePassphrase,
                                onValueChange = {
                                    restorePassphrase = it
                                    restorePassphraseError = null
                                },
                                label = { Text("عبارة مرور فك التشفير") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_restore_passphrase")
                            )

                            restorePassphraseError?.let { err ->
                                Text(
                                    text = err,
                                    color = SemanticExpenseRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "ملاحظة: هذه نسخة متوافقة تلقائياً ولا تتطلب عبارة مرور.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = SemanticIncomeGreen.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ميزة الأمان: يتم حفظ لقطة أمان محلية تلقائياً قبل أي تفريغ أو استبدال للبيانات لتأمين الرجوع الفوري.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (meta != null) {
                    Button(
                        onClick = {
                            if (meta.isChunked && restorePassphrase.trim().isBlank()) {
                                restorePassphraseError = "يرجى إدخال عبارة المرور لفك التشفير"
                                return@Button
                            }
                            val pass = if (meta.isChunked) restorePassphrase.trim() else ""
                            showRestoreConfirmDialog = false
                            viewModel.restoreFromCloudWithPassphrase(
                                passphrase = pass,
                                targetUid = meta.uid,
                                targetEmail = meta.userEmail
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                        modifier = Modifier.testTag("btn_confirm_restore_passphrase")
                    ) {
                        Text("فك التشفير والاستعادة الذرية", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreConfirmDialog = false },
                    modifier = Modifier.testTag("btn_cancel_restore_passphrase")
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun SyncMetricItem(title: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BrandCyanPrimary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SyncHistoryRow(item: SyncLogItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (item.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (item.isSuccess) SemanticIncomeGreen else SemanticExpenseRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = item.action,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = item.summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            Text(
                text = sdf.format(Date(item.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
