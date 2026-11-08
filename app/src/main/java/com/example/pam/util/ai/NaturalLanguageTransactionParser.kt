package com.example.pam.util.ai

import com.example.pam.data.model.Account
import com.example.pam.data.model.Category
import com.example.pam.data.model.TransactionType
import java.util.Locale
import java.util.regex.Pattern

data class ParsedTransactionDraft(
    val rawInput: String,
    val amount: Double?,
    val title: String,
    val type: TransactionType,
    val matchedCategory: Category?,
    val matchedAccount: Account?,
    val matchedToAccount: Account? = null,
    val isSplit: Boolean = false,
    val splitWith: String? = null,
    val splitPeopleCount: Int = 2,
    val confidence: Float = 0f,
    val note: String = rawInput,
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val hasExplicitDateTime: Boolean = false
)

object NaturalLanguageTransactionParser {

    private val amountPattern = Pattern.compile(
        "(?:^|[\\s₹]|rs\\.?|inr)\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\s*(k|l|cr|thousand|lac|lakh)?(?:\\b|(?=[\\s,]))",
        Pattern.CASE_INSENSITIVE
    )

    private val wordToNumberMap = mapOf(
        "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10
    )

    private val splitCountPattern = Pattern.compile(
        "\\b(?:split(?:ted)?|shared|divided)\\s+(?:equally\\s+)?(?:with|between|among|in|into|for)?\\s*(\\d+|two|three|four|five|six|seven|eight|nine|ten)\\s*(?:people|persons?|peoples|friends?|roommates?|members?|guys|folks|ways?|each)?\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val splitPersonPattern = Pattern.compile(
        "\\b(?:split(?:ted)?|shared)\\s+(?:equally\\s+)?with\\s+([a-zA-Z]+(?:\\s+and\\s+[a-zA-Z]+)*)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val splitGeneralPattern = Pattern.compile(
        "\\b(?:split(?:ted)?|shared|divided)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val transferPattern = Pattern.compile(
        "\\b(?:transfer(?:red)?|move(?:d)?|send\\s+to|sent\\s+to)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val incomeKeywords = setOf(
        "salary", "received", "income", "earned", "stipend", "bonus",
        "refund", "cashback", "dividend", "interest", "profit", "credited", "got"
    )

    fun parse(
        input: String,
        availableAccounts: List<Account>,
        availableCategories: List<Category>
    ): ParsedTransactionDraft {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedTransactionDraft(
                rawInput = "",
                amount = null,
                title = "",
                type = TransactionType.EXPENSE,
                matchedCategory = availableCategories.firstOrNull { it.isApplicableFor(TransactionType.EXPENSE) },
                matchedAccount = availableAccounts.firstOrNull { it.isDefault } ?: availableAccounts.firstOrNull(),
                confidence = 0f
            )
        }

        var detectedType = TransactionType.EXPENSE
        var matchedAmount: Double? = null
        var amountRawMatch = ""

        // 1. Detect Date & Time
        val parsedDateTime = DateTimeParser.parse(trimmed)

        // Mask matched date/time strings so times like "8 pm" or dates like "25th" don't get falsely picked as amounts
        var textForAmount = trimmed
        for (dateStr in parsedDateTime.matchedStrings) {
            textForAmount = textForAmount.replace(dateStr, " ")
        }

        // 2. Detect Amount
        val amtMatcher = amountPattern.matcher(textForAmount)
        if (amtMatcher.find()) {
            val numStr = amtMatcher.group(1)?.replace(",", "")
            val unitStr = amtMatcher.group(2)?.lowercase()
            val baseNum = numStr?.toDoubleOrNull()
            if (baseNum != null) {
                matchedAmount = when (unitStr) {
                    "k", "thousand" -> baseNum * 1000.0
                    "l", "lac", "lakh" -> baseNum * 100000.0
                    "cr" -> baseNum * 10000000.0
                    else -> baseNum
                }
                amountRawMatch = amtMatcher.group(0) ?: ""
            }
        }

