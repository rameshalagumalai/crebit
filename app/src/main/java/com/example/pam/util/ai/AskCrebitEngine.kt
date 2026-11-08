package com.example.pam.util.ai

import com.example.pam.data.model.Account
import com.example.pam.data.model.AccountType
import com.example.pam.data.model.Category
import com.example.pam.data.model.SplitGroupWithDetails
import com.example.pam.data.model.SplitStatus
import com.example.pam.data.model.TransactionType
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.util.CurrencyFormatter
import com.example.pam.util.DateUtils
import java.util.Calendar
import java.util.Locale

enum class QueryIntentType {
    CATEGORY_SPEND,
    MERCHANT_SEARCH,
    BIGGEST_EXPENSE,
    LOWEST_EXPENSE,
    SPLITS_RECEIVABLE,
    ACCOUNT_BALANCE,
    NET_WORTH,
    PERIOD_SUMMARY,
    INCOME_SUMMARY,
    FREEFORM_SEARCH
}

data class ResolvedDateRange(
    val startMillis: Long,
    val endMillis: Long,
    val label: String,
    val compareStartMillis: Long? = null,
    val compareEndMillis: Long? = null,
    val compareLabel: String? = null
)

data class AskCrebitQueryResult(
    val query: String,
    val intentType: QueryIntentType,
    val headline: String,
    val subhead: String? = null,
    val primaryMetric: String? = null,
    val comparisonText: String? = null,
    val matchingTransactions: List<TransactionWithDetails> = emptyList(),
    val matchingSplits: List<SplitGroupWithDetails> = emptyList(),
    val matchingAccount: Account? = null,
    val suggestedFollowUps: List<String> = emptyList()
)

object AskCrebitEngine {

    fun query(
        rawQuery: String,
        allTransactions: List<TransactionWithDetails>,
        accounts: List<Account>,
        categories: List<Category>,
        splits: List<SplitGroupWithDetails> = emptyList(),
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): AskCrebitQueryResult {
        val q = rawQuery.trim().lowercase(Locale.US)
        if (q.isBlank()) {
            return AskCrebitQueryResult(
                query = rawQuery,
                intentType = QueryIntentType.PERIOD_SUMMARY,
                headline = "Ask Crebit Anything",
                subhead = "100% private on-device financial query assistant",
                suggestedFollowUps = listOf(
                    "Food spend this month",
                    "Pending splits",
                    "Biggest expense this week",
                    "Total bank balance"
                )
            )
        }

        // 1. Splits / Receivables Intent: "pending splits", "split receivables", "how much to receive"
        if (isSplitsQuery(q)) {
            return resolveSplitsQuery(rawQuery, splits)
        }

        // 2. Net Worth Intent: "net worth", "total balance", "how much money do i have"
        if (isNetWorthQuery(q)) {
            return resolveNetWorthQuery(rawQuery, accounts, allTransactions)
        }

        // 3. Account Balance Intent: "balance in hdfc", "how much cash", "icici balance"
        val matchedAccount = findAccountInQuery(q, accounts)
        if (matchedAccount != null && isAccountBalanceQuery(q)) {
            return resolveAccountBalanceQuery(rawQuery, matchedAccount, allTransactions)
        }

        // 4. Extremum / Max Expense Intent: "biggest expense", "highest spend", "most expensive"
        if (isExtremumMaxQuery(q)) {
            return resolveBiggestExpenseQuery(rawQuery, q, allTransactions, categories, referenceTimeMillis)
        }

        // 4b. Extremum / Min Expense Intent: "lowest expense", "cheapest", "smallest spend"
        if (isExtremumMinQuery(q)) {
            return resolveLowestExpenseQuery(rawQuery, q, allTransactions, categories, referenceTimeMillis)
        }

        // 5. Direct Category Spend Intent: "how much spent on food", "travel this month", "dining expenses"
        val directCategory = findDirectCategoryInQuery(q, categories)
        if (directCategory != null) {
            return resolveCategorySpendQuery(rawQuery, q, directCategory, allTransactions, categories, referenceTimeMillis)
        }

        // 6. Income Intent: "how much income", "total salary", "earnings this month"
        if (isIncomeQuery(q)) {
            return resolveIncomeQuery(rawQuery, q, allTransactions, referenceTimeMillis)
        }

        // 7. Merchant / Specific Keyword Search
        val merchantResult = resolveMerchantQuery(rawQuery, q, allTransactions, referenceTimeMillis)
        if (merchantResult != null) {
            return merchantResult
        }

        // 8. Category Alias Spend Intent (e.g. "commute", "groceries", "eating out")
        val aliasCategory = findCategoryByAliasInQuery(q, categories)
        if (aliasCategory != null) {
            return resolveCategorySpendQuery(rawQuery, q, aliasCategory, allTransactions, categories, referenceTimeMillis)
        }

        // 9. General Period Summary: "how much did i spend today", "yesterday's spend", "summary for this month"
        if (isPeriodSpendQuery(q)) {
            return resolvePeriodSpendQuery(rawQuery, q, allTransactions, categories, referenceTimeMillis)
        }

        // 10. Fallback Freeform Search
        return resolveFreeformSearch(rawQuery, q, allTransactions)
    }

