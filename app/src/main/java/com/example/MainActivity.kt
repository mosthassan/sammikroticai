package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.inspector.ChartOfAccountsScreen
import com.example.ui.inspector.HealthCheckScreen
import com.example.ui.inspector.InspectorViewModel
import com.example.ui.inspector.JournalBrowserScreen
import com.example.ui.inspector.TrialBalanceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PackagesStockScreen
import com.example.ui.screens.PartiesScreen
import com.example.ui.screens.PurchasesAssetsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.VouchersScreen
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.data.sync.SyncState
import com.example.ui.screens.CloudSyncSheet
import com.example.ui.screens.NetworkHubScreen
import com.example.ui.theme.BrandCyanPrimary
import com.example.ui.theme.SamMikrotikTheme
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.ui.ai.AIAssistantScreen
import com.example.ui.viewmodel.AppViewModel

object AppBrand {
    const val NAME = "سام ميكروتك"
    const val DASHBOARD_TITLE = "سام ميكروتك نظام ادارة الشبكات"
    const val TAGLINE = "النظام المالي المحاسبي المتكامل لشبكات الإنترنت"
}

sealed class MainTab(val title: String, val icon: ImageVector, val tag: String) {
    object Operations : MainTab("الميكروتك", Icons.Default.Router, "tab_operations")
    object Dashboard : MainTab("المالية", Icons.Default.Dashboard, "tab_dashboard")
    object Sales : MainTab("المبيعات", Icons.Default.Receipt, "tab_sales")
    object Vouchers : MainTab("السندات", Icons.Default.AccountBalanceWallet, "tab_vouchers")
    object Purchases : MainTab("المشتريات", Icons.Default.ShoppingCart, "tab_purchases")
    object PartiesAndStock : MainTab("العملاء والبقالات", Icons.Default.Store, "tab_parties_stock")
    object NetworkHub : MainTab("الشبكة", Icons.Default.Settings, "tab_network_hub")
    object Reports : MainTab("التقارير", Icons.Default.Assessment, "tab_reports")
    object Audit : MainTab("الرقابة", Icons.Default.Security, "tab_audit")

