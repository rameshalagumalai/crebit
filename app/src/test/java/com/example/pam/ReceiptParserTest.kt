package com.example.pam

import com.example.pam.data.model.Category
import com.example.pam.data.model.CategoryType
import com.example.pam.util.ai.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReceiptParserTest {

    private val sampleCategories = listOf(
        Category(id = 1, name = "Food & Dining", type = CategoryType.EXPENSE, isDefault = true),
        Category(id = 2, name = "Groceries", type = CategoryType.EXPENSE),
        Category(id = 3, name = "Transportation", type = CategoryType.EXPENSE),
        Category(id = 4, name = "Shopping", type = CategoryType.EXPENSE),
        Category(id = 5, name = "Healthcare", type = CategoryType.EXPENSE),
        Category(id = 6, name = "Entertainment", type = CategoryType.EXPENSE),
        Category(id = 7, name = "Bills & Utilities", type = CategoryType.EXPENSE),
        Category(id = 8, name = "General", type = CategoryType.EXPENSE)
    )

    @Test
    fun testParseStarbucksReceipt() {
        val receiptText = """
            STARBUCKS COFFEE
            Indiranagar 100ft Road
            GSTIN: 29AAAAA0000A1Z5
            Date: 07/11/2026 Time: 15:45
            Bill No: #48291
            Caffe Latte 1 260.00
            Blueberry Muffin 1 160.00
            Subtotal: 420.00
            CGST 2.5%: 10.50
            SGST 2.5%: 10.50
            Total Amount: ₹ 441.00
            Thank you for visiting!
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleCategories)

        assertEquals("Starbucks", parsed.merchantName)
        assertEquals(441.00, parsed.amount ?: 0.0, 0.01)
        assertEquals("Food & Dining", parsed.suggestedCategory?.name)
        assertEquals(2, parsed.items.size)
        assertEquals("Caffe Latte", parsed.items[0].name)
        assertEquals(260.0, parsed.items[0].price ?: 0.0, 0.01)
        assertNotNull(parsed.dateEpochMillis)
        assertTrue(parsed.confidence > 0.8f)

        val note = parsed.buildSummaryNote()
        assertTrue(note.contains("Caffe Latte"))
        assertTrue(note.contains("Taxes / GST: ₹21"))
    }

    @Test
    fun testParseSupermarketReceipt() {
        val receiptText = """
            TAX INVOICE
            D-MART
            Avenue Supermarts Ltd
            Date: 25-10-2026
            Amul Butter 1 275.00
            Britannia Bread 2 80.00
            Farm Fresh Eggs 1 55.00
            Net Payable: Rs. 410.00
            Cash Tendered: 500.00
            Change Due: 90.00
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleCategories)

        assertEquals("DMart", parsed.merchantName)
        assertEquals(410.00, parsed.amount ?: 0.0, 0.01)
        assertEquals("Groceries", parsed.suggestedCategory?.name)
        assertEquals(3, parsed.items.size)
    }

    @Test
    fun testParsePharmacyReceipt() {
        val receiptText = """
            APOLLO PHARMACY
            Hospital Road Branch
            Ph: 080-23456789
            Bill Date: 12-Nov-2026
            Dolo 650mg Tab 2 60.00
            Cetirizine 10mg 1 35.00
            Grand Total: Rs. 95.00
            Cash Tendered: 100.00
            Change Returned: 5.00
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleCategories)

        assertEquals("Apollo Pharmacy", parsed.merchantName)
        assertEquals(95.00, parsed.amount ?: 0.0, 0.01)
        assertEquals("Healthcare", parsed.suggestedCategory?.name)
        assertNotNull(parsed.dateEpochMillis)
    }

    @Test
    fun testParseFuelReceipt() {
        val receiptText = """
            SHELL PETROL PUMP
            Retail Outlet #8839
            05/11/2026 18:22
            Fuel: Unleaded Petrol
            Volume: 9.72 L
            Rate: 102.84
            Total Sale: Rs. 1000.00
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleCategories)

        assertEquals("Shell Petrol", parsed.merchantName)
        assertEquals(1000.00, parsed.amount ?: 0.0, 0.01)
        assertEquals("Transportation", parsed.suggestedCategory?.name)
    }

    @Test
    fun testParseDecathlonShoppingReceipt() {
        val receiptText = """
            DECATHLON SPORTS INDIA
            Whitefield Store
            Date: 2026-10-15
            Running Shoes 1 2499.00
            Sports Socks 2 299.00
            Total Amount: 2798.00
            Card Payment: 2798.00
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleCategories)

        assertEquals("Decathlon", parsed.merchantName)
        assertEquals(2798.00, parsed.amount ?: 0.0, 0.01)
        assertEquals("Shopping", parsed.suggestedCategory?.name)
        assertEquals(2, parsed.items.size)
    }

    @Test
    fun testParseReceiptWithNextLineTotal() {
        // Many OCR receipts have "GRAND TOTAL" on one line and the number below it
        val receiptText = """
            CAFE MOCHA
            MG Road
            Date: 01/11/2026
            Cappuccino 2 360.00
            GRAND TOTAL
            ₹ 360.00
            Thank You!
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleCategories)

        assertEquals("Cafe Mocha", parsed.merchantName)
        assertEquals(360.00, parsed.amount ?: 0.0, 0.01)
        assertEquals("Food & Dining", parsed.suggestedCategory?.name)
    }

    @Test
    fun testParseFallbackMaxAmountWhenNoKeyword() {
        val receiptText = """
            LOCAL CORNER STORE
            Items:
            Notebook 60.00
            Pen 40.00
            Final: 100.00
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleCategories)

        assertEquals("Local Corner Store", parsed.merchantName)
        assertEquals(100.00, parsed.amount ?: 0.0, 0.01)
    }
}

