package com.example.pam.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.pam.data.model.Account
import com.example.pam.data.model.AccountExpenseSummary
import com.example.pam.data.model.AccountType
import com.example.pam.data.model.Category
import com.example.pam.data.model.CategoryExpenseSummary
import com.example.pam.data.model.CategoryType
import com.example.pam.data.model.FinancialOverview
import com.example.pam.data.model.SplitGroup
import com.example.pam.data.model.SplitGroupWithDetails
import com.example.pam.data.model.SplitSettlementEntry
import com.example.pam.data.model.SplitStatus
import com.example.pam.data.model.Transaction
import com.example.pam.data.model.TransactionType
import com.example.pam.data.model.TransactionWithDetails

class PAMDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "pam_database.db"
        const val DATABASE_VERSION = 4

        // Table Accounts
        const val TABLE_ACCOUNTS = "accounts"
        const val COL_ACC_ID = "id"
        const val COL_ACC_NAME = "name"
        const val COL_ACC_TYPE = "type"
        const val COL_ACC_INIT_BALANCE = "initial_balance"
        const val COL_ACC_CURR_BALANCE = "current_balance"
        const val COL_ACC_NUMBER = "account_number"
        const val COL_ACC_COLOR = "color_hex"
        const val COL_ACC_ICON = "icon_name"
        const val COL_ACC_IS_DEFAULT = "is_default"
        const val COL_ACC_CREATED_AT = "created_at"

        // Table Categories
        const val TABLE_CATEGORIES = "categories"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NAME = "name"
        const val COL_CAT_TYPE = "type"
        const val COL_CAT_COLOR = "color_hex"
        const val COL_CAT_ICON = "icon_name"
        const val COL_CAT_IS_DEFAULT = "is_default"

        // Table Transactions
        const val TABLE_TRANSACTIONS = "transactions"
        const val COL_TXN_ID = "id"
        const val COL_TXN_TITLE = "title"
        const val COL_TXN_AMOUNT = "amount"
        const val COL_TXN_TYPE = "type"
        const val COL_TXN_ACC_ID = "account_id"
        const val COL_TXN_TO_ACC_ID = "to_account_id"
        const val COL_TXN_CAT_ID = "category_id"
        const val COL_TXN_DATE = "date_epoch_millis"
        const val COL_TXN_NOTE = "note"
        const val COL_TXN_PAYEE_UPI = "payee_upi_id"
        const val COL_TXN_PAYEE_NAME = "payee_name"
        const val COL_TXN_UPI_REF = "upi_ref_id"
        const val COL_TXN_STATUS = "payment_status"
        const val COL_TXN_CREATED_AT = "created_at"
        const val COL_TXN_SPLIT_GROUP_ID = "split_group_id"

        // Table Split Groups
        const val TABLE_SPLIT_GROUPS = "split_groups"
        const val COL_SPLIT_ID = "id"
        const val COL_SPLIT_DESC = "description"
        const val COL_SPLIT_TOTAL = "total_amount"
        const val COL_SPLIT_MY_SHARE = "my_share"
        const val COL_SPLIT_RECEIVABLE = "receivable_amount"
        const val COL_SPLIT_EXPENSE_TXN_ID = "expense_transaction_id"
        const val COL_SPLIT_CREATED_AT = "created_at"
        const val COL_SPLIT_STATUS = "status"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createAccountsTable = """
            CREATE TABLE $TABLE_ACCOUNTS (
                $COL_ACC_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ACC_NAME TEXT NOT NULL,
                $COL_ACC_TYPE TEXT NOT NULL,
                $COL_ACC_INIT_BALANCE REAL NOT NULL DEFAULT 0.0,
                $COL_ACC_CURR_BALANCE REAL NOT NULL DEFAULT 0.0,
                $COL_ACC_NUMBER TEXT,
                $COL_ACC_COLOR TEXT NOT NULL,
                $COL_ACC_ICON TEXT NOT NULL,
                $COL_ACC_IS_DEFAULT INTEGER NOT NULL DEFAULT 0,
                $COL_ACC_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()

        val createCategoriesTable = """
            CREATE TABLE $TABLE_CATEGORIES (
                $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAT_NAME TEXT NOT NULL,
                $COL_CAT_TYPE TEXT NOT NULL,
                $COL_CAT_COLOR TEXT NOT NULL,
                $COL_CAT_ICON TEXT NOT NULL,
                $COL_CAT_IS_DEFAULT INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()

        val createTransactionsTable = """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COL_TXN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TXN_TITLE TEXT NOT NULL,
                $COL_TXN_AMOUNT REAL NOT NULL,
                $COL_TXN_TYPE TEXT NOT NULL,
                $COL_TXN_ACC_ID INTEGER NOT NULL,
                $COL_TXN_TO_ACC_ID INTEGER,
                $COL_TXN_CAT_ID INTEGER,
                $COL_TXN_DATE INTEGER NOT NULL,
                $COL_TXN_NOTE TEXT,
                $COL_TXN_PAYEE_UPI TEXT,
                $COL_TXN_PAYEE_NAME TEXT,
                $COL_TXN_UPI_REF TEXT,
                $COL_TXN_STATUS TEXT NOT NULL DEFAULT 'COMPLETED',
                $COL_TXN_CREATED_AT INTEGER NOT NULL,
                $COL_TXN_SPLIT_GROUP_ID INTEGER,
                FOREIGN KEY ($COL_TXN_ACC_ID) REFERENCES $TABLE_ACCOUNTS($COL_ACC_ID),
                FOREIGN KEY ($COL_TXN_TO_ACC_ID) REFERENCES $TABLE_ACCOUNTS($COL_ACC_ID),
                FOREIGN KEY ($COL_TXN_CAT_ID) REFERENCES $TABLE_CATEGORIES($COL_CAT_ID)
            )
        """.trimIndent()

        val createSplitGroupsTable = """
            CREATE TABLE $TABLE_SPLIT_GROUPS (
                $COL_SPLIT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SPLIT_DESC TEXT NOT NULL,
                $COL_SPLIT_TOTAL REAL NOT NULL,
                $COL_SPLIT_MY_SHARE REAL NOT NULL,
                $COL_SPLIT_RECEIVABLE REAL NOT NULL,
                $COL_SPLIT_EXPENSE_TXN_ID INTEGER NOT NULL,
                $COL_SPLIT_CREATED_AT INTEGER NOT NULL,
                $COL_SPLIT_STATUS TEXT NOT NULL DEFAULT 'OPEN',
                FOREIGN KEY ($COL_SPLIT_EXPENSE_TXN_ID) REFERENCES $TABLE_TRANSACTIONS($COL_TXN_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createAccountsTable)
        db.execSQL(createCategoriesTable)
        db.execSQL(createTransactionsTable)
        db.execSQL(createSplitGroupsTable)
        createIndices(db)

        // General category is always present by default
        getOrCreateGeneralCategory(db)
    }

    private fun createIndices(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_txn_date ON $TABLE_TRANSACTIONS($COL_TXN_DATE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_txn_account ON $TABLE_TRANSACTIONS($COL_TXN_ACC_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_txn_to_account ON $TABLE_TRANSACTIONS($COL_TXN_TO_ACC_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_txn_category ON $TABLE_TRANSACTIONS($COL_TXN_CAT_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_txn_split_group ON $TABLE_TRANSACTIONS($COL_TXN_SPLIT_GROUP_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_split_expense ON $TABLE_SPLIT_GROUPS($COL_SPLIT_EXPENSE_TXN_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_SPLIT_GROUPS (
                    $COL_SPLIT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_SPLIT_DESC TEXT NOT NULL,
                    $COL_SPLIT_TOTAL REAL NOT NULL,
                    $COL_SPLIT_MY_SHARE REAL NOT NULL,
                    $COL_SPLIT_RECEIVABLE REAL NOT NULL,
                    $COL_SPLIT_EXPENSE_TXN_ID INTEGER NOT NULL,
                    $COL_SPLIT_CREATED_AT INTEGER NOT NULL,
                    $COL_SPLIT_STATUS TEXT NOT NULL DEFAULT 'OPEN',
                    FOREIGN KEY ($COL_SPLIT_EXPENSE_TXN_ID) REFERENCES $TABLE_TRANSACTIONS($COL_TXN_ID) ON DELETE CASCADE
                )
            """.trimIndent())
            try {
                db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COL_TXN_SPLIT_GROUP_ID INTEGER")
            } catch (_: Exception) {}
        }
        if (oldVersion < 3) {
            createIndices(db)
        }
        if (oldVersion < 4) {
            db.execSQL(
                "UPDATE $TABLE_CATEGORIES SET $COL_CAT_NAME = 'Split', $COL_CAT_TYPE = '${com.example.pam.data.model.CategoryType.BOTH.name}' " +
                "WHERE $COL_CAT_NAME = 'Split Collection' OR $COL_CAT_NAME = 'Split'"
            )
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        try {
            getOrCreateGeneralCategory(db)
            db.execSQL(
                "UPDATE $TABLE_CATEGORIES SET $COL_CAT_NAME = 'Split', $COL_CAT_TYPE = '${com.example.pam.data.model.CategoryType.BOTH.name}' " +
                "WHERE $COL_CAT_NAME = 'Split Collection'"
            )
            // Ensure user share expenses (expenses not in Split category) do not have split_group_id tag
            db.execSQL(
                "UPDATE $TABLE_TRANSACTIONS SET $COL_TXN_SPLIT_GROUP_ID = NULL " +
                "WHERE $COL_TXN_SPLIT_GROUP_ID IS NOT NULL AND $COL_TXN_TYPE = 'EXPENSE' AND $COL_TXN_CAT_ID != (" +
                "SELECT $COL_CAT_ID FROM $TABLE_CATEGORIES WHERE $COL_CAT_NAME = 'Split' LIMIT 1)"
            )
        } catch (_: Exception) {}
    }

    fun seedStarterCategories(db: SQLiteDatabase = writableDatabase) {
        val existing = HashSet<String>()
        val cursor = db.rawQuery("SELECT $COL_CAT_NAME FROM $TABLE_CATEGORIES", null)
        cursor.use {
            while (it.moveToNext()) {
                existing.add(it.getString(0).lowercase().trim())
            }
        }

        val expenseCategories = listOf(
            Triple("Food & Dining", "#FF7043", "Restaurant"),
            Triple("Groceries", "#4CAF50", "ShoppingCart"),
            Triple("Shopping", "#AB47BC", "ShoppingBag"),
            Triple("Transportation", "#29B6F6", "DirectionsCar"),
            Triple("Bills & Utilities", "#FFA726", "ReceiptLong"),
            Triple("Entertainment", "#EC407A", "Movie"),
            Triple("Healthcare", "#EF5350", "MedicalServices"),
            Triple("Education", "#26A69A", "School"),
            Triple("Travel", "#5C6BC0", "Flight")
        )

        val incomeCategories = listOf(
            Triple("Salary", "#43A047", "Payments"),
            Triple("Freelance / Consulting", "#1E88E5", "Work"),
            Triple("Rental Income", "#3949AB", "Home"),
            Triple("Cashback / Rewards", "#FB8C00", "CardGiftcard")
        )

        val bothCategories = listOf(
            Triple("Investments", "#00ACC1", "TrendingUp"),
            Triple("Gifts & Donations", "#D81B60", "CardGiftcard"),
            Triple("Split", "#00897B", "CallSplit"),
            Triple("General", "#64748B", "Category")
        )

        db.beginTransaction()
        try {
            expenseCategories.forEachIndexed { idx, item ->
                if (!existing.contains(item.first.lowercase().trim())) {
                    val cv = ContentValues().apply {
                        put(COL_CAT_NAME, item.first)
                        put(COL_CAT_TYPE, CategoryType.EXPENSE.name)
                        put(COL_CAT_COLOR, item.second)
                        put(COL_CAT_ICON, item.third)
                        put(COL_CAT_IS_DEFAULT, if (idx == 0) 1 else 0)
                    }
                    db.insert(TABLE_CATEGORIES, null, cv)
                }
            }

            incomeCategories.forEachIndexed { idx, item ->
                if (!existing.contains(item.first.lowercase().trim())) {
                    val cv = ContentValues().apply {
                        put(COL_CAT_NAME, item.first)
                        put(COL_CAT_TYPE, CategoryType.INCOME.name)
                        put(COL_CAT_COLOR, item.second)
                        put(COL_CAT_ICON, item.third)
                        put(COL_CAT_IS_DEFAULT, if (idx == 0) 1 else 0)
                    }
                    db.insert(TABLE_CATEGORIES, null, cv)
                }
            }

            bothCategories.forEach { item ->
                if (!existing.contains(item.first.lowercase().trim())) {
                    val cv = ContentValues().apply {
                        put(COL_CAT_NAME, item.first)
                        put(COL_CAT_TYPE, CategoryType.BOTH.name)
                        put(COL_CAT_COLOR, item.second)
                        put(COL_CAT_ICON, item.third)
                        put(COL_CAT_IS_DEFAULT, 0)
                    }
                    db.insert(TABLE_CATEGORIES, null, cv)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ==========================================
    // ACCOUNT OPERATIONS
    // ==========================================

    fun insertAccount(account: Account): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_ACC_NAME, account.name)
            put(COL_ACC_TYPE, account.type.name)
            put(COL_ACC_INIT_BALANCE, account.initialBalance)
            put(COL_ACC_CURR_BALANCE, account.initialBalance)
            put(COL_ACC_NUMBER, account.accountNumber)
            put(COL_ACC_COLOR, account.colorHex)
            put(COL_ACC_ICON, account.iconName)
            put(COL_ACC_IS_DEFAULT, if (account.isDefault) 1 else 0)
            put(COL_ACC_CREATED_AT, account.createdAt)
        }
        val id = db.insert(TABLE_ACCOUNTS, null, cv)
        if (account.isDefault && id != -1L) {
            setDefaultAccount(id)
        }
        return id
    }

    fun updateAccount(account: Account): Int {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_ACC_NAME, account.name)
            put(COL_ACC_TYPE, account.type.name)
            put(COL_ACC_INIT_BALANCE, account.initialBalance)
            put(COL_ACC_NUMBER, account.accountNumber)
            put(COL_ACC_COLOR, account.colorHex)
            put(COL_ACC_ICON, account.iconName)
            put(COL_ACC_IS_DEFAULT, if (account.isDefault) 1 else 0)
        }
        val rows = db.update(TABLE_ACCOUNTS, cv, "$COL_ACC_ID = ?", arrayOf(account.id.toString()))
        if (account.isDefault) {
            setDefaultAccount(account.id)
        }
        recalculateAccountBalance(account.id)
        return rows
    }

    fun deleteAccount(accountId: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Delete associated transactions
            db.delete(TABLE_TRANSACTIONS, "$COL_TXN_ACC_ID = ? OR $COL_TXN_TO_ACC_ID = ?", arrayOf(accountId.toString(), accountId.toString()))
            val rows = db.delete(TABLE_ACCOUNTS, "$COL_ACC_ID = ?", arrayOf(accountId.toString()))
            db.setTransactionSuccessful()
            return rows > 0
        } finally {
            db.endTransaction()
        }
    }

    fun setDefaultAccount(accountId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_IS_DEFAULT = 0")
            db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_IS_DEFAULT = 1 WHERE $COL_ACC_ID = ?", arrayOf(accountId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAccountById(id: Long): Account? {
        val db = readableDatabase
        val cursor = db.query(TABLE_ACCOUNTS, null, "$COL_ACC_ID = ?", arrayOf(id.toString()), null, null, null)
        return cursor.use {
            if (it.moveToFirst()) cursorToAccount(it) else null
        }
    }

    fun getAllAccounts(): List<Account> {
        val list = mutableListOf<Account>()
        val db = readableDatabase
        val cursor = db.query(TABLE_ACCOUNTS, null, null, null, null, null, "$COL_ACC_IS_DEFAULT DESC, $COL_ACC_NAME ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToAccount(it))
            }
        }
        return list
    }

    fun recalculateAccountBalance(accountId: Long) {
        val db = writableDatabase
        val account = getAccountById(accountId) ?: return

        val accIdStr = accountId.toString()
        val query = """
            SELECT 
                COALESCE(SUM(CASE WHEN $COL_TXN_ACC_ID = ? AND $COL_TXN_TYPE = '${TransactionType.INCOME.name}' THEN $COL_TXN_AMOUNT ELSE 0.0 END), 0.0) -
                COALESCE(SUM(CASE WHEN $COL_TXN_ACC_ID = ? AND $COL_TXN_TYPE = '${TransactionType.EXPENSE.name}' THEN $COL_TXN_AMOUNT ELSE 0.0 END), 0.0) -
                COALESCE(SUM(CASE WHEN $COL_TXN_ACC_ID = ? AND $COL_TXN_TYPE = '${TransactionType.TRANSFER.name}' THEN $COL_TXN_AMOUNT ELSE 0.0 END), 0.0) +
                COALESCE(SUM(CASE WHEN $COL_TXN_TO_ACC_ID = ? AND $COL_TXN_TYPE = '${TransactionType.TRANSFER.name}' THEN $COL_TXN_AMOUNT ELSE 0.0 END), 0.0)
            FROM $TABLE_TRANSACTIONS
            WHERE $COL_TXN_ACC_ID = ? OR $COL_TXN_TO_ACC_ID = ?
        """.trimIndent()

        var netDelta = 0.0
        val cursor = db.rawQuery(query, arrayOf(accIdStr, accIdStr, accIdStr, accIdStr, accIdStr, accIdStr))
        cursor.use {
            if (it.moveToFirst()) {
                netDelta = it.getDouble(0)
            }
        }

        val balance = account.initialBalance + netDelta
        val cv = ContentValues().apply {
            put(COL_ACC_CURR_BALANCE, balance)
        }
        db.update(TABLE_ACCOUNTS, cv, "$COL_ACC_ID = ?", arrayOf(accIdStr))
    }

    private fun cursorToAccount(c: Cursor): Account {
        return Account(
            id = c.getLong(c.getColumnIndexOrThrow(COL_ACC_ID)),
            name = c.getString(c.getColumnIndexOrThrow(COL_ACC_NAME)),
            type = try { AccountType.valueOf(c.getString(c.getColumnIndexOrThrow(COL_ACC_TYPE))) } catch (e: Exception) { AccountType.OTHER },
            initialBalance = c.getDouble(c.getColumnIndexOrThrow(COL_ACC_INIT_BALANCE)),
            currentBalance = c.getDouble(c.getColumnIndexOrThrow(COL_ACC_CURR_BALANCE)),
            accountNumber = c.getString(c.getColumnIndexOrThrow(COL_ACC_NUMBER)),
            colorHex = c.getString(c.getColumnIndexOrThrow(COL_ACC_COLOR)),
            iconName = c.getString(c.getColumnIndexOrThrow(COL_ACC_ICON)),
            isDefault = c.getInt(c.getColumnIndexOrThrow(COL_ACC_IS_DEFAULT)) == 1,
            createdAt = c.getLong(c.getColumnIndexOrThrow(COL_ACC_CREATED_AT))
        )
    }

    // ==========================================
    // CATEGORY OPERATIONS
    // ==========================================

    fun getOrCreateGeneralCategory(db: SQLiteDatabase = writableDatabase): Long {
        val cursor = db.rawQuery(
            "SELECT $COL_CAT_ID FROM $TABLE_CATEGORIES WHERE $COL_CAT_NAME = ? LIMIT 1",
            arrayOf("General")
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        val cv = ContentValues().apply {
            put(COL_CAT_NAME, "General")
            put(COL_CAT_TYPE, CategoryType.BOTH.name)
            put(COL_CAT_COLOR, "#64748B")
            put(COL_CAT_ICON, "Category")
            put(COL_CAT_IS_DEFAULT, 1)
        }
        return db.insert(TABLE_CATEGORIES, null, cv)
    }

    fun insertCategory(category: Category): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_CAT_NAME, category.name)
            put(COL_CAT_TYPE, category.type.name)
            put(COL_CAT_COLOR, category.colorHex)
            put(COL_CAT_ICON, category.iconName)
            put(COL_CAT_IS_DEFAULT, if (category.isDefault) 1 else 0)
        }
        return db.insert(TABLE_CATEGORIES, null, cv)
    }

    fun updateCategory(category: Category): Int {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_CAT_NAME, category.name)
            put(COL_CAT_TYPE, category.type.name)
            put(COL_CAT_COLOR, category.colorHex)
            put(COL_CAT_ICON, category.iconName)
            put(COL_CAT_IS_DEFAULT, if (category.isDefault) 1 else 0)
        }
        return db.update(TABLE_CATEGORIES, cv, "$COL_CAT_ID = ?", arrayOf(category.id.toString()))
    }

    fun deleteCategory(categoryId: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Nullify category_id in existing transactions rather than deleting transactions
            val cv = ContentValues().apply {
                putNull(COL_TXN_CAT_ID)
            }
            db.update(TABLE_TRANSACTIONS, cv, "$COL_TXN_CAT_ID = ?", arrayOf(categoryId.toString()))
            val rows = db.delete(TABLE_CATEGORIES, "$COL_CAT_ID = ?", arrayOf(categoryId.toString()))
            db.setTransactionSuccessful()
            return rows > 0
        } finally {
            db.endTransaction()
        }
    }

    fun getCategoryById(id: Long): Category? {
        val db = readableDatabase
        val cursor = db.query(TABLE_CATEGORIES, null, "$COL_CAT_ID = ?", arrayOf(id.toString()), null, null, null)
        return cursor.use {
            if (it.moveToFirst()) cursorToCategory(it) else null
        }
    }

    fun getAllCategories(): List<Category> {
        val list = mutableListOf<Category>()
        val db = readableDatabase
        val cursor = db.query(TABLE_CATEGORIES, null, null, null, null, null, "$COL_CAT_TYPE ASC, $COL_CAT_NAME ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToCategory(it))
            }
        }
        return list
    }

    fun getCategoriesByType(type: com.example.pam.data.model.CategoryType): List<Category> {
        val list = mutableListOf<Category>()
        val db = readableDatabase
        val cursor = db.query(TABLE_CATEGORIES, null, "$COL_CAT_TYPE = ?", arrayOf(type.name), null, null, "$COL_CAT_NAME ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToCategory(it))
            }
        }
        return list
    }

    fun getCategoriesForTransactionType(type: TransactionType): List<Category> {
        val list = mutableListOf<Category>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CATEGORIES,
            null,
            "$COL_CAT_TYPE = ? OR $COL_CAT_TYPE = ?",
            arrayOf(type.name, com.example.pam.data.model.CategoryType.BOTH.name),
            null,
            null,
            "$COL_CAT_NAME ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToCategory(it))
            }
        }
        return list
    }

    private fun cursorToCategory(c: Cursor): Category {
        return Category(
            id = c.getLong(c.getColumnIndexOrThrow(COL_CAT_ID)),
            name = c.getString(c.getColumnIndexOrThrow(COL_CAT_NAME)),
            type = try { com.example.pam.data.model.CategoryType.valueOf(c.getString(c.getColumnIndexOrThrow(COL_CAT_TYPE))) } catch (e: Exception) { com.example.pam.data.model.CategoryType.EXPENSE },
            colorHex = c.getString(c.getColumnIndexOrThrow(COL_CAT_COLOR)),
            iconName = c.getString(c.getColumnIndexOrThrow(COL_CAT_ICON)),
            isDefault = c.getInt(c.getColumnIndexOrThrow(COL_CAT_IS_DEFAULT)) == 1
        )
    }

    // ==========================================
    // TRANSACTION OPERATIONS
    // ==========================================

    private fun insertTransactionInternal(db: SQLiteDatabase, txn: Transaction): Long {
        val cv = ContentValues().apply {
            put(COL_TXN_TITLE, txn.title)
            put(COL_TXN_AMOUNT, txn.amount)
            put(COL_TXN_TYPE, txn.type.name)
            put(COL_TXN_ACC_ID, txn.accountId)
            if (txn.toAccountId != null) put(COL_TXN_TO_ACC_ID, txn.toAccountId) else putNull(COL_TXN_TO_ACC_ID)
            if (txn.categoryId != null) put(COL_TXN_CAT_ID, txn.categoryId) else putNull(COL_TXN_CAT_ID)
            put(COL_TXN_DATE, txn.dateEpochMillis)
            put(COL_TXN_NOTE, txn.note)
            put(COL_TXN_PAYEE_UPI, txn.payeeUpiId)
            put(COL_TXN_PAYEE_NAME, txn.payeeName)
            put(COL_TXN_UPI_REF, txn.upiRefId)
            put(COL_TXN_STATUS, txn.paymentStatus)
            put(COL_TXN_CREATED_AT, txn.createdAt)
            if (txn.splitGroupId != null) put(COL_TXN_SPLIT_GROUP_ID, txn.splitGroupId) else putNull(COL_TXN_SPLIT_GROUP_ID)
        }
        val id = db.insert(TABLE_TRANSACTIONS, null, cv)
        if (id != -1L) {
            // Adjust account balances
            when (txn.type) {
                TransactionType.EXPENSE -> {
                    db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?", arrayOf(txn.amount.toString(), txn.accountId.toString()))
                }
                TransactionType.INCOME -> {
                    db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?", arrayOf(txn.amount.toString(), txn.accountId.toString()))
                }
                TransactionType.TRANSFER -> {
                    db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?", arrayOf(txn.amount.toString(), txn.accountId.toString()))
                    txn.toAccountId?.let { toAccId ->
                        db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?", arrayOf(txn.amount.toString(), toAccId.toString()))
                    }
                }
            }
        }
        return id
    }

    fun insertTransaction(txn: Transaction): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val id = insertTransactionInternal(db, txn)
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    fun updateTransaction(newTxn: Transaction): Int {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val oldTxn = getTransactionById(newTxn.id) ?: return 0

            // Revert old transaction balance effect
            when (oldTxn.type) {
                TransactionType.EXPENSE -> {
                    db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?", arrayOf(oldTxn.amount.toString(), oldTxn.accountId.toString()))
                }
                TransactionType.INCOME -> {
                    db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?", arrayOf(oldTxn.amount.toString(), oldTxn.accountId.toString()))
                }
                TransactionType.TRANSFER -> {
                    db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?", arrayOf(oldTxn.amount.toString(), oldTxn.accountId.toString()))
                    oldTxn.toAccountId?.let { toAccId ->
                        db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?", arrayOf(oldTxn.amount.toString(), toAccId.toString()))
                    }
                }
            }

            // Update row
            val cv = ContentValues().apply {
                put(COL_TXN_TITLE, newTxn.title)
                put(COL_TXN_AMOUNT, newTxn.amount)
                put(COL_TXN_TYPE, newTxn.type.name)
                put(COL_TXN_ACC_ID, newTxn.accountId)
                if (newTxn.toAccountId != null) put(COL_TXN_TO_ACC_ID, newTxn.toAccountId) else putNull(COL_TXN_TO_ACC_ID)
                if (newTxn.categoryId != null) put(COL_TXN_CAT_ID, newTxn.categoryId) else putNull(COL_TXN_CAT_ID)
                put(COL_TXN_DATE, newTxn.dateEpochMillis)
                put(COL_TXN_NOTE, newTxn.note)
                put(COL_TXN_PAYEE_UPI, newTxn.payeeUpiId)
                put(COL_TXN_PAYEE_NAME, newTxn.payeeName)
                put(COL_TXN_UPI_REF, newTxn.upiRefId)
                put(COL_TXN_STATUS, newTxn.paymentStatus)
                if (newTxn.splitGroupId != null) put(COL_TXN_SPLIT_GROUP_ID, newTxn.splitGroupId) else putNull(COL_TXN_SPLIT_GROUP_ID)
            }
            val rows = db.update(TABLE_TRANSACTIONS, cv, "$COL_TXN_ID = ?", arrayOf(newTxn.id.toString()))

            // Apply new transaction balance effect
            when (newTxn.type) {
                TransactionType.EXPENSE -> {
                    db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?", arrayOf(newTxn.amount.toString(), newTxn.accountId.toString()))
                }
                TransactionType.INCOME -> {
                    db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?", arrayOf(newTxn.amount.toString(), newTxn.accountId.toString()))
                }
                TransactionType.TRANSFER -> {
                    db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?", arrayOf(newTxn.amount.toString(), newTxn.accountId.toString()))
                    newTxn.toAccountId?.let { toAccId ->
                        db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?", arrayOf(newTxn.amount.toString(), toAccId.toString()))
                    }
                }
            }

            db.setTransactionSuccessful()
            return rows
        } finally {
            db.endTransaction()
        }
    }

    fun deleteTransaction(transactionId: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val oldTxn = getTransactionById(transactionId) ?: return false

            // Revert balance impact
            when (oldTxn.type) {
                TransactionType.EXPENSE -> {
                    db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?", arrayOf(oldTxn.amount.toString(), oldTxn.accountId.toString()))
                }
                TransactionType.INCOME -> {
                    db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?", arrayOf(oldTxn.amount.toString(), oldTxn.accountId.toString()))
                }
                TransactionType.TRANSFER -> {
                    db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?", arrayOf(oldTxn.amount.toString(), oldTxn.accountId.toString()))
                    oldTxn.toAccountId?.let { toAccId ->
                        db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?", arrayOf(oldTxn.amount.toString(), toAccId.toString()))
                    }
                }
            }

            // If this was an expense transaction for a SplitGroup, delete that SplitGroup
            db.delete(TABLE_SPLIT_GROUPS, "$COL_SPLIT_EXPENSE_TXN_ID = ?", arrayOf(transactionId.toString()))

            val splitGroupId = oldTxn.splitGroupId
            val rows = db.delete(TABLE_TRANSACTIONS, "$COL_TXN_ID = ?", arrayOf(transactionId.toString()))

            // If this was a collection for a split, verify if the split should be reopened
            if (splitGroupId != null && oldTxn.type == TransactionType.INCOME) {
                val sumCursor = db.rawQuery(
                    "SELECT SUM($COL_TXN_AMOUNT) FROM $TABLE_TRANSACTIONS WHERE $COL_TXN_SPLIT_GROUP_ID = ? AND $COL_TXN_TYPE = 'INCOME'",
                    arrayOf(splitGroupId.toString())
                )
                var totalRec = 0.0
                sumCursor.use {
                    if (it.moveToFirst() && !it.isNull(0)) totalRec = it.getDouble(0)
                }
                val sg = getSplitGroupById(splitGroupId)
                if (sg != null && totalRec < sg.receivableAmount - 0.001) {
                    val cv = ContentValues().apply { put(COL_SPLIT_STATUS, SplitStatus.OPEN.name) }
                    db.update(TABLE_SPLIT_GROUPS, cv, "$COL_SPLIT_ID = ?", arrayOf(splitGroupId.toString()))
                }
            }

            db.setTransactionSuccessful()
            return rows > 0
        } finally {
            db.endTransaction()
        }
    }

    fun deleteTransactions(transactionIds: List<Long>): Boolean {
        if (transactionIds.isEmpty()) return true
        val db = writableDatabase
        db.beginTransaction()
        try {
            val affectedSplitGroups = mutableSetOf<Long>()
            for (id in transactionIds) {
                val oldTxn = getTransactionById(id) ?: continue
                when (oldTxn.type) {
                    TransactionType.EXPENSE -> {
                        db.execSQL(
                            "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?",
                            arrayOf(oldTxn.amount.toString(), oldTxn.accountId.toString())
                        )
                    }
                    TransactionType.INCOME -> {
                        db.execSQL(
                            "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?",
                            arrayOf(oldTxn.amount.toString(), oldTxn.accountId.toString())
                        )
                    }
                    TransactionType.TRANSFER -> {
                        db.execSQL(
                            "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?",
                            arrayOf(oldTxn.amount.toString(), oldTxn.accountId.toString())
                        )
                        oldTxn.toAccountId?.let { toAccId ->
                            db.execSQL(
                                "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?",
                                arrayOf(oldTxn.amount.toString(), toAccId.toString())
                            )
                        }
                    }
                }
                db.delete(TABLE_SPLIT_GROUPS, "$COL_SPLIT_EXPENSE_TXN_ID = ?", arrayOf(id.toString()))
                if (oldTxn.splitGroupId != null && oldTxn.type == TransactionType.INCOME) {
                    affectedSplitGroups.add(oldTxn.splitGroupId)
                }
                db.delete(TABLE_TRANSACTIONS, "$COL_TXN_ID = ?", arrayOf(id.toString()))
            }

            // Check affected split groups
            for (splitGroupId in affectedSplitGroups) {
                val sumCursor = db.rawQuery(
                    "SELECT SUM($COL_TXN_AMOUNT) FROM $TABLE_TRANSACTIONS WHERE $COL_TXN_SPLIT_GROUP_ID = ? AND $COL_TXN_TYPE = 'INCOME'",
                    arrayOf(splitGroupId.toString())
                )
                var totalRec = 0.0
                sumCursor.use {
                    if (it.moveToFirst() && !it.isNull(0)) totalRec = it.getDouble(0)
                }
                val sg = getSplitGroupById(splitGroupId)
                if (sg != null && totalRec < sg.receivableAmount - 0.001) {
                    val cv = ContentValues().apply { put(COL_SPLIT_STATUS, SplitStatus.OPEN.name) }
                    db.update(TABLE_SPLIT_GROUPS, cv, "$COL_SPLIT_ID = ?", arrayOf(splitGroupId.toString()))
                }
            }

            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    fun getTransactionById(id: Long): Transaction? {
        val db = readableDatabase
        val cursor = db.query(TABLE_TRANSACTIONS, null, "$COL_TXN_ID = ?", arrayOf(id.toString()), null, null, null)
        return cursor.use {
            if (it.moveToFirst()) cursorToTransaction(it) else null
        }
    }

    fun getTransactionWithDetailsById(id: Long): TransactionWithDetails? {
        val txn = getTransactionById(id) ?: return null
        val account = getAccountById(txn.accountId)
        val toAccount = txn.toAccountId?.let { getAccountById(it) }
        val category = txn.categoryId?.let { getCategoryById(it) }
        return TransactionWithDetails(txn, account, toAccount, category)
    }

    fun getTransactionsWithDetails(
        type: TransactionType? = null,
        accountId: Long? = null,
        categoryId: Long? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        searchQuery: String? = null,
        splitGroupId: Long? = null,
        limit: Int = 200
    ): List<TransactionWithDetails> {
        val db = readableDatabase
        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (type != null) {
            whereClauses.add("t.$COL_TXN_TYPE = ?")
            args.add(type.name)
        }
        if (accountId != null && accountId > 0) {
            whereClauses.add("(t.$COL_TXN_ACC_ID = ? OR t.$COL_TXN_TO_ACC_ID = ?)")
            args.add(accountId.toString())
            args.add(accountId.toString())
        }
        if (categoryId != null && categoryId > 0) {
            whereClauses.add("t.$COL_TXN_CAT_ID = ?")
            args.add(categoryId.toString())
        }
        if (startDate != null && startDate > 0) {
            whereClauses.add("t.$COL_TXN_DATE >= ?")
            args.add(startDate.toString())
        }
        if (endDate != null && endDate > 0) {
            whereClauses.add("t.$COL_TXN_DATE <= ?")
            args.add(endDate.toString())
        }
        if (splitGroupId != null && splitGroupId > 0) {
            whereClauses.add("t.$COL_TXN_SPLIT_GROUP_ID = ?")
            args.add(splitGroupId.toString())
        }
        if (!searchQuery.isNullOrBlank()) {
            val q = "%${searchQuery.trim()}%"
            whereClauses.add("(t.$COL_TXN_TITLE LIKE ? OR t.$COL_TXN_PAYEE_NAME LIKE ? OR t.$COL_TXN_PAYEE_UPI LIKE ? OR t.$COL_TXN_NOTE LIKE ?)")
            args.add(q)
            args.add(q)
            args.add(q)
            args.add(q)
        }

        val whereString = if (whereClauses.isNotEmpty()) "WHERE " + whereClauses.joinToString(" AND ") else ""
        val query = """
            SELECT 
                t.*,
                a.$COL_ACC_NAME as acc_name, a.$COL_ACC_TYPE as acc_type, a.$COL_ACC_COLOR as acc_color, a.$COL_ACC_ICON as acc_icon,
                a2.$COL_ACC_NAME as to_acc_name, a2.$COL_ACC_TYPE as to_acc_type, a2.$COL_ACC_COLOR as to_acc_color, a2.$COL_ACC_ICON as to_acc_icon,
                c.$COL_CAT_NAME as cat_name, c.$COL_CAT_TYPE as cat_type, c.$COL_CAT_COLOR as cat_color, c.$COL_CAT_ICON as cat_icon
            FROM $TABLE_TRANSACTIONS t
            LEFT JOIN $TABLE_ACCOUNTS a ON t.$COL_TXN_ACC_ID = a.$COL_ACC_ID
            LEFT JOIN $TABLE_ACCOUNTS a2 ON t.$COL_TXN_TO_ACC_ID = a2.$COL_ACC_ID
            LEFT JOIN $TABLE_CATEGORIES c ON t.$COL_TXN_CAT_ID = c.$COL_CAT_ID
            $whereString
            ORDER BY t.$COL_TXN_DATE DESC
            LIMIT $limit
        """.trimIndent()

        val list = mutableListOf<TransactionWithDetails>()
        val cursor = db.rawQuery(query, args.toTypedArray())
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_TXN_ID)
            val titleIdx = c.getColumnIndexOrThrow(COL_TXN_TITLE)
            val amountIdx = c.getColumnIndexOrThrow(COL_TXN_AMOUNT)
            val typeIdx = c.getColumnIndexOrThrow(COL_TXN_TYPE)
            val accIdIdx = c.getColumnIndexOrThrow(COL_TXN_ACC_ID)
            val toAccIdIdx = c.getColumnIndexOrThrow(COL_TXN_TO_ACC_ID)
            val catIdIdx = c.getColumnIndexOrThrow(COL_TXN_CAT_ID)
            val dateIdx = c.getColumnIndexOrThrow(COL_TXN_DATE)
            val noteIdx = c.getColumnIndexOrThrow(COL_TXN_NOTE)
            val payeeUpiIdx = c.getColumnIndexOrThrow(COL_TXN_PAYEE_UPI)
            val payeeNameIdx = c.getColumnIndexOrThrow(COL_TXN_PAYEE_NAME)
            val upiRefIdx = c.getColumnIndexOrThrow(COL_TXN_UPI_REF)
            val statusIdx = c.getColumnIndexOrThrow(COL_TXN_STATUS)
            val createdIdx = c.getColumnIndexOrThrow(COL_TXN_CREATED_AT)
            val splitGroupIdx = c.getColumnIndexOrThrow(COL_TXN_SPLIT_GROUP_ID)

            val accNameIdx = c.getColumnIndex("acc_name")
            val accTypeIdx = c.getColumnIndex("acc_type")
            val accColorIdx = c.getColumnIndex("acc_color")
            val accIconIdx = c.getColumnIndex("acc_icon")

            val toAccNameIdx = c.getColumnIndex("to_acc_name")
            val toAccTypeIdx = c.getColumnIndex("to_acc_type")
            val toAccColorIdx = c.getColumnIndex("to_acc_color")
            val toAccIconIdx = c.getColumnIndex("to_acc_icon")

            val catNameIdx = c.getColumnIndex("cat_name")
            val catTypeIdx = c.getColumnIndex("cat_type")
            val catColorIdx = c.getColumnIndex("cat_color")
            val catIconIdx = c.getColumnIndex("cat_icon")

            while (c.moveToNext()) {
                val txn = Transaction(
                    id = c.getLong(idIdx),
                    title = c.getString(titleIdx),
                    amount = c.getDouble(amountIdx),
                    type = try { TransactionType.valueOf(c.getString(typeIdx)) } catch (_: Exception) { TransactionType.EXPENSE },
                    accountId = c.getLong(accIdIdx),
                    toAccountId = if (c.isNull(toAccIdIdx)) null else c.getLong(toAccIdIdx),
                    categoryId = if (c.isNull(catIdIdx)) null else c.getLong(catIdIdx),
                    dateEpochMillis = c.getLong(dateIdx),
                    note = c.getString(noteIdx),
                    payeeUpiId = c.getString(payeeUpiIdx),
                    payeeName = c.getString(payeeNameIdx),
                    upiRefId = c.getString(upiRefIdx),
                    paymentStatus = c.getString(statusIdx),
                    createdAt = c.getLong(createdIdx),
                    splitGroupId = if (c.isNull(splitGroupIdx)) null else c.getLong(splitGroupIdx)
                )

                val account = if (accNameIdx != -1 && !c.isNull(accNameIdx)) {
                    Account(
                        id = txn.accountId,
                        name = c.getString(accNameIdx),
                        type = try { AccountType.valueOf(c.getString(accTypeIdx)) } catch (_: Exception) { AccountType.OTHER },
                        colorHex = c.getString(accColorIdx),
                        iconName = c.getString(accIconIdx)
                    )
                } else null

                val toAccount = if (txn.toAccountId != null && toAccNameIdx != -1 && !c.isNull(toAccNameIdx)) {
                    Account(
                        id = txn.toAccountId,
                        name = c.getString(toAccNameIdx),
                        type = try { AccountType.valueOf(c.getString(toAccTypeIdx)) } catch (_: Exception) { AccountType.OTHER },
                        colorHex = c.getString(toAccColorIdx),
                        iconName = c.getString(toAccIconIdx)
                    )
                } else null

                val category = if (txn.categoryId != null && catNameIdx != -1 && !c.isNull(catNameIdx)) {
                    Category(
                        id = txn.categoryId,
                        name = c.getString(catNameIdx),
                        type = try { CategoryType.valueOf(c.getString(catTypeIdx)) } catch (_: Exception) { CategoryType.EXPENSE },
                        colorHex = c.getString(catColorIdx),
                        iconName = c.getString(catIconIdx)
                    )
                } else null

                list.add(TransactionWithDetails(txn, account, toAccount, category))
            }
        }
        return list
    }

    private fun cursorToTransaction(c: Cursor): Transaction {
        return Transaction(
            id = c.getLong(c.getColumnIndexOrThrow(COL_TXN_ID)),
            title = c.getString(c.getColumnIndexOrThrow(COL_TXN_TITLE)),
            amount = c.getDouble(c.getColumnIndexOrThrow(COL_TXN_AMOUNT)),
            type = try { TransactionType.valueOf(c.getString(c.getColumnIndexOrThrow(COL_TXN_TYPE))) } catch (e: Exception) { TransactionType.EXPENSE },
            accountId = c.getLong(c.getColumnIndexOrThrow(COL_TXN_ACC_ID)),
            toAccountId = if (c.isNull(c.getColumnIndexOrThrow(COL_TXN_TO_ACC_ID))) null else c.getLong(c.getColumnIndexOrThrow(COL_TXN_TO_ACC_ID)),
            categoryId = if (c.isNull(c.getColumnIndexOrThrow(COL_TXN_CAT_ID))) null else c.getLong(c.getColumnIndexOrThrow(COL_TXN_CAT_ID)),
            dateEpochMillis = c.getLong(c.getColumnIndexOrThrow(COL_TXN_DATE)),
            note = c.getString(c.getColumnIndexOrThrow(COL_TXN_NOTE)),
            payeeUpiId = c.getString(c.getColumnIndexOrThrow(COL_TXN_PAYEE_UPI)),
            payeeName = c.getString(c.getColumnIndexOrThrow(COL_TXN_PAYEE_NAME)),
            upiRefId = c.getString(c.getColumnIndexOrThrow(COL_TXN_UPI_REF)),
            paymentStatus = c.getString(c.getColumnIndexOrThrow(COL_TXN_STATUS)),
            createdAt = c.getLong(c.getColumnIndexOrThrow(COL_TXN_CREATED_AT)),
            splitGroupId = if (c.isNull(c.getColumnIndexOrThrow(COL_TXN_SPLIT_GROUP_ID))) null else c.getLong(c.getColumnIndexOrThrow(COL_TXN_SPLIT_GROUP_ID))
        )
    }

    // ==========================================
    // REPORT & AGGREGATE OPERATIONS
    // ==========================================

    fun getFinancialOverview(startDate: Long, endDate: Long): FinancialOverview {
        val db = readableDatabase
        var totalIncome = 0.0
        var totalExpense = 0.0
        var count = 0

        val query = """
            SELECT $COL_TXN_TYPE, SUM($COL_TXN_AMOUNT), COUNT(*) 
            FROM $TABLE_TRANSACTIONS 
            WHERE $COL_TXN_DATE >= ? AND $COL_TXN_DATE <= ?
            GROUP BY $COL_TXN_TYPE
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(startDate.toString(), endDate.toString()))
        cursor.use {
            while (it.moveToNext()) {
                val typeStr = it.getString(0)
                val sum = it.getDouble(1)
                val c = it.getInt(2)
                count += c

                if (typeStr == TransactionType.INCOME.name) {
                    totalIncome = sum
                } else if (typeStr == TransactionType.EXPENSE.name) {
                    totalExpense = sum
                }
            }
        }

        val netSavings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0.0) ((netSavings / totalIncome) * 100).toFloat() else 0f
        return FinancialOverview(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = netSavings,
            savingsRatePercentage = savingsRate,
            transactionCount = count
        )
    }

    fun getCategoryExpenseBreakdown(startDate: Long, endDate: Long): List<CategoryExpenseSummary> {
        val db = readableDatabase
        val query = """
            SELECT 
                c.$COL_CAT_ID, c.$COL_CAT_NAME, c.$COL_CAT_COLOR, c.$COL_CAT_ICON,
                SUM(t.$COL_TXN_AMOUNT) as total_amount,
                COUNT(t.$COL_TXN_ID) as txn_count
            FROM $TABLE_TRANSACTIONS t
            INNER JOIN $TABLE_CATEGORIES c ON t.$COL_TXN_CAT_ID = c.$COL_CAT_ID
            WHERE t.$COL_TXN_TYPE = ? AND t.$COL_TXN_DATE >= ? AND t.$COL_TXN_DATE <= ?
            GROUP BY c.$COL_CAT_ID
            ORDER BY total_amount DESC
        """.trimIndent()

        var grandTotal = 0.0
        val temp = mutableListOf<Triple<Category, Double, Int>>()
        val cursor = db.rawQuery(query, arrayOf(TransactionType.EXPENSE.name, startDate.toString(), endDate.toString()))
        cursor.use {
            while (it.moveToNext()) {
                val cat = Category(
                    id = it.getLong(0),
                    name = it.getString(1),
                    colorHex = it.getString(2),
                    iconName = it.getString(3)
                )
                val amount = it.getDouble(4)
                val count = it.getInt(5)
                grandTotal += amount
                temp.add(Triple(cat, amount, count))
            }
        }

        return temp.map { (cat, amount, count) ->
            val pct = if (grandTotal > 0.0) ((amount / grandTotal) * 100).toFloat() else 0f
            CategoryExpenseSummary(
                category = cat,
                totalAmount = amount,
                percentage = pct,
                count = count
            )
        }
    }

    fun getAccountExpenseBreakdown(startDate: Long, endDate: Long): List<AccountExpenseSummary> {
        val db = readableDatabase
        val query = """
            SELECT 
                a.$COL_ACC_ID, a.$COL_ACC_NAME, a.$COL_ACC_TYPE, a.$COL_ACC_COLOR, a.$COL_ACC_ICON,
                SUM(t.$COL_TXN_AMOUNT) as total_amount,
                COUNT(t.$COL_TXN_ID) as txn_count
            FROM $TABLE_TRANSACTIONS t
            INNER JOIN $TABLE_ACCOUNTS a ON t.$COL_TXN_ACC_ID = a.$COL_ACC_ID
            WHERE t.$COL_TXN_TYPE = ? AND t.$COL_TXN_DATE >= ? AND t.$COL_TXN_DATE <= ?
            GROUP BY a.$COL_ACC_ID
            ORDER BY total_amount DESC
        """.trimIndent()

        var grandTotal = 0.0
        val temp = mutableListOf<Triple<Account, Double, Int>>()
        val cursor = db.rawQuery(query, arrayOf(TransactionType.EXPENSE.name, startDate.toString(), endDate.toString()))
        cursor.use {
            while (it.moveToNext()) {
                val acc = Account(
                    id = it.getLong(0),
                    name = it.getString(1),
                    type = try { AccountType.valueOf(it.getString(2)) } catch (e: Exception) { AccountType.OTHER },
                    colorHex = it.getString(3),
                    iconName = it.getString(4)
                )
                val amount = it.getDouble(5)
                val count = it.getInt(6)
                grandTotal += amount
                temp.add(Triple(acc, amount, count))
            }
        }

        return temp.map { (acc, amount, count) ->
            val pct = if (grandTotal > 0.0) ((amount / grandTotal) * 100).toFloat() else 0f
            AccountExpenseSummary(
                account = acc,
                totalAmount = amount,
                percentage = pct,
                count = count
            )
        }
    }

    // ==========================================
    // SPLIT GROUP OPERATIONS
    // ==========================================

    fun getOrCreateSplitCategory(): Long {
        val db = writableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_CAT_ID FROM $TABLE_CATEGORIES WHERE $COL_CAT_NAME = ? OR $COL_CAT_NAME = ? LIMIT 1",
            arrayOf("Split", "Split Collection")
        )
        cursor.use {
            if (it.moveToFirst()) {
                val catId = it.getLong(0)
                val cv = ContentValues().apply {
                    put(COL_CAT_NAME, "Split")
                    put(COL_CAT_TYPE, CategoryType.BOTH.name)
                }
                db.update(TABLE_CATEGORIES, cv, "$COL_CAT_ID = ?", arrayOf(catId.toString()))
                return catId
            }
        }
        val cv = ContentValues().apply {
            put(COL_CAT_NAME, "Split")
            put(COL_CAT_TYPE, CategoryType.BOTH.name)
            put(COL_CAT_COLOR, "#00897B")
            put(COL_CAT_ICON, "CallSplit")
            put(COL_CAT_IS_DEFAULT, 0)
        }
        return db.insert(TABLE_CATEGORIES, null, cv)
    }

    fun getOrCreateSplitCollectionCategory(): Long = getOrCreateSplitCategory()

    fun createSplitExpense(
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
    ): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val receivable = maxOf(0.0, totalAmount - myShare)
            val splitCatId = getOrCreateSplitCategory()

            var myShareTxnId: Long? = null
            var othersShareTxnId: Long? = null

            // 1. My share expense transaction (in user's selected category)
            if (myShare > 0.0) {
                val myTxn = Transaction(
                    title = title,
                    amount = myShare,
                    type = TransactionType.EXPENSE,
                    accountId = accountId,
                    categoryId = categoryId,
                    dateEpochMillis = dateEpochMillis,
                    note = note?.trim()?.ifBlank { null },
                    payeeUpiId = payeeUpiId?.trim()?.ifBlank { null },
                    payeeName = payeeName?.trim()?.ifBlank { null },
                    upiRefId = upiRefId?.trim()?.ifBlank { null }
                )
                myShareTxnId = insertTransactionInternal(db, myTxn)
            }

            // 2. Others' share expense transaction (in Split category)
            if (receivable > 0.0) {
                val othersTitle = if (title.startsWith("Split: ")) title else "Split: $title"
                val othersNote = if (!note.isNullOrBlank()) "Split share for $title. ${note.trim()}" else "Split share for $title"
                val othersTxn = Transaction(
                    title = othersTitle,
                    amount = receivable,
                    type = TransactionType.EXPENSE,
                    accountId = accountId,
                    categoryId = splitCatId,
                    dateEpochMillis = dateEpochMillis,
                    note = othersNote,
                    payeeUpiId = payeeUpiId?.trim()?.ifBlank { null },
                    payeeName = payeeName?.trim()?.ifBlank { null },
                    upiRefId = upiRefId?.trim()?.ifBlank { null }
                )
                othersShareTxnId = insertTransactionInternal(db, othersTxn)
            }

            // Primary expense txn for SplitGroup reference
            val primaryTxnId = othersShareTxnId ?: myShareTxnId ?: 0L

            // 3. Create SplitGroup
            val splitGroup = SplitGroup(
                description = title,
                totalAmount = totalAmount,
                myShare = myShare,
                receivableAmount = receivable,
                expenseTransactionId = primaryTxnId,
                createdAt = dateEpochMillis,
                status = if (receivable <= 0.0) SplitStatus.SETTLED else SplitStatus.OPEN
            )

            val cv = ContentValues().apply {
                put(COL_SPLIT_DESC, splitGroup.description)
                put(COL_SPLIT_TOTAL, splitGroup.totalAmount)
                put(COL_SPLIT_MY_SHARE, splitGroup.myShare)
                put(COL_SPLIT_RECEIVABLE, splitGroup.receivableAmount)
                put(COL_SPLIT_EXPENSE_TXN_ID, splitGroup.expenseTransactionId)
                put(COL_SPLIT_CREATED_AT, splitGroup.createdAt)
                put(COL_SPLIT_STATUS, splitGroup.status.name)
            }
            val splitGroupId = db.insert(TABLE_SPLIT_GROUPS, null, cv)

            // 4. Link only the split expense transaction (others' share) to this split group.
            // My share's expense is intentionally NOT tagged with split tag as requested.
            if (splitGroupId != -1L) {
                if (othersShareTxnId != null) {
                    db.execSQL(
                        "UPDATE $TABLE_TRANSACTIONS SET $COL_TXN_SPLIT_GROUP_ID = ? WHERE $COL_TXN_ID = ?",
                        arrayOf(splitGroupId.toString(), othersShareTxnId.toString())
                    )
                }
            }

            db.setTransactionSuccessful()
            return splitGroupId
        } finally {
            db.endTransaction()
        }
    }

    fun updateSplitExpense(
        splitGroupId: Long,
        title: String,
        totalAmount: Double,
        myShare: Double,
        accountId: Long,
        categoryId: Long,
        dateEpochMillis: Long,
        note: String? = null
    ): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val splitGroup = getSplitGroupById(splitGroupId) ?: return false
            val splitCatId = getOrCreateSplitCategory()
            val receivable = maxOf(0.0, totalAmount - myShare)

            val existingExpenses = getTransactionsWithDetails(
                type = TransactionType.EXPENSE,
                splitGroupId = splitGroupId,
                limit = 100
            )

            // Revert balances of existing expense transactions in this split
            for (item in existingExpenses) {
                db.execSQL(
                    "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?",
                    arrayOf(item.transaction.amount.toString(), item.transaction.accountId.toString())
                )
            }

            val myExisting = existingExpenses.firstOrNull { it.transaction.categoryId != splitCatId }
                ?: existingExpenses.firstOrNull { it.transaction.id != splitGroup.expenseTransactionId }
            val othersExisting = existingExpenses.firstOrNull { it.transaction.categoryId == splitCatId }
                ?: existingExpenses.firstOrNull { it.transaction.id == splitGroup.expenseTransactionId }

            var myTxnId: Long? = myExisting?.transaction?.id
            var othersTxnId: Long? = othersExisting?.transaction?.id

            // Update or create my share transaction
            if (myShare > 0.0) {
                if (myExisting != null) {
                    val cv = ContentValues().apply {
                        put(COL_TXN_TITLE, title)
                        put(COL_TXN_AMOUNT, myShare)
                        put(COL_TXN_ACC_ID, accountId)
                        put(COL_TXN_CAT_ID, categoryId)
                        put(COL_TXN_DATE, dateEpochMillis)
                        put(COL_TXN_NOTE, note)
                        putNull(COL_TXN_SPLIT_GROUP_ID)
                    }
                    db.update(TABLE_TRANSACTIONS, cv, "$COL_TXN_ID = ?", arrayOf(myExisting.transaction.id.toString()))
                    db.execSQL(
                        "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?",
                        arrayOf(myShare.toString(), accountId.toString())
                    )
                } else {
                    val myTxn = Transaction(
                        title = title,
                        amount = myShare,
                        type = TransactionType.EXPENSE,
                        accountId = accountId,
                        categoryId = categoryId,
                        dateEpochMillis = dateEpochMillis,
                        note = note,
                        splitGroupId = null
                    )
                    myTxnId = insertTransactionInternal(db, myTxn)
                }
            } else if (myExisting != null) {
                db.delete(TABLE_TRANSACTIONS, "$COL_TXN_ID = ?", arrayOf(myExisting.transaction.id.toString()))
                myTxnId = null
            }

            // Update or create others' share transaction
            val othersTitle = if (title.startsWith("Split: ")) title else "Split: $title"
            val othersNote = if (!note.isNullOrBlank()) "Split share for $title. ${note.trim()}" else "Split share for $title"
            if (receivable > 0.0) {
                if (othersExisting != null) {
                    val cv = ContentValues().apply {
                        put(COL_TXN_TITLE, othersTitle)
                        put(COL_TXN_AMOUNT, receivable)
                        put(COL_TXN_ACC_ID, accountId)
                        put(COL_TXN_CAT_ID, splitCatId)
                        put(COL_TXN_DATE, dateEpochMillis)
                        put(COL_TXN_NOTE, othersNote)
                    }
                    db.update(TABLE_TRANSACTIONS, cv, "$COL_TXN_ID = ?", arrayOf(othersExisting.transaction.id.toString()))
                    db.execSQL(
                        "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?",
                        arrayOf(receivable.toString(), accountId.toString())
                    )
                } else {
                    val othersTxn = Transaction(
                        title = othersTitle,
                        amount = receivable,
                        type = TransactionType.EXPENSE,
                        accountId = accountId,
                        categoryId = splitCatId,
                        dateEpochMillis = dateEpochMillis,
                        note = othersNote,
                        splitGroupId = splitGroupId
                    )
                    othersTxnId = insertTransactionInternal(db, othersTxn)
                }
            } else if (othersExisting != null) {
                db.delete(TABLE_TRANSACTIONS, "$COL_TXN_ID = ?", arrayOf(othersExisting.transaction.id.toString()))
                othersTxnId = null
            }

            // Re-evaluate settlement status
            val sumCursor = db.rawQuery(
                "SELECT SUM($COL_TXN_AMOUNT) FROM $TABLE_TRANSACTIONS WHERE $COL_TXN_SPLIT_GROUP_ID = ? AND $COL_TXN_TYPE = 'INCOME'",
                arrayOf(splitGroupId.toString())
            )
            var totalCollected = 0.0
            sumCursor.use {
                if (it.moveToFirst() && !it.isNull(0)) totalCollected = it.getDouble(0)
            }
            val status = if (totalCollected >= receivable - 0.01) SplitStatus.SETTLED else SplitStatus.OPEN

            val primaryTxnId = othersTxnId ?: myTxnId ?: splitGroup.expenseTransactionId
            val cvSplit = ContentValues().apply {
                put(COL_SPLIT_DESC, title)
                put(COL_SPLIT_TOTAL, totalAmount)
                put(COL_SPLIT_MY_SHARE, myShare)
                put(COL_SPLIT_RECEIVABLE, receivable)
                put(COL_SPLIT_EXPENSE_TXN_ID, primaryTxnId)
                put(COL_SPLIT_STATUS, status.name)
            }
            db.update(TABLE_SPLIT_GROUPS, cvSplit, "$COL_SPLIT_ID = ?", arrayOf(splitGroupId.toString()))

            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    fun splitExistingExpense(
        transactionId: Long,
        myShare: Double,
        receivableAmount: Double
    ): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val oldTxn = getTransactionById(transactionId) ?: return -1L
            val splitCatId = getOrCreateSplitCategory()
            val totalAmount = oldTxn.amount

            // 1. Update existing transaction to be "my share" (user's selected category)
            db.execSQL(
                "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?",
                arrayOf(oldTxn.amount.toString(), oldTxn.accountId.toString())
            )
            db.execSQL(
                "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?",
                arrayOf(myShare.toString(), oldTxn.accountId.toString())
            )

            val cvMy = ContentValues().apply {
                put(COL_TXN_AMOUNT, myShare)
                putNull(COL_TXN_SPLIT_GROUP_ID)
            }
            db.update(TABLE_TRANSACTIONS, cvMy, "$COL_TXN_ID = ?", arrayOf(transactionId.toString()))

            // 2. Insert others' share transaction in Split category
            val othersTitle = if (oldTxn.title.startsWith("Split: ")) oldTxn.title else "Split: ${oldTxn.title}"
            val othersNote = if (!oldTxn.note.isNullOrBlank()) "Split share for ${oldTxn.title}. ${oldTxn.note}" else "Split share for ${oldTxn.title}"
            val othersTxn = Transaction(
                title = othersTitle,
                amount = receivableAmount,
                type = TransactionType.EXPENSE,
                accountId = oldTxn.accountId,
                categoryId = splitCatId,
                dateEpochMillis = oldTxn.dateEpochMillis,
                note = othersNote,
                payeeUpiId = oldTxn.payeeUpiId,
                payeeName = oldTxn.payeeName,
                upiRefId = oldTxn.upiRefId
            )
            val othersTxnId = insertTransactionInternal(db, othersTxn)

            // 3. Create SplitGroup
            val splitGroup = SplitGroup(
                description = oldTxn.title,
                totalAmount = totalAmount,
                myShare = myShare,
                receivableAmount = receivableAmount,
                expenseTransactionId = othersTxnId,
                createdAt = oldTxn.dateEpochMillis,
                status = if (receivableAmount <= 0.0) SplitStatus.SETTLED else SplitStatus.OPEN
            )

            val cv = ContentValues().apply {
                put(COL_SPLIT_DESC, splitGroup.description)
                put(COL_SPLIT_TOTAL, splitGroup.totalAmount)
                put(COL_SPLIT_MY_SHARE, splitGroup.myShare)
                put(COL_SPLIT_RECEIVABLE, splitGroup.receivableAmount)
                put(COL_SPLIT_EXPENSE_TXN_ID, splitGroup.expenseTransactionId)
                put(COL_SPLIT_CREATED_AT, splitGroup.createdAt)
                put(COL_SPLIT_STATUS, splitGroup.status.name)
            }
            val splitGroupId = db.insert(TABLE_SPLIT_GROUPS, null, cv)

            // 4. Link only the split expense transaction (others' share) to this split group.
            // My share's expense is intentionally NOT tagged with split tag as requested.
            if (splitGroupId != -1L) {
                db.execSQL(
                    "UPDATE $TABLE_TRANSACTIONS SET $COL_TXN_SPLIT_GROUP_ID = ? WHERE $COL_TXN_ID = ?",
                    arrayOf(splitGroupId.toString(), othersTxnId.toString())
                )
            }

            db.setTransactionSuccessful()
            return splitGroupId
        } finally {
            db.endTransaction()
        }
    }

    fun removeSplitFromExpense(
        splitGroupId: Long,
        keepTransactionId: Long,
        totalAmount: Double
    ): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val existingExpenses = getTransactionsWithDetails(
                type = TransactionType.EXPENSE,
                splitGroupId = splitGroupId,
                limit = 100
            )

            // Revert balances of all existing expense transactions in this split
            for (item in existingExpenses) {
                db.execSQL(
                    "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?",
                    arrayOf(item.transaction.amount.toString(), item.transaction.accountId.toString())
                )
            }

            // Delete secondary transactions (the ones other than keepTransactionId)
            for (item in existingExpenses) {
                if (item.transaction.id != keepTransactionId) {
                    db.delete(TABLE_TRANSACTIONS, "$COL_TXN_ID = ?", arrayOf(item.transaction.id.toString()))
                }
            }

            // Update keepTransactionId to full totalAmount and null splitGroupId
            val targetTxn = getTransactionById(keepTransactionId)
            if (targetTxn != null) {
                val cv = ContentValues().apply {
                    put(COL_TXN_AMOUNT, totalAmount)
                    putNull(COL_TXN_SPLIT_GROUP_ID)
                }
                db.update(TABLE_TRANSACTIONS, cv, "$COL_TXN_ID = ?", arrayOf(keepTransactionId.toString()))
                db.execSQL(
                    "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE - ? WHERE $COL_ACC_ID = ?",
                    arrayOf(totalAmount.toString(), targetTxn.accountId.toString())
                )
            }

            // Delete the split group
            db.delete(TABLE_SPLIT_GROUPS, "$COL_SPLIT_ID = ?", arrayOf(splitGroupId.toString()))

            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    fun insertSplitGroup(splitGroup: SplitGroup): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put(COL_SPLIT_DESC, splitGroup.description)
                put(COL_SPLIT_TOTAL, splitGroup.totalAmount)
                put(COL_SPLIT_MY_SHARE, splitGroup.myShare)
                put(COL_SPLIT_RECEIVABLE, splitGroup.receivableAmount)
                put(COL_SPLIT_EXPENSE_TXN_ID, splitGroup.expenseTransactionId)
                put(COL_SPLIT_CREATED_AT, splitGroup.createdAt)
                put(COL_SPLIT_STATUS, splitGroup.status.name)
            }
            val id = db.insert(TABLE_SPLIT_GROUPS, null, cv)
            if (id != -1L) {
                // Link expense transaction to this split group as well
                db.execSQL(
                    "UPDATE $TABLE_TRANSACTIONS SET $COL_TXN_SPLIT_GROUP_ID = ? WHERE $COL_TXN_ID = ?",
                    arrayOf(id.toString(), splitGroup.expenseTransactionId.toString())
                )
            }
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    fun updateSplitGroup(splitGroup: SplitGroup): Int {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_SPLIT_DESC, splitGroup.description)
            put(COL_SPLIT_TOTAL, splitGroup.totalAmount)
            put(COL_SPLIT_MY_SHARE, splitGroup.myShare)
            put(COL_SPLIT_RECEIVABLE, splitGroup.receivableAmount)
            put(COL_SPLIT_STATUS, splitGroup.status.name)
        }
        return db.update(TABLE_SPLIT_GROUPS, cv, "$COL_SPLIT_ID = ?", arrayOf(splitGroup.id.toString()))
    }

    fun deleteSplitGroup(id: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Unlink all transactions associated with this split group
            db.execSQL("UPDATE $TABLE_TRANSACTIONS SET $COL_TXN_SPLIT_GROUP_ID = NULL WHERE $COL_TXN_SPLIT_GROUP_ID = ?", arrayOf(id.toString()))
            val rows = db.delete(TABLE_SPLIT_GROUPS, "$COL_SPLIT_ID = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
            return rows > 0
        } finally {
            db.endTransaction()
        }
    }

    fun settleSplitGroup(id: Long): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply { put(COL_SPLIT_STATUS, SplitStatus.SETTLED.name) }
        val rows = db.update(TABLE_SPLIT_GROUPS, cv, "$COL_SPLIT_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    fun reopenSplitGroup(id: Long): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply { put(COL_SPLIT_STATUS, SplitStatus.OPEN.name) }
        val rows = db.update(TABLE_SPLIT_GROUPS, cv, "$COL_SPLIT_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    fun getSplitGroupById(id: Long): SplitGroup? {
        val db = readableDatabase
        val cursor = db.query(TABLE_SPLIT_GROUPS, null, "$COL_SPLIT_ID = ?", arrayOf(id.toString()), null, null, null)
        return cursor.use {
            if (it.moveToFirst()) cursorToSplitGroup(it) else null
        }
    }

    fun getSplitGroupByExpenseId(expenseTxnId: Long): SplitGroup? {
        val db = readableDatabase
        val cursor = db.query(TABLE_SPLIT_GROUPS, null, "$COL_SPLIT_EXPENSE_TXN_ID = ?", arrayOf(expenseTxnId.toString()), null, null, null)
        return cursor.use {
            if (it.moveToFirst()) cursorToSplitGroup(it) else null
        }
    }

    fun getAllSplitGroups(): List<SplitGroup> {
        val list = mutableListOf<SplitGroup>()
        val db = readableDatabase
        val cursor = db.query(TABLE_SPLIT_GROUPS, null, null, null, null, null, "$COL_SPLIT_CREATED_AT DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToSplitGroup(it))
            }
        }
        return list
    }

    fun getExpenseTransactionsForSplit(splitGroupId: Long): List<TransactionWithDetails> {
        return getTransactionsWithDetails(
            type = TransactionType.EXPENSE,
            splitGroupId = splitGroupId,
            limit = 100
        )
    }

    fun getSplitGroupWithDetailsById(id: Long): SplitGroupWithDetails? {
        val splitGroup = getSplitGroupById(id) ?: return null
        val expenseTxn = getTransactionWithDetailsById(splitGroup.expenseTransactionId)
        val settlements = getSettlementTransactions(splitGroup.id)
        val totalReceived = settlements.sumOf { it.transaction.amount }
        val remaining = maxOf(0.0, splitGroup.receivableAmount - totalReceived)
        return SplitGroupWithDetails(
            splitGroup = splitGroup,
            expenseTransaction = expenseTxn,
            settlementTransactions = settlements,
            totalReceived = totalReceived,
            remainingAmount = remaining
        )
    }

    fun getAllSplitGroupsWithDetails(): List<SplitGroupWithDetails> {
        val splits = getAllSplitGroups()
        return splits.map { splitGroup ->
            val expenseTxn = getTransactionWithDetailsById(splitGroup.expenseTransactionId)
            val settlements = getSettlementTransactions(splitGroup.id)
            val totalReceived = settlements.sumOf { it.transaction.amount }
            val remaining = maxOf(0.0, splitGroup.receivableAmount - totalReceived)
            SplitGroupWithDetails(
                splitGroup = splitGroup,
                expenseTransaction = expenseTxn,
                settlementTransactions = settlements,
                totalReceived = totalReceived,
                remainingAmount = remaining
            )
        }
    }

    fun getSettlementTransactions(splitGroupId: Long): List<TransactionWithDetails> {
        return getTransactionsWithDetails(
            type = TransactionType.INCOME,
            splitGroupId = splitGroupId,
            limit = 1000
        )
    }

    fun recordSplitSettlementBatch(splitGroupId: Long, settlements: List<SplitSettlementEntry>): Boolean {
        if (settlements.isEmpty()) return false
        val db = writableDatabase
        db.beginTransaction()
        try {
            val splitGroup = getSplitGroupById(splitGroupId) ?: return false
            val splitCatId = getOrCreateSplitCollectionCategory()

            for (entry in settlements) {
                if (entry.amount <= 0.0) continue
                val noteText = entry.note?.trim()?.ifBlank { null } ?: "Split collection for ${splitGroup.description}"
                val txnTitle = if (!entry.note.isNullOrBlank()) "Split: ${entry.note}" else "Split: ${splitGroup.description}"

                val txn = Transaction(
                    title = txnTitle,
                    amount = entry.amount,
                    type = TransactionType.INCOME,
                    accountId = entry.accountId,
                    categoryId = splitCatId,
                    dateEpochMillis = entry.dateEpochMillis,
                    note = noteText,
                    splitGroupId = splitGroupId,
                    paymentStatus = "COMPLETED"
                )

                val cv = ContentValues().apply {
                    put(COL_TXN_TITLE, txn.title)
                    put(COL_TXN_AMOUNT, txn.amount)
                    put(COL_TXN_TYPE, txn.type.name)
                    put(COL_TXN_ACC_ID, txn.accountId)
                    putNull(COL_TXN_TO_ACC_ID)
                    if (txn.categoryId != null) put(COL_TXN_CAT_ID, txn.categoryId) else putNull(COL_TXN_CAT_ID)
                    put(COL_TXN_DATE, txn.dateEpochMillis)
                    put(COL_TXN_NOTE, txn.note)
                    put(COL_TXN_PAYEE_UPI, txn.payeeUpiId)
                    put(COL_TXN_PAYEE_NAME, txn.payeeName)
                    put(COL_TXN_UPI_REF, txn.upiRefId)
                    put(COL_TXN_STATUS, txn.paymentStatus)
                    put(COL_TXN_CREATED_AT, txn.createdAt)
                    put(COL_TXN_SPLIT_GROUP_ID, splitGroupId)
                }
                val insertedId = db.insert(TABLE_TRANSACTIONS, null, cv)
                if (insertedId != -1L) {
                    // Update account balance
                    db.execSQL(
                        "UPDATE $TABLE_ACCOUNTS SET $COL_ACC_CURR_BALANCE = $COL_ACC_CURR_BALANCE + ? WHERE $COL_ACC_ID = ?",
                        arrayOf(entry.amount.toString(), entry.accountId.toString())
                    )
                }
            }

            // Check if split is now fully collected
            val sumCursor = db.rawQuery(
                "SELECT SUM($COL_TXN_AMOUNT) FROM $TABLE_TRANSACTIONS WHERE $COL_TXN_SPLIT_GROUP_ID = ? AND $COL_TXN_TYPE = 'INCOME'",
                arrayOf(splitGroupId.toString())
            )
            var totalCollected = 0.0
            sumCursor.use {
                if (it.moveToFirst() && !it.isNull(0)) {
                    totalCollected = it.getDouble(0)
                }
            }

            if (totalCollected >= splitGroup.receivableAmount - 0.01) {
                val cvStatus = ContentValues().apply {
                    put(COL_SPLIT_STATUS, SplitStatus.SETTLED.name)
                }
                db.update(TABLE_SPLIT_GROUPS, cvStatus, "$COL_SPLIT_ID = ?", arrayOf(splitGroupId.toString()))
            }

            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    private fun cursorToSplitGroup(c: Cursor): SplitGroup {
        return SplitGroup(
            id = c.getLong(c.getColumnIndexOrThrow(COL_SPLIT_ID)),
            description = c.getString(c.getColumnIndexOrThrow(COL_SPLIT_DESC)),
            totalAmount = c.getDouble(c.getColumnIndexOrThrow(COL_SPLIT_TOTAL)),
            myShare = c.getDouble(c.getColumnIndexOrThrow(COL_SPLIT_MY_SHARE)),
            receivableAmount = c.getDouble(c.getColumnIndexOrThrow(COL_SPLIT_RECEIVABLE)),
            expenseTransactionId = c.getLong(c.getColumnIndexOrThrow(COL_SPLIT_EXPENSE_TXN_ID)),
            createdAt = c.getLong(c.getColumnIndexOrThrow(COL_SPLIT_CREATED_AT)),
            status = try {
                SplitStatus.valueOf(c.getString(c.getColumnIndexOrThrow(COL_SPLIT_STATUS)))
            } catch (e: Exception) {
                SplitStatus.OPEN
            }
        )
    }
}
