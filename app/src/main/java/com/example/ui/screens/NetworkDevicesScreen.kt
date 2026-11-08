package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.DeviceSaveResult
import com.example.data.network.DeviceStatus
import com.example.data.network.DeviceType
import com.example.data.network.NetworkDevice
import com.example.data.network.NetworkRepository
import com.example.ui.components.AddEditDeviceDialog
import com.example.ui.components.DeviceItemCard
import com.example.ui.components.DeviceJsonBackupDialog
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCanvas
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.launch

/**
 * Screen for managing and monitoring Network Devices (APs, Stations, Switches, Routers).
 * Matches MkCards style with ARP table discovery, Ping latency, and instant Browser launch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDevicesScreen(
    networkRepository: NetworkRepository,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val devices by networkRepository.devices.collectAsState()
    val arpDevices by networkRepository.arpDiscoveredDevices.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<DeviceStatus?>(null) }
    var activeSubTab by remember { mutableStateOf(0) } // 0: Monitored Devices, 1: ARP Discovered
    var isPingingAll by remember { mutableStateOf(false) }

    var isAddingDevice by remember { mutableStateOf(false) }
    var deviceToEdit by remember { mutableStateOf<NetworkDevice?>(null) }
    var isCloningDevice by remember { mutableStateOf(false) }
    var deviceToDelete by remember { mutableStateOf<NetworkDevice?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    val licenseManager = remember { com.example.util.LicenseManager.getInstance(context) }

    if (onNavigateBack != null) {
        BackHandler { onNavigateBack() }
    }

    // Counters for the status chips row
    val totalCount = devices.size
    val onlineCount = remember(devices) { devices.count { it.status == DeviceStatus.ONLINE } }
    val warningCount = remember(devices) { devices.count { it.status == DeviceStatus.WARNING } }
    val offlineCount = remember(devices) { devices.count { it.status == DeviceStatus.OFFLINE } }
    val uncheckedCount = remember(devices) { devices.count { it.status == DeviceStatus.UNCHECKED } }

    // Filtered monitored devices
    val filteredDevices = remember(devices, searchQuery, selectedStatusFilter) {
        devices.filter { dev ->
            val matchesSearch = searchQuery.isBlank() ||
                dev.name.contains(searchQuery, ignoreCase = true) ||
                dev.ipAddress.contains(searchQuery, ignoreCase = true) ||
                dev.macAddress.contains(searchQuery, ignoreCase = true) ||
                dev.model.contains(searchQuery, ignoreCase = true)

            val matchesStatus = selectedStatusFilter == null || dev.status == selectedStatusFilter
            matchesSearch && matchesStatus
        }
    }

    // Filtered ARP devices
    val filteredArp = remember(arpDevices, searchQuery) {
        arpDevices.filter { arp ->
            searchQuery.isBlank() ||
                arp.name.contains(searchQuery, ignoreCase = true) ||
                arp.ipAddress.contains(searchQuery, ignoreCase = true) ||
                arp.macAddress.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkCanvas)
            .padding(14.dp)
    ) {
        // Top Bar: Back icon + Title ("مراقبة الأجهزة - 10.0.0.1") + Cloud status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_network_devices")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F1E36)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = MikroTikCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "مراقبة الأجهزة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.WifiTethering, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = "الراوتر الأساسي: 10.0.0.1",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick Status Capsule
            Surface(
                color = Color(0xFF0C243C),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusOnline))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("متصل", color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar (MkCards style: "بحث بالاسم أو IP أو MAC")
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_device_input"),
            placeholder = { Text("بحث بالاسم أو IP أو MAC", color = TextMutedDark, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح", tint = TextMutedDark, modifier = Modifier.size(18.dp))
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CyberDarkCardElevated,
                unfocusedContainerColor = CyberDarkCardElevated,
                focusedBorderColor = MikroTikCyan,
                unfocusedBorderColor = CyberBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Status Summary Counters Row (MkCards style: [الكل] [متصل] [بلا رد] [خطأ] [لم يُفحص])
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusCounterChip(
                label = "الكل",
                count = totalCount,
                color = MikroTikCyan,
                isSelected = selectedStatusFilter == null,
                onClick = { selectedStatusFilter = null },
                modifier = Modifier.weight(1f)
            )
            StatusCounterChip(
                label = "متصل",
                count = onlineCount,
                color = StatusOnline,
                isSelected = selectedStatusFilter == DeviceStatus.ONLINE,
                onClick = { selectedStatusFilter = if (selectedStatusFilter == DeviceStatus.ONLINE) null else DeviceStatus.ONLINE },
                modifier = Modifier.weight(1f)
            )
            StatusCounterChip(
                label = "بلا رد",
                count = warningCount,
                color = StatusWarning,
                isSelected = selectedStatusFilter == DeviceStatus.WARNING,
                onClick = { selectedStatusFilter = if (selectedStatusFilter == DeviceStatus.WARNING) null else DeviceStatus.WARNING },
                modifier = Modifier.weight(1f)
            )
            StatusCounterChip(
                label = "خطأ",
                count = offlineCount,
                color = StatusOffline,
                isSelected = selectedStatusFilter == DeviceStatus.OFFLINE,
                onClick = { selectedStatusFilter = if (selectedStatusFilter == DeviceStatus.OFFLINE) null else DeviceStatus.OFFLINE },
                modifier = Modifier.weight(1f)
            )
            StatusCounterChip(
                label = "لم يُفحص",
                count = uncheckedCount,
                color = Color(0xFF94A3B8),
                isSelected = selectedStatusFilter == DeviceStatus.UNCHECKED,
                onClick = { selectedStatusFilter = if (selectedStatusFilter == DeviceStatus.UNCHECKED) null else DeviceStatus.UNCHECKED },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary Tabs: [الأجهزة المراقبة] vs [جدول ARP المكتشف]
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = CyberDarkCardElevated,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                    color = MikroTikCyan,
                    height = 2.5.dp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, CyberBorder), RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = {
                    Text(
                        "الأجهزة المراقبة (${filteredDevices.size})",
                        fontWeight = if (activeSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeSubTab == 0) MikroTikCyan else TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = {
                    Text(
                        "جدول ARP المكتشف (${arpDevices.size})",
                        fontWeight = if (activeSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeSubTab == 1) MikroTikCyan else TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
            )
        }

        if (isPingingAll) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = MikroTikCyan,
                trackColor = Color(0xFF0F1E36)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main List Content
        Box(modifier = Modifier.weight(1f)) {
            if (activeSubTab == 0) {
                // Tab 0: Monitored Devices List
                if (filteredDevices.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Router, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد أجهزة مطابقة للتصفية", color = TextSecondaryDark, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredDevices, key = { it.id }) { dev ->
                            DeviceItemCard(
                                device = dev,
                                onEdit = { deviceToEdit = dev; isCloningDevice = false },
                                onClone = {
                                    if (!licenseManager.canCreateDevice(devices.size)) {
                                        showLimitDialog = true
                                    } else {
                                        deviceToEdit = dev
                                        isCloningDevice = true
                                    }
                                },
                                onDelete = { deviceToDelete = dev },
                                onPing = {
                                    scope.launch {
                                        val updated = networkRepository.pingDevice(dev)
                                        Toast.makeText(context, "استجابة ${updated.name}: ${updated.pingLatencyMs} ms", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onOpenInBrowser = {
                                    val port = if (dev.webManagementPort != 80) ":${dev.webManagementPort}" else ""
                                    val url = "http://${dev.ipAddress}$port"
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }.onFailure {
                                        Toast.makeText(context, "تعذر فتح المتصفح للعنوان: $url", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Tab 1: ARP Discovered Devices
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = Color(0xFF0C243C),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lan, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "500 عنوان مكتشف ضمن جدول الـ ARP (منفذ bridge-LAN)",
                                color = TextPrimaryDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredArp, key = { it.id }) { arp ->
                            ArpDiscoveredCard(
                                device = arp,
                                onAddToList = {
                                    scope.launch {
                                        val result = networkRepository.importFromArp(arp)
                                        when (result) {
                                            is DeviceSaveResult.Success -> {
                                                Toast.makeText(context, "تمت إضافة الجهاز ${arp.name} للمراقبة بنجاح", Toast.LENGTH_SHORT).show()
                                            }
                                            is DeviceSaveResult.IpConflict -> {
                                                Toast.makeText(context, "الجهاز مضاف مسبقاً بنفس الـ IP", Toast.LENGTH_SHORT).show()
                                            }
                                            is DeviceSaveResult.LimitReached -> {
                                                showLimitDialog = true
                                            }
                                        }
                                    }
                                },
                                onOpenInBrowser = {
                                    val url = "http://${arp.ipAddress}"
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }.onFailure {
                                        Toast.makeText(context, "تعذر فتح المتصفح للعنوان: $url", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Action Dock (MkCards style)
        Surface(
            color = CyberDarkSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Status strip: "اكتمل الفحص."
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusOnline, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPingingAll) "جاري فحص جميع الأجهزة..." else "اكتمل الفحص.",
                            color = if (isPingingAll) MikroTikCyan else StatusOnline,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "آخر تحديث: 4:19 ص",
                        color = TextMutedDark,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom 3 Action Buttons: [فحص الجميع] [استيراد الأجهزة] [إضافة جهاز]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isPingingAll) {
                                isPingingAll = true
                                scope.launch {
                                    networkRepository.pingAllDevices()
                                    isPingingAll = false
                                    Toast.makeText(context, "تم فحص اتصال جميع الأجهزة بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_ping_all_devices")
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فحص الجميع", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showBackupDialog = true },
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("استيراد الأجهزة", color = TextPrimaryDark, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (!licenseManager.canCreateDevice(devices.size)) {
                                showLimitDialog = true
                            } else {
                                deviceToEdit = null
                                isCloningDevice = false
                                isAddingDevice = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_add_device_main")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة جهاز", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Dialogs: Add/Edit, Delete, Limit, Backup
    if (isAddingDevice || deviceToEdit != null) {
        AddEditDeviceDialog(
            device = deviceToEdit,
            isCloning = isCloningDevice,
            existingDevices = devices,
            onDismiss = {
                isAddingDevice = false
                deviceToEdit = null
                isCloningDevice = false
            },
            onSave = { updated ->
                scope.launch {
                    val result = networkRepository.addOrUpdateDevice(updated)
                    when (result) {
                        is DeviceSaveResult.Success -> {
                            isAddingDevice = false
                            deviceToEdit = null
                            isCloningDevice = false
                            Toast.makeText(context, "تم حفظ بيانات الجهاز بنجاح", Toast.LENGTH_SHORT).show()
                        }
                        is DeviceSaveResult.IpConflict -> {
                            Toast.makeText(context, "خطأ: عنوان الـ IP مستخدم مسبقاً (${result.conflictingDevice.name})", Toast.LENGTH_LONG).show()
                        }
                        is DeviceSaveResult.LimitReached -> {
                            showLimitDialog = true
                        }
                    }
                }
            }
        )
    }

    if (deviceToDelete != null) {
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            title = { Text("تأكيد حذف الجهاز") },
            text = { Text("هل أنت متأكد من حذف الجهاز (${deviceToDelete?.name}) من قائمة المراقبة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = deviceToDelete?.id
                        if (id != null) {
                            scope.launch { networkRepository.deleteDevice(id) }
                        }
                        deviceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticExpenseRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deviceToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showBackupDialog) {
        DeviceJsonBackupDialog(
            devices = devices,
            onImport = { json ->
                scope.launch {
                    networkRepository.importDevicesFromJson(json)
                }
            },
            onDismissRequest = { showBackupDialog = false }
        )
    }
}

@Composable
fun StatusCounterChip(
    label: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) color.copy(alpha = 0.2f) else CyberDarkCardElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSelected) color else CyberBorder),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isSelected) color else TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (isSelected) color else TextSecondaryDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ArpDiscoveredCard(
    device: NetworkDevice,
    onAddToList: () -> Unit,
    onOpenInBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        border = BorderStroke(1.dp, CyberBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = device.name,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Surface(
                    color = Color(0xFF0F1E36),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E3A5F))
                ) {
                    Text(
                        text = device.interfaceName,
                        color = MikroTikCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "جدول ARP · MAC: ${device.macAddress} · IP: ${device.ipAddress}",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenInBrowser,
                    border = BorderStroke(1.dp, Color(0xFF0369A1)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.45f)
                ) {
                    Text("فتح بالمتصفح", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddToList,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.55f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة للقائمة +", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
