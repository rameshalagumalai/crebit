package com.example.pam.data.repository

import android.content.Context
import com.example.pam.data.db.PAMDatabaseHelper
import com.example.pam.data.model.Account
import com.example.pam.data.model.AccountExpenseSummary
import com.example.pam.data.model.Category
import com.example.pam.data.model.CategoryExpenseSummary
import com.example.pam.data.model.CategoryType
import com.example.pam.data.model.FinancialOverview
import com.example.pam.data.model.SplitGroup
import com.example.pam.data.model.SplitGroupWithDetails
import com.example.pam.data.model.SplitSettlementEntry
import com.example.pam.data.model.Transaction
import com.example.pam.data.model.TransactionType
import com.example.pam.data.model.TransactionWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PAMRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val dbHelper = PAMDatabaseHelper(appContext)
    private val dataUpdateTrigger = MutableSharedFlow<Unit>(replay = 1).apply {
        tryEmit(Unit)
    }

    companion object {
        @Volatile
        private var instance: PAMRepository? = null

        fun getInstance(context: Context): PAMRepository {
            return instance ?: synchronized(this) {
                instance ?: PAMRepository(context).also { instance = it }
            }
        }
    }

    private fun notifyDataChanged() {
        dataUpdateTrigger.tryEmit(Unit)
        try {
            com.example.pam.ui.widget.OverviewWidgetProvider.updateAllWidgets(appContext)
        } catch (_: Exception) {}
    }

    // ==========================================
    // ACCOUNTS
    // ==========================================

    fun getAccountsFlow(): Flow<List<Account>> = flow {
        dataUpdateTrigger.collect {
            val accounts = dbHelper.getAllAccounts()
            emit(accounts)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getAllAccounts(): List<Account> = withContext(Dispatchers.IO) {
        dbHelper.getAllAccounts()
    }

    suspend fun getAccountById(id: Long): Account? = withContext(Dispatchers.IO) {
        dbHelper.getAccountById(id)
    }

    suspend fun addAccount(account: Account): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.insertAccount(account)
        notifyDataChanged()
        id
    }

    suspend fun updateAccount(account: Account): Int = withContext(Dispatchers.IO) {
        val rows = dbHelper.updateAccount(account)
        notifyDataChanged()
        rows
    }

    suspend fun deleteAccount(accountId: Long): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.deleteAccount(accountId)
        notifyDataChanged()
        success
    }

    suspend fun setDefaultAccount(accountId: Long) = withContext(Dispatchers.IO) {
        dbHelper.setDefaultAccount(accountId)
        notifyDataChanged()
    }

    // ==========================================
    // CATEGORIES
    // ==========================================

    fun getCategoriesFlow(): Flow<List<Category>> = flow {
        dataUpdateTrigger.collect {
            val categories = dbHelper.getAllCategories()
            emit(categories)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getAllCategories(): List<Category> = withContext(Dispatchers.IO) {
        dbHelper.getAllCategories()
    }

    suspend fun getCategoriesByType(type: CategoryType): List<Category> = withContext(Dispatchers.IO) {
        dbHelper.getCategoriesByType(type)
    }

    suspend fun getCategoriesForTransactionType(type: TransactionType): List<Category> = withContext(Dispatchers.IO) {
        dbHelper.getCategoriesForTransactionType(type)
    }

    suspend fun getCategoryById(id: Long): Category? = withContext(Dispatchers.IO) {
        dbHelper.getCategoryById(id)
    }

    suspend fun addCategory(category: Category): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.insertCategory(category)
        notifyDataChanged()
        id
    }

    suspend fun updateCategory(category: Category): Int = withContext(Dispatchers.IO) {
        val rows = dbHelper.updateCategory(category)
        notifyDataChanged()
        rows
    }

    suspend fun deleteCategory(categoryId: Long): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.deleteCategory(categoryId)
        notifyDataChanged()
        success
    }

    suspend fun seedStarterCategories() = withContext(Dispatchers.IO) {
        dbHelper.seedStarterCategories()
        notifyDataChanged()
    }

    suspend fun getOrCreateGeneralCategory(): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.getOrCreateGeneralCategory()
        notifyDataChanged()
        id
    }

    // ==========================================
    // TRANSACTIONS
    // ==========================================

    fun getTransactionsFlow(
        type: TransactionType? = null,
        accountId: Long? = null,
        categoryId: Long? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        searchQuery: String? = null,
        splitGroupId: Long? = null,
        limit: Int = 200
    ): Flow<List<TransactionWithDetails>> = flow {
        dataUpdateTrigger.collect {
            val txns = dbHelper.getTransactionsWithDetails(
                type = type,
                accountId = accountId,
                categoryId = categoryId,
                startDate = startDate,
                endDate = endDate,
                searchQuery = searchQuery,
                splitGroupId = splitGroupId,
                limit = limit
            )
            emit(txns)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getTransactionWithDetails(id: Long): TransactionWithDetails? = withContext(Dispatchers.IO) {
        dbHelper.getTransactionWithDetailsById(id)
    }

    suspend fun addTransaction(txn: Transaction): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.insertTransaction(txn)
        notifyDataChanged()
        id
    }

    suspend fun updateTransaction(txn: Transaction): Int = withContext(Dispatchers.IO) {
        val rows = dbHelper.updateTransaction(txn)
        notifyDataChanged()
        rows
    }

    suspend fun deleteTransaction(txnId: Long): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.deleteTransaction(txnId)
        notifyDataChanged()
        success
    }

    suspend fun deleteTransactions(txnIds: List<Long>): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.deleteTransactions(txnIds)
        notifyDataChanged()
        success
    }

    // ==========================================
    // REPORTS & ANALYTICS
    // ==========================================

    suspend fun getFinancialOverview(startDate: Long, endDate: Long): FinancialOverview = withContext(Dispatchers.IO) {
        dbHelper.getFinancialOverview(startDate, endDate)
    }

    suspend fun getCategoryExpenseBreakdown(startDate: Long, endDate: Long): List<CategoryExpenseSummary> = withContext(Dispatchers.IO) {
        dbHelper.getCategoryExpenseBreakdown(startDate, endDate)
    }

    suspend fun getAccountExpenseBreakdown(startDate: Long, endDate: Long): List<AccountExpenseSummary> = withContext(Dispatchers.IO) {
        dbHelper.getAccountExpenseBreakdown(startDate, endDate)
    }

    fun getReportsFlow(startDate: Long, endDate: Long): Flow<Triple<FinancialOverview, List<CategoryExpenseSummary>, List<AccountExpenseSummary>>> = flow {
        dataUpdateTrigger.collect {
            val overview = dbHelper.getFinancialOverview(startDate, endDate)
            val categories = dbHelper.getCategoryExpenseBreakdown(startDate, endDate)
            val accounts = dbHelper.getAccountExpenseBreakdown(startDate, endDate)
            emit(Triple(overview, categories, accounts))
        }
    }.flowOn(Dispatchers.IO)

    // ==========================================
    // SPLITS
    // ==========================================

    fun getSplitGroupsFlow(): Flow<List<SplitGroupWithDetails>> = flow {
        dataUpdateTrigger.collect {
            val splits = dbHelper.getAllSplitGroupsWithDetails()
            emit(splits)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getAllSplitGroupsWithDetails(): List<SplitGroupWithDetails> = withContext(Dispatchers.IO) {
        dbHelper.getAllSplitGroupsWithDetails()
    }

    suspend fun getSplitGroupWithDetailsById(id: Long): SplitGroupWithDetails? = withContext(Dispatchers.IO) {
        dbHelper.getSplitGroupWithDetailsById(id)
    }

    suspend fun getSplitGroupByExpenseId(expenseTxnId: Long): SplitGroup? = withContext(Dispatchers.IO) {
        dbHelper.getSplitGroupByExpenseId(expenseTxnId)
    }

    suspend fun addSplitGroup(splitGroup: SplitGroup): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.insertSplitGroup(splitGroup)
        notifyDataChanged()
        id
    }

    suspend fun updateSplitGroup(splitGroup: SplitGroup): Int = withContext(Dispatchers.IO) {
        val rows = dbHelper.updateSplitGroup(splitGroup)
        notifyDataChanged()
        rows
    }

    suspend fun deleteSplitGroup(id: Long): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.deleteSplitGroup(id)
        notifyDataChanged()
        success
    }

    suspend fun settleSplitGroup(id: Long): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.settleSplitGroup(id)
        notifyDataChanged()
        success
    }

    suspend fun reopenSplitGroup(id: Long): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.reopenSplitGroup(id)
        notifyDataChanged()
        success
    }

    suspend fun recordSplitSettlementBatch(splitGroupId: Long, settlements: List<SplitSettlementEntry>): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.recordSplitSettlementBatch(splitGroupId, settlements)
        notifyDataChanged()
        success
    }

    suspend fun createSplitExpense(
        title: String,
        totalAmount: Double,
        myShare: Double,
        accountId: Long,
        categoryId: Long,
        dateEpochMillis: Long = System.currentTimeMillis(),
        note: String? = null,
        payeeUpiId: String? = null,
        payeeName: String? = null,
        upiRefId: String? = null,
        rawUpiUri: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.createSplitExpense(
            title = title,
            totalAmount = totalAmount,
            myShare = myShare,
            accountId = accountId,
            categoryId = categoryId,
            dateEpochMillis = dateEpochMillis,
            note = note,
            payeeUpiId = payeeUpiId,
            payeeName = payeeName,
            upiRefId = upiRefId,
            rawUpiUri = rawUpiUri
        )
        notifyDataChanged()
        id
    }

    suspend fun updateSplitExpense(
        splitGroupId: Long,
        title: String,
        totalAmount: Double,
        myShare: Double,
        accountId: Long,
        categoryId: Long,
        dateEpochMillis: Long,
        note: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.updateSplitExpense(
            splitGroupId = splitGroupId,
            title = title,
            totalAmount = totalAmount,
            myShare = myShare,
            accountId = accountId,
            categoryId = categoryId,
            dateEpochMillis = dateEpochMillis,
            note = note
        )
        notifyDataChanged()
        success
    }

    suspend fun splitExistingExpense(
        transactionId: Long,
        myShare: Double,
        receivableAmount: Double
    ): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.splitExistingExpense(transactionId, myShare, receivableAmount)
        notifyDataChanged()
        id
    }

    suspend fun removeSplitFromExpense(
        splitGroupId: Long,
        keepTransactionId: Long,
        totalAmount: Double
    ): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.removeSplitFromExpense(splitGroupId, keepTransactionId, totalAmount)
        notifyDataChanged()
        success
    }

    suspend fun getOrCreateSplitCategory(): Long = withContext(Dispatchers.IO) {
        dbHelper.getOrCreateSplitCategory()
    }
}
