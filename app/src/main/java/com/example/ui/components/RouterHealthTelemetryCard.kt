package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.RouterTelemetry
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

/**
 * Executive Live Router Health & Telemetry Card matching MkCards Screenshot 4.
 * Displays IP, Cloud state, connected devices, uptime, RouterOS version, CPU%,
 * Temperature with smart fan warning pill, and RAM progress bar.
 */
@Composable
fun RouterHealthTelemetryCard(
    telemetry: RouterTelemetry,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("router_health_telemetry_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF062C24) // Deep emerald-slate telecom gradient
        ),
        border = BorderStroke(1.dp, Color(0xFF0F5A47))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A3B30),
                            Color(0xFF05221B)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ROW 1: Router IP & Icon | Connected Devices Badge | Cloud Pill | Refresh
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Router IP and Icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0E5343)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = telemetry.routerIp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Connected devices badge
                    Surface(
                        color = Color(0xFF0D4638),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF156955))
                    ) {
                        Text(
                            text = "${telemetry.totalDiscoveredDevices} الأجهزة المتصلة",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Cloud Pill & Refresh Button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF0C382D),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF145343))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "كلاود",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث القياسات",
                                tint = Color(0xFF6EE7B7),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // ROW 2: Uptime & RouterOS Version
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Uptime
                    Surface(
                        color = Color(0xFF0B2D24),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF165243)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مدة التشغيل ${telemetry.uptime}",
                                color = Color(0xFFF1F5F9),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // RouterOS Version
                    Surface(
                        color = Color(0xFF0B2D24),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF165243)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إصدار الراوتر ${telemetry.routerOsVersion}",
                                color = Color(0xFFF1F5F9),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ROW 3: CPU Load % & Temperature with Cooling Fan Warning
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // CPU Load
                    Surface(
                        color = Color(0xFF0B2D24),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF165243)),
                        modifier = Modifier.weight(0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "المعالج ${telemetry.cpuLoadPercent}%",
                                color = Color(0xFFF1F5F9),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Temperature & Warning pill
                    Surface(
                        color = Color(0xFF0B2D24),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF165243)),
                        modifier = Modifier.weight(0.55f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DeviceThermostat, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "الحرارة ${telemetry.temperatureCelsius}°",
                                    color = Color(0xFFF1F5F9),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (telemetry.needsCoolingFanAlert) {
                                Surface(
                                    color = Color(0xFF114234),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, Color(0xFF1E6C56))
                                ) {
                                    Text(
                                        text = "تفعيل مروحة التبريد",
                                        color = Color(0xFF6EE7B7),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ROW 4: Memory / RAM Usage Bar & Active Users Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // RAM progress bar
                    Surface(
                        color = Color(0xFF0B2D24),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF165243)),
                        modifier = Modifier.weight(0.55f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الذاكرة", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(
                                    "${telemetry.memoryUsedMb} MB / 1 GB",
                                    fontSize = 10.sp,
                                    color = Color(0xFFF1F5F9),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (telemetry.memoryUsedMb / telemetry.memoryTotalMb).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF06221A),
                            )
                        }
                    }

                    // Active Users Count
                    Surface(
                        color = Color(0xFF0B2D24),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF165243)),
                        modifier = Modifier.weight(0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "المستخدمون النشطون ${telemetry.activeUsersCount}",
                                color = Color(0xFFF1F5F9),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