        // 3. Detect Type (Transfer / Income / Expense)
        val lowerInput = trimmed.lowercase()
        if (transferPattern.matcher(lowerInput).find()) {
            detectedType = TransactionType.TRANSFER
        } else if (incomeKeywords.any { lowerInput.contains(Regex("\\b$it\\b")) }) {
            detectedType = TransactionType.INCOME
        }

        // 4. Detect Split
        var isSplit = false
        var splitWith: String? = null
        var splitPeopleCount = 2

        val countMatcher = splitCountPattern.matcher(trimmed)
        val genericWords = setOf(
            "people", "person", "persons", "peoples", "friends", "friend",
            "roommates", "roommate", "members", "member", "guys", "folks", "ways", "way",
            "each", "everyone", "us", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"
        )

        if (countMatcher.find()) {
            isSplit = true
            val countToken = countMatcher.group(1)?.lowercase(Locale.US) ?: ""
            splitPeopleCount = countToken.toIntOrNull() ?: wordToNumberMap[countToken] ?: 2
        } else {
            val personMatcher = splitPersonPattern.matcher(trimmed)
            if (personMatcher.find()) {
                val candidate = personMatcher.group(1)?.trim() ?: ""
                val lowerCandidate = candidate.lowercase(Locale.US)
                if (lowerCandidate !in genericWords) {
                    isSplit = true
                    splitWith = candidate
                    // If candidate contains "and" (e.g., "Rahul and Priya"), calculate names + 1
                    val namesCount = candidate.split(Regex("(?i)\\s+and\\s+|,\\s*")).filter { it.isNotBlank() }.size
                    splitPeopleCount = maxOf(2, namesCount + 1)
                } else {
                    isSplit = true
                    splitPeopleCount = wordToNumberMap[lowerCandidate] ?: 2
                }
            } else if (splitGeneralPattern.matcher(trimmed).find()) {
                isSplit = true
                splitPeopleCount = 2
            }
        }

        // 5. Match Account(s) ordered by their appearance in the text
        data class AccountMatch(val account: Account, val startIndex: Int)
        val accountMatches = mutableListOf<AccountMatch>()

        for (acc in availableAccounts) {
            val accNameLower = acc.name.lowercase()
            val nameIndex = lowerInput.indexOf(accNameLower)
            if (nameIndex != -1) {
                accountMatches.add(AccountMatch(acc, nameIndex))
                continue
            }

            // Keyword fallbacks
            val keywordIndex = when (acc.type) {
                com.example.pam.data.model.AccountType.CASH -> lowerInput.indexOf("cash")
                com.example.pam.data.model.AccountType.BANK -> lowerInput.indexOf("bank")
                com.example.pam.data.model.AccountType.CREDIT_CARD -> {
                    val ccIndex = lowerInput.indexOf("credit card")
                    if (ccIndex != -1) ccIndex else lowerInput.indexOf("card")
                }
                com.example.pam.data.model.AccountType.SAVINGS -> lowerInput.indexOf("savings")
                else -> -1
            }

            if (keywordIndex != -1) {
                accountMatches.add(AccountMatch(acc, keywordIndex))
            }
        }

        val sortedMatches = accountMatches.distinctBy { it.account.id }.sortedBy { it.startIndex }
        var matchedAccount = sortedMatches.getOrNull(0)?.account
        var matchedToAccount = if (detectedType == TransactionType.TRANSFER) sortedMatches.getOrNull(1)?.account else null

        if (matchedAccount == null) {
            matchedAccount = availableAccounts.firstOrNull { it.isDefault } ?: availableAccounts.firstOrNull()
        }

        // If transfer and toAccount missing, pick second account if available
        if (detectedType == TransactionType.TRANSFER && matchedToAccount == null) {
            matchedToAccount = availableAccounts.firstOrNull { it.id != matchedAccount?.id }
        }

        // 6. Match Category using Hierarchical Semantic Classifier (parent vs child aware)
        val matchedCategory = SemanticCategoryClassifier.classify(
            query = trimmed,
            availableCategories = availableCategories,
            transactionType = detectedType
        )