    private fun isSplitsQuery(q: String): Boolean {
        val oweRegex = Regex("\\b(owe|owes|owed|owing)\\b")
        return oweRegex.containsMatchIn(q) || q.contains("split") ||
                q.contains("receivable") || q.contains("pending") || q.contains("lent") ||
                q.contains("borrow")
    }

    private fun resolveSplitsQuery(
        rawQuery: String,
        splits: List<SplitGroupWithDetails>
    ): AskCrebitQueryResult {
        val openSplits = splits.filter { it.splitGroup.status == SplitStatus.OPEN && it.remainingAmount > 0.01 }
        val totalReceivable = openSplits.sumOf { it.remainingAmount }

        return if (openSplits.isEmpty()) {
            AskCrebitQueryResult(
                query = rawQuery,
                intentType = QueryIntentType.SPLITS_RECEIVABLE,
                headline = "No Pending Receivables",
                subhead = "All your split expenses are fully settled! 🎉",
                primaryMetric = "₹0",
                suggestedFollowUps = listOf(
                    "Food spend this month",
                    "Biggest expense this week",
                    "Total net worth"
                )
            )
        } else {
            AskCrebitQueryResult(
                query = rawQuery,
                intentType = QueryIntentType.SPLITS_RECEIVABLE,
                headline = "${CurrencyFormatter.format(totalReceivable)} to Receive",
                subhead = "Across ${openSplits.size} open split${if (openSplits.size > 1) "s" else ""}",
                primaryMetric = CurrencyFormatter.format(totalReceivable),
                matchingSplits = openSplits,
                suggestedFollowUps = listOf(
                    "Food spend this month",
                    "Biggest expense this week",
                    "Total bank balance"
                )
            )
        }
    }

    private fun isNetWorthQuery(q: String): Boolean {
        return q.contains("net worth") ||
                (q.contains("total") && (q.contains("balance") || q.contains("money") || q.contains("worth") || q.contains("asset"))) ||
                q == "balance" || q == "all balances"
    }

    private fun resolveNetWorthQuery(
        rawQuery: String,
        accounts: List<Account>,
        allTransactions: List<TransactionWithDetails>
    ): AskCrebitQueryResult {
        val netWorth = accounts.sumOf {
            if (it.type == AccountType.CREDIT_CARD) -it.currentBalance else it.currentBalance
        }
        val totalAssets = accounts.filter { it.type != AccountType.CREDIT_CARD }.sumOf { it.currentBalance }
        val totalLiabilities = accounts.filter { it.type == AccountType.CREDIT_CARD }.sumOf { it.currentBalance }

        val subhead = if (totalLiabilities > 0) {
            "Assets: ${CurrencyFormatter.format(totalAssets)} • Credit debt: ${CurrencyFormatter.format(totalLiabilities)}"
        } else {
            "Across ${accounts.size} active accounts"
        }

        return AskCrebitQueryResult(
            query = rawQuery,
            intentType = QueryIntentType.NET_WORTH,
            headline = "Net Worth: ${CurrencyFormatter.format(netWorth)}",
            subhead = subhead,
            primaryMetric = CurrencyFormatter.format(netWorth),
            matchingTransactions = allTransactions.take(5),
            suggestedFollowUps = listOf(
                "How much cash do I have?",
                "Expenses this month",
                "Pending splits"
            )
        )
    }

