package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.HotspotUser
import com.example.data.network.HotspotUserStatus
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCanvas
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotUsersScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val activeProfile by viewModel.activeRouterProfile.collectAsState()

    var users by remember { mutableStateOf<List<HotspotUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isDeleting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<HotspotUserStatus?>(null) }
    var selectedProfileFilter by remember { mutableStateOf<String?>(null) }
    var showCleanupConfirmDialog by remember { mutableStateOf(false) }

    fun reloadUsers() {
        scope.launch {
            isLoading = true
            errorMessage = null
            viewModel.fetchHotspotUsers().fold(
                onSuccess = { loaded ->
                    users = loaded
                },
                onFailure = { err ->
                    errorMessage = err.message ?: "تعذر جلب مستخدمي الهوتسبوت من الراوتر."
                }
            )
            isLoading = false
        }
    }

    LaunchedEffect(activeProfile?.id) {
        reloadUsers()
    }

    val availableProfiles = remember(users) {
        users.map { it.profile }.distinct().sorted()
    }

    val filteredUsers = remember(users, searchQuery, selectedStatusFilter, selectedProfileFilter) {
        users.filter { user ->
            val matchesQuery = searchQuery.isBlank() ||
                user.username.contains(searchQuery, ignoreCase = true) ||
                user.profile.contains(searchQuery, ignoreCase = true) ||
                user.comment.contains(searchQuery, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == null || user.status == selectedStatusFilter
            val matchesProfile = selectedProfileFilter == null || user.profile == selectedProfileFilter
            matchesQuery && matchesStatus && matchesProfile
        }
    }

    val activeCount = remember(users) { users.count { it.status == HotspotUserStatus.ACTIVE } }
    val unusedCount = remember(users) { users.count { it.status == HotspotUserStatus.UNUSED } }
    val expiredCount = remember(users) { users.count { it.status == HotspotUserStatus.EXPIRED } }

    val cleanupProfileTarget = selectedProfileFilter
        ?: users.firstOrNull { it.status == HotspotUserStatus.EXPIRED }?.profile
    val expiredIdsInTargetProfile = remember(users, cleanupProfileTarget) {
        if (cleanupProfileTarget == null) emptySet()
        else users.filter {
            it.profile == cleanupProfileTarget &&
                it.status == HotspotUserStatus.EXPIRED &&
                it.id.isNotBlank()
        }.map { it.id }.toSet()
    }

    if (showCleanupConfirmDialog && cleanupProfileTarget != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showCleanupConfirmDialog = false },
            containerColor = CyberDarkSurface,
            title = {
                Text(
                    "تأكيد تنظيف الكروت المنتهية",
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "سيتم حذف ${expiredIdsInTargetProfile.size} كرت منتهي من البروفايل ($cleanupProfileTarget) على الراوتر بعد التحقق المباشر من حالتها.",
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    enabled = !isDeleting,
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            val result = viewModel.safelyDeleteExpiredHotspotUsers(
                                profileName = cleanupProfileTarget,
                                expectedIds = expiredIdsInTargetProfile
                            )
                            isDeleting = false
                            showCleanupConfirmDialog = false
                            result.fold(
                                onSuccess = { deleted ->
                                    snackbarHostState.showSnackbar("تم حذف $deleted كرت منتهي بنجاح.")
                                    reloadUsers()
                                },
                                onFailure = { err ->
                                    snackbarHostState.showSnackbar(err.message ?: "فشل حذف الكروت المنتهية.")
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticExpenseRed)
                ) {
                    Text(if (isDeleting) "جاري الحذف..." else "تأكيد الحذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = { showCleanupConfirmDialog = false }
                ) {
                    Text("إلغاء", color = TextSecondaryDark)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkCanvas),
        containerColor = CyberDarkCanvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "مستخدمو الهوتسبوت",
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = activeProfile?.let { "${it.name} (${it.host})" } ?: "غير متصل براوتر",
                            color = MikroTikCyan,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("hotspot_users_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    if (expiredIdsInTargetProfile.isNotEmpty() && !isLoading) {
                        IconButton(
                            onClick = { showCleanupConfirmDialog = true },
                            enabled = !isDeleting,
                            modifier = Modifier.testTag("hotspot_users_cleanup_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "حذف الكروت المنتهية",
                                tint = SemanticExpenseRed
                            )
                        }
                    }
                    IconButton(
                        onClick = { reloadUsers() },
                        enabled = !isLoading,
                        modifier = Modifier.testTag("hotspot_users_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = MikroTikCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDarkSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HotspotSummaryChip("الإجمالي", users.size, MikroTikCyan, Modifier.weight(1f))
                HotspotSummaryChip("نشط", activeCount, SemanticIncomeGreen, Modifier.weight(1f))
                HotspotSummaryChip("جديد", unusedCount, SemanticWarningAmber, Modifier.weight(1f))
                HotspotSummaryChip("منتهي", expiredCount, SemanticExpenseRed, Modifier.weight(1f))
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث باسم المستخدم أو البروفايل...", color = TextMutedDark) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MikroTikCyan) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hotspot_users_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MikroTikCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedContainerColor = CyberDarkSurface,
                    unfocusedContainerColor = CyberDarkSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Status filter chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("الكل") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MikroTikPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = CyberDarkSurface,
                            labelColor = TextSecondaryDark
                        )
                    )
                }
                items(HotspotUserStatus.entries) { status ->
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == status) null else status
                        },
                        label = { Text(status.labelArabic) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MikroTikPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = CyberDarkSurface,
                            labelColor = TextSecondaryDark
                        )
                    )
                }
            }

            // Profile filter chips when multiple profiles exist
            if (availableProfiles.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedProfileFilter == null,
                            onClick = { selectedProfileFilter = null },
                            label = { Text("كل الباقات") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0E7490),
                                selectedLabelColor = Color.White,
                                containerColor = CyberDarkSurface,
                                labelColor = TextSecondaryDark
                            )
                        )
                    }
                    items(availableProfiles) { profileName ->
                        FilterChip(
                            selected = selectedProfileFilter == profileName,
                            onClick = {
                                selectedProfileFilter = if (selectedProfileFilter == profileName) null else profileName
                            },
                            label = { Text(profileName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0E7490),
                                selectedLabelColor = Color.White,
                                containerColor = CyberDarkSurface,
                                labelColor = TextSecondaryDark
                            )
                        )
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = MikroTikCyan)
                            Text("جاري قراءة مستخدمي الهوتسبوت من الراوتر...", color = TextSecondaryDark)
                        }
                    }
                }

                errorMessage != null -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SemanticExpenseRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SemanticExpenseRed.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = TextPrimaryDark,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedButton(
                                onClick = { reloadUsers() },
                                border = BorderStroke(1.dp, MikroTikCyan)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = MikroTikCyan)
                                Spacer(Modifier.width(6.dp))
                                Text("إعادة المحاولة", color = MikroTikCyan)
                            }
                        }
                    }
                }

                filteredUsers.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد حسابات هوتسبوت مطابقة للبحث الحالي.",
                            color = TextSecondaryDark
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredUsers, key = { it.id.ifBlank { it.username } }) { user ->
                            HotspotUserCard(user = user)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HotspotSummaryChip(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = CyberDarkCardElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                color = accent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = label,
                color = TextSecondaryDark,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HotspotUserCard(user: HotspotUser) {
    val statusColor = when (user.status) {
        HotspotUserStatus.ACTIVE -> SemanticIncomeGreen
        HotspotUserStatus.UNUSED -> SemanticWarningAmber
        HotspotUserStatus.EXPIRED -> SemanticExpenseRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        border = BorderStroke(1.dp, CyberBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (user.isOnline) Icons.Default.Wifi else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (user.isOnline) SemanticIncomeGreen else MikroTikCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = user.username.ifBlank { "(بدون اسم)" },
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "البروفايل: ${user.profile}",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (user.isOnline) "متصل الآن" else user.status.labelArabic,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الاستهلاك: ${formatBytesShort(user.totalBytesUsed)}",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
                Text(
                    text = "المدة: ${formatDurationShort(user.uptimeSeconds)}",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }

            if (user.comment.isNotBlank()) {
                Text(
                    text = user.comment,
                    color = TextMutedDark,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatBytesShort(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}

private fun formatDurationShort(seconds: Long): String {
    if (seconds <= 0L) return "0s"
    val hours = seconds / 3600L
    val minutes = (seconds % 3600L) / 60L
    val secs = seconds % 60L
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}
