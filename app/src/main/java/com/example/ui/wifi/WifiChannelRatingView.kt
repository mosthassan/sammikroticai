package com.example.ui.wifi

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.wifi.ChannelRecommendation
import com.example.data.wifi.ChannelScore
import com.example.data.wifi.CurrentWifiConnection
import com.example.data.wifi.WifiBand
import com.example.ui.theme.BrandCyanPrimary
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen

/**
 * Channel Rating View matching the industry-standard WiFi Analyzer interface.
 * Features:
 * - Current connection banner (SSID, BSSID, RSSI, Frequency, Channel, Link Speed, IP)
 * - Wi-Fi scan throttling warning banner
 * - Best recommended channels highlighted in green
 * - Table of channels: Access Point Count | Channel Number (20/40 MHz) | 10-Star Rating Bar
 */
@Composable
fun WifiChannelRatingView(
    recommendation: ChannelRecommendation?,
    currentConnection: CurrentWifiConnection,
    isScanThrottlingEnabled: Boolean,
    selectedBand: WifiBand,
    modifier: Modifier = Modifier
) {
    val scores = recommendation?.allChannelScores ?: emptyList()

    // Determine Best Channels (top star rating)
    val bestChannels = scores.filter { it.bestChannelRecommendation }.map { it.channel }
    val bestChannelsText = if (bestChannels.isNotEmpty()) {
        bestChannels.joinToString(separator = ",")
    } else {
        recommendation?.recommendedChannel?.toString() ?: "1,6,11"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wifi_channel_rating_view"),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 1. Current Connection Header Banner (matching screenshot)
            if (currentConnection.isConnected) {
                CurrentConnectionBanner(currentConnection = currentConnection)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 2. Wi-Fi Scan Throttling Notice
            if (isScanThrottlingEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wi-Fi scan throttling is enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Throttling",
                        tint = SemanticExpenseRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. Best Channels Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Best Channels:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bestChannelsText,
                            color = SemanticIncomeGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "20 MHz",
                            color = BrandCyanPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Rating Table Column Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Access\nPoint\nCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.width(52.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Channel\nNumber",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.width(76.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Channel Rating",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.1f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 5. Channel Rating Rows (1 to 13 / 14 for 2.4 GHz, 36 to 165 for 5 GHz)
            scores.forEach { score ->
                ChannelRatingRowItem(score = score)
            }
        }
    }
}

@Composable
private fun CurrentConnectionBanner(currentConnection: CurrentWifiConnection) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E2836).copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current connection",
                    fontSize = 11.sp,
                    color = BrandCyanPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(${currentConnection.bssid})",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Line 1: SSID + lock
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentConnection.ssid,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(12.dp)
                )
            }

            // Line 2: Approx Distance ~Xm, Frequency MHz, Channel, RSSI dBm
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val distFormatted = String.format(java.util.Locale.US, "%.1fm", currentConnection.distanceMetersApprox.coerceIn(0.5, 99.0))
                    Text(
                        text = "~$distFormatted",
                        fontSize = 11.sp,
                        color = Color(0xFF80D8FF),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${currentConnection.frequencyMhz}MHz",
                        fontSize = 11.sp,
                        color = BrandCyanPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${currentConnection.channel} CH",
                        fontSize = 11.sp,
                        color = BrandCyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${currentConnection.rssiDbm}dBm",
                    fontSize = 11.sp,
                    color = if (currentConnection.rssiDbm >= -70) SemanticIncomeGreen else Color(0xFFFFB300),
                    fontWeight = FontWeight.Bold
                )
            }

            // Line 3: IP Address & Link Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentConnection.ipAddress.ifBlank { "192.168.88.1" },
                    fontSize = 11.sp,
                    color = BrandCyanPrimary,
                    fontFamily = FontFamily.Monospace
                )
                if (currentConnection.linkSpeedMbps > 0) {
                    Text(
                        text = "${currentConnection.linkSpeedMbps}Mbps",
                        fontSize = 11.sp,
                        color = BrandCyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Single Row for Channel Rating Table:
 * [AP Count (Cyan)] [20 MHz (White)] [Channel (Cyan)] [10-Star Rating Bar (Green if best, Yellow if moderate/poor)]
 */
@Composable
private fun ChannelRatingRowItem(score: ChannelScore) {
    val isBest = score.bestChannelRecommendation
    val starColor = if (isBest) Color(0xFF00E676) else Color(0xFFFFD700)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. AP Count
        Text(
            text = score.totalApCount.toString(),
            color = BrandCyanPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.width(52.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 2. Channel Number with bandwidth (e.g. "20 MHz 1")
        Row(
            modifier = Modifier.width(76.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${score.channelWidthMhz} MHz",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = score.channel.toString(),
                color = BrandCyanPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 3. 10-Star Rating Bar
        TenStarRatingBar(
            stars = score.starRating,
            starColor = starColor,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 10-Star rating bar.
 * Active stars are filled with starColor (Green for best channels, Gold/Yellow for other channels).
 * Inactive stars (10 - stars) are greyed out.
 */
@Composable
private fun TenStarRatingBar(
    stars: Int,
    starColor: Color,
    modifier: Modifier = Modifier
) {
    val clampedStars = stars.coerceIn(0, 10)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        for (i in 1..10) {
            val filled = i <= clampedStars
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (filled) starColor else Color(0xFF37474F),
                modifier = Modifier
                    .size(15.dp)
                    .padding(horizontal = 0.5.dp)
            )
        }
    }
}