    private fun isAccountBalanceQuery(q: String): Boolean {
        return q.contains("balance") || q.contains("how much") || q.contains("have") ||
                q.contains("account") || q.contains("money in")
    }

    private fun findAccountInQuery(q: String, accounts: List<Account>): Account? {
        for (acc in accounts) {
            val name = acc.name.lowercase(Locale.US)
            if (q.contains(name)) return acc
            // Check distinctive words in account name (e.g. "hdfc", "icici", "sbi", "axis")
            val words = name.split(" ", "_", "-").filter {
                it.length >= 3 && it !in listOf("bank", "account", "card", "credit", "savings", "current")
            }
            if (words.any { q.contains(it) }) return acc
        }
        if (q.contains("cash")) {
            return accounts.firstOrNull { it.type == AccountType.CASH }
        }
        return null
    }

    private fun resolveAccountBalanceQuery(
        rawQuery: String,
        account: Account,
        allTransactions: List<TransactionWithDetails>
    ): AskCrebitQueryResult {
        val balance = account.currentBalance
        val matchingTxns = allTransactions.filter { it.transaction.accountId == account.id }.take(10)

        return AskCrebitQueryResult(
            query = rawQuery,
            intentType = QueryIntentType.ACCOUNT_BALANCE,
            headline = "${CurrencyFormatter.format(balance)} in ${account.name}",
            subhead = "${account.type.displayName} • ${matchingTxns.size} recent transactions",
            primaryMetric = CurrencyFormatter.format(balance),
            matchingAccount = account,
            matchingTransactions = matchingTxns,
            suggestedFollowUps = listOf(
                "Total net worth",
                "Expenses this month",
                "Biggest expense this week"
            )
        )
    }

    private fun isExtremumMaxQuery(q: String): Boolean {
        return q.contains("biggest") || q.contains("highest") || q.contains("largest") ||
                q.contains("most expensive") || q.contains("max expense") || q.contains("top expense")
    }

    private fun resolveBiggestExpenseQuery(
        rawQuery: String,
        q: String,
        allTransactions: List<TransactionWithDetails>,
        categories: List<Category>,
        referenceTimeMillis: Long
    ): AskCrebitQueryResult {
        val dateRange = resolveDateRange(q, referenceTimeMillis)
        val matchedCategory = findDirectCategoryInQuery(q, categories) ?: findCategoryByAliasInQuery(q, categories)

        val expenses = allTransactions
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .filter { it.transaction.dateEpochMillis in dateRange.startMillis..dateRange.endMillis }
            .filter { matchedCategory == null || it.category?.id == matchedCategory.id }
            .sortedByDescending { it.transaction.amount }

        val top = expenses.firstOrNull()
        return if (top == null) {
            AskCrebitQueryResult(
                query = rawQuery,
                intentType = QueryIntentType.BIGGEST_EXPENSE,
                headline = "No expenses found",
                subhead = "No recorded spends for ${dateRange.label}${matchedCategory?.let { " in ${it.name}" } ?: ""}",
                suggestedFollowUps = listOf(
                    "Expenses this month",
                    "Food spend this month",
                    "Total net worth"
                )
            )
        } else {
            val catName = top.category?.name ?: "Uncategorized"
            AskCrebitQueryResult(
                query = rawQuery,
                intentType = QueryIntentType.BIGGEST_EXPENSE,
                headline = "Biggest: ${CurrencyFormatter.format(top.transaction.amount)}",
                subhead = "'${top.transaction.title}' on ${DateUtils.formatDate(top.transaction.dateEpochMillis)} • $catName",
                primaryMetric = CurrencyFormatter.format(top.transaction.amount),
                matchingTransactions = expenses.take(5),
                suggestedFollowUps = listOf(
                    "Total spent ${dateRange.label}",
                    "Pending splits",
                    "Food spend this month"
                )
            )
        }
    }

    private fun isExtremumMinQuery(q: String): Boolean {
        return q.contains("lowest") || q.contains("smallest") || q.contains("cheapest") ||
                q.contains("min expense") || q.contains("minimum expense")
    }

