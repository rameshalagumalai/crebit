package com.example.pam.util.ai

import com.example.pam.data.model.Category
import com.example.pam.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class ReceiptItem(
    val name: String,
    val quantity: Int? = null,
    val price: Double? = null
)

data class ParsedReceipt(
    val merchantName: String?,
    val amount: Double?,
    val dateEpochMillis: Long?,
    val items: List<ReceiptItem> = emptyList(),
    val taxAmount: Double? = null,
    val suggestedCategory: Category? = null,
    val rawText: String,
    val confidence: Float = 0f
) {
    /**
     * Formats line items and detected tax into a clean note description.
     */
    fun buildSummaryNote(): String {
        val sb = StringBuilder()
        if (items.isNotEmpty()) {
            sb.appendLine("Items:")
            for (item in items) {
                val qtyStr = item.quantity?.let { " x$it" } ?: ""
                val priceStr = item.price?.let { " - ₹${if (it % 1.0 == 0.0) it.toLong() else String.format(Locale.US, "%.2f", it)}" } ?: ""
                sb.appendLine(" • ${item.name}$qtyStr$priceStr")
            }
        }
        if (taxAmount != null && taxAmount > 0.0) {
            val taxStr = if (taxAmount % 1.0 == 0.0) taxAmount.toLong().toString() else String.format(Locale.US, "%.2f", taxAmount)
            sb.appendLine("Taxes / GST: ₹$taxStr")
        }
        return sb.toString().trim()
    }
}

object ReceiptParser {

    // Common noise in receipt headers that should NOT be considered merchant names
    private val headerNoiseWords = setOf(
        "tax invoice", "retail invoice", "cash memo", "invoice", "receipt",
        "bill of supply", "estimate", "original for buyer", "duplicate",
        "customer copy", "merchant copy", "welcome", "visit again", "thank you",
        "order", "bill", "table", "token", "counter", "store copy"
    )

    private val knownMerchantBrands = mapOf(
        "starbucks" to "Starbucks",
        "mcdonald" to "McDonald's",
        "kfc" to "KFC",
        "subway" to "Subway",
        "domino" to "Domino's Pizza",
        "pizza hut" to "Pizza Hut",
        "burger king" to "Burger King",
        "chai point" to "Chai Point",
        "chaayos" to "Chaayos",
        "cafe coffee day" to "Cafe Coffee Day",
        "ccd" to "Cafe Coffee Day",
        "third wave" to "Third Wave Coffee",
        "blue tokai" to "Blue Tokai Coffee",
        "swiggy" to "Swiggy",
        "zomato" to "Zomato",
        "dmart" to "DMart",
        "d-mart" to "DMart",
        "reliance" to "Reliance Retail",
        "bigbasket" to "BigBasket",
        "zepto" to "Zepto",
        "blinkit" to "Blinkit",
        "instamart" to "Swiggy Instamart",
        "nature's basket" to "Nature's Basket",
        "spencer" to "Spencer's",
        "more retail" to "More Supermarket",
        "apollo" to "Apollo Pharmacy",
        "medplus" to "MedPlus",
        "pharmeasy" to "PharmEasy",
        "1mg" to "Tata 1mg",
        "shell" to "Shell Petrol",
        "bharat petroleum" to "Bharat Petroleum",
        "hindustan petroleum" to "HP Petrol",
        "hp petrol" to "HP Petrol",
        "indian oil" to "Indian Oil",
        "decathlon" to "Decathlon",
        "zara" to "Zara",
        "h&m" to "H&M",
        "uniqlo" to "Uniqlo",
        "shoppers stop" to "Shoppers Stop",
        "lifestyle" to "Lifestyle",
        "westside" to "Westside",
        "pvr" to "PVR Cinemas",
        "inox" to "INOX Cinemas",
        "cinepolis" to "Cinepolis",
        "ikea" to "IKEA"
    )