    companion object {
        val all = listOf(Operations, Dashboard, Sales, Vouchers, Purchases, PartiesAndStock, NetworkHub, Reports, Audit)
    }
}

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == com.example.data.auth.AuthManager.RC_GOOGLE_SIGN_IN) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
            appViewModel.handleLegacyGoogleSignInResult(task)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.core.firebase.FirebaseInitializer.ensureInitialized(this)
        enableEdgeToEdge()
        setContent {
            SamMikrotikTheme {
                val appViewModel: AppViewModel = this@MainActivity.appViewModel
                val inspectorViewModel: InspectorViewModel = viewModel()
                val snackbarHostState = remember { SnackbarHostState() }

                var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
                var vouchersInitialTab by rememberSaveable { mutableIntStateOf(0) }
                var networkHubInitialTab by rememberSaveable { mutableIntStateOf(0) }
                val invariantResult by appViewModel.invariantResult.collectAsState()
                val syncState by appViewModel.syncState.collectAsState()
                val currentUser by appViewModel.currentUser.collectAsState()

                var showCloudSyncSheet by rememberSaveable { mutableStateOf(false) }
                var showAiAssistant by rememberSaveable { mutableStateOf(false) }
                var showSubscriptionScreen by rememberSaveable { mutableStateOf(false) }
                var showAdminDashboard by rememberSaveable { mutableStateOf(false) }
                var showCardStudio by rememberSaveable { mutableStateOf(false) }
                var showDeviceMonitorScreen by rememberSaveable { mutableStateOf(false) }
                var showRouterLoginScreen by rememberSaveable { mutableStateOf(false) }
                var isRouterSessionLoggedIn by rememberSaveable { mutableStateOf(false) }
                val syncSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                // Listen for business feedback messages
                LaunchedEffect(appViewModel) {
                    appViewModel.userMessage.collect { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                // Handle system back navigation (navigate back to Dashboard if on sub-screens)
                if (selectedTabIndex != 0) {
                    BackHandler {
                        selectedTabIndex = 0
                    }
                }

                if (showRouterLoginScreen || (selectedTabIndex == 0 && !isRouterSessionLoggedIn)) {
                    BackHandler(enabled = isRouterSessionLoggedIn || selectedTabIndex != 0) {
                        showRouterLoginScreen = false
                        if (!isRouterSessionLoggedIn) {
                            selectedTabIndex = 1
                        }
                    }
                    com.example.ui.screens.RouterLoginScreen(
                        repository = appViewModel.routerProfilesRepository,
                        viewModel = appViewModel,
                        onLoginSuccess = { profile ->
                            isRouterSessionLoggedIn = true
                            showRouterLoginScreen = false
                            selectedTabIndex = 0
                        },
                        onOfflineEnter = {
                            showRouterLoginScreen = false
                            selectedTabIndex = 1
                        }
                    )
                } else if (showAdminDashboard) {
                    com.example.ui.screens.AdminDashboardScreen(
                        viewModel = appViewModel,
                        onNavigateBack = { showAdminDashboard = false }
                    )
                } else if (showSubscriptionScreen) {
                    com.example.ui.screens.SubscriptionScreen(
                        viewModel = appViewModel,
                        onNavigateBack = { showSubscriptionScreen = false },
                        onNavigateToAdmin = {
                            showSubscriptionScreen = false
                            showAdminDashboard = true
                        }
                    )
                } else if (showAiAssistant) {
                    AIAssistantScreen(
                        onNavigateBack = { showAiAssistant = false }
                    )
                } else if (showCardStudio) {
                    com.example.ui.cardstudio.CardStudioScreen(
                        onNavigateBack = { showCardStudio = false }
                    )
                } else if (showDeviceMonitorScreen) {
                    com.example.ui.screens.NetworkDevicesScreen(
                        networkRepository = appViewModel.networkRepository,
                        onNavigateBack = { showDeviceMonitorScreen = false }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = AppBrand.NAME,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = MainTab.all[selectedTabIndex].title,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandCyanPrimary
                                        )
                                    }
                                },
                                actions = {
                                    val isBalanced = invariantResult?.isValid ?: true
                                    val badgeColor = if (isBalanced) SemanticIncomeGreen else SemanticExpenseRed
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedTabIndex = MainTab.all.indexOf(MainTab.Audit) /* Jump to Audit tab */ }
                                            .testTag("top_bar_invariant_badge")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isBalanced) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = badgeColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBalanced) "IFRS متزن" else "خلل",
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }

                                    val activeRouter by appViewModel.activeRouterProfile.collectAsState()
                                    Surface(
                                        color = Color(0xFF0F3A47),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showRouterLoginScreen = true }
                                            .testTag("top_bar_router_pill")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Router,
                                                contentDescription = "Router",
                                                tint = BrandCyanPrimary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = activeRouter?.name ?: "10.0.0.1",
                                                color = TextPrimaryDark,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }

                                    val isAdmin by appViewModel.isAdmin.collectAsState()
                                    if (isAdmin) {
                                        IconButton(
                                            onClick = { showAdminDashboard = true },
                                            modifier = Modifier.testTag("top_bar_admin_dashboard_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AdminPanelSettings,
                                                contentDescription = "لوحة تحكم المسؤول",
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    val subStatus by appViewModel.subscriptionStatus.collectAsState()
                                    val isProActive = subStatus.isPro && !subStatus.isExpired
                                    IconButton(
                                        onClick = { showSubscriptionScreen = true },
                                        modifier = Modifier.testTag("top_bar_subscription_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = "الاشتراك والتفعيل",
                                            tint = if (isProActive) Color(0xFFFFD700) else BrandCyanPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { showAiAssistant = true },
                                        modifier = Modifier.testTag("top_bar_ai_assistant_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "مساعد سامي الذكي (ERP AI)",
                                            tint = BrandCyanPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { showCloudSyncSheet = true },
                                        modifier = Modifier.testTag("top_bar_cloud_sync_btn")
                                    ) {
                                        Icon(
                                            imageVector = when (syncState) {
                                                is SyncState.InProgress -> Icons.Default.Sync
                                                is SyncState.Success -> Icons.Default.CloudDone
                                                is SyncState.Error -> Icons.Default.CloudOff
                                                else -> Icons.Default.Cloud
                                            },
                                            contentDescription = "المزامنة السحابية",
                                            tint = BrandCyanPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = CyberDarkSurface
                                )
                            )
                        },
                        bottomBar = {
                            Surface(
                                color = CyberDarkSurface,
                                tonalElevation = 6.dp,
                                shadowElevation = 12.dp,
                                border = BorderStroke(1.dp, CyberBorder.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MainTab.all.forEachIndexed { index, tab ->
                                        val isSelected = selectedTabIndex == index
                                        Surface(
                                            onClick = { selectedTabIndex = index },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) BrandCyanPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                            border = if (isSelected) BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.6f)) else null,
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag(tab.tag)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = tab.icon,
                                                    contentDescription = tab.title,
                                                    tint = if (isSelected) BrandCyanPrimary else TextSecondaryDark,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                                Text(
                                                    text = tab.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) BrandCyanPrimary else TextSecondaryDark
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                ) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)
                    when (selectedTabIndex) {
                        0 -> com.example.ui.screens.MikrotikOperationsScreen(
                            networkRepository = appViewModel.networkRepository,
                            viewModel = appViewModel,
                            onNavigateToDeviceMonitor = { showDeviceMonitorScreen = true },
                            onNavigateToCardStudio = { showCardStudio = true },
                            onNavigateToWifiSpectrum = {
                                networkHubInitialTab = 5
                                selectedTabIndex = 6
                            },
                            onNavigateToAiAssistant = { showAiAssistant = true },
                            onNavigateToCloudSync = { showCloudSyncSheet = true },
                            onNavigateToRouterLogin = { showRouterLoginScreen = true },
                            modifier = modifier
                        )
                        1 -> DashboardScreen(
                            viewModel = appViewModel,
                            onNavigateToQuickSale = { selectedTabIndex = 2 },
                            onNavigateToSales = { selectedTabIndex = 2 },
                            onNavigateToReceipts = {
                                vouchersInitialTab = 0
                                selectedTabIndex = 3
                            },
                            onNavigateToPayments = {
                                vouchersInitialTab = 1
                                selectedTabIndex = 3
                            },
                            onNavigateToReports = { selectedTabIndex = 7 },
                            onNavigateToWifiSpectrum = {
                                networkHubInitialTab = 5
                                selectedTabIndex = 6
                            },
                            onNavigateToAiAssistant = { showAiAssistant = true },
                            onNavigateToCardStudio = { showCardStudio = true },
                            modifier = modifier
                        )
                        2 -> SalesScreen(viewModel = appViewModel, modifier = modifier)
                        3 -> VouchersScreen(viewModel = appViewModel, initialTab = vouchersInitialTab, modifier = modifier)
                        4 -> PurchasesAssetsScreen(viewModel = appViewModel, modifier = modifier)
                        5 -> PartiesAndStockCombinedScreen(viewModel = appViewModel, modifier = modifier)
                        6 -> NetworkHubScreen(
                            networkRepository = appViewModel.networkRepository,
                            viewModel = appViewModel,
                            initialTab = networkHubInitialTab,
                            onNavigateToSubscription = { showSubscriptionScreen = true },
                            onNavigateToCardStudio = { showCardStudio = true },
                            modifier = modifier
                        )
                        7 -> ReportsScreen(viewModel = appViewModel, modifier = modifier)
                        8 -> AuditScreen(inspectorViewModel = inspectorViewModel, modifier = modifier)
                    }
                }
            }

                if (showCloudSyncSheet) {
                    CloudSyncSheet(
                        viewModel = appViewModel,
                        sheetState = syncSheetState,
                        onDismissRequest = { showCloudSyncSheet = false }
                    )
                }

                // Freemium Limit Alert Dialog
                val showLimitDialog by appViewModel.showSubscriptionLimitDialog.collectAsState()
                if (showLimitDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { appViewModel.dismissSubscriptionLimitDialog() },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SemanticExpenseRed
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تنبيه الحد الأقصى للخطة المجانية", fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "لقد وصلت للحد الأقصى المسموح به في الخطة المجانية (100 فاتورة / 50 جهازاً). يرجى ترقية اشتراكك للاستمرار.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0F2417),
                                    border = BorderStroke(1.dp, Color(0xFF25D366)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val hwId = appViewModel.licenseManager.hardwareDeviceId
                                            com.example.util.WhatsAppDispatcher.sendTextMessage(
                                                context = this@MainActivity,
                                                phoneNumber = com.example.domain.subscription.SubscriptionLimits.ACTIVATION_WHATSAPP_NUMBER,
                                                message = com.example.domain.subscription.SubscriptionLimits.buildActivationWhatsAppMessage(hwId)
                                            )
                                        }
                                        .testTag("dialog_whatsapp_activation_row")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_whatsapp),
                                            contentDescription = "واتساب للتفعيل",
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "للتفعيل راسلنا وتساب على الرقم 770446040",
                                                color = Color(0xFF25D366),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "انقر هنا لفتح المحادثة ومراسلتنا مباشرة",
                                                color = Color.LightGray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(
                                onClick = {
                                    val hwId = appViewModel.licenseManager.hardwareDeviceId
                                    com.example.util.WhatsAppDispatcher.sendTextMessage(
                                        context = this@MainActivity,
                                        phoneNumber = com.example.domain.subscription.SubscriptionLimits.ACTIVATION_WHATSAPP_NUMBER,
                                        message = com.example.domain.subscription.SubscriptionLimits.buildActivationWhatsAppMessage(hwId)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.testTag("dialog_btn_whatsapp_direct")
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_whatsapp),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("واتساب (770446040)", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        appViewModel.dismissSubscriptionLimitDialog()
                                        showSubscriptionScreen = true
                                    }
                                ) {
                                    Text("شاشة الاشتراك")
                                }
                                androidx.compose.material3.TextButton(
                                    onClick = { appViewModel.dismissSubscriptionLimitDialog() }
                                ) {
                                    Text("إغلاق")
                                }
                            }
                        }
                    )
                }

                // Prompt to restore if remote cloud backup exists on empty/fresh app
                val pendingRestore by appViewModel.pendingCloudRestorePrompt.collectAsState()
                pendingRestore?.let { backup ->
                    val dateStr = if (backup.timestamp > 0L) {
                        com.example.util.TransactionDateUtils.formatDateTime(backup.timestamp)
                    } else "غير محدد"

                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { appViewModel.dismissCloudRestorePrompt() },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = BrandCyanPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("استعادة النسخة الاحتياطية السحابية", fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column {
                                Text(
                                    text = "تم العثور على نسخة احتياطية سحابية سابقة لحسابك (${backup.userEmail}) بتاريخ $dateStr تحتوي على ${backup.documentsCount} مستنداً و ${backup.journalLinesCount} قيداً و ${backup.networkDevicesCount} جهازاً و ${backup.currencyRatesCount} سعر صرف.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "هل ترغب في استعادة بياناتك السحابية بالكامل الآن ومطابقتها محلياً في عملية ذرية واحدة؟",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(
                                onClick = { appViewModel.restorePendingCloudBackup() },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                                modifier = Modifier.testTag("btn_confirm_restore_cloud")
                            ) {
                                Text("استعادة من السحابة الآن")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { appViewModel.dismissCloudRestorePrompt() },
                                modifier = Modifier.testTag("btn_dismiss_restore_cloud")
                            ) {
                                Text("تخطي والبدء بحساب فارغ")
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Host screen combining Parties (Customers, Agents, Vendors) and Card Packages / Physical Stock.
 */
@Composable
private fun PartiesAndStockCombinedScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var subTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("الأطراف والوكلاء والديون", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_parties")
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("باقات الكروت والمخزون", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_packages_stock")
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (subTab == 0) {
                PartiesScreen(viewModel = viewModel)
            } else {
                PackagesStockScreen(viewModel = viewModel)
            }
        }
    }
}

/**
 * Audit & Inspector Host screen with direct access to Core Double-Entry accounting verification tools.
 */
@Composable
private fun AuditScreen(
    inspectorViewModel: InspectorViewModel,
    modifier: Modifier = Modifier
) {
    var auditSubTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = auditSubTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            edgePadding = 8.dp
        ) {
            Tab(
                selected = auditSubTab == 0,
                onClick = { auditSubTab = 0 },
                text = { Text("السلامة والاتزان", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("audit_subtab_health")
            )
            Tab(
                selected = auditSubTab == 1,
                onClick = { auditSubTab = 1 },
                text = { Text("دليل الحسابات الموحد", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("audit_subtab_accounts")
            )
            Tab(
                selected = auditSubTab == 2,
                onClick = { auditSubTab = 2 },
                text = { Text("دفتر اليومية العام", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("audit_subtab_journal")
            )
            Tab(
                selected = auditSubTab == 3,
                onClick = { auditSubTab = 3 },
                text = { Text("ميزان المراجعة", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("audit_subtab_trial_balance")
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (auditSubTab) {
                0 -> HealthCheckScreen(inspectorViewModel)
                1 -> ChartOfAccountsScreen(inspectorViewModel)
                2 -> JournalBrowserScreen(inspectorViewModel)
                3 -> TrialBalanceScreen(inspectorViewModel)
            }
        }
    }
}
