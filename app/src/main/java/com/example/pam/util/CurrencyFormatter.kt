package com.example.pam.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    private val indianLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()
    private val currencyFormat = NumberFormat.getCurrencyInstance(indianLocale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    @Synchronized
    fun format(amount: Double, showSign: Boolean = false, isExpense: Boolean = false): String {
        val formatted = try {
            currencyFormat.format(amount)
        } catch (e: Exception) {
            "₹" + String.format(Locale.US, "%,.2f", amount)
        }

        return if (showSign) {
            if (isExpense) "-$formatted" else "+$formatted"
        } else {
            formatted
        }
    }

    @Synchronized
    fun formatCompact(amount: Double): String {
        return when {
            amount >= 10000000 -> String.format(Locale.US, "₹%.2f Cr", amount / 10000000)
            amount >= 100000 -> String.format(Locale.US, "₹%.2f L", amount / 100000)
            amount >= 1000 -> String.format(Locale.US, "₹%.1f k", amount / 1000)
            else -> format(amount)
        }
    }
}