    // Regex for capturing numbers that represent monetary amounts
    private val moneyPattern = Pattern.compile(
        "(?:(?:₹|rs\\.?|inr|amt|val)\\s*)?([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})|[0-9]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // Primary keywords indicating grand total
    private val grandTotalKeywords = listOf(
        "grand total", "net payable", "net amount", "total payable", "amount payable",
        "bill total", "total amount", "final total", "balance due", "amount due",
        "total sale", "total inr", "total rs", "total:", "total"
    )

    // Subtotal keywords
    private val subtotalKeywords = listOf(
        "subtotal", "sub total", "items total", "gross amount"
    )

    // Keywords that indicate cash payment / change return (should NOT be picked as the total bill)
    private val tenderNoiseKeywords = listOf(
        "cash tendered", "tendered", "cash given", "cash paid", "change returned", "change due", "balance returned"
    )

    fun parse(
        visionText: com.google.mlkit.vision.text.Text,
        availableCategories: List<Category>
    ): ParsedReceipt {
        // Collect all lines across blocks, sort primarily by vertical top coordinate (with slight tolerance), then left coordinate
        val allLines = visionText.textBlocks.flatMap { it.lines }
            .sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
            .map { it.text }

        val rawString = if (allLines.isNotEmpty()) allLines.joinToString("\n") else visionText.text
        return parse(rawString, availableCategories)
    }

    fun parse(
        rawText: String,
        availableCategories: List<Category>
    ): ParsedReceipt {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return ParsedReceipt(
                merchantName = null,
                amount = null,
                dateEpochMillis = null,
                rawText = rawText
            )
        }

        // 1. Extract Merchant Name
        val merchantName = extractMerchantName(lines)

        // 2. Extract Date
        val dateEpochMillis = extractDate(lines)

        // 3. Extract Grand Total Amount
        val totalAmount = extractTotalAmount(lines)

        // 4. Extract Tax
        val taxAmount = extractTaxAmount(lines)

        // 5. Extract Line Items
        val items = extractLineItems(lines)

        // 6. Suggest Category using Merchant + Items + Raw text
        val suggestedCategory = suggestCategory(merchantName, items, rawText, availableCategories)

        var confidence = 0f
        if (totalAmount != null) confidence += 0.5f
        if (merchantName != null) confidence += 0.3f
        if (dateEpochMillis != null) confidence += 0.2f

        return ParsedReceipt(
            merchantName = merchantName,
            amount = totalAmount,
            dateEpochMillis = dateEpochMillis,
            items = items,
            taxAmount = taxAmount,
            suggestedCategory = suggestedCategory,
            rawText = rawText,
            confidence = confidence
        )
    }

    private fun extractMerchantName(lines: List<String>): String? {
        // First check for known brands in the top 10 lines
        val topLines = lines.take(10)
        for (line in topLines) {
            val lower = line.lowercase()
            for ((key, brand) in knownMerchantBrands) {
                if (lower.contains(Regex("\\b${Regex.escape(key)}\\b"))) {
                    return brand
                }
            }
        }

        // Otherwise evaluate candidate lines from the top 5 lines
        for (line in lines.take(5)) {
            val lower = line.lowercase()

            // Skip noise headers
            if (headerNoiseWords.any { lower.contains(it) }) continue
            if (lower.startsWith("gstin") || lower.startsWith("cin") || lower.startsWith("fssai") ||
                lower.startsWith("pan") || lower.startsWith("tin") || lower.startsWith("tel") ||
                lower.startsWith("ph:") || lower.startsWith("phone") || lower.startsWith("mob")) {
                continue
            }
            // Skip pure numbers or dates or symbols
            if (line.matches(Regex("^[0-9\\s\\-\\/:.#]+$"))) continue
            if (line.matches(Regex("^[=\\-_*~]+$"))) continue
            // Skip address hints
            if (lower.contains("road") || lower.contains("street") || lower.contains("layout") ||
                lower.contains("floor") || lower.contains("bangalore") || lower.contains("mumbai") ||
                lower.contains("delhi") || lower.contains("pincode") || lower.contains("pin -")) {
                continue
            }

            // Must have at least 3 letters
            val letterCount = line.count { it.isLetter() }
            if (letterCount >= 3) {
                // Clean up any extraneous symbols
                val cleaned = line.replace(Regex("[^a-zA-Z0-9&'\\s\\-]"), "").trim()
                if (cleaned.isNotEmpty()) {
                    return toTitleCase(cleaned)
                }
            }
        }

        return null
    }

