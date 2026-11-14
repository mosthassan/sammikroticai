package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ledger.AccountConstants
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.core.time.AppClock
import com.example.data.auth.DocumentVerificationRecord
import com.example.data.auth.DocumentVerificationStatus
import com.example.data.auth.PosRestockOrder
import com.example.data.auth.PosRestockOrderItem
import com.example.data.auth.PosSupportTicket
import com.example.data.auth.RestockOrderStatus
import com.example.data.auth.TicketCategory
import com.example.data.auth.TicketStatus
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.PartyEntity
import com.example.domain.usecase.StatementItem
import com.example.domain.usecase.StatementOfAccountReport
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber
import com.example.ui.viewmodel.AppViewModel
import com.example.util.PdfDocumentGenerator
import com.example.util.WhatsAppDispatcher
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

enum class PosStatementQuickFilter(val labelAr: String, val tag: String) {
    ALL("الكل", "pos_filter_all"),
    SALES_INVOICES("فواتير الكروت", "pos_filter_invoices"),
    RECEIPT_VOUCHERS("سندات القبض والتسديد", "pos_filter_receipts"),
    THIS_MONTH("هذا الشهر", "pos_filter_this_month")
}

/**
 * Complete Read-Only POS Merchant Digital Transparency Portal (`بوابة الشفافية الرقمية لنقطة البيع`).
 * Bound exclusively to the authenticated POS merchant's `boundPartyId` and `networkId`.
 * Features:
 * 1. Top 3 Executive Summary Cards (`إجمالي الفواتير والمسحوبات`, `إجمالي المبالغ المسددة`, `الرصيد المتبقي (المديونية الحالية)`).
 * 2. Quick Filter Chips (`[الكل]`, `[فواتير الكروت]`, `[سندات القبض والتسديد]`, `[هذا الشهر]`).
 * 3. One-Click Official PDF Statement Export (`كشف حساب نقطة بيع معتمد`).
 * 4. Digital Handshake: Mutual Invoice & Receipt Verification (`التأكيد الرقمي المتبادل`) — metadata-only layer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosMerchantPortalScreen(
    viewModel: AppViewModel,
    onSwitchOrLogoutRole: () -> Unit
) {
    val context = LocalContext.current
    val session by viewModel.currentUser.collectAsStateWithLifecycle()
    val allParties by viewModel.allParties.collectAsStateWithLifecycle()
    val allDocuments by viewModel.allDocuments.collectAsStateWithLifecycle()
    val allPackages by viewModel.allPackages.collectAsStateWithLifecycle()
    val report by viewModel.currentPartyStatement.collectAsStateWithLifecycle()
    val verificationsMap by viewModel.documentVerifications.collectAsStateWithLifecycle()
    val allRestockOrders by viewModel.posRestockOrders.collectAsStateWithLifecycle()
    val allSupportTickets by viewModel.posSupportTickets.collectAsStateWithLifecycle()
    val networkConfig by viewModel.networkRepository.config.collectAsStateWithLifecycle()

    val boundPartyId = session?.boundPartyId.orEmpty()
    val boundPartyName = session?.boundPartyName ?: session?.displayName ?: "نقطة بيع"
    val networkName = remember(session, networkConfig) {
        session?.networkName?.takeIf { it.isNotBlank() } ?: viewModel.resolveCurrentNetworkName()
    }

    val partyEntity = remember(allParties, boundPartyId, boundPartyName) {
        allParties.firstOrNull { it.id == boundPartyId } ?: PartyEntity(
            id = boundPartyId.ifBlank { "POS_BOUND" },
            name = boundPartyName,
            phone = "",
            isCustomer = true
        )
    }

    val myOrders = remember(allRestockOrders, boundPartyId) {
        if (boundPartyId.isBlank()) allRestockOrders
        else allRestockOrders.filter { it.partyId == boundPartyId }
    }

    val myTickets = remember(allSupportTickets, boundPartyId) {
        if (boundPartyId.isBlank()) allSupportTickets
        else allSupportTickets.filter { it.partyId == boundPartyId }
    }

    LaunchedEffect(boundPartyId) {
        if (boundPartyId.isNotBlank()) {
            viewModel.loadStatementOfAccount(
                partyId = boundPartyId,
                controlAccountCode = AccountConstants.ACCOUNTS_RECEIVABLE,
                startEpoch = null,
                endEpoch = null
            )
            viewModel.syncPartyVerificationsWithCloud(boundPartyId)
            viewModel.syncPosOrdersAndTicketsWithCloud(boundPartyId)
        }
    }

    val partyDocuments = remember(allDocuments, boundPartyId) {
        if (boundPartyId.isBlank()) emptyList()
        else allDocuments.filter { it.partyId == boundPartyId && it.status == "POSTED" }
            .sortedByDescending { it.dateEpochDay }
    }

    val totalInvoicesMinor = remember(report, partyDocuments) {
        report?.totalDebitMinor?.takeIf { it > 0L || partyDocuments.isEmpty() }
            ?: partyDocuments.filter { it.type == "SALES_INVOICE" }.sumOf { it.totalBaseMinor }
    }
    val totalReceiptsMinor = remember(report, partyDocuments) {
        report?.totalCreditMinor?.takeIf { it > 0L || partyDocuments.isEmpty() }
            ?: partyDocuments.filter { it.type == "RECEIPT_VOUCHER" }.sumOf { it.totalBaseMinor }
    }
    val closingBalanceMinor = remember(report, totalInvoicesMinor, totalReceiptsMinor) {
        report?.closingBalanceMinor ?: (totalInvoicesMinor - totalReceiptsMinor)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var activeFilter by remember { mutableStateOf(PosStatementQuickFilter.ALL) }
    var previewPdfFile by remember { mutableStateOf<File?>(null) }

    // Dispute Dialog State
    var disputingDocId by remember { mutableStateOf<String?>(null) }
    var disputingDocLabel by remember { mutableStateOf("") }
    var disputeNoteInput by remember { mutableStateOf("") }

    // Phase 3: In-App Restock Order Dialog & Support Ticket Dialog States
    var showNewRestockOrderDialog by remember { mutableStateOf(false) }
    var showNewSupportTicketDialog by remember { mutableStateOf(false) }

    val curr = CurrencyCode.FUNCTIONAL
    val currentMonthStartEpochDay = remember {
        val today = runCatching { AppClock.todayLocalDate() }.getOrElse { LocalDate.now() }
        today.withDayOfMonth(1).toEpochDay()
    }

    // Filtered Statement Items
    val filteredStatementItems = remember(report, activeFilter, currentMonthStartEpochDay) {
        val itemsList = report?.items.orEmpty()
        itemsList.filter { item ->
            val isInvoice = item.docType.equals("SALES_INVOICE", ignoreCase = true) ||
                item.docType.contains("INVOICE", ignoreCase = true) ||
                (item.baseDebitMinor > 0L && item.baseCreditMinor == 0L)
            val isReceipt = item.docType.equals("RECEIPT_VOUCHER", ignoreCase = true) ||
                item.docType.contains("RECEIPT", ignoreCase = true) ||
                (item.baseCreditMinor > 0L && item.baseDebitMinor == 0L)

            when (activeFilter) {
                PosStatementQuickFilter.ALL -> true
                PosStatementQuickFilter.SALES_INVOICES -> isInvoice
                PosStatementQuickFilter.RECEIPT_VOUCHERS -> isReceipt
                PosStatementQuickFilter.THIS_MONTH -> item.entryDateEpochDay >= currentMonthStartEpochDay
            }
        }
    }

    // Filtered Documents for Tab 1
    val filteredPartyDocuments = remember(partyDocuments, activeFilter, currentMonthStartEpochDay) {
        partyDocuments.filter { doc ->
            when (activeFilter) {
                PosStatementQuickFilter.ALL -> true
                PosStatementQuickFilter.SALES_INVOICES -> doc.type == "SALES_INVOICE"
                PosStatementQuickFilter.RECEIPT_VOUCHERS -> doc.type == "RECEIPT_VOUCHER"
                PosStatementQuickFilter.THIS_MONTH -> doc.dateEpochDay >= currentMonthStartEpochDay
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = partyEntity.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MikroTikCyan.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "قراءة فقط",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MikroTikCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "بوابة الشفافية الرقمية — $networkName (${session?.accessCode ?: "POS"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (boundPartyId.isNotBlank()) {
                                viewModel.syncPartyVerificationsWithCloud(boundPartyId)
                                viewModel.syncPosOrdersAndTicketsWithCloud(boundPartyId)
                                viewModel.loadStatementOfAccount(
                                    partyId = boundPartyId,
                                    controlAccountCode = AccountConstants.ACCOUNTS_RECEIVABLE,
                                    startEpoch = null,
                                    endEpoch = null
                                )
                            }
                        },
                        modifier = Modifier.testTag("pos_portal_refresh_btn")
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "تحديث كشف الحساب والتأكيدات",
                            tint = MikroTikCyan
                        )
                    }
                    OutlinedButton(
                        onClick = onSwitchOrLogoutRole,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("pos_portal_switch_role_btn"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "تبديل الدور أو تسجيل الخروج",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("تبديل الدور", style = MaterialTheme.typography.labelMedium)
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
            // =========================================================================
            // 1. TOP 3 EXECUTIVE SUMMARY CARDS + ONE-CLICK OFFICIAL PDF STATEMENT EXPORT
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: إجمالي الفواتير والمسحوبات
                    ExecutiveKpiCard(
                        title = "إجمالي الفواتير والمسحوبات",
                        amountText = Money(totalInvoicesMinor, curr).format(),
                        accentColor = MikroTikCyan,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pos_kpi_total_invoices")
                    )

                    // Card 2: إجمالي المبالغ المسددة
                    ExecutiveKpiCard(
                        title = "إجمالي المبالغ المسددة",
                        amountText = Money(totalReceiptsMinor, curr).format(),
                        accentColor = SemanticIncomeGreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pos_kpi_total_receipts")
                    )

                    // Card 3: الرصيد المتبقي (المديونية الحالية)
                    val balanceColor = when {
                        closingBalanceMinor > 0L -> SemanticWarningAmber
                        closingBalanceMinor < 0L -> MikroTikCyan
                        else -> SemanticIncomeGreen
                    }
                    ExecutiveKpiCard(
                        title = "الرصيد المتبقي (المديونية الحالية)",
                        amountText = Money(closingBalanceMinor, curr).format(),
                        accentColor = balanceColor,
                        isHighlighted = true,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("pos_kpi_closing_balance")
                    )
                }

                // One-Click Official PDF Statement Export Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val effectiveReport = report ?: StatementOfAccountReport(
                                partyId = partyEntity.id,
                                partyName = partyEntity.name,
                                controlAccountCode = AccountConstants.ACCOUNTS_RECEIVABLE,
                                startDateEpochDay = null,
                                endDateEpochDay = null,
                                openingBalanceMinor = 0L,
                                totalDebitMinor = totalInvoicesMinor,
                                totalCreditMinor = totalReceiptsMinor,
                                closingBalanceMinor = closingBalanceMinor,
                                items = emptyList()
                            )
                            val file = PdfDocumentGenerator.generateAgentStatementPdf(
                                context = context,
                                report = effectiveReport,
                                party = partyEntity
                            )
                            previewPdfFile = file
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pos_portal_pdf_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "كشف حساب نقطة بيع معتمد (PDF)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val effectiveReport = report ?: StatementOfAccountReport(
                                partyId = partyEntity.id,
                                partyName = partyEntity.name,
                                controlAccountCode = AccountConstants.ACCOUNTS_RECEIVABLE,
                                startDateEpochDay = null,
                                endDateEpochDay = null,
                                openingBalanceMinor = 0L,
                                totalDebitMinor = totalInvoicesMinor,
                                totalCreditMinor = totalReceiptsMinor,
                                closingBalanceMinor = closingBalanceMinor,
                                items = emptyList()
                            )
                            val file = PdfDocumentGenerator.generateAgentStatementPdf(
                                context = context,
                                report = effectiveReport,
                                party = partyEntity
                            )
                            val supportPhone = networkConfig.supportWhatsapp.ifBlank { partyEntity.phone }
                            WhatsAppDispatcher.sendDocument(
                                context = context,
                                phoneNumber = supportPhone,
                                file = file,
                                caption = "كشف حساب نقطة بيع معتمد — ${partyEntity.name}"
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("pos_portal_share_pdf_whatsapp_btn")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "مشاركة PDF",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("مشاركة", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // =========================================================================
            // 2. READ-ONLY PORTAL SUB-TABS
            // =========================================================================
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("pos_tab_statement"),
                    text = { Text("كشف الحساب والمطابقة", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("pos_tab_history"),
                    text = { Text("الفواتير والسندات (${filteredPartyDocuments.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.testTag("pos_tab_restock"),
                    text = {
                        val badgeCount = myOrders.count { it.status == RestockOrderStatus.PENDING || it.status == RestockOrderStatus.PREPARING } +
                            myTickets.count { it.status != TicketStatus.RESOLVED }
                        Text(
                            text = if (badgeCount > 0) "الطلبات والدعم ($badgeCount)" else "طلب كروت / دعم",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // =========================================================================
            // 3. DATE RANGE & TYPE QUICK FILTER CHIPS (For Tabs 0 & 1)
            // =========================================================================
            if (selectedTab == 0 || selectedTab == 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PosStatementQuickFilter.entries.forEach { filterOption ->
                        val isSelected = activeFilter == filterOption
                        FilterChip(
                            selected = isSelected,
                            onClick = { activeFilter = filterOption },
                            label = {
                                Text(
                                    text = filterOption.labelAr,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag(filterOption.tag)
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    if (filteredStatementItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "لا توجد حركات محاسبية مطابقة للفلتر المحدد حتى الآن.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredStatementItems, key = { "${it.entryId}_${it.docId}" }) { item ->
                                val verification = verificationsMap[item.docId]
                                    ?: DocumentVerificationRecord(
                                        documentId = item.docId,
                                        partyId = boundPartyId,
                                        status = DocumentVerificationStatus.PENDING_POS_CONFIRMATION
                                    )

                                PosStatementVerificationCard(
                                    item = item,
                                    currency = curr,
                                    verification = verification,
                                    onConfirmClick = {
                                        viewModel.confirmDocumentReceiptByPos(
                                            documentId = item.docId,
                                            partyId = boundPartyId
                                        )
                                    },
                                    onDisputeClick = {
                                        disputingDocId = item.docId
                                        disputingDocLabel = "${formatDocTypeAr(item.docType)} #${item.docNumber}"
                                        disputeNoteInput = verification.disputeNote
                                    }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    if (filteredPartyDocuments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "لا توجد فواتير أو سندات قبض مطابقة للفلتر المحدد.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredPartyDocuments, key = { it.id }) { doc ->
                                val isReceipt = doc.type == "RECEIPT_VOUCHER"
                                val verification = verificationsMap[doc.id]
                                    ?: DocumentVerificationRecord(
                                        documentId = doc.id,
                                        partyId = boundPartyId,
                                        status = DocumentVerificationStatus.PENDING_POS_CONFIRMATION
                                    )

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(
                                        1.dp,
                                        when (verification.status) {
                                            DocumentVerificationStatus.CONFIRMED_BY_POS -> SemanticIncomeGreen.copy(alpha = 0.45f)
                                            DocumentVerificationStatus.DISPUTED_BY_POS -> SemanticWarningAmber.copy(alpha = 0.55f)
                                            DocumentVerificationStatus.PENDING_POS_CONFIRMATION -> MaterialTheme.colorScheme.outlineVariant
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                                Icon(
                                                    if (isReceipt) Icons.Default.Payments else Icons.AutoMirrored.Filled.ReceiptLong,
                                                    contentDescription = null,
                                                    tint = if (isReceipt) SemanticIncomeGreen else MaterialTheme.colorScheme.primary
                                                )
                                                Column {
                                                    Text(
                                                        text = if (isReceipt) "سند قبض #${doc.docNumber}" else "فاتورة مبيعات #${doc.docNumber}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (doc.notes.isNotBlank()) {
                                                        Text(
                                                            text = doc.notes,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        text = runCatching { LocalDate.ofEpochDay(doc.dateEpochDay).toString() }.getOrDefault(""),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Text(
                                                text = Money(doc.totalBaseMinor, curr).format(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isReceipt) SemanticIncomeGreen else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Verification Status Badge & Mutual Handshake Actions
                                        PosVerificationHandshakeRow(
                                            documentId = doc.id,
                                            verification = verification,
                                            onConfirmClick = {
                                                viewModel.confirmDocumentReceiptByPos(
                                                    documentId = doc.id,
                                                    partyId = boundPartyId
                                                )
                                            },
                                            onDisputeClick = {
                                                disputingDocId = doc.id
                                                disputingDocLabel = if (isReceipt) "سند قبض #${doc.docNumber}" else "فاتورة مبيعات #${doc.docNumber}"
                                                disputeNoteInput = verification.disputeNote
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top Action Card: In-App Restock Order + Support Ticket
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "الخدمات الذاتية لنقطة البيع",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "اطلب دفعة كروت جديدة مباشرة داخل النظام أو أرسل بلاغاً فنياً/مالياً لإدارة الشبكة والموزع الميداني.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { showNewRestockOrderDialog = true },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("pos_open_restock_order_dialog_btn"),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan)
                                        ) {
                                            Text(
                                                text = "📦 طلب دفعة كروت جديدة",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { showNewSupportTicketDialog = true },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("pos_open_support_ticket_dialog_btn"),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, SemanticWarningAmber)
                                        ) {
                                            Text(
                                                text = "💬 مراسلة الإدارة / إبلاغ عن مشكلة",
                                                color = SemanticWarningAmber,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Section 1: طلباتي السابقة (حالة الطلبات)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pos_previous_orders_section"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "طلباتي السابقة (حالة الطلبات) (${myOrders.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MikroTikCyan.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "تحديث لحظي",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MikroTikCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (myOrders.isEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Text(
                                        text = "لا توجد طلبات تغذية كروت سابقة. اضغط على «📦 طلب دفعة كروت جديدة» لإرسال أول طلب.",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(myOrders, key = { it.orderId }) { order ->
                                PosRestockOrderHistoryCard(
                                    order = order,
                                    onSendWhatsAppReminder = {
                                        val supportPhone = networkConfig.supportWhatsapp.ifBlank { networkConfig.supportPhone }
                                        val msg = order.buildWhatsAppReminderMessage(networkName)
                                        shareViaWhatsApp(context, supportPhone, msg)
                                    }
                                )
                            }
                        }

                        // Section 2: البلاغات والشكاوى ومراسلة الإدارة
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "سجل البلاغات ومراسلات الإدارة (${myTickets.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("pos_support_tickets_section")
                            )
                        }

                        if (myTickets.isEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Text(
                                        text = "لا توجد بلاغات أو شكاوى مسجلة. يمكنك التواصل المباشر مع الإدارة عبر زر «💬 مراسلة الإدارة / إبلاغ عن مشكلة».",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(myTickets, key = { it.ticketId }) { ticket ->
                                PosSupportTicketHistoryCard(ticket = ticket)
                            }
                        }
                    }
                }
            }
        }
    }

    // Phase 3: New POS Restock Order Dialog ("📦 طلب دفعة كروت جديدة")
    if (showNewRestockOrderDialog) {
        PosNewRestockOrderDialog(
            packages = allPackages.filter { it.isActive },
            partyName = partyEntity.name,
            onDismiss = { showNewRestockOrderDialog = false },
            onSubmitInApp = { items, note ->
                viewModel.submitPosRestockOrder(
                    partyId = partyEntity.id,
                    partyName = partyEntity.name,
                    items = items,
                    note = note
                )
                showNewRestockOrderDialog = false
            },
            onSubmitAndShareWhatsApp = { items, note ->
                viewModel.submitPosRestockOrder(
                    partyId = partyEntity.id,
                    partyName = partyEntity.name,
                    items = items,
                    note = note,
                    onSubmitted = { createdOrder ->
                        val supportPhone = networkConfig.supportWhatsapp.ifBlank { networkConfig.supportPhone }
                        val msg = createdOrder.buildWhatsAppReminderMessage(networkName)
                        shareViaWhatsApp(context, supportPhone, msg)
                    }
                )
                showNewRestockOrderDialog = false
            }
        )
    }

    // Phase 3: New Support / Complaint Ticket Dialog ("💬 مراسلة الإدارة / إبلاغ عن مشكلة")
    if (showNewSupportTicketDialog) {
        PosNewSupportTicketDialog(
            onDismiss = { showNewSupportTicketDialog = false },
            onSubmitTicket = { category, voucherCode, message ->
                viewModel.submitPosSupportTicket(
                    partyId = partyEntity.id,
                    partyName = partyEntity.name,
                    category = category,
                    message = message,
                    voucherCode = voucherCode
                )
                showNewSupportTicketDialog = false
            }
        )
    }

    // Dispute / Error Note Dialog ("⚠️ إبلاغ عن خطأ")
    disputingDocId?.let { targetDocId ->
        AlertDialog(
            onDismissRequest = { disputingDocId = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = SemanticWarningAmber
                    )
                    Text(
                        text = "إبلاغ عن خطأ أو ملاحظة ($disputingDocLabel)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "سيتم إرسال هذه الملاحظة فوراً إلى مالك الشبكة والموزع لمراجعتها دون المساس بالقيود المحاسبية الأصلية:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = disputeNoteInput,
                        onValueChange = { disputeNoteInput = it },
                        label = { Text("تفاصيل الملاحظة أو الخطأ") },
                        placeholder = { Text("مثال: المبلغ المسلم للموزع 50,000 وليس 40,000") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_dispute_note_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val note = disputeNoteInput.trim().ifBlank { "يوجد ملاحظة من نقطة البيع تحتاج للمراجعة" }
                        viewModel.disputeDocumentByPos(
                            documentId = targetDocId,
                            partyId = boundPartyId,
                            disputeNote = note
                        )
                        disputingDocId = null
                        disputeNoteInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticWarningAmber),
                    modifier = Modifier.testTag("pos_confirm_dispute_btn")
                ) {
                    Text("إرسال الملاحظة / الاعتراض", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { disputingDocId = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    previewPdfFile?.let { file ->
        PdfPreviewDialog(
            pdfFile = file,
            title = "كشف حساب نقطة بيع معتمد — ${partyEntity.name}",
            onDismiss = { previewPdfFile = null }
        )
    }
}

@Composable
private fun ExecutiveKpiCard(
    title: String,
    amountText: String,
    accentColor: Color,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                accentColor.copy(alpha = 0.10f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isHighlighted) 1.5.dp else 1.dp,
            color = accentColor.copy(alpha = if (isHighlighted) 0.55f else 0.30f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PosStatementVerificationCard(
    item: StatementItem,
    currency: CurrencyCode,
    verification: DocumentVerificationRecord,
    onConfirmClick: () -> Unit,
    onDisputeClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = when (verification.status) {
                DocumentVerificationStatus.CONFIRMED_BY_POS -> SemanticIncomeGreen.copy(alpha = 0.45f)
                DocumentVerificationStatus.DISPUTED_BY_POS -> SemanticWarningAmber.copy(alpha = 0.55f)
                DocumentVerificationStatus.PENDING_POS_CONFIRMATION -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier.testTag("pos_statement_row_${item.docId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${formatDocTypeAr(item.docType)} #${item.docNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.memo.ifBlank { "قيد محاسبي موثق" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = runCatching { LocalDate.ofEpochDay(item.entryDateEpochDay).toString() }.getOrDefault(""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (item.baseDebitMinor > 0L) {
                        Text(
                            text = "فاتورة/مدين: +${Money(item.baseDebitMinor, currency).format()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = SemanticWarningAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (item.baseCreditMinor > 0L) {
                        Text(
                            text = "دفعة/دائن: -${Money(item.baseCreditMinor, currency).format()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = SemanticIncomeGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "الرصيد التراكمي: ${Money(item.runningBalanceMinor, currency).format()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            PosVerificationHandshakeRow(
                documentId = item.docId,
                verification = verification,
                onConfirmClick = onConfirmClick,
                onDisputeClick = onDisputeClick
            )
        }
    }
}

@Composable
private fun PosVerificationHandshakeRow(
    documentId: String,
    verification: DocumentVerificationRecord,
    onConfirmClick: () -> Unit,
    onDisputeClick: () -> Unit
) {
    val badgeColor = when (verification.status) {
        DocumentVerificationStatus.CONFIRMED_BY_POS -> SemanticIncomeGreen
        DocumentVerificationStatus.DISPUTED_BY_POS -> SemanticWarningAmber
        DocumentVerificationStatus.PENDING_POS_CONFIRMATION -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor.copy(alpha = 0.14f),
                border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = verification.status.posBadgeAr,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            }

            if (verification.status != DocumentVerificationStatus.PENDING_POS_CONFIRMATION) {
                val formattedTime = remember(verification.verifiedAtEpochMs) {
                    runCatching {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                            .format(Date(verification.verifiedAtEpochMs))
                    }.getOrDefault("")
                }
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (verification.status == DocumentVerificationStatus.DISPUTED_BY_POS &&
            verification.disputeNote.isNotBlank()
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SemanticWarningAmber.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, SemanticWarningAmber.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ملاحظة نقطة البيع: ${verification.disputeNote}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SemanticWarningAmber,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onConfirmClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("pos_resolve_confirm_btn_$documentId")
                    ) {
                        Text("تأكيد المطابقة الآن", fontSize = 11.sp, color = SemanticIncomeGreen)
                    }
                }
            }
        }

        if (verification.status == DocumentVerificationStatus.PENDING_POS_CONFIRMATION) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirmClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticIncomeGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.25f)
                        .testTag("pos_confirm_receipt_btn_$documentId")
                ) {
                    Text(
                        text = "✅ تأكيد الاستلام والمطابقة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                OutlinedButton(
                    onClick = onDisputeClick,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SemanticWarningAmber),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_dispute_btn_$documentId")
                ) {
                    Text(
                        text = "⚠️ إبلاغ عن خطأ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SemanticWarningAmber
                    )
                }
            }
        }
    }
}

private fun formatDocTypeAr(docType: String): String {
    return when (docType.uppercase()) {
        "SALES_INVOICE" -> "فاتورة مبيعات كروت"
        "RECEIPT_VOUCHER", "RECEIPT" -> "سند قبض وتسديد"
        "CREDIT_NOTE" -> "إشعار دائن"
        else -> docType
    }
}

@Composable
private fun PosNewRestockOrderDialog(
    packages: List<CardPackageEntity>,
    partyName: String,
    onDismiss: () -> Unit,
    onSubmitInApp: (items: List<PosRestockOrderItem>, note: String?) -> Unit,
    onSubmitAndShareWhatsApp: (items: List<PosRestockOrderItem>, note: String?) -> Unit
) {
    val effectivePackages = remember(packages) {
        if (packages.isNotEmpty()) {
            packages
        } else {
            listOf(
                CardPackageEntity(
                    id = "PKG_200",
                    name = "كرت 200",
                    durationOrQuota = "1 جيجا",
                    wholesalePriceMinor = 18000L,
                    retailPriceMinor = 20000L,
                    isActive = true
                ),
                CardPackageEntity(
                    id = "PKG_500",
                    name = "كرت 500",
                    durationOrQuota = "3 جيجا",
                    wholesalePriceMinor = 45000L,
                    retailPriceMinor = 50000L,
                    isActive = true
                ),
                CardPackageEntity(
                    id = "PKG_1000",
                    name = "كرت 1000",
                    durationOrQuota = "8 جيجا",
                    wholesalePriceMinor = 90000L,
                    retailPriceMinor = 100000L,
                    isActive = true
                )
            )
        }
    }

    val quantitiesMap = remember(effectivePackages) {
        mutableStateMapOf<String, Int>().apply {
            effectivePackages.firstOrNull()?.let { put(it.id, 50) }
        }
    }
    var orderNote by remember { mutableStateOf("") }

    val selectedOrderItems = remember(effectivePackages, quantitiesMap.toMap()) {
        effectivePackages.mapNotNull { pkg ->
            val qty = quantitiesMap[pkg.id] ?: 0
            if (qty > 0) {
                val unitPriceYer = (pkg.wholesalePriceMinor / 100L).takeIf { it > 0L }
                    ?: (pkg.retailPriceMinor / 100L).takeIf { it > 0L }
                    ?: 200L
                PosRestockOrderItem(
                    packageId = pkg.id,
                    packageName = pkg.name,
                    unitPrice = unitPriceYer,
                    requestedQuantity = qty
                )
            } else null
        }
    }

    val estimatedTotalYer = remember(selectedOrderItems) {
        selectedOrderItems.sumOf { it.lineTotal }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "📦 طلب دفعة كروت جديدة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "نقطة البيع: $partyName — حدد الكميات المطلوبة لكل فئة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                effectivePackages.forEach { pkg ->
                    val qty = quantitiesMap[pkg.id] ?: 0
                    val unitPriceYer = (pkg.wholesalePriceMinor / 100L).takeIf { it > 0L }
                        ?: (pkg.retailPriceMinor / 100L).takeIf { it > 0L }
                        ?: 200L

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (qty > 0) MikroTikCyan.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (qty > 0) MikroTikCyan.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pkg.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "سعر الكرت: $unitPriceYer ر.ي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedIconButton(
                                    onClick = {
                                        val next = (qty - 10).coerceAtLeast(0)
                                        quantitiesMap[pkg.id] = next
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("pos_restock_dec_${pkg.id}")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "إنقاص", modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = "$qty",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                OutlinedIconButton(
                                    onClick = {
                                        val next = qty + 10
                                        quantitiesMap[pkg.id] = next
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("pos_restock_inc_${pkg.id}")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Live Estimated Summary Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SemanticIncomeGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SemanticIncomeGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الإجمالي التقديري (${selectedOrderItems.sumOf { it.requestedQuantity }} كرت):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$estimatedTotalYer ر.ي",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = SemanticIncomeGreen,
                            modifier = Modifier.testTag("pos_restock_estimated_total")
                        )
                    }
                }

                OutlinedTextField(
                    value = orderNote,
                    onValueChange = { orderNote = it },
                    label = { Text("ملاحظة للطلب (اختياري)") },
                    placeholder = { Text("مثال: يرجى التوصيل صباحاً مع الموزع") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_restock_note_input")
                )

                // Secondary optional WhatsApp button
                OutlinedButton(
                    onClick = {
                        if (selectedOrderItems.isNotEmpty()) {
                            onSubmitAndShareWhatsApp(selectedOrderItems, orderNote.takeIf { it.isNotBlank() })
                        }
                    },
                    enabled = selectedOrderItems.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_send_restock_whatsapp_btn"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF128C7E))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color(0xFF128C7E),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "إرسال الطلب + إشعار عبر واتساب",
                        color = Color(0xFF128C7E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedOrderItems.isNotEmpty()) {
                        onSubmitInApp(selectedOrderItems, orderNote.takeIf { it.isNotBlank() })
                    }
                },
                enabled = selectedOrderItems.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan),
                modifier = Modifier.testTag("pos_submit_restock_order_btn")
            ) {
                Text("إرسال الطلب لإدارة الشبكة", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun PosRestockOrderHistoryCard(
    order: PosRestockOrder,
    onSendWhatsAppReminder: () -> Unit
) {
    val statusColor = when (order.status) {
        RestockOrderStatus.PENDING -> SemanticWarningAmber
        RestockOrderStatus.PREPARING -> MikroTikCyan
        RestockOrderStatus.FULFILLED -> SemanticIncomeGreen
        RestockOrderStatus.CANCELLED -> SemanticExpenseRed
    }

    val formattedTime = remember(order.createdAtEpochMs) {
        runCatching {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(order.createdAtEpochMs))
        }.getOrDefault("")
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
        modifier = Modifier.testTag("pos_order_card_${order.orderId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "طلب كروت #${order.orderId}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = order.status.badgeAr,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Items Summary
            val itemsDesc = remember(order.items) {
                order.items.joinToString(" ، ") { "${it.requestedQuantity} × ${it.packageName}" }
            }
            Text(
                text = "الأصناف: $itemsDesc",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الإجمالي التقديري: ${order.estimatedTotal} ر.ي",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MikroTikCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!order.note.isNullOrBlank()) {
                Text(
                    text = "ملاحظة: ${order.note}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!order.handledBy.isNullOrBlank()) {
                Text(
                    text = "بمتابعة: ${order.handledBy}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SemanticIncomeGreen
                )
            }

            // Quick WhatsApp Reminder Button
            OutlinedButton(
                onClick = onSendWhatsAppReminder,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pos_order_whatsapp_reminder_btn_${order.orderId}"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "📲 إرسال تذكير عبر واتساب",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PosNewSupportTicketDialog(
    onDismiss: () -> Unit,
    onSubmitTicket: (category: TicketCategory, voucherCode: String?, message: String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(TicketCategory.VOUCHER_ISSUE) }
    var voucherCodeInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "💬 مراسلة الإدارة / إبلاغ عن مشكلة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اختر نوع البلاغ:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TicketCategory.entries.forEach { cat ->
                        val selected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MikroTikCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) MikroTikCyan else MaterialTheme.colorScheme.outlineVariant
                            ),
                            onClick = { selectedCategory = cat },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pos_ticket_cat_${cat.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cat.labelAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MikroTikCyan else MaterialTheme.colorScheme.onSurface
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MikroTikCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = voucherCodeInput,
                    onValueChange = { voucherCodeInput = it },
                    label = { Text("رقم الكرت المتضرر (اختياري)") },
                    placeholder = { Text("مثال: 77482910") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_ticket_voucher_code_input")
                )

                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    label = { Text("تفاصيل البلاغ أو الرسالة") },
                    placeholder = { Text("اكتب تفاصيل المشكلة أو الاستفسار هنا...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_ticket_message_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanMsg = messageInput.trim()
                    if (cleanMsg.isNotBlank()) {
                        onSubmitTicket(
                            selectedCategory,
                            voucherCodeInput.trim().takeIf { it.isNotBlank() },
                            cleanMsg
                        )
                    }
                },
                enabled = messageInput.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan),
                modifier = Modifier.testTag("pos_submit_ticket_btn")
            ) {
                Text("إرسال البلاغ للإدارة", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun PosSupportTicketHistoryCard(
    ticket: PosSupportTicket
) {
    val statusColor = when (ticket.status) {
        TicketStatus.OPEN -> SemanticExpenseRed
        TicketStatus.IN_PROGRESS -> SemanticWarningAmber
        TicketStatus.RESOLVED -> SemanticIncomeGreen
    }

    val formattedTime = remember(ticket.createdAtEpochMs) {
        runCatching {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(ticket.createdAtEpochMs))
        }.getOrDefault("")
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
        modifier = Modifier.testTag("pos_ticket_card_${ticket.ticketId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${ticket.category.labelAr} (#${ticket.ticketId})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = ticket.status.badgeAr,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!ticket.voucherCode.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MikroTikCyan.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "رقم الكرت: ${ticket.voucherCode}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MikroTikCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = ticket.message,
                style = MaterialTheme.typography.bodySmall
            )

            if (!ticket.adminReply.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SemanticIncomeGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SemanticIncomeGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "رد الإدارة / الدعم الفني:",
                            style = MaterialTheme.typography.labelSmall,
                            color = SemanticIncomeGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ticket.adminReply,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

