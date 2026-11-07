package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// 2026 Cyber-Fintech & Telecom Next-Gen Palette (Executive High-Contrast)
// ==========================================
val CyberDarkCanvas = Color(0xFF090D16)        // Rich Deep Obsidian Navy Canvas
val CyberDarkSurface = Color(0xFF111827)       // Deep sleek card surface (9:1 contrast against canvas)
val CyberDarkCardElevated = Color(0xFF162032)  // Elevated interactive card surface
val CyberCardHighlight = Color(0xFF1E2D47)     // Focused / Selected card surface
val CyberBorder = Color(0xFF263756)            // Crisp tech border (1dp) - clearly visible
val CyberBorderGlow = Color(0xFF38BDF8)        // Sky Cyan Accent Border
val CyberBorderSubtle = Color(0xFF1E293B)      // Secondary subtle divider

// Brand & MikroTik Core Accents (Refined & Balanced)
val MikroTikCyan = Color(0xFF00D4E6)          // Laser Cyan (used selectively for highlights)
val MikroTikCyanGlow = Color(0xFF38BDF8)      // Sky Blue
val MikroTikNavy = Color(0xFF0B1426)          // Deep Command Navy
val MikroTikNavyLight = Color(0xFF142442)
val MikroTikPrimary = Color(0xFF0284C7)        // Ocean Sky Blue (Tech Primary)
val MikroTikPrimaryDark = Color(0xFF0369A1)
val MikroTikTeal = Color(0xFF0D9488)          // Deep Teal
val MikroTikLightBg = Color(0xFFF8FAFC)
val MikroTikLightSurface = Color(0xFFFFFFFF)
val MikroTikDarkBg = CyberDarkCanvas
val MikroTikDarkSurface = CyberDarkSurface

// Status & Real-time Telemetry (High-Vibrancy)
val StatusOnline = Color(0xFF10B981)           // Radiant Emerald
val StatusOnlineBg = Color(0xFF064E3B)
val StatusWarning = Color(0xFFF59E0B)          // Solar Amber
val StatusWarningBg = Color(0xFF78350F)
val StatusOffline = Color(0xFFF43F5E)          // Vivid Rose Red
val StatusOfflineBg = Color(0xFF881337)
val StatusDistributed = Color(0xFF3B82F6)      // Electric Indigo

// Financial & Ledger Tokens
val ReceiptGreen = Color(0xFF10B981)           // Emerald Inflow
val PaymentRed = Color(0xFFF43F5E)             // Rose Outflow / Alert
val InvestmentGold = Color(0xFFFBBF24)         // Radiant Gold
val AssetPurple = Color(0xFF8B5CF6)            // Quantum Purple
val EquityBlue = Color(0xFF3B82F6)

// Social & Integrations
val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppDarkGreen = Color(0xFF075E54)

// AI & Intelligence Accents
val AiGeminiPurple = Color(0xFFA855F7)
val AiGeminiBlue = Color(0xFF38BDF8)
val AiGeminiGradientStart = Color(0xFF8B5CF6)
val AiGeminiGradientEnd = Color(0xFF06B6D4)

// High-contrast Typography Tokens (WCAG AAA Compliant)
val TextPrimaryDark = Color(0xFFF9FAFB)        // 98% Pure Crisp White
val TextSecondaryDark = Color(0xFF9CA3AF)      // Neutral Cool Slate (High Readability)
val TextMutedDark = Color(0xFF6B7280)          // Secondary Muted
val TextTertiaryDark = Color(0xFF4B5563)       // Deep Caption

// Backward Compatibility Tokens
val BrandCyanPrimary = MikroTikCyan
val BrandCyanOnPrimary = Color(0xFF070B14)
val BrandCyanContainer = Color(0xFF0C243C)
val BrandCyanOnContainer = Color(0xFFBAE6FD)

val BrandBlueSecondary = MikroTikPrimary
val BrandBlueOnSecondary = Color(0xFFFFFFFF)
val BrandBlueContainer = Color(0xFF1E3A8A)
val BrandBlueOnContainer = Color(0xFFDBEAFE)

val SemanticIncomeGreen = StatusOnline
val SemanticIncomeGreenDark = StatusOnlineBg
val SemanticIncomeGreenLight = Color(0xFFD1FAE5)

val SemanticExpenseRed = PaymentRed
val SemanticExpenseRedDark = StatusOfflineBg
val SemanticExpenseRedLight = Color(0xFFFEE2E2)

val SemanticWarningAmber = StatusWarning
val SemanticWarningAmberDark = StatusWarningBg
val SemanticWarningAmberLight = Color(0xFFFEF3C7)

val DarkBackground = CyberDarkCanvas
val DarkSurface = CyberDarkSurface
val DarkSurfaceVariant = CyberDarkCardElevated
val DarkSurfaceBorder = CyberBorder
val DarkOnBackground = TextPrimaryDark
val DarkOnSurface = TextPrimaryDark
val DarkOnSurfaceVariant = TextSecondaryDark

val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightSurfaceBorder = Color(0xFFCBD5E1)
val LightOnBackground = Color(0xFF0F172A)
val LightOnSurface = Color(0xFF1E293B)
val LightOnSurfaceVariant = Color(0xFF64748B)