    private fun resolveLowestExpenseQuery(
        rawQuery: String,
        q: String,
        allTransactions: List<TransactionWithDetails>,
        categories: List<Category>,
        referenceTimeMillis: Long
    ): AskCrebitQueryResult {
        val dateRange = resolveDateRange(q, referenceTimeMillis)
        val matchedCategory = findDirectCategoryInQuery(q, categories) ?: findCategoryByAliasInQuery(q, categories)

        val expenses = allTransactions
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .filter { it.transaction.dateEpochMillis in dateRange.startMillis..dateRange.endMillis }
            .filter { matchedCategory == null || it.category?.id == matchedCategory.id }
            .sortedBy { it.transaction.amount }

        val lowest = expenses.firstOrNull()
        return if (lowest == null) {
            AskCrebitQueryResult(
                query = rawQuery,
                intentType = QueryIntentType.LOWEST_EXPENSE,
                headline = "No expenses found",
                subhead = "No recorded spends for ${dateRange.label}${matchedCategory?.let { " in ${it.name}" } ?: ""}",
                suggestedFollowUps = listOf(
                    "Expenses this month",
                    "Food spend this month",
                    "Total net worth"
                )
            )
        } else {
            val catName = lowest.category?.name ?: "Uncategorized"
            AskCrebitQueryResult(
                query = rawQuery,
                intentType = QueryIntentType.LOWEST_EXPENSE,
                headline = "Lowest: ${CurrencyFormatter.format(lowest.transaction.amount)}",
                subhead = "'${lowest.transaction.title}' on ${DateUtils.formatDate(lowest.transaction.dateEpochMillis)} • $catName",
                primaryMetric = CurrencyFormatter.format(lowest.transaction.amount),
                matchingTransactions = expenses.take(5),
                suggestedFollowUps = listOf(
                    "Biggest expense ${dateRange.label}",
                    "Total spent ${dateRange.label}",
                    "Pending splits"
                )
            )
        }
    }

    private fun findDirectCategoryInQuery(q: String, categories: List<Category>): Category? {
        // Direct category name match (or words like "food", "travel", "shopping")
        for (cat in categories) {
            val name = cat.name.lowercase(Locale.US)
            if (q.contains(name)) return cat
            val words = name.split("&", " ", "/", "-").map { it.trim() }.filter { it.length >= 3 }
            if (words.any { q.contains(it) }) return cat
        }
        return null
    }

    private fun findCategoryByAliasInQuery(q: String, categories: List<Category>): Category? {
        val aliases = mapOf(
            "food" to listOf("dining", "eating", "eat", "restaurant", "lunch", "dinner", "breakfast", "snacks", "cafe", "coffee", "tea"),
            "groceries" to listOf("groceries", "grocery", "supermarket", "vegetables", "milk"),
            "travel" to listOf("commute", "cab", "cabs", "auto", "metro", "bus", "flight", "train", "petrol", "fuel"),
            "shopping" to listOf("clothes", "electronics", "shoes"),
            "entertainment" to listOf("entertainment", "movies", "movie", "cinema", "netflix", "spotify", "prime", "games"),
            "bills" to listOf("bills", "electricity", "water", "wifi", "internet", "recharge", "mobile recharge", "rent"),
            "health" to listOf("health", "medicine", "medical", "doctor", "pharmacy", "hospital", "gym", "fitness"),
            "investment" to listOf("investment", "invest", "stocks", "mutual funds", "gold", "crypto")
        )

        for ((key, words) in aliases) {
            if (words.any { q.contains(it) }) {
                val match = categories.firstOrNull { it.name.lowercase(Locale.US).contains(key) }
                if (match != null) return match
            }
        }
        return null
    }

