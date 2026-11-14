package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.auth.PosRestockOrder
import com.example.data.auth.PosSupportTicket
import com.example.data.auth.RestockOrderStatus
import com.example.data.auth.RoleAccessPolicy
import com.example.data.auth.TicketStatus
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase 3: Owner & Distributor "Orders & Support Inbox" (`صندوق طلبات الكروت وبلاغات نقاط البيع`).
 *
 * - Tab 1: `📦 طلبات الكروت (Restock Orders)` with status controls (`قبول وتجهيز`, `تم التسليم`, `اعتذار/إلغاء`)
 *   and smart `"⚡ إصدار فاتورة مبيعات للطلب"` button that opens [NewSalesInvoiceBottomSheet] pre-filled for that POS `partyId`.
 * - Tab 2: `🛠️ بلاغات وشكاوى النقاط (Support Tickets)` with `voucherCode` display, `adminReply` dialog,
 *   and status transitions (`IN_PROGRESS`, `RESOLVED`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosInboxScreen(
    viewModel: AppViewModel,
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val orders by viewModel.posRestockOrders.collectAsStateWithLifecycle()
    val tickets by viewModel.posSupportTickets.collectAsStateWithLifecycle()
    val parties by viewModel.allParties.collectAsStateWithLifecycle()
    val packages by viewModel.allPackages.collectAsStateWithLifecycle()
    val treasuries by viewModel.allTreasuries.collectAsStateWithLifecycle()
    val stockMovements by viewModel.allStockMovements.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val agentPermissions by viewModel.activeAgentPermissions.collectAsStateWithLifecycle()

    val canCreateInvoices = RoleAccessPolicy.canCreateSalesInvoices(activeRole, agentPermissions)

    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var orderForInvoiceSheet by remember { mutableStateOf<PosRestockOrder?>(null) }
    var replyingTicket by remember { mutableStateOf<PosSupportTicket?>(null) }
    var replyTextInput by remember { mutableStateOf("") }
    var replyTargetStatus by remember { mutableStateOf(TicketStatus.RESOLVED) }

    var partyBalances by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

    LaunchedEffect(Unit) {
        viewModel.syncPosOrdersAndTicketsWithCloud()
        viewModel.getCustomerReceivableBalances { balances ->
            partyBalances = balances
        }
    }

    val stockBalanceByPackageId = remember(stockMovements) {
        stockMovements.groupBy { it.packageId }.mapValues { (_, movements) ->
            movements.sumOf { m ->
                when (m.type) {
                    "RECEIVE", "RETURN" -> m.quantity
                    "SELL" -> -m.quantity
                    "ADJUST" -> m.quantity
                    else -> 0
                }
            }.coerceAtLeast(0)
        }
    }

    val pendingOrdersCount = remember(orders) {
        orders.count { it.status == RestockOrderStatus.PENDING || it.status == RestockOrderStatus.PREPARING }
    }
    val activeTicketsCount = remember(tickets) {
        tickets.count { it.status == TicketStatus.OPEN || it.status == TicketStatus.IN_PROGRESS }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("pos_inbox_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "صندوق طلبات الكروت وبلاغات النقاط",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "إدارة طلبات تغذية الكروت والرد على بلاغات نقاط البيع",
                            style = MaterialTheme.typography.labelSmall,
                            color = MikroTikCyan
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncPosOrdersAndTicketsWithCloud() },
                        modifier = Modifier.testTag("pos_inbox_sync_btn")
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "مزامنة الطلبات والبلاغات",
                            tint = MikroTikCyan
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("pos_inbox_tab_orders"),
                    text = {
                        Text(
                            text = if (pendingOrdersCount > 0) {
                                "📦 طلبات الكروت ($pendingOrdersCount)"
                            } else {
                                "📦 طلبات الكروت"
                            },
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("pos_inbox_tab_tickets"),
                    text = {
                        Text(
                            text = if (activeTicketsCount > 0) {
                                "🛠️ بلاغات وشكاوى النقاط ($activeTicketsCount)"
                            } else {
                                "🛠️ بلاغات وشكاوى النقاط"
                            },
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> {
                    if (orders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(42.dp)
                                )
                                Text(
                                    text = "لا توجد طلبات تغذية كروت واردة من نقاط البيع حالياً.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(orders, key = { it.orderId }) { order ->
                                InboxRestockOrderCard(
                                    order = order,
                                    canCreateInvoices = canCreateInvoices,
                                    onUpdateStatus = { nextStatus ->
                                        viewModel.updatePosRestockOrderStatus(
                                            orderId = order.orderId,
                                            newStatus = nextStatus
                                        )
                                    },
                                    onIssueSalesInvoice = {
                                        orderForInvoiceSheet = order
                                    }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    if (tickets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(42.dp)
                                )
                                Text(
                                    text = "لا توجد بلاغات أو شكاوى مفتوحة من نقاط البيع حالياً.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(tickets, key = { it.ticketId }) { ticket ->
                                InboxSupportTicketCard(
                                    ticket = ticket,
                                    onMarkInProgress = {
                                        viewModel.updatePosSupportTicketStatus(
                                            ticketId = ticket.ticketId,
                                            newStatus = TicketStatus.IN_PROGRESS,
                                            adminReply = ticket.adminReply
                                        )
                                    },
                                    onOpenReplyDialog = { targetStatus ->
                                        replyingTicket = ticket
                                        replyTextInput = ticket.adminReply.orEmpty()
                                        replyTargetStatus = targetStatus
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Smart Action: Pre-filled Sales Invoice Sheet for POS Restock Order
    orderForInvoiceSheet?.let { targetOrder ->
        val initialDraftItems = remember(targetOrder, packages) {
            targetOrder.items.map { item ->
                val matchedPkg = packages.firstOrNull {
                    it.id == item.packageId || it.name.equals(item.packageName, ignoreCase = true)
                }
                val unitPriceText = if (matchedPkg != null &&
                    matchedPkg.wholesalePriceMinor >= 10_000L &&
                    item.unitPrice == matchedPkg.wholesalePriceMinor
                ) {
                    (item.unitPrice / 100L).toString()
                } else if (item.unitPrice > 0L) {
                    item.unitPrice.toString()
                } else {
                    matchedPkg?.let { (it.wholesalePriceMinor / 100L).toString() } ?: "200"
                }

                SalesItemDraftState(
                    selectedPackageId = matchedPkg?.id ?: item.packageId,
                    description = matchedPkg?.name ?: item.packageName,
                    quantityText = item.requestedQuantity.toString(),
                    unitPriceText = unitPriceText
                )
            }
        }

        val prefilledNotes = buildString {
            append("تلبية طلب دفعة كروت #${targetOrder.orderId} لنقطة البيع ${targetOrder.partyName}")
            if (!targetOrder.note.isNullOrBlank()) {
                append(" — ${targetOrder.note}")
            }
        }

        NewSalesInvoiceBottomSheet(
            parties = parties.filter { it.isCustomer },
            packages = packages,
            treasuries = treasuries,
            stockBalances = stockBalanceByPackageId,
            partyBalances = partyBalances,
            initialPartyId = targetOrder.partyId,
            initialItems = initialDraftItems,
            initialNotes = prefilledNotes,
            onDismiss = { orderForInvoiceSheet = null },
            onSubmit = { partyId, salesItems, cashPaidMinor, treasuryId, notes ->
                viewModel.postSalesInvoiceWithSettlement(
                    partyId = partyId,
                    cardItems = salesItems,
                    cashPaidMinor = cashPaidMinor,
                    treasuryId = treasuryId,
                    notes = notes,
                    onSuccess = {
                        viewModel.updatePosRestockOrderStatus(
                            orderId = targetOrder.orderId,
                            newStatus = RestockOrderStatus.FULFILLED
                        )
                        orderForInvoiceSheet = null
                    }
                )
            }
        )
    }

    // Admin Reply & Ticket Status Dialog
    replyingTicket?.let { targetTicket ->
        AlertDialog(
            onDismissRequest = { replyingTicket = null },
            title = {
                Text(
                    text = "الرد على بلاغ ${targetTicket.partyName} (#${targetTicket.ticketId})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "التصنيف: ${targetTicket.category.labelAr}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MikroTikCyan,
                        fontWeight = FontWeight.Bold
                    )
                    if (!targetTicket.voucherCode.isNullOrBlank()) {
                        Text(
                            text = "رقم الكرت المبلّغ عنه: ${targetTicket.voucherCode}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "نص البلاغ: ${targetTicket.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = replyTextInput,
                        onValueChange = { replyTextInput = it },
                        label = { Text("رد الإدارة / الموزع لنقطة البيع") },
                        placeholder = { Text("مثال: تم فحص الكرت وتجديد الصلاحية / تم إرسال فريق الصيانة") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_inbox_ticket_reply_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = replyTargetStatus == TicketStatus.IN_PROGRESS,
                            onClick = { replyTargetStatus = TicketStatus.IN_PROGRESS },
                            label = { Text(TicketStatus.IN_PROGRESS.badgeAr) }
                        )
                        FilterChip(
                            selected = replyTargetStatus == TicketStatus.RESOLVED,
                            onClick = { replyTargetStatus = TicketStatus.RESOLVED },
                            label = { Text(TicketStatus.RESOLVED.badgeAr) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePosSupportTicketStatus(
                            ticketId = targetTicket.ticketId,
                            newStatus = replyTargetStatus,
                            adminReply = replyTextInput.trim().ifBlank { "تمت المراجعة والمعالجة من قبل إدارة الشبكة" }
                        )
                        replyingTicket = null
                        replyTextInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticIncomeGreen),
                    modifier = Modifier.testTag("pos_inbox_confirm_reply_btn")
                ) {
                    Text("حفظ الرد وتحديث الحالة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { replyingTicket = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun InboxRestockOrderCard(
    order: PosRestockOrder,
    canCreateInvoices: Boolean,
    onUpdateStatus: (RestockOrderStatus) -> Unit,
    onIssueSalesInvoice: () -> Unit
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
        modifier = Modifier.testTag("inbox_order_card_${order.orderId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.16f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = order.partyName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "طلب #${order.orderId} • $formattedTime",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = order.status.badgeAr,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Requested Items Breakdown
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.requestedQuantity} × ${item.packageName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${item.lineTotal} ر.ي",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MikroTikCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الإجمالي التقديري (${order.totalRequestedCards} كرت):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${order.estimatedTotal} ر.ي",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = FontFamily.Monospace,
                            color = SemanticIncomeGreen,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            if (!order.note.isNullOrBlank()) {
                Text(
                    text = "ملاحظة نقطة البيع: ${order.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SemanticWarningAmber,
                    fontWeight = FontWeight.Medium
                )
            }

            // Status Controls: قبول وتجهيز | تم التسليم | اعتذار/إلغاء
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onUpdateStatus(RestockOrderStatus.PREPARING) },
                    enabled = order.status != RestockOrderStatus.PREPARING && order.status != RestockOrderStatus.FULFILLED,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inbox_order_prepare_btn_${order.orderId}")
                ) {
                    Text("قبول وتجهيز", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onUpdateStatus(RestockOrderStatus.FULFILLED) },
                    enabled = order.status != RestockOrderStatus.FULFILLED,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inbox_order_fulfill_btn_${order.orderId}")
                ) {
                    Text("تم التسليم", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SemanticIncomeGreen)
                }

                OutlinedButton(
                    onClick = { onUpdateStatus(RestockOrderStatus.CANCELLED) },
                    enabled = order.status != RestockOrderStatus.CANCELLED && order.status != RestockOrderStatus.FULFILLED,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inbox_order_cancel_btn_${order.orderId}")
                ) {
                    Text("اعتذار/إلغاء", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SemanticExpenseRed)
                }
            }

            // Smart Action Button: "⚡ إصدار فاتورة مبيعات للطلب"
            if (canCreateInvoices && order.status != RestockOrderStatus.CANCELLED) {
                Button(
                    onClick = onIssueSalesInvoice,
                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_inbox_issue_invoice_btn_${order.orderId}")
                ) {
                    Text(
                        text = "⚡ إصدار فاتورة مبيعات للطلب",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxSupportTicketCard(
    ticket: PosSupportTicket,
    onMarkInProgress: () -> Unit,
    onOpenReplyDialog: (TicketStatus) -> Unit
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
        modifier = Modifier.testTag("inbox_ticket_card_${ticket.ticketId}")
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${ticket.partyName} — ${ticket.category.labelAr}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "بلاغ #${ticket.ticketId} • $formattedTime",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = ticket.status.badgeAr,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!ticket.voucherCode.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MikroTikCyan.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "رقم الكرت المبلّغ عنه: ${ticket.voucherCode}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MikroTikCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = ticket.message,
                style = MaterialTheme.typography.bodyMedium
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
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "رد الإدارة المسجل:",
                            style = MaterialTheme.typography.labelSmall,
                            color = SemanticIncomeGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ticket.adminReply,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onMarkInProgress,
                    enabled = ticket.status == TicketStatus.OPEN,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inbox_ticket_in_progress_btn_${ticket.ticketId}")
                ) {
                    Text("قيد المعالجة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onOpenReplyDialog(TicketStatus.RESOLVED) },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticIncomeGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("inbox_ticket_reply_resolve_btn_${ticket.ticketId}")
                ) {
                    Text(
                        text = if (ticket.adminReply.isNullOrBlank()) "الرد وحل البلاغ ✅" else "تعديل الرد / الحالة",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
