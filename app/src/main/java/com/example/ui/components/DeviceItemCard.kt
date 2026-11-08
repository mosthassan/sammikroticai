package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.DeviceStatus
import com.example.data.network.DeviceType
import com.example.data.network.NetworkDevice
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun DeviceItemCard(
    device: NetworkDevice,
    onEdit: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit,
    onPing: () -> Unit = {},
    onOpenInBrowser: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val statusColor = when (device.status) {
        DeviceStatus.ONLINE -> StatusOnline
        DeviceStatus.WARNING -> StatusWarning
        DeviceStatus.OFFLINE -> StatusOffline
        DeviceStatus.UNCHECKED -> Color(0xFF94A3B8)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        border = BorderStroke(1.dp, CyberBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Status badge, Device Name, Device Type Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge (MkCards pill style)
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = device.status.labelArabic,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Name and Type Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (device.deviceType) {
                                DeviceType.ACCESS_POINT -> Icons.Default.WifiTethering
                                DeviceType.ROUTER -> Icons.Default.Router
                                DeviceType.STATION -> Icons.Default.Wifi
                                DeviceType.SWITCH -> Icons.Default.Lan
                                DeviceType.ANTENNA -> Icons.Default.Public
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // IP & MAC Row (clean container)
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "IP: ",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = device.ipAddress,
                            color = MikroTikCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    if (device.macAddress.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MAC: ",
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = device.macAddress,
                                color = TextPrimaryDark,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Telemetry line: Source · Latency ms · Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${device.source} · ",
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                    if (device.pingLatencyMs != null) {
                        Text(
                            text = "استجابة ${device.pingLatencyMs} ms · ",
                            color = if (device.pingLatencyMs < 20.0) StatusOnline else StatusWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = device.lastCheckedTime,
                        color = TextMutedDark,
                        fontSize = 10.sp
                    )
                }

                if (device.towerLocation.isNotBlank() && device.towerLocation != "البرج الرئيسي") {
                    Text(
                        text = device.towerLocation,
                        color = TextMutedDark,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row: [Trash] [Edit] [Clone] ... [فتح بالمتصفح] [فحص الآن]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary management actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_delete_device_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الجهاز",
                            tint = StatusOffline,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_edit_device_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل الجهاز",
                            tint = MikroTikCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onClone,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_clone_device_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "تكرار الجهاز",
                            tint = StatusOnline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Primary operational actions (MkCards style)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Open in Browser (Direct WebFig / AirOS / LuCI)
                    OutlinedButton(
                        onClick = onOpenInBrowser,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        border = BorderStroke(1.dp, Color(0xFF0369A1)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_open_browser_${device.id}")
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فتح بالمتصفح", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Instant Ping Check
                    Button(
                        onClick = onPing,
                        colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_ping_device_${device.id}")
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فحص الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