    private fun resolveCategorySpendQuery(
        rawQuery: String,
        q: String,
        category: Category,
        allTransactions: List<TransactionWithDetails>,
        allCategories: List<Category>,
        referenceTimeMillis: Long
    ): AskCrebitQueryResult {
        val dateRange = resolveDateRange(q, referenceTimeMillis)

        val txns = allTransactions
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .filter { it.category?.id == category.id }
            .filter { it.transaction.dateEpochMillis in dateRange.startMillis..dateRange.endMillis }
            .sortedByDescending { it.transaction.dateEpochMillis }

        val total = txns.sumOf { it.transaction.amount }

        var comparisonText: String? = null
        if (dateRange.compareStartMillis != null && dateRange.compareEndMillis != null) {
            val compareTxns = allTransactions
                .filter { it.transaction.type == TransactionType.EXPENSE }
                .filter { it.category?.id == category.id }
                .filter { it.transaction.dateEpochMillis in dateRange.compareStartMillis..dateRange.compareEndMillis }
            val compareTotal = compareTxns.sumOf { it.transaction.amount }

            if (compareTotal > 0.0) {
                val diffPercent = ((total - compareTotal) / compareTotal) * 100.0
                val dir = if (diffPercent >= 0) "higher" else "lower"
                comparisonText = "${String.format(Locale.US, "%.0f", Math.abs(diffPercent))}% $dir than ${dateRange.compareLabel} (${CurrencyFormatter.format(compareTotal)})"
            }
        }

        val subhead = "${dateRange.label} • ${txns.size} transaction${if (txns.size != 1) "s" else ""}"

        return AskCrebitQueryResult(
            query = rawQuery,
            intentType = QueryIntentType.CATEGORY_SPEND,
            headline = "${CurrencyFormatter.format(total)} on ${category.name}",
            subhead = subhead,
            primaryMetric = CurrencyFormatter.format(total),
            comparisonText = comparisonText,
            matchingTransactions = txns,
            suggestedFollowUps = listOf(
                "${category.name} spend last month",
                "Biggest ${category.name} expense",
                "Total spent ${dateRange.label}"
            )
        )
    }

    private fun isIncomeQuery(q: String): Boolean {
        return q.contains("income") || q.contains("salary") || q.contains("earning") ||
                q.contains("earned") || q.contains("received")
    }

    private fun resolveIncomeQuery(
        rawQuery: String,
        q: String,
        allTransactions: List<TransactionWithDetails>,
        referenceTimeMillis: Long
    ): AskCrebitQueryResult {
        val dateRange = resolveDateRange(q, referenceTimeMillis)

        val incomes = allTransactions
            .filter { it.transaction.type == TransactionType.INCOME }
            .filter { it.transaction.dateEpochMillis in dateRange.startMillis..dateRange.endMillis }
            .sortedByDescending { it.transaction.dateEpochMillis }

        val total = incomes.sumOf { it.transaction.amount }

        return AskCrebitQueryResult(
            query = rawQuery,
            intentType = QueryIntentType.INCOME_SUMMARY,
            headline = "${CurrencyFormatter.format(total)} Income",
            subhead = "${dateRange.label} • ${incomes.size} deposit${if (incomes.size != 1) "s" else ""}",
            primaryMetric = CurrencyFormatter.format(total),
            matchingTransactions = incomes,
            suggestedFollowUps = listOf(
                "Expenses ${dateRange.label}",
                "Total net worth",
                "Food spend this month"
            )
        )
    }

    private fun isPeriodSpendQuery(q: String): Boolean {
        return q.contains("spend") || q.contains("spent") || q.contains("expense") ||
                q.contains("summary") || q.contains("today") || q.contains("yesterday") ||
                q.contains("this week") || q.contains("this month")
    }

