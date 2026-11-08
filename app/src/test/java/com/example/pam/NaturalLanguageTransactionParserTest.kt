package com.example.pam

import com.example.pam.data.model.Account
import com.example.pam.data.model.AccountType
import com.example.pam.data.model.Category
import com.example.pam.data.model.CategoryType
import com.example.pam.data.model.TransactionType
import com.example.pam.util.ai.NaturalLanguageTransactionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NaturalLanguageTransactionParserTest {

    private lateinit var accounts: List<Account>
    private lateinit var categories: List<Category>

    @Before
    fun setUp() {
        accounts = listOf(
            Account(id = 1, name = "HDFC Bank", type = AccountType.BANK, isDefault = true),
            Account(id = 2, name = "Hand Cash", type = AccountType.CASH),
            Account(id = 3, name = "ICICI Amazon Card", type = AccountType.CREDIT_CARD),
            Account(id = 4, name = "Emergency Savings", type = AccountType.SAVINGS)
        )

        categories = listOf(
            Category(id = 1, name = "Food & Dining", type = CategoryType.EXPENSE),
            Category(id = 2, name = "Groceries", type = CategoryType.EXPENSE),
            Category(id = 3, name = "Transportation", type = CategoryType.EXPENSE),
            Category(id = 4, name = "Bills & Utilities", type = CategoryType.EXPENSE),
            Category(id = 5, name = "Shopping", type = CategoryType.EXPENSE),
            Category(id = 6, name = "Salary", type = CategoryType.INCOME),
            Category(id = 7, name = "General", type = CategoryType.BOTH, isDefault = true)
        )
    }

    @Test
    fun testParseSimpleExpenseWithAmountAndCategory() {
        val input = "Coffee 180"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(180.0, parsed.amount)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("Food & Dining", parsed.matchedCategory?.name)
        assertEquals("HDFC Bank", parsed.matchedAccount?.name) // default
        assertEquals("Coffee", parsed.title)
    }

    @Test
    fun testParseExpenseWithSpecificAccountAndSplit() {
        val input = "Dinner at Toit 1850 cash split with rahul"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(1850.0, parsed.amount)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("Food & Dining", parsed.matchedCategory?.name)
        assertEquals("Hand Cash", parsed.matchedAccount?.name)
        assertTrue(parsed.isSplit)
        assertEquals("rahul", parsed.splitWith?.lowercase())
        assertTrue(parsed.title.contains("Toit", ignoreCase = true))
    }

    @Test
    fun testParseAmountWithKMultiplier() {
        val input = "Shopping at Zara 2.5k credit card"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(2500.0, parsed.amount)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("Shopping", parsed.matchedCategory?.name)
        assertEquals("ICICI Amazon Card", parsed.matchedAccount?.name)
    }

    @Test
    fun testParseSalaryIncome() {
        val input = "Salary received 85000 in bank"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(85000.0, parsed.amount)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals("Salary", parsed.matchedCategory?.name)
        assertEquals("HDFC Bank", parsed.matchedAccount?.name)
    }

    @Test
    fun testParseTransferBetweenAccounts() {
        val input = "Transfer 10000 from HDFC Bank to Emergency Savings"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(10000.0, parsed.amount)
        assertEquals(TransactionType.TRANSFER, parsed.type)
        assertEquals("HDFC Bank", parsed.matchedAccount?.name)
        assertEquals("Emergency Savings", parsed.matchedToAccount?.name)
    }

    @Test
    fun testParseSplitBetweenNPeople() {
        val input = "Cab to airport 600 split between 3"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(600.0, parsed.amount)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("Transportation", parsed.matchedCategory?.name)
        assertTrue(parsed.isSplit)
        assertEquals(3, parsed.splitPeopleCount)
    }

    @Test
    fun testParseGroceryBlinkit() {
        val input = "Blinkit veggies ₹450"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(450.0, parsed.amount)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("Groceries", parsed.matchedCategory?.name)
    }

    @Test
    fun testParseAmountOnly() {
        val input = "500"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(500.0, parsed.amount)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("General", parsed.title)
    }

    @Test
    fun testParseRsPrefix() {
        val input = "rs 120 chai"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(120.0, parsed.amount)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("Food & Dining", parsed.matchedCategory?.name)
    }

    @Test
    fun testParseSplitWithoutName() {
        val input = "Dinner 1200 split"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(1200.0, parsed.amount)
        assertTrue(parsed.isSplit)
        assertEquals(2, parsed.splitPeopleCount)
    }

    @Test
    fun testParseConversationalSpeechInput() {
        val input = "I spent 150 rupees for coffee at Starbucks"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(150.0, parsed.amount)
        assertEquals("Coffee at Starbucks", parsed.title)
        assertEquals("Food & Dining", parsed.matchedCategory?.name)
        assertEquals(input, parsed.note)
    }

    @Test
    fun testParseConversationalSpeechPaidForLunch() {
        val input = "Paid 300 for lunch with friends"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(300.0, parsed.amount)
        assertEquals("Lunch", parsed.title)
        assertEquals("Food & Dining", parsed.matchedCategory?.name)
    }

    @Test
    fun testParseConversationalGroceriesWithCreditCard() {
        val input = "I bought groceries for 450 using credit card"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(450.0, parsed.amount)
        assertEquals("Groceries", parsed.title)
        assertEquals("ICICI Amazon Card", parsed.matchedAccount?.name)
    }

    @Test
    fun testParseDosaFor200MatchesFoodCategory() {
        // Custom user category called "Food" (instead of standard "Food & Dining")
        val customCategories = listOf(
            Category(id = 10, name = "Food", type = CategoryType.EXPENSE),
            Category(id = 11, name = "Travel", type = CategoryType.EXPENSE),
            Category(id = 12, name = "General", type = CategoryType.EXPENSE, isDefault = true)
        )

        val input = "Dosa for 200"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, customCategories)

        assertEquals(200.0, parsed.amount)
        assertEquals("Dosa", parsed.title)
        assertEquals("Food", parsed.matchedCategory?.name)
    }

    @Test
    fun testParseDecidesBetweenFoodAndSnacks_ChildSelected() {
        val categories = listOf(
            Category(id = 10, name = "Food", type = CategoryType.EXPENSE),
            Category(id = 11, name = "Snacks", type = CategoryType.EXPENSE),
            Category(id = 12, name = "General", type = CategoryType.EXPENSE, isDefault = true)
        )

        val input = "Samosa for 40"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(40.0, parsed.amount)
        assertEquals("Samosa", parsed.title)
        assertEquals("Snacks", parsed.matchedCategory?.name)
    }

    @Test
    fun testParseDecidesBetweenFoodAndSnacks_ParentSelected() {
        val categories = listOf(
            Category(id = 10, name = "Food", type = CategoryType.EXPENSE),
            Category(id = 11, name = "Snacks", type = CategoryType.EXPENSE),
            Category(id = 12, name = "General", type = CategoryType.EXPENSE, isDefault = true)
        )

        val input = "Dosa for 200"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(200.0, parsed.amount)
        assertEquals("Dosa", parsed.title)
        assertEquals("Food", parsed.matchedCategory?.name)
    }

    @Test
    fun testParseSnackFallsBackToFoodWhenNoSnacksCategory() {
        val categories = listOf(
            Category(id = 10, name = "Food", type = CategoryType.EXPENSE),
            Category(id = 12, name = "General", type = CategoryType.EXPENSE, isDefault = true)
        )

        val input = "Samosa for 40"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(40.0, parsed.amount)
        assertEquals("Samosa", parsed.title)
        assertEquals("Food", parsed.matchedCategory?.name)
    }

    @Test
    fun testParseTransportationAndFuel_ChildSelected() {
        val categories = listOf(
            Category(id = 20, name = "Transportation", type = CategoryType.EXPENSE),
            Category(id = 21, name = "Fuel", type = CategoryType.EXPENSE),
            Category(id = 22, name = "General", type = CategoryType.EXPENSE, isDefault = true)
        )

        val input = "Petrol 500"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(500.0, parsed.amount)
        assertEquals("Fuel", parsed.matchedCategory?.name)
    }

    @Test
    fun testCategoryNameSpokenInSpeechIsStrippedFromTitle_Trailing() {
        val categories = listOf(
            Category(id = 10, name = "Food", type = CategoryType.EXPENSE),
            Category(id = 11, name = "General", type = CategoryType.EXPENSE, isDefault = true)
        )

        val input = "Dosa for 200 food"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(200.0, parsed.amount)
        assertEquals("Dosa", parsed.title) // "food" stripped from title!
        assertEquals("Food", parsed.matchedCategory?.name)
    }

    @Test
    fun testCategoryNameSpokenInSpeechIsStrippedFromTitle_InPreposition() {
        val categories = listOf(
            Category(id = 10, name = "Food", type = CategoryType.EXPENSE),
            Category(id = 11, name = "General", type = CategoryType.EXPENSE, isDefault = true)
        )

        val input = "Dosa for 200 in food"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(200.0, parsed.amount)
        assertEquals("Dosa", parsed.title) // "in food" stripped from title!
        assertEquals("Food", parsed.matchedCategory?.name)
    }

    @Test
    fun testCategoryNameSpokenInSpeechIsStrippedFromTitle_UnderCategory() {
        val categories = listOf(
            Category(id = 10, name = "Food", type = CategoryType.EXPENSE),
            Category(id = 11, name = "General", type = CategoryType.EXPENSE, isDefault = true)
        )

        val input = "Dosa for 200 under food category"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(200.0, parsed.amount)
        assertEquals("Dosa", parsed.title) // "under food category" stripped!
        assertEquals("Food", parsed.matchedCategory?.name)
    }

    @Test
    fun testCategoryNameSpokenInSpeechIsStrippedFromTitle_Leading() {
        val categories = listOf(
            Category(id = 10, name = "Food", type = CategoryType.EXPENSE),
            Category(id = 11, name = "General", type = CategoryType.EXPENSE, isDefault = true)
        )

        val input = "Food dosa 200"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(200.0, parsed.amount)
        assertEquals("Dosa", parsed.title) // "Food" at start stripped!
        assertEquals("Food", parsed.matchedCategory?.name)
    }

    @Test
    fun testMultiWordCategoryNameSpokenInSpeechIsStripped() {
        val input = "Coffee 150 under food and dining"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(150.0, parsed.amount)
        assertEquals("Coffee", parsed.title) // "under food and dining" stripped!
        assertEquals("Food & Dining", parsed.matchedCategory?.name)
    }

    @Test
    fun testSnacksCategorySpokenInSpeechIsStripped() {
        val customCategories = listOf(
            Category(id = 10, name = "Food", type = CategoryType.EXPENSE),
            Category(id = 11, name = "Snacks", type = CategoryType.EXPENSE),
            Category(id = 12, name = "General", type = CategoryType.EXPENSE, isDefault = true)
        )

        val input = "Samosa 40 snacks"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, customCategories)

        assertEquals(40.0, parsed.amount)
        assertEquals("Samosa", parsed.title) // "snacks" stripped!
        assertEquals("Snacks", parsed.matchedCategory?.name)
    }

    @Test
    fun testCompoundFoodPreserved_DogFood() {
        val input = "Dog food 500"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(500.0, parsed.amount)
        assertEquals("Dog Food", parsed.title) // "Dog food" preserved as title!
    }

    @Test
    fun testOnlyCategoryNameSpoken_FallsBackToCategoryTitle() {
        val input = "Food 200"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(200.0, parsed.amount)
        assertEquals("Food & Dining", parsed.title) // Only category spoken -> title is category!
    }

    @Test
    fun testParseSpeechWithYesterdayDate() {
        val input = "Coffee 180 yesterday"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(180.0, parsed.amount)
        assertEquals("Coffee", parsed.title) // "yesterday" stripped from title!
        assertTrue(parsed.hasExplicitDateTime)

        val cal = java.util.Calendar.getInstance().apply { timeInMillis = parsed.dateEpochMillis }
        val now = java.util.Calendar.getInstance()
        assertEquals(now.get(java.util.Calendar.DAY_OF_YEAR) - 1, cal.get(java.util.Calendar.DAY_OF_YEAR))
    }

    @Test
    fun testParseSpeechWithTime() {
        val input = "Dinner 1200 at 8 pm"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(1200.0, parsed.amount)
        assertEquals("Dinner", parsed.title) // "at 8 pm" stripped from title!
        assertTrue(parsed.hasExplicitDateTime)

        val cal = java.util.Calendar.getInstance().apply { timeInMillis = parsed.dateEpochMillis }
        assertEquals(20, cal.get(java.util.Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testParseSpeechWithTimeBeforeAmount() {
        // Test that "8" in "8 pm" does not get wrongly picked as the amount
        val input = "Dinner at 8 pm 1200"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(1200.0, parsed.amount) // 1200 is the amount, not 8!
        assertEquals("Dinner", parsed.title)
        assertTrue(parsed.hasExplicitDateTime)

        val cal = java.util.Calendar.getInstance().apply { timeInMillis = parsed.dateEpochMillis }
        assertEquals(20, cal.get(java.util.Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testParseSpeechWithYesterdayAndEvening() {
        val input = "Petrol 500 yesterday at 6 pm"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(500.0, parsed.amount)
        assertEquals("Petrol", parsed.title)
        assertTrue(parsed.hasExplicitDateTime)

        val cal = java.util.Calendar.getInstance().apply { timeInMillis = parsed.dateEpochMillis }
        assertEquals(18, cal.get(java.util.Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testParseSpeechRailwayTime_ParsedAndFormattedIn12Hour() {
        val input = "Dinner 1200 at 20:00"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(1200.0, parsed.amount)
        assertEquals("Dinner", parsed.title) // "at 20:00" stripped cleanly!
        assertTrue(parsed.hasExplicitDateTime)

        val cal = java.util.Calendar.getInstance().apply { timeInMillis = parsed.dateEpochMillis }
        assertEquals(20, cal.get(java.util.Calendar.HOUR_OF_DAY))
        // Verify displayed in 12-hour format:
        assertEquals("08:00 PM", com.example.pam.util.DateUtils.formatTime(parsed.dateEpochMillis))
    }

    @Test
    fun testParseSpeechWithDottedAmPm() {
        // Speech recognition commonly transcribes "p.m." with dots
        val input = "Dinner at 8 p.m. 1200"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(1200.0, parsed.amount)
        assertEquals("Dinner", parsed.title) // "at 8 p.m." stripped cleanly!
        assertTrue(parsed.hasExplicitDateTime)

        val cal = java.util.Calendar.getInstance().apply { timeInMillis = parsed.dateEpochMillis }
        assertEquals(20, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals("08:00 PM", com.example.pam.util.DateUtils.formatTime(parsed.dateEpochMillis))
    }

    @Test
    fun testParseSpeechWithPeriodOfDayPhrase() {
        val input = "Dinner 600 at 8 in the evening"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(600.0, parsed.amount)
        assertEquals("Dinner", parsed.title) // "at 8 in the evening" stripped cleanly!
        assertTrue(parsed.hasExplicitDateTime)

        val cal = java.util.Calendar.getInstance().apply { timeInMillis = parsed.dateEpochMillis }
        assertEquals(20, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals("08:00 PM", com.example.pam.util.DateUtils.formatTime(parsed.dateEpochMillis))
    }

    @Test
    fun testParseSplitWithThreePeople_TitleDoesNotContainPeople() {
        val input = "Dinner 900 split with three people"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(900.0, parsed.amount)
        assertEquals("Dinner", parsed.title) // Title alone, NO "people" or "three"!
        assertTrue(parsed.isSplit)
        assertEquals(3, parsed.splitPeopleCount)
        assertEquals(null, parsed.splitWith)
    }

    @Test
    fun testParseSplitWithDigitsAndPerson() {
        val input = "Cab 600 split with 3 people"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(600.0, parsed.amount)
        assertEquals("Cab", parsed.title)
        assertTrue(parsed.isSplit)
        assertEquals(3, parsed.splitPeopleCount)

        val inputPersons = "Dinner 1200 split with two persons"
        val parsedPersons = NaturalLanguageTransactionParser.parse(inputPersons, accounts, categories)
        assertEquals(1200.0, parsedPersons.amount)
        assertEquals("Dinner", parsedPersons.title)
        assertTrue(parsedPersons.isSplit)
        assertEquals(2, parsedPersons.splitPeopleCount)
    }

    @Test
    fun testParseSplitWays() {
        val input = "Pizza 750 split 3 ways"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(750.0, parsed.amount)
        assertEquals("Pizza", parsed.title) // "split 3 ways" stripped
        assertTrue(parsed.isSplit)
        assertEquals(3, parsed.splitPeopleCount)
    }

    @Test
    fun testParseSplitAmongFriends() {
        val input = "Lunch 1200 split among 4 friends"
        val parsed = NaturalLanguageTransactionParser.parse(input, accounts, categories)

        assertEquals(1200.0, parsed.amount)
        assertEquals("Lunch", parsed.title) // "split among 4 friends" stripped
        assertTrue(parsed.isSplit)
        assertEquals(4, parsed.splitPeopleCount)
    }
}
