package com.example.pam.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

fun parseHexColor(hex: String?, fallback: Color = Color(0xFF6B7280)): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        Color(clean.toColorInt())
    } catch (_: Exception) {
        fallback
    }
}

fun String?.toComposeColor(fallback: Color = Color(0xFF6B7280)): Color = parseHexColor(this, fallback)

// Brand Primary Colors (Modern Charcoal / Platinum Gray)
val PrimaryLight = Color(0xFF27272A)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE4E4E7)
val OnPrimaryContainerLight = Color(0xFF18181B)

val PrimaryDark = Color(0xFFE4E4E7)
val OnPrimaryDark = Color(0xFF18181B)
val PrimaryContainerDark = Color(0xFF2E2E34)
val OnPrimaryContainerDark = Color(0xFFF4F4F5)

// Secondary Colors (Neutral Zinc)
val SecondaryLight = Color(0xFF52525B)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE4E4E7)
val OnSecondaryContainerLight = Color(0xFF18181B)

val SecondaryDark = Color(0xFFA1A1AA)
val OnSecondaryDark = Color(0xFF18181B)
val SecondaryContainerDark = Color(0xFF2E2E34)
val OnSecondaryContainerDark = Color(0xFFF4F4F5)

// Financial Semantic Colors
val IncomeGreen = Color(0xFF10B981)
val IncomeGreenContainer = Color(0xFFD1FAE5)
val OnIncomeGreen = Color(0xFF065F46)

val ExpenseRed = Color(0xFFEF4444)
val ExpenseRedContainer = Color(0xFFFEE2E2)
val OnExpenseRed = Color(0xFF991B1B)

val WarningAmber = Color(0xFFF59E0B)
val WarningAmberContainer = Color(0xFFFEF3C7)
val OnWarningAmber = Color(0xFF92400E)

val WarningAmberDark = Color(0xFFFBBF24)
val WarningAmberContainerDark = Color(0xFF332005)
val OnWarningAmberDark = Color(0xFFFEF3C7)

val TransferBlue = Color(0xFF64748B)
val TransferBlueContainer = Color(0xFFF1F5F9)
val OnTransferBlue = Color(0xFF334155)

// Background & Surface (Pure Neutral & Dark Grayish)
val BackgroundLight = Color(0xFFF9FAFB)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF3F4F6)
val OutlineLight = Color(0xFFE5E7EB)

val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1C1C1E)
val SurfaceVariantDark = Color(0xFF2C2C2E)
val OutlineDark = Color(0xFF3A3A3C)

val AvailableColorHexes = listOf(
    "#27272A", // Charcoal
    "#4F46E5", // Indigo
    "#2563EB", // Blue
    "#0D9488", // Teal
    "#10B981", // Emerald
    "#84CC16", // Lime
    "#F59E0B", // Amber
    "#EA580C", // Orange
    "#EF4444", // Red
    "#EC4899", // Pink
    "#8B5CF6", // Purple
    "#06B6D4", // Cyan
    "#64748B"  // Slate
)