    private fun resolvePeriodSpendQuery(
        rawQuery: String,
        q: String,
        allTransactions: List<TransactionWithDetails>,
        categories: List<Category>,
        referenceTimeMillis: Long
    ): AskCrebitQueryResult {
        val dateRange = resolveDateRange(q, referenceTimeMillis)

        val expenses = allTransactions
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .filter { it.transaction.dateEpochMillis in dateRange.startMillis..dateRange.endMillis }
            .sortedByDescending { it.transaction.dateEpochMillis }

        val total = expenses.sumOf { it.transaction.amount }
        val topCategory = expenses
            .groupBy { it.category?.name ?: "Uncategorized" }
            .maxByOrNull { it.value.sumOf { t -> t.transaction.amount } }

        val topCategoryText = topCategory?.let {
            "Top: ${it.key} (${CurrencyFormatter.format(it.value.sumOf { t -> t.transaction.amount })})"
        } ?: "No expenses"

        var comparisonText: String? = null
        if (dateRange.compareStartMillis != null && dateRange.compareEndMillis != null) {
            val compareTxns = allTransactions
                .filter { it.transaction.type == TransactionType.EXPENSE }
                .filter { it.transaction.dateEpochMillis in dateRange.compareStartMillis..dateRange.compareEndMillis }
            val compareTotal = compareTxns.sumOf { it.transaction.amount }

            if (compareTotal > 0.0) {
                val diffPercent = ((total - compareTotal) / compareTotal) * 100.0
                val dir = if (diffPercent >= 0) "higher" else "lower"
                comparisonText = "${String.format(Locale.US, "%.0f", Math.abs(diffPercent))}% $dir than ${dateRange.compareLabel} (${CurrencyFormatter.format(compareTotal)})"
            }
        }

        val subhead = "${dateRange.label} • ${expenses.size} expenses • $topCategoryText"

        return AskCrebitQueryResult(
            query = rawQuery,
            intentType = QueryIntentType.PERIOD_SUMMARY,
            headline = "${CurrencyFormatter.format(total)} Total Spend",
            subhead = subhead,
            primaryMetric = CurrencyFormatter.format(total),
            comparisonText = comparisonText,
            matchingTransactions = expenses,
            suggestedFollowUps = listOf(
                "Biggest expense ${dateRange.label}",
                "Food spend ${dateRange.label}",
                "Pending splits"
            )
        )
    }

    private fun resolveMerchantQuery(
        rawQuery: String,
        q: String,
        allTransactions: List<TransactionWithDetails>,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): AskCrebitQueryResult? {
        // Strip common question, action, and time words
        val clean = q.replace(
            Regex("\\b(how|much|did|i|spend|spends|spent|spending|expense|expenses|on|show|all|me|transactions|orders|payments|for|the|in|at|this|last|past|month|week|year|today|yesterday|january|february|march|april|may|june|july|august|september|october|november|december)\\b"),
            " "
        ).trim().replace(Regex("\\s+"), " ")

        if (clean.length < 3) return null

        val hasDateFilter = listOf("today", "yesterday", "month", "week", "year", "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december").any { q.contains(it) }
        val dateRange = if (hasDateFilter) resolveDateRange(q, referenceTimeMillis) else null

        val matches = allTransactions.filter {
            val title = it.transaction.title.lowercase(Locale.US)
            val note = it.transaction.note?.lowercase(Locale.US) ?: ""
            val matchesText = title.contains(clean) || note.contains(clean)
            val matchesDate = dateRange == null || it.transaction.dateEpochMillis in dateRange.startMillis..dateRange.endMillis
            matchesText && matchesDate
        }.sortedByDescending { it.transaction.dateEpochMillis }

        if (matches.isEmpty()) return null

        val total = matches.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
        val displayName = clean.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }

        val subhead = if (dateRange != null) {
            "${dateRange.label} • ${matches.size} transactions found"
        } else {
            "${matches.size} transactions found"
        }

