package com.example.pam

import com.example.pam.data.model.Category
import com.example.pam.data.model.CategoryType
import com.example.pam.data.model.TransactionType
import com.example.pam.util.ai.SemanticCategoryClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SemanticCategoryClassifierTest {

    @Test
    fun testParentAndChildPresent_SnackMapsToChildSnacks() {
        // User has both parent "Food" and child "Snacks"
        val categories = listOf(
            Category(id = 1, name = "Food", type = CategoryType.EXPENSE),
            Category(id = 2, name = "Snacks", type = CategoryType.EXPENSE),
            Category(id = 3, name = "General", type = CategoryType.BOTH, isDefault = true)
        )

        // Samosa is a snack -> should prefer child "Snacks" over parent "Food"
        val samosaCat = SemanticCategoryClassifier.classify("Samosa 40", categories)
        assertNotNull(samosaCat)
        assertEquals("Snacks", samosaCat?.name)

        // Chips is a snack -> should prefer "Snacks"
        val chipsCat = SemanticCategoryClassifier.classify("Lays chips 20", categories)
        assertNotNull(chipsCat)
        assertEquals("Snacks", chipsCat?.name)
    }

    @Test
    fun testParentAndChildPresent_MealMapsToParentFood() {
        // User has both parent "Food" and child "Snacks"
        val categories = listOf(
            Category(id = 1, name = "Food", type = CategoryType.EXPENSE),
            Category(id = 2, name = "Snacks", type = CategoryType.EXPENSE),
            Category(id = 3, name = "General", type = CategoryType.BOTH, isDefault = true)
        )

        // Dosa is a full meal -> should match "Food", NOT "Snacks"
        val dosaCat = SemanticCategoryClassifier.classify("Dosa for 200", categories)
        assertNotNull(dosaCat)
        assertEquals("Food", dosaCat?.name)

        // Biryani is a meal -> should match "Food", NOT "Snacks"
        val biryaniCat = SemanticCategoryClassifier.classify("Chicken biryani 350", categories)
        assertNotNull(biryaniCat)
        assertEquals("Food", biryaniCat?.name)
    }

    @Test
    fun testOnlyParentPresent_SnackFallsBackToParentFood() {
        // User only has "Food", no "Snacks" category
        val categories = listOf(
            Category(id = 1, name = "Food", type = CategoryType.EXPENSE),
            Category(id = 2, name = "General", type = CategoryType.BOTH, isDefault = true)
        )

        // Samosa is a snack, but since no "Snacks" category exists, it seamlessly falls back to parent "Food"
        val samosaCat = SemanticCategoryClassifier.classify("Samosa 40", categories)
        assertNotNull(samosaCat)
        assertEquals("Food", samosaCat?.name)
    }

    @Test
    fun testTransportationAndFuel_Hierarchy() {
        // User has parent "Transportation" and child "Fuel"
        val categories = listOf(
            Category(id = 1, name = "Transportation", type = CategoryType.EXPENSE),
            Category(id = 2, name = "Fuel", type = CategoryType.EXPENSE),
            Category(id = 3, name = "General", type = CategoryType.BOTH, isDefault = true)
        )

        // Petrol -> matches child "Fuel"
        val petrolCat = SemanticCategoryClassifier.classify("Petrol 500", categories)
        assertEquals("Fuel", petrolCat?.name)

        // Shell fuel -> matches child "Fuel"
        val shellCat = SemanticCategoryClassifier.classify("Shell 1200", categories)
        assertEquals("Fuel", shellCat?.name)

        // Uber cab -> matches parent "Transportation" (user doesn't have a specific Cabs category)
        val uberCat = SemanticCategoryClassifier.classify("Uber to office 250", categories)
        assertEquals("Transportation", uberCat?.name)
    }

    @Test
    fun testOnlyTransportationPresent_FuelFallsBackToTransportation() {
        // User only has "Transportation", no "Fuel"
        val categories = listOf(
            Category(id = 1, name = "Transportation", type = CategoryType.EXPENSE),
            Category(id = 2, name = "General", type = CategoryType.BOTH, isDefault = true)
        )

        val petrolCat = SemanticCategoryClassifier.classify("Petrol 500", categories)
        assertEquals("Transportation", petrolCat?.name)
    }

    @Test
    fun testMultiSiblingHierarchy_Food_Snacks_Drinks() {
        val categories = listOf(
            Category(id = 1, name = "Food & Dining", type = CategoryType.EXPENSE),
            Category(id = 2, name = "Snacks", type = CategoryType.EXPENSE),
            Category(id = 3, name = "Drinks", type = CategoryType.EXPENSE),
            Category(id = 4, name = "General", type = CategoryType.BOTH, isDefault = true)
        )

        // Cold coffee -> "Drinks"
        val coffeeCat = SemanticCategoryClassifier.classify("Cold coffee at Starbucks 220", categories)
        assertEquals("Drinks", coffeeCat?.name)

        // Chai -> "Drinks"
        val chaiCat = SemanticCategoryClassifier.classify("Chai point 50", categories)
        assertEquals("Drinks", chaiCat?.name)

        // Samosa -> "Snacks"
        val samosaCat = SemanticCategoryClassifier.classify("Samosa and tea 40", categories)
        assertNotNull(samosaCat)
        // Tea is drinks, Samosa is snacks - child concept specificity
        assertEquals("Snacks", samosaCat?.name)

        // Pizza -> "Food & Dining"
        val pizzaCat = SemanticCategoryClassifier.classify("Pizza hut 599", categories)
        assertEquals("Food & Dining", pizzaCat?.name)
    }

    @Test
    fun testHealthcareHierarchy_PharmacyVsDoctor() {
        val categories = listOf(
            Category(id = 1, name = "Healthcare", type = CategoryType.EXPENSE),
            Category(id = 2, name = "Medicines", type = CategoryType.EXPENSE),
            Category(id = 3, name = "General", type = CategoryType.BOTH, isDefault = true)
        )

        // Crocin / tablets -> "Medicines"
        val doloCat = SemanticCategoryClassifier.classify("Dolo 650 30", categories)
        assertEquals("Medicines", doloCat?.name)

        // Apollo pharmacy -> "Medicines"
        val apolloCat = SemanticCategoryClassifier.classify("Apollo pharmacy 420", categories)
        assertEquals("Medicines", apolloCat?.name)

        // Hospital / dentist consultation -> parent "Healthcare"
        val dentistCat = SemanticCategoryClassifier.classify("Dentist checkup 800", categories)
        assertEquals("Healthcare", dentistCat?.name)
    }
}