        // 7. Generate Clean Title
        val cleanTitle = extractCleanTitle(
            rawInput = trimmed,
            amountRawMatch = amountRawMatch,
            matchedAccount = matchedAccount,
            matchedToAccount = matchedToAccount,
            availableAccounts = availableAccounts,
            detectedType = detectedType,
            matchedCategory = matchedCategory,
            availableCategories = availableCategories,
            matchedDateStrings = parsedDateTime.matchedStrings
        )

        // 8. Calculate Confidence
        var confidenceScore = 0.2f
        if (matchedAmount != null && matchedAmount > 0) confidenceScore += 0.35f
        if (matchedCategory != null && !matchedCategory.name.equals("General", ignoreCase = true)) confidenceScore += 0.25f
        if (matchedAccount != null) confidenceScore += 0.15f
        if (parsedDateTime.hasExplicitDate || parsedDateTime.hasExplicitTime) confidenceScore += 0.15f

        return ParsedTransactionDraft(
            rawInput = trimmed,
            amount = matchedAmount,
            title = cleanTitle,
            type = detectedType,
            matchedCategory = matchedCategory,
            matchedAccount = matchedAccount,
            matchedToAccount = matchedToAccount,
            isSplit = isSplit,
            splitWith = splitWith,
            splitPeopleCount = splitPeopleCount,
            confidence = minOf(1.0f, confidenceScore),
            note = trimmed,
            dateEpochMillis = parsedDateTime.epochMillis,
            hasExplicitDateTime = parsedDateTime.hasExplicitDate || parsedDateTime.hasExplicitTime
        )
    }

    private fun extractCleanTitle(
        rawInput: String,
        amountRawMatch: String,
        matchedAccount: Account?,
        matchedToAccount: Account?,
        availableAccounts: List<Account>,
        detectedType: TransactionType,
        matchedCategory: Category?,
        availableCategories: List<Category> = emptyList(),
        matchedDateStrings: List<String> = emptyList()
    ): String {
        var text = rawInput.trim()

        // 0. Remove parsed date and time phrases FIRST (before amounts/numbers, so "8 pm" or "2 days ago" are preserved intact)
        for (dateStr in matchedDateStrings) {
            text = text.replace(Regex("(?i)\\b${Regex.escape(dateStr)}(?:\\b|(?<=[^\\w])|$)"), " ")
        }
        text = text.replace(Regex("(?i)\\b(?:a\\.?m\\.?|p\\.?m\\.?|am|pm|o'clock|hrs?|hours?)\\b"), " ")
        text = text.replace(Regex("(?i)\\b[ap]\\.m\\."), " ")
        text = text.replace(Regex("(?i)\\b[ap]\\.m\\b"), " ")

        // 1. Remove matched amount if present
        if (amountRawMatch.isNotEmpty()) {
            text = text.replace(amountRawMatch, " ")
        }
        // Also remove standalone currency / amount numbers
        text = text.replace(Regex("(?i)\\b(?:rs\\.?|inr|rupees?|bucks|₹)?\\s*\\d+(?:,\\d+)*(?:\\.\\d+)?\\s*(?:k|l|cr|thousand|lac|lakh)?\\b"), " ")
        text = text.replace(Regex("(?i)\\b(?:rs\\.?|inr|rupees?|bucks|₹)\\b"), " ")

        // 2. Remove split clauses and split keywords (including words like "people", "person", "friends", etc.)
        text = text.replace(
            Regex("(?i)\\b(?:split(?:ted)?|shared|divided)\\s+(?:equally\\s+)?(?:with|between|among|in|into|for)?\\s*(?:\\d+|two|three|four|five|six|seven|eight|nine|ten|[a-zA-Z]+(?:\\s+and\\s+[a-zA-Z]+)*)?\\s*(?:people|persons?|peoples|friends?|roommates?|members?|guys|folks|ways?|each)?\\b"),
            " "
        )
        text = text.replace(
            Regex("(?i)\\b(?:split(?:ted)?|shared|divided)\\s+(?:equally|between\\s+us|among\\s+us|in\\s+half|ways?)\\b"),
            " "
        )
        text = text.replace(Regex("(?i)\\b(?:split(?:ted)?|shared|divided)\\b"), " ")
        text = text.replace(Regex("(?i)\\bwith\\s+[a-zA-Z]+(?:\\s+and\\s+[a-zA-Z]+)*\\b"), " ")
        text = text.replace(Regex("(?i)\\b(?:between|among|for)\\s+(?:\\d+|two|three|four|five|six|seven|eight|nine|ten)\\s*(?:people|persons?|peoples|friends?|roommates?|members?|guys|folks)?\\b"), " ")
        text = text.replace(Regex("(?i)\\b(?:\\d+|two|three|four|five|six|seven|eight|nine|ten)\\s+(?:people|persons?|peoples|friends?|roommates?|members?|guys|folks|ways)\\b"), " ")
        text = text.replace(Regex("(?i)\\b(?:people|persons?|peoples|friends?|roommates?|members?|guys|folks|ways?|equally)\\b"), " ")

        // 3. Remove conversational speech command prefixes
        text = text.replace(
            Regex("(?i)\\b(?:i\\s+have\\s+spent|i\\s+spent|i\\s+have\\s+paid|i\\s+paid|i\\s+bought|i\\s+got|i\\s+received|i\\s+sent|i\\s+transferred|please\\s+add|please\\s+log|add\\s+expense|log\\s+expense|new\\s+expense|add|log|record)\\b"),
            " "
        )

        // 4. Remove common transaction action verbs
        text = text.replace(
            Regex("(?i)\\b(?:paid|spent|spend|bought|buy|purchased|purchase|cost|costs|charged|charge|received|credited|earned|transferred|transfer|send\\s+to|sent\\s+to|send|sent|move\\s+to|moved\\s+to|move|moved)\\b"),
            " "
        )

        // 5. Remove account names and payment channel words
        val accountsToCheck = (availableAccounts + listOfNotNull(matchedAccount, matchedToAccount)).distinctBy { it.id }
        for (acc in accountsToCheck) {
            text = text.replace(Regex("(?i)\\b(?:from|to|via|paid via|using|in|through)?\\s*${Regex.escape(acc.name)}\\b"), " ")
            val tokens = acc.name.split(Regex("[\\s\\-_]+")).filter {
                it.length >= 3 && !it.equals("bank", ignoreCase = true) && !it.equals("card", ignoreCase = true) && !it.equals("account", ignoreCase = true)
            }
            for (token in tokens) {
                text = text.replace(Regex("(?i)\\b(?:from|to|via|paid via|using|in|through)?\\s*${Regex.escape(token)}\\b"), " ")
            }
        }

        // Generic payment keywords
        text = text.replace(
            Regex("(?i)\\b(?:paid\\s+(?:via|using|through|by|with)|using|via|through|by|in|from)?\\s*(?:credit\\s+card|debit\\s+card|credit|debit|card|cash|bank\\s+account|bank|savings|wallet|upi|gpay|google\\s+pay|phonepe|paytm|netbanking|net\\s+banking)\\b"),
            " "
        )

        // 5.5 Remove category names, prepositional category tags, and "category" mentions from title
        val protectedCompounds = listOf(
            "dog food", "cat food", "pet food", "baby food", "fast food",
            "sea food", "seafood", "street food", "junk food",
            "shopping bag", "shopping mall", "window shopping", "public transport"
        )
        val placeholderMap = mutableMapOf<String, String>()
        var placeholderIndex = 0
        for (compound in protectedCompounds) {
            val pattern = Regex("(?i)\\b${Regex.escape(compound)}\\b")
            val match = pattern.find(text)
            if (match != null) {
                val placeholder = "__COMPOUND_${placeholderIndex++}__"
                placeholderMap[placeholder] = match.value
                text = text.replace(pattern, placeholder)
            }
        }

        val categoryTerms = buildSet {
            fun addWithVariations(name: String) {
                val trimmed = name.trim()
                if (trimmed.length < 3) return
                add(trimmed)
                add(trimmed.replace("&", "and"))
                if (trimmed.endsWith("ies", ignoreCase = true)) {
                    add(trimmed.dropLast(3) + "y")
                } else if (trimmed.endsWith("s", ignoreCase = true)) {
                    add(trimmed.dropLast(1))
                }
                if (trimmed.equals("transportation", ignoreCase = true)) {
                    add("transport")
                }
                trimmed.split("&", "/", "and").map { it.trim() }.filter { it.length >= 3 }.forEach { part ->
                    add(part)
                    if (part.endsWith("ies", ignoreCase = true)) {
                        add(part.dropLast(3) + "y")
                    } else if (part.endsWith("s", ignoreCase = true)) {
                        add(part.dropLast(1))
                    }
                }
            }

            if (matchedCategory != null) {
                addWithVariations(matchedCategory.name)
            }
            for (cat in availableCategories) {
                addWithVariations(cat.name)
            }
        }.sortedByDescending { it.length }

        for (catTerm in categoryTerms) {
            // A1) Explicit phrases: "in category food", "under category food", "category food"
            text = text.replace(
                Regex("(?i)\\b(?:in|under|as|for|to)?\\s*(?:the\\s+)?category\\s+(?:of\\s+)?${Regex.escape(catTerm)}\\b"),
                " "
            )
            // A2) "in food category", "under food category", "food category"
            text = text.replace(
                Regex("(?i)\\b(?:in|under|as|for|to)?\\s*(?:the\\s+)?${Regex.escape(catTerm)}\\s+category\\b"),
                " "
            )

            // B) Prepositional phrases: "in food", "under food", "as food", "for food"
            text = text.replace(
                Regex("(?i)\\b(?:in|under|as|into)\\s+(?:the\\s+)?${Regex.escape(catTerm)}\\b"),
                " "
            )
            text = text.replace(
                Regex("(?i)\\b(?:for)\\s+(?:the\\s+)?${Regex.escape(catTerm)}\\b"),
                " "
            )

            // C) Trailing category name: "dosa food" -> "dosa", "samosa snacks" -> "samosa"
            text = text.replace(
                Regex("(?i)(?<=\\s)${Regex.escape(catTerm)}\\s*$"),
                " "
            )

            // D) Leading category name: "food dosa" -> "dosa", "snacks samosa" -> "samosa"
            text = text.replace(
                Regex("(?i)^\\s*${Regex.escape(catTerm)}(?=\\s)"),
                " "
            )
        }

        // Clean any standalone word "category" or "categories"
        text = text.replace(Regex("(?i)\\b(?:categories|category)\\b"), " ")

        // Restore protected compound phrases
        for ((placeholder, original) in placeholderMap) {
            text = text.replace(placeholder, original)
        }

        // 6. Clean up leading/trailing prepositions, conjunctions and articles
        val prepWords = setOf(
            "for", "on", "at", "in", "to", "into", "from", "of", "a", "an", "the",
            "my", "some", "and", "under", "as", "with", "by", "about"
        )

        var words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        while (words.isNotEmpty() && prepWords.contains(words.first().lowercase())) {
            words = words.drop(1)
        }
        while (words.isNotEmpty() && prepWords.contains(words.last().lowercase())) {
            words = words.dropLast(1)
        }
        val cleaned = words.joinToString(" ").trim()

        // 7. Fallback if empty (e.g. user literally said "Food 200" or "Groceries 500")
        if (cleaned.length < 2 || !cleaned.any { it.isLetter() }) {
            return when (detectedType) {
                TransactionType.TRANSFER -> "Transfer"
                TransactionType.INCOME -> matchedCategory?.name ?: "Income"
                TransactionType.EXPENSE -> matchedCategory?.name ?: "Expense"
            }
        }

        return toTitleCase(cleaned)
    }

    private fun toTitleCase(str: String): String {
        val minorWords = setOf("at", "of", "in", "on", "to", "for", "the", "a", "an", "and", "by")
        val words = str.split(" ").filter { it.isNotEmpty() }
        return words.mapIndexed { index, word ->
            val lower = word.lowercase(Locale.US)
            if (index > 0 && minorWords.contains(lower)) {
                lower
            } else {
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }
        }.joinToString(" ")
    }
}