        return AskCrebitQueryResult(
            query = rawQuery,
            intentType = QueryIntentType.MERCHANT_SEARCH,
            headline = "${CurrencyFormatter.format(total)} on $displayName",
            subhead = subhead,
            primaryMetric = CurrencyFormatter.format(total),
            matchingTransactions = matches,
            suggestedFollowUps = listOf(
                "Biggest expense this month",
                "Food spend this month",
                "Total net worth"
            )
        )
    }

    private fun resolveFreeformSearch(
        rawQuery: String,
        q: String,
        allTransactions: List<TransactionWithDetails>
    ): AskCrebitQueryResult {
        val keywords = q.split(" ").filter { it.length >= 2 }
        val matches = allTransactions.filter { item ->
            val title = item.transaction.title.lowercase(Locale.US)
            val note = item.transaction.note?.lowercase(Locale.US) ?: ""
            val cat = item.category?.name?.lowercase(Locale.US) ?: ""
            val acc = item.account?.name?.lowercase(Locale.US) ?: ""
            keywords.any { title.contains(it) || note.contains(it) || cat.contains(it) || acc.contains(it) }
        }.sortedByDescending { it.transaction.dateEpochMillis }

        val total = matches.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }

        return AskCrebitQueryResult(
            query = rawQuery,
            intentType = QueryIntentType.FREEFORM_SEARCH,
            headline = "${matches.size} result${if (matches.size != 1) "s" else ""} for '$rawQuery'",
            subhead = if (total > 0) "Total matching expense: ${CurrencyFormatter.format(total)}" else "Search results",
            primaryMetric = if (total > 0) CurrencyFormatter.format(total) else "${matches.size}",
            matchingTransactions = matches,
            suggestedFollowUps = listOf(
                "Food spend this month",
                "Pending splits",
                "Total net worth"
            )
        )
    }

    fun resolveDateRange(query: String, referenceTimeMillis: Long = System.currentTimeMillis()): ResolvedDateRange {
        val q = query.lowercase(Locale.US)
        val cal = Calendar.getInstance().apply { timeInMillis = referenceTimeMillis }

        // Helper for zeroing out time
        fun Calendar.startOfDay() {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        fun Calendar.endOfDay() {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // Today
        if (q.contains("today")) {
            val start = (cal.clone() as Calendar).apply { startOfDay() }.timeInMillis
            val end = (cal.clone() as Calendar).apply { endOfDay() }.timeInMillis
            return ResolvedDateRange(start, end, "Today")
        }

        // Yesterday
        if (q.contains("yesterday")) {
            val yStart = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1); startOfDay() }.timeInMillis
            val yEnd = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1); endOfDay() }.timeInMillis
            return ResolvedDateRange(yStart, yEnd, "Yesterday")
        }

        // Last Week
        if (q.contains("last week")) {
            val start = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -14); startOfDay() }.timeInMillis
            val end = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7); endOfDay() }.timeInMillis
            return ResolvedDateRange(start, end, "Last Week")
        }

        // This Week
        if (q.contains("this week") || q.contains("past 7 days") || q.contains("last 7 days")) {
            val start = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7); startOfDay() }.timeInMillis
            val end = (cal.clone() as Calendar).apply { endOfDay() }.timeInMillis
            val cmpStart = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -14); startOfDay() }.timeInMillis
            val cmpEnd = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7); endOfDay() }.timeInMillis
            return ResolvedDateRange(start, end, "This Week", cmpStart, cmpEnd, "last week")
        }

        // Last Month
        if (q.contains("last month") && !q.contains("vs last month") && !q.contains("vs. last month")) {
            val mCal = (cal.clone() as Calendar).apply {
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, 1)
                startOfDay()
            }
            val start = mCal.timeInMillis
            val maxDay = mCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            mCal.set(Calendar.DAY_OF_MONTH, maxDay)
            mCal.endOfDay()
            val end = mCal.timeInMillis
            return ResolvedDateRange(start, end, "Last Month")
        }

        // Specific named month (e.g. "in October")
        val months = listOf("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december")
        for ((idx, mName) in months.withIndex()) {
            if (q.contains(mName)) {
                val mCal = (cal.clone() as Calendar).apply {
                    set(Calendar.MONTH, idx)
                    set(Calendar.DAY_OF_MONTH, 1)
                    startOfDay()
                }
                val start = mCal.timeInMillis
                val maxDay = mCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                mCal.set(Calendar.DAY_OF_MONTH, maxDay)
                mCal.endOfDay()
                val end = mCal.timeInMillis
                val capName = mName.replaceFirstChar { it.titlecase(Locale.US) }
                return ResolvedDateRange(start, end, capName)
            }
        }

        // Default: This Month (with comparison against last month)
        val startThisMonth = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            startOfDay()
        }.timeInMillis
        val endThisMonth = (cal.clone() as Calendar).apply { endOfDay() }.timeInMillis

        val startLastMonth = (cal.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            startOfDay()
        }.timeInMillis
        val maxDayLastMonth = (cal.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
        val endLastMonth = (cal.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, maxDayLastMonth)
            endOfDay()
        }.timeInMillis

        return ResolvedDateRange(
            startMillis = startThisMonth,
            endMillis = endThisMonth,
            label = "This Month",
            compareStartMillis = startLastMonth,
            compareEndMillis = endLastMonth,
            compareLabel = "last month"
        )
    }
}
