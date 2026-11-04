package com.example.pam

import com.example.pam.data.model.AccountType
import com.example.pam.ui.screens.walkthrough.WalkthroughUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkthroughUiTest {

    @Test
    fun testWalkthroughUiStateDefaults() {
        val defaultState = WalkthroughUiState()
        assertEquals("Main Bank", defaultState.accountName)
        assertEquals(AccountType.BANK, defaultState.accountType)
        assertEquals("", defaultState.startingBalance)
        assertFalse(defaultState.includeStarterCategories)
        assertFalse(defaultState.isCompleting)
    }

    @Test
    fun testWalkthroughUiStateCustomValues() {
        val customState = WalkthroughUiState(
            accountName = "Cash Pocket",
            accountType = AccountType.CASH,
            startingBalance = "2500",
            includeStarterCategories = true,
            isCompleting = true
        )
        assertEquals("Cash Pocket", customState.accountName)
        assertEquals(AccountType.CASH, customState.accountType)
        assertEquals("2500", customState.startingBalance)
        assertTrue(customState.includeStarterCategories)
        assertTrue(customState.isCompleting)
    }
}