    private fun extractDate(lines: List<String>): Long? {
        // Date regex patterns
        val patterns = listOf(
            // DD/MM/YYYY or DD-MM-YYYY or DD.MM.YYYY
            Pattern.compile("\\b(\\d{1,2})[\\/\\-\\.](\\d{1,2})[\\/\\-\\.](\\d{4})\\b"),
            // DD/MM/YY or DD-MM-YY
            Pattern.compile("\\b(\\d{1,2})[\\/\\-\\.](\\d{1,2})[\\/\\-\\.](\\d{2})\\b"),
            // YYYY-MM-DD or YYYY/MM/DD
            Pattern.compile("\\b(\\d{4})[\\/\\-\\.](\\d{1,2})[\\/\\-\\.](\\d{1,2})\\b"),
            // DD-MMM-YYYY (e.g. 07-Nov-2026 or 07 Nov 2026)
            Pattern.compile("\\b(\\d{1,2})[\\s\\-]+([A-Za-z]{3,9})[\\s\\-]+(\\d{2,4})\\b")
        )

        val dateFormats = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy",
            "dd/MM/yy", "dd-MM-yy",
            "yyyy-MM-dd", "yyyy/MM/dd",
            "dd-MMM-yyyy", "dd MMM yyyy", "dd-MMMM-yyyy", "dd MMMM yyyy"
        )

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        for (line in lines) {
            val lower = line.lowercase()
            // Check if line contains date indicator or match directly
            for (p in patterns) {
                val m = p.matcher(line)
                if (m.find()) {
                    val dateStr = m.group(0) ?: continue
                    for (fmtStr in dateFormats) {
                        try {
                            val sdf = SimpleDateFormat(fmtStr, Locale.ENGLISH)
                            sdf.isLenient = false
                            val parsed = sdf.parse(dateStr)
                            if (parsed != null) {
                                calendar.time = parsed
                                val year = calendar.get(Calendar.YEAR)
                                // Adjust 2-digit year if needed (e.g. 26 -> 2026)
                                val finalYear = if (year < 100) 2000 + year else year
                                calendar.set(Calendar.YEAR, finalYear)
                                val time = calendar.timeInMillis

                                // Sanity check: between year 2015 and 30 days in the future
                                val minTime = 1420070400000L // Jan 1, 2015
                                val maxTime = now + (30L * 24 * 3600 * 1000L)
                                if (time in minTime..maxTime) {
                                    return time
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        return null
    }

    private fun extractTotalAmount(lines: List<String>): Double? {
        // Pass 1: Look for explicit Grand Total keywords
        for (keyword in grandTotalKeywords) {
            for (i in lines.indices) {
                val line = lines[i]
                val lower = line.lowercase()

                // Skip tender noise
                if (tenderNoiseKeywords.any { lower.contains(it) }) continue

                if (lower.contains(keyword)) {
                    // 1. Try to find amount on the SAME line after the keyword
                    val amountOnLine = findLastAmountInLine(line)
                    if (amountOnLine != null && isValidExpenseAmount(amountOnLine)) {
                        return amountOnLine
                    }

                    // 2. Try to find amount on the NEXT line
                    if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1]
                        val nextLineLower = nextLine.lowercase()
                        if (!tenderNoiseKeywords.any { nextLineLower.contains(it) }) {
                            val amountNextLine = findFirstAmountInLine(nextLine)
                            if (amountNextLine != null && isValidExpenseAmount(amountNextLine)) {
                                return amountNextLine
                            }
                        }
                    }
                }
            }
        }

        // Pass 2: Look for Subtotal keywords
        for (keyword in subtotalKeywords) {
            for (line in lines) {
                val lower = line.lowercase()
                if (tenderNoiseKeywords.any { lower.contains(it) }) continue
                if (lower.contains(keyword)) {
                    val amount = findLastAmountInLine(line)
                    if (amount != null && isValidExpenseAmount(amount)) {
                        return amount
                    }
                }
            }
        }

        // Pass 3: Fallback - pick the maximum monetary amount that is reasonable
        var maxAmount: Double? = null
        for (line in lines) {
            val lower = line.lowercase()
            if (tenderNoiseKeywords.any { lower.contains(it) }) continue
            // Skip lines with phone numbers, GSTIN, invoice IDs
            if (lower.contains("gstin") || lower.contains("ph:") || lower.contains("tel:") || lower.contains("cin")) continue

            val amounts = findAllAmountsInLine(line)
            for (amt in amounts) {
                if (isValidExpenseAmount(amt)) {
                    if (maxAmount == null || amt > maxAmount) {
                        maxAmount = amt
                    }
                }
            }
        }

        return maxAmount
    }

    private fun extractTaxAmount(lines: List<String>): Double? {
        val taxKeywords = listOf("cgst", "sgst", "igst", "vat", "service tax")
        var totalTax = 0.0
        var foundTax = false

        for (line in lines) {
            val lower = line.lowercase()
            // Skip GSTIN or registration IDs
            if (lower.contains("gstin") || lower.contains("gst no") || lower.contains("gst reg")) continue

            val isTaxLine = taxKeywords.any { lower.contains(it) } || lower.contains(Regex("\\bgst\\b"))
            if (isTaxLine) {
                // Extract amount on this line
                val amt = findLastAmountInLine(line)
                if (amt != null && amt < 5000.0) { // Tax is normally a reasonable number
                    totalTax += amt
                    foundTax = true
                }
            }
        }
        return if (foundTax && totalTax > 0.0) totalTax else null
    }

    private fun extractLineItems(lines: List<String>): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()
        // Pattern: [Item description] followed by optional [Qty] and [Price]
        // Example: "Caffe Latte 1 260.00" or "Amul Butter 1 275.00"
        val itemPatternWithQty = Pattern.compile(
            "^([A-Za-z0-9\\s&'\\-\\.]{2,35}?)\\s+(\\d{1,2})\\s+(?:[₹rs.]*\\s*)?(\\d+(?:\\.\\d{1,2})?)$",
            Pattern.CASE_INSENSITIVE
        )
        val itemPatternWithoutQty = Pattern.compile(
            "^([A-Za-z\\s&'\\-\\.]{3,35})\\s+(?:[₹rs.]*\\s*)?(\\d+(?:\\.\\d{1,2})?)$",
            Pattern.CASE_INSENSITIVE
        )

        for (line in lines) {
            val lower = line.lowercase()
            // Skip total, header, subtotal, tax, cash lines
            if (grandTotalKeywords.any { lower.contains(it) }) continue
            if (subtotalKeywords.any { lower.contains(it) }) continue
            if (headerNoiseWords.any { lower.contains(it) }) continue
            if (tenderNoiseKeywords.any { lower.contains(it) }) continue
            if (lower.contains("gst") || lower.contains("tax") || lower.contains("subtotal")) continue
            if (lower.contains("discount") || lower.contains("round off")) continue

            val matcherWithQty = itemPatternWithQty.matcher(line)
            if (matcherWithQty.find()) {
                val itemName = matcherWithQty.group(1)?.trim() ?: continue
                val qty = matcherWithQty.group(2)?.toIntOrNull()
                val price = matcherWithQty.group(3)?.toDoubleOrNull()
                if (itemName.length >= 2 && price != null && isValidExpenseAmount(price)) {
                    items.add(ReceiptItem(name = toTitleCase(itemName), quantity = qty, price = price))
                    continue
                }
            }

            val matcherWithoutQty = itemPatternWithoutQty.matcher(line)
            if (matcherWithoutQty.find()) {
                val itemName = matcherWithoutQty.group(1)?.trim() ?: continue
                val price = matcherWithoutQty.group(2)?.toDoubleOrNull()
                if (itemName.length >= 3 && price != null && isValidExpenseAmount(price)) {
                    items.add(ReceiptItem(name = toTitleCase(itemName), quantity = null, price = price))
                }
            }
        }
        return items
    }

    private fun suggestCategory(
        merchantName: String?,
        items: List<ReceiptItem>,
        rawText: String,
        availableCategories: List<Category>
    ): Category? {
        val combinedText = buildString {
            if (merchantName != null) append(merchantName).append(" ")
            for (item in items) append(item.name).append(" ")
            append(rawText)
        }

        return SemanticCategoryClassifier.classify(
            query = combinedText,
            availableCategories = availableCategories,
            transactionType = TransactionType.EXPENSE
        )
    }

    private fun findLastAmountInLine(line: String): Double? {
        val amounts = findAllAmountsInLine(line)
        return amounts.lastOrNull()
    }

    private fun findFirstAmountInLine(line: String): Double? {
        val amounts = findAllAmountsInLine(line)
        return amounts.firstOrNull()
    }

    private fun findAllAmountsInLine(line: String): List<Double> {
        val list = mutableListOf<Double>()
        val matcher = moneyPattern.matcher(line)
        while (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", "") ?: continue
            val num = numStr.toDoubleOrNull()
            if (num != null) {
                list.add(num)
            }
        }
        return list
    }

    private fun isValidExpenseAmount(amount: Double): Boolean {
        // Avoid years (e.g. 2024, 2025, 2026), phone numbers, or microscopic values
        if (amount <= 0.0 || amount > 1000000.0) return false
        if (amount == 2024.0 || amount == 2025.0 || amount == 2026.0 || amount == 2027.0) return false
        return true
    }

    private fun toTitleCase(str: String): String {
        return str.split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }
    }
}
