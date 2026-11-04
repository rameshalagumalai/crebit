package com.example.pam

import com.example.pam.data.model.Category
import com.example.pam.data.model.CategoryType
import com.example.pam.data.model.SplitGroup
import com.example.pam.data.model.SplitStatus
import com.example.pam.data.model.TransactionType
import com.example.pam.ui.screens.transaction_entry.AddEditTransactionUiState
import com.example.pam.ui.screens.transaction_entry.SplitMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitExpenseTest {

    @Test
    fun testSplitCategoryIsBothExpenseAndIncome() {
        val splitCategory = Category(
            name = "Split",
            type = CategoryType.BOTH,
            iconName = "CallSplit"
        )

        // Split category MUST be applicable for BOTH Expense and Income
        assertTrue("Split category must be applicable for EXPENSE", splitCategory.isApplicableFor(TransactionType.EXPENSE))
        assertTrue("Split category must be applicable for INCOME", splitCategory.isApplicableFor(TransactionType.INCOME))
        assertFalse("Split category must not be applicable for TRANSFER", splitCategory.isApplicableFor(TransactionType.TRANSFER))
        assertEquals(CategoryType.BOTH, splitCategory.type)
    }

    @Test
    fun testSplitEqualCalculation() {
        val state = AddEditTransactionUiState(
            amountText = "1000",
            isSplitEnabled = true,
            splitMode = SplitMode.EQUAL,
            splitPeopleCount = 4
        )

        assertEquals(250.0, state.computedMyShare, 0.001)
        assertEquals(750.0, state.computedReceivable, 0.001)
    }

    @Test
    fun testSplitCustomCalculation() {
        val state = AddEditTransactionUiState(
            amountText = "1200",
            isSplitEnabled = true,
            splitMode = SplitMode.CUSTOM,
            customMyShareText = "400"
        )

        assertEquals(400.0, state.computedMyShare, 0.001)
        assertEquals(800.0, state.computedReceivable, 0.001)
    }

    @Test
    fun testSplitCustomZeroShare() {
        // User paid entire amount for others (myShare = 0)
        val state = AddEditTransactionUiState(
            amountText = "500",
            isSplitEnabled = true,
            splitMode = SplitMode.CUSTOM,
            customMyShareText = "0"
        )

        assertEquals(0.0, state.computedMyShare, 0.001)
        assertEquals(500.0, state.computedReceivable, 0.001)
    }

    @Test
    fun testSplitGroupStatusTracking() {
        val split = SplitGroup(
            id = 1L,
            description = "Team Dinner",
            totalAmount = 1000.0,
            myShare = 250.0,
            receivableAmount = 750.0,
            expenseTransactionId = 2L,
            status = SplitStatus.OPEN
        )

        assertEquals(750.0, split.receivableAmount, 0.001)
        assertEquals(250.0, split.myShare, 0.001)
        assertEquals(1000.0, split.totalAmount, 0.001)
        assertEquals(SplitStatus.OPEN, split.status)
    }

    @Test
    fun testMyShareNotTaggedWithSplitGroup() {
        // My share transaction should have splitGroupId = null (no split tag on my share's personal expense)
        val myShareTransaction = com.example.pam.data.model.Transaction(
            id = 101L,
            title = "Team Dinner",
            amount = 250.0,
            type = TransactionType.EXPENSE,
            accountId = 1L,
            categoryId = 10L, // Food & Dining
            splitGroupId = null
        )

        // The split expense transaction representing others' share has splitGroupId and represents original split expense
        val othersSplitExpense = com.example.pam.data.model.Transaction(
            id = 102L,
            title = "Split: Team Dinner",
            amount = 750.0,
            type = TransactionType.EXPENSE,
            accountId = 1L,
            categoryId = 99L, // Split category
            splitGroupId = 1L
        )

        org.junit.Assert.assertNull("My share transaction must not have splitGroupId (no split tag)", myShareTransaction.splitGroupId)
        org.junit.Assert.assertNotNull("Others split expense must have splitGroupId", othersSplitExpense.splitGroupId)
        assertEquals(1L, othersSplitExpense.splitGroupId)
    }
}

