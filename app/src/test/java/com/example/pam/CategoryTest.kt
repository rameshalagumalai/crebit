package com.example.pam

import com.example.pam.data.model.Category
import com.example.pam.data.model.CategoryType
import com.example.pam.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTest {

    @Test
    fun testGeneralCategoryDefaults() {
        val generalCategory = Category(
            name = "General",
            type = CategoryType.BOTH,
            colorHex = "#64748B",
            iconName = "Category",
            isDefault = true
        )
        assertTrue(generalCategory.isApplicableFor(TransactionType.EXPENSE))
        assertTrue(generalCategory.isApplicableFor(TransactionType.INCOME))
        assertEquals("#64748B", generalCategory.colorHex)
        assertEquals("Category", generalCategory.iconName)
        assertTrue(generalCategory.isDefault)
    }

    @Test
    fun testBothCategoryIsApplicableForExpenseAndIncome() {
        val bothCategory = Category(
            name = "Investments",
            type = CategoryType.BOTH
        )

        assertTrue(bothCategory.isApplicableFor(TransactionType.EXPENSE))
        assertTrue(bothCategory.isApplicableFor(TransactionType.INCOME))
        assertFalse(bothCategory.isApplicableFor(TransactionType.TRANSFER))
    }

    @Test
    fun testExpenseCategoryApplicability() {
        val expenseCategory = Category(
            name = "Groceries",
            type = CategoryType.EXPENSE
        )

        assertTrue(expenseCategory.isApplicableFor(TransactionType.EXPENSE))
        assertFalse(expenseCategory.isApplicableFor(TransactionType.INCOME))
    }

    @Test
    fun testIncomeCategoryApplicability() {
        val incomeCategory = Category(
            name = "Salary",
            type = CategoryType.INCOME
        )

        assertTrue(incomeCategory.isApplicableFor(TransactionType.INCOME))
        assertFalse(incomeCategory.isApplicableFor(TransactionType.EXPENSE))
    }
}
