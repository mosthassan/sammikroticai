package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.auth.AccessCodeEntry
import com.example.data.auth.AgentPermissions
import com.example.data.auth.AppUserRole
import com.example.data.auth.RoleAccessPolicy
import com.example.data.local.entity.PartyEntity
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamAccessCodesScreen(
    viewModel: AppViewModel,
    initialTab: Int = 0,
    onBack: () -> Unit,
    onPreviewRoleActivated: () -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val accessCodes by viewModel.accessCodes.collectAsStateWithLifecycle()
    val allParties by viewModel.allParties.collectAsStateWithLifecycle()
    val networkName = remember(viewModel.organization.collectAsStateWithLifecycle().value) {
        viewModel.resolveCurrentNetworkName()
    }

    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var showCreateAgentForm by remember { mutableStateOf(false) }

    // New Agent Form State
    var newAgentName by remember { mutableStateOf("") }
    var newAgentPin by remember { mutableStateOf("") }
    var newAgentPhone by remember { mutableStateOf("") }
    var newAgentPerms by remember { mutableStateOf(AgentPermissions()) }

    // POS Search State
    var posSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.syncAccessCodesWithCloud()
    }

    if (!RoleAccessPolicy.canAccessTeamAndCodesManager(activeRole)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "مقيد",
                        tint = SemanticExpenseRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "قسم حصري لمالك الشبكة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "إدارة الوكلاء الميدانيين وأكواد الشفافية متاحة فقط في وضع مالك الشبكة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onBack) {
                        Text("رجوع")
                    }
                }
            }
        }
        return
    }

    val agentCodes = remember(accessCodes) {
        accessCodes.filter { it.role == AppUserRole.DISTRIBUTOR_AGENT }
    }

    val posCodesByPartyId = remember(accessCodes) {
        accessCodes
            .filter { it.role == AppUserRole.POS_MERCHANT && it.isActive && !it.partyId.isNullOrBlank() }
            .associateBy { it.partyId!! }
    }

    val customerParties = remember(allParties, posSearchQuery) {
        val customers = allParties.filter { it.isCustomer && it.isActive }
        if (posSearchQuery.isBlank()) {
            customers
        } else {
            val q = posSearchQuery.trim()
            customers.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.phone.contains(q, ignoreCase = true) ||
                    (posCodesByPartyId[it.id]?.code?.contains(q, ignoreCase = true) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "إدارة الوكلاء وأكواد نقاط البيع",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "نظام الصلاحيات متعدد الأدوار وبوابة الشفافية الرقمية — $networkName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("team_codes_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncAccessCodesWithCloud() },
                        modifier = Modifier.testTag("sync_access_codes_btn")
                    ) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = "مزامنة الأكواد مع السحابة",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented Role Manager Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("tab_manage_agents"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                "الوكلاء والموزعون (${agentCodes.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("tab_manage_pos_codes"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                "أكواد نقاط البيع (${customerParties.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Banner + Create Agent Button
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.LocalShipping,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                "تفويض الوكلاء والموزعين الميدانيين",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "لا يمكن للوكيل مطلقاً الوصول لإعدادات الميكروتك أو رأس المال أو التقارير الختامية.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { showCreateAgentForm = !showCreateAgentForm },
                                        modifier = Modifier.testTag("toggle_create_agent_btn")
                                    ) {
                                        Icon(
                                            if (showCreateAgentForm) Icons.Default.Close else Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (showCreateAgentForm) "إغلاق" else "وكيل جديد")
                                    }
                                }

                                AnimatedVisibility(visible = showCreateAgentForm) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        HorizontalDivider()
                                        OutlinedTextField(
                                            value = newAgentName,
                                            onValueChange = { newAgentName = it },
                                            label = { Text("اسم الوكيل / الموزع الميداني *") },
                                            placeholder = { Text("مثال: أحمد الموزع - خط الميدان") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("new_agent_name_input")
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = newAgentPin,
                                                onValueChange = { newAgentPin = it },
                                                label = { Text("رمز PIN (اختياري)") },
                                                placeholder = { Text("1234") },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("new_agent_pin_input")
                                            )
                                            OutlinedTextField(
                                                value = newAgentPhone,
                                                onValueChange = { newAgentPhone = it },
                                                label = { Text("رقم واتساب الوكيل") },
                                                placeholder = { Text("96777xxxxxxx") },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("new_agent_phone_input")
                                            )
                                        }

                                        Text(
                                            "صلاحيات الوكيل المسموحة:",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )

                                        AgentPermissionsSwitchesSection(
                                            permissions = newAgentPerms,
                                            onPermissionsChange = { newAgentPerms = it }
                                        )

                                        Button(
                                            onClick = {
                                                if (newAgentName.isBlank()) {
                                                    Toast.makeText(
                                                        context,
                                                        "يرجى إدخال اسم الوكيل أولاً",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    return@Button
                                                }
                                                viewModel.createOrUpdateAgentInviteCode(
                                                    agentName = newAgentName,
                                                    agentPin = newAgentPin,
                                                    agentPhone = newAgentPhone,
                                                    permissions = newAgentPerms
                                                ) {
                                                    newAgentName = ""
                                                    newAgentPin = ""
                                                    newAgentPhone = ""
                                                    newAgentPerms = AgentPermissions()
                                                    showCreateAgentForm = false
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("confirm_generate_agent_code_btn")
                                        ) {
                                            Icon(Icons.Default.VpnKey, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("إصدار كود دعوة الوكيل (AGT-XXXX)", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (agentCodes.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Groups,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "لم يتم إضافة وكلاء أو موزعين بعد",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "اضغط على 'وكيل جديد' لإصدار كود دعوة وتحديد الصلاحيات الميدانية بدقة.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(agentCodes, key = { it.code }) { entry ->
                            AgentAccessCodeCard(
                                entry = entry,
                                networkName = networkName,
                                onPermissionsUpdated = { updatedPerms ->
                                    viewModel.updateAgentPermissions(entry.code, updatedPerms)
                                },
                                onToggleActive = { active ->
                                    viewModel.setAccessCodeActive(entry.code, active)
                                },
                                onPreviewRole = {
                                    viewModel.previewRoleFromAccessCode(entry) {
                                        onPreviewRoleActivated()
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Tab 1: POS Transparency Codes
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "🏪 بوابة الشفافية الرقمية لنقاط البيع والبقالات",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "امنح كل صاحب بقالة أو نقطة بيع كود شفافية فريد (POS-XXXXXX) يتيح له الاطلاع الفوري (قراءة فقط) على كشف حسابه وفواتيره وسنداته دون أي صلاحية للتعديل.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = posSearchQuery,
                                    onValueChange = { posSearchQuery = it },
                                    label = { Text("ابحث عن نقطة بيع أو بقالة أو كود...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pos_codes_search_input")
                                )
                            }
                        }
                    }

                    if (customerParties.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "لا توجد نقاط بيع مطابقة للبحث",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "أضف عملاء / نقاط بيع من شاشة الأطراف لتوليد أكواد الشفافية الخاصة بهم.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(customerParties, key = { it.id }) { party ->
                            val existingCodeEntry = posCodesByPartyId[party.id]
                            PosPartyTransparencyCodeCard(
                                party = party,
                                codeEntry = existingCodeEntry,
                                networkName = networkName,
                                onGenerateOrRefreshCode = { forceNew ->
                                    viewModel.generateOrGetPosTransparencyCode(
                                        partyId = party.id,
                                        partyName = party.name,
                                        partyPhone = party.phone,
                                        forceNew = forceNew
                                    )
                                },
                                onPreviewPosPortal = { entry ->
                                    viewModel.previewRoleFromAccessCode(entry) {
                                        onPreviewRoleActivated()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgentPermissionsSwitchesSection(
    permissions: AgentPermissions,
    onPermissionsChange: (AgentPermissions) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PermissionSwitchRow(
            title = "البيع وإصدار الفواتير لنقاط البيع",
            subtitle = "السماح للوكيل بإنشاء وترحيل فواتير مبيعات الكروت لنقاط البيع",
            checked = permissions.canCreateSalesInvoices,
            onCheckedChange = { onPermissionsChange(permissions.copy(canCreateSalesInvoices = it)) },
            testTag = "perm_switch_sales"
        )
        PermissionSwitchRow(
            title = "إصدار سندات القبض والتحصيل",
            subtitle = "السماح بتحصيل المبالغ النقدية وتخصيص الفواتير من نقاط البيع",
            checked = permissions.canCollectReceipts,
            onCheckedChange = { onPermissionsChange(permissions.copy(canCollectReceipts = it)) },
            testTag = "perm_switch_receipts"
        )
        PermissionSwitchRow(
            title = "إصدار سندات الصرف",
            subtitle = "السماح بتسجيل سندات الصرف والمصروفات الميدانية",
            checked = permissions.canCreateDisbursements,
            onCheckedChange = { onPermissionsChange(permissions.copy(canCreateDisbursements = it)) },
            testTag = "perm_switch_disbursements"
        )
        PermissionSwitchRow(
            title = "الاطلاع على مخزون الكروت المتاح",
            subtitle = "عرض كميات الباقات والكروت المتوفرة للبيع دون تعديل الباقات",
            checked = permissions.canViewAssignedStock,
            onCheckedChange = { onPermissionsChange(permissions.copy(canViewAssignedStock = it)) },
            testTag = "perm_switch_stock"
        )
        PermissionSwitchRow(
            title = "إصدار أكواد الشفافية لنقاط البيع",
            subtitle = "السماح للوكيل بتوليد ومشاركة كود POS-XXXXXX مع البقالات",
            checked = permissions.canGeneratePosCodes,
            onCheckedChange = { onPermissionsChange(permissions.copy(canGeneratePosCodes = it)) },
            testTag = "perm_switch_pos_codes"
        )
    }
}

@Composable
private fun PermissionSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
private fun AgentAccessCodeCard(
    entry: AccessCodeEntry,
    networkName: String,
    onPermissionsUpdated: (AgentPermissions) -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onPreviewRole: () -> Unit
) {
    val context = LocalContext.current
    var expandedPermissions by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (entry.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            entry.agentName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = if (entry.isActive) SemanticIncomeGreen.copy(alpha = 0.16f) else SemanticExpenseRed.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (entry.isActive) "نشط" else "موقوف",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (entry.isActive) SemanticIncomeGreen else SemanticExpenseRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (entry.agentPin.isNotBlank() || entry.agentPhone.isNotBlank()) {
                        Text(
                            buildString {
                                if (entry.agentPin.isNotBlank()) append("رمز PIN: ${entry.agentPin}")
                                if (entry.agentPin.isNotBlank() && entry.agentPhone.isNotBlank()) append("  •  ")
                                if (entry.agentPhone.isNotBlank()) append("هاتف: ${entry.agentPhone}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Code Pill
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = entry.code,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        copyToClipboard(context, "كود الوكيل", entry.code)
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("نسخ الكود", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = {
                        val msg = AccessCodeEntry.buildAgentWhatsAppMessage(
                            networkName = networkName,
                            agentName = entry.agentName,
                            code = entry.code,
                            pin = entry.agentPin
                        )
                        shareViaWhatsApp(context, entry.agentPhone, msg)
                    },
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("مشاركة واتساب", style = MaterialTheme.typography.labelMedium)
                }

                FilledTonalButton(
                    onClick = onPreviewRole,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("معاينة الدور", style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { expandedPermissions = !expandedPermissions }) {
                    Icon(
                        if (expandedPermissions) Icons.Default.ExpandLess else Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (expandedPermissions) "إخفاء إعدادات الصلاحيات" else "تعديل صلاحيات الوكيل (5 صلاحيات)",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("تفعيل الكود", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = entry.isActive,
                        onCheckedChange = onToggleActive
                    )
                }
            }

            AnimatedVisibility(visible = expandedPermissions) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()
                    AgentPermissionsSwitchesSection(
                        permissions = entry.permissions,
                        onPermissionsChange = onPermissionsUpdated
                    )
                }
            }
        }
    }
}

@Composable
private fun PosPartyTransparencyCodeCard(
    party: PartyEntity,
    codeEntry: AccessCodeEntry?,
    networkName: String,
    onGenerateOrRefreshCode: (Boolean) -> Unit,
    onPreviewPosPortal: (AccessCodeEntry) -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            party.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (party.phone.isNotBlank()) "هاتف/واتساب: ${party.phone}" else "بدون رقم هاتف مسجل",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (codeEntry != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = codeEntry.code,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                } else {
                    Button(
                        onClick = { onGenerateOrRefreshCode(false) },
                        modifier = Modifier.testTag("generate_pos_code_${party.id}")
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إصدار كود الشفافية")
                    }
                }
            }

            if (codeEntry != null) {
                val whatsappMessage = remember(networkName, codeEntry.code) {
                    AccessCodeEntry.buildPosWhatsAppMessage(networkName, codeEntry.code)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = whatsappMessage,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            copyToClipboard(context, "كود نقطة البيع", codeEntry.code)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("نسخ", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = {
                            shareViaWhatsApp(context, party.phone, whatsappMessage)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("whatsapp_pos_code_${party.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إرسال عبر واتساب", style = MaterialTheme.typography.labelMedium)
                    }

                    FilledTonalButton(
                        onClick = { onPreviewPosPortal(codeEntry) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("معاينة", style = MaterialTheme.typography.labelMedium)
                    }

                    IconButton(
                        onClick = { onGenerateOrRefreshCode(true) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "توليد كود جديد",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "تم نسخ $label: $text", Toast.LENGTH_SHORT).show()
}

fun shareViaWhatsApp(context: Context, phone: String, message: String) {
    val cleanPhone = phone.filter { it.isDigit() }
    try {
        val uri = if (cleanPhone.isNotBlank()) {
            Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(message)}")
        } else {
            Uri.parse("https://wa.me/?text=${Uri.encode(message)}")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(sendIntent, "مشاركة الكود"))
    }
}
