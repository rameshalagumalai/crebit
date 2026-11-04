package com.example.pam

import com.example.pam.util.CurrencyFormatter
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun testCurrencyFormatting() {
        val formatted = CurrencyFormatter.format(1500.0)
        assertTrue(formatted.contains("1,500") || formatted.contains("1500"))
    }

    @Test
    fun testCompactFormatting() {
        val kFormatted = CurrencyFormatter.formatCompact(4500.0)
        assertTrue(kFormatted.contains("4.5") || kFormatted.contains("4.5 k"))

        val lFormatted = CurrencyFormatter.formatCompact(250000.0)
        assertTrue(lFormatted.contains("2.50 L") || lFormatted.contains("2.5"))
    }

    @Test
    fun testSignedFormatting() {
        val expense = CurrencyFormatter.format(200.0, showSign = true, isExpense = true)
        assertTrue(expense.startsWith("-"))

        val income = CurrencyFormatter.format(500.0, showSign = true, isExpense = false)
        assertTrue(income.startsWith("+"))
    }
}
