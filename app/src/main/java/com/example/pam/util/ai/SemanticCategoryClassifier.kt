package com.example.pam.util.ai

import com.example.pam.data.model.Category
import com.example.pam.data.model.TransactionType

/**
 * Hierarchical semantic concept representing domain taxonomy.
 * Concepts can be top-level (parent) or fine-grained (child) with explicit parent references.
 */
enum class SemanticConcept(
    val parent: SemanticConcept? = null,
    val canonicalName: String,
    val keywords: List<String>
) {
    // === FOOD & DINING HIERARCHY ===
    FOOD_PARENT(
        parent = null,
        canonicalName = "Food & Dining",
        keywords = listOf(
            "food", "dining", "meal", "meals", "eating", "restaurant", "cafe", "dhaba",
            "mess", "canteen", "bhojanalaya", "darshini", "eatclub", "swiggy", "zomato"
        )
    ),
    FOOD_SNACKS(
        parent = FOOD_PARENT,
        canonicalName = "Snacks",
        keywords = listOf(
            "snack", "snacks", "chips", "crisps", "lays", "doritos", "bingo", "kurkure",
            "samosa", "samosas", "biscuit", "biscuits", "cookies", "namkeen", "bhujia",
            "fries", "french fries", "popcorn", "pakoda", "pakoras", "puffs", "puff",
            "kachori", "chaat", "pani puri", "golgappa", "sev puri", "bhel", "bhelpuri",
            "vada pav", "dabeli", "munchies", "croissant", "nachos", "mixture"
        )
    ),
    FOOD_BEVERAGES(
        parent = FOOD_PARENT,
        canonicalName = "Drinks & Beverages",
        keywords = listOf(
            "drinks", "drink", "beverage", "beverages", "coffee", "tea", "chai", "latte",
            "cappuccino", "espresso", "americano", "mocha", "cold coffee", "boba", "bubble tea",
            "juice", "fresh juice", "shake", "milkshake", "smoothie", "lassi", "soda", "coke",
            "pepsi", "sprite", "beer", "wine", "liquor", "alcohol", "cocktail", "mocktail",
            "starbucks", "cafe coffee day", "third wave", "blue tokai", "chaayos", "chai point"
        )
    ),
    FOOD_DESSERTS(
        parent = FOOD_PARENT,
        canonicalName = "Desserts & Sweets",
        keywords = listOf(
            "dessert", "desserts", "sweet", "sweets", "ice cream", "icecream", "cake", "pastry",
            "brownie", "waffle", "waffles", "pancake", "pancakes", "donut", "doughnut", "cupcake",
            "mithai", "gulab jamun", "rasgulla", "halwa", "jalebi", "kaju katli", "chocolate",
            "chocolates", "candy", "gelato", "frozen yogurt", "naturals", "baskin robbins"
        )
    ),
    FOOD_MEALS(
        parent = FOOD_PARENT,
        canonicalName = "Meals & Dining",
        keywords = listOf(
            "dosa", "masala dosa", "ghee roast", "idli", "vada", "biryani", "thali", "pizza",
            "burger", "roti", "naan", "kulcha", "paratha", "chapati", "curry", "paneer",
            "chicken", "mutton", "fish", "prawns", "kebab", "tikka", "dal", "rice", "fried rice",
            "noodles", "chowmein", "manchurian", "pasta", "sandwich", "shawarma", "roll",
            "kathi roll", "wrap", "momo", "momos", "dim sum", "chole bhature", "poori", "puri",
            "breakfast", "lunch", "dinner", "brunch", "dominos", "pizza hut", "kfc", "mcdonalds",
            "subway", "burger king", "taco", "tacos", "burrito"
        )
    ),

    // === GROCERIES HIERARCHY ===
    GROCERIES_PARENT(
        parent = null,
        canonicalName = "Groceries",
        keywords = listOf(
            "groceries", "grocery", "supermarket", "provision", "provisions", "ration", "mart",
            "dmart", "blinkit", "zepto", "instamart", "bigbasket", "nature's basket", "spencer"
        )
    ),
    GROCERIES_STAPLES(
        parent = GROCERIES_PARENT,
        canonicalName = "Vegetables & Daily Needs",
        keywords = listOf(
            "vegetables", "veggies", "fruits", "milk", "curd", "yogurt", "bread", "eggs", "butter",
            "cheese", "atta", "wheat", "flour", "rice raw", "dal raw", "oil", "cooking oil",
            "ghee", "sugar", "salt", "spices", "masala raw", "onions", "potatoes", "tomatoes"
        )
    ),

    // === TRANSPORTATION HIERARCHY ===
    TRANSPORT_PARENT(
        parent = null,
        canonicalName = "Transportation",
        keywords = listOf("transportation", "transport", "travel", "commute", "traveling")
    ),
    TRANSPORT_FUEL(
        parent = TRANSPORT_PARENT,
        canonicalName = "Fuel & Petrol",
        keywords = listOf(
            "petrol", "diesel", "fuel", "gas", "cng", "shell", "hp petrol", "indian oil",
            "bharat petroleum", "speed petrol", "power petrol", "petrol pump"
        )
    ),
    TRANSPORT_CABS(
        parent = TRANSPORT_PARENT,
        canonicalName = "Cabs & Taxi",
        keywords = listOf(
            "uber", "ola", "rapido", "auto", "rickshaw", "cab", "taxi", "ride", "drive", "inDrive"
        )
    ),
    TRANSPORT_TRANSIT(
        parent = TRANSPORT_PARENT,
        canonicalName = "Public Transit & Tolls",
        keywords = listOf(
            "metro", "subway metro", "bus", "train", "toll", "fastag", "parking", "ticket transit"
        )
    ),

    // === HEALTHCARE HIERARCHY ===
    HEALTH_PARENT(
        parent = null,
        canonicalName = "Healthcare",
        keywords = listOf("healthcare", "health", "medical", "wellness")
    ),
    HEALTH_PHARMACY(
        parent = HEALTH_PARENT,
        canonicalName = "Pharmacy & Medicines",
        keywords = listOf(
            "medicine", "medicines", "pharmacy", "tablet", "tablets", "syrup", "capsule",
            "capsules", "pills", "ointment", "bandage", "crocin", "dolo", "paracetamol",
            "apollo pharmacy", "apollo", "medplus", "1mg", "pharmeasy", "medical store", "drugs"
        )
    ),
    HEALTH_DOCTOR(
        parent = HEALTH_PARENT,
        canonicalName = "Doctor & Hospital",
        keywords = listOf(
            "doctor", "hospital", "clinic", "consultation", "dental", "dentist", "lab",
            "blood test", "scan", "checkup", "xray", "physician", "surgeon"
        )
    ),
    HEALTH_FITNESS(
        parent = HEALTH_PARENT,
        canonicalName = "Fitness & Gym",
        keywords = listOf(
            "gym", "fitness", "workout", "trainer", "membership gym", "yoga", "protein",
            "whey", "creatine", "supplement", "cult.fit"
        )
    ),

    // === SHOPPING HIERARCHY ===
    SHOPPING_PARENT(
        parent = null,
        canonicalName = "Shopping",
        keywords = listOf("shopping", "retail", "store", "purchase", "purchases", "mall")
    ),
    SHOPPING_APPAREL(
        parent = SHOPPING_PARENT,
        canonicalName = "Clothing & Apparel",
        keywords = listOf(
            "clothes", "clothing", "apparel", "dress", "shirt", "tshirt", "jeans", "pants",
            "trousers", "shoes", "sneakers", "sandals", "fashion", "zara", "h&m", "uniqlo",
            "myntra", "ajio", "decathlon", "shoppers stop", "lifestyle", "westside"
        )
    ),
    SHOPPING_ELECTRONICS(
        parent = SHOPPING_PARENT,
        canonicalName = "Electronics & Gadgets",
        keywords = listOf(
            "electronics", "gadget", "gadgets", "phone", "mobile", "laptop", "headphones",
            "earbuds", "earphones", "charger", "cable", "tech", "croma", "apple", "samsung",
            "ipad", "tablet electronics"
        )
    ),
    SHOPPING_HOME(
        parent = SHOPPING_PARENT,
        canonicalName = "Home & Furniture",
        keywords = listOf(
            "furniture", "home decor", "decor", "kitchen", "appliance", "appliances",
            "utensils", "curtains", "bedsheet", "ikea", "urban ladder"
        )
    ),

    // === ENTERTAINMENT HIERARCHY ===
    ENTERTAINMENT_PARENT(
        parent = null,
        canonicalName = "Entertainment",
        keywords = listOf("entertainment", "fun", "leisure", "recreation", "outing")
    ),
    ENTERTAINMENT_MOVIES(
        parent = ENTERTAINMENT_PARENT,
        canonicalName = "Movies & Cinema",
        keywords = listOf(
            "movie", "movies", "cinema", "theatre", "theater", "film", "pvr", "inox",
            "cinepolis", "bookmyshow", "tickets movie"
        )
    ),
    ENTERTAINMENT_STREAMING(
        parent = ENTERTAINMENT_PARENT,
        canonicalName = "Subscriptions & OTT",
        keywords = listOf(
            "netflix", "spotify", "prime", "prime video", "hotstar", "youtube premium",
            "disney", "subscription", "membership ott", "apple music"
        )
    ),

    // === BILLS & UTILITIES HIERARCHY ===
    BILLS_PARENT(
        parent = null,
        canonicalName = "Bills & Utilities",
        keywords = listOf("bills", "utilities", "utility", "bill")
    ),
    BILLS_RECHARGE(
        parent = BILLS_PARENT,
        canonicalName = "Mobile Recharge",
        keywords = listOf(
            "recharge", "mobile recharge", "phone recharge", "airtel", "jio", "vi", "prepaid", "postpaid"
        )
    ),
    BILLS_UTILITIES(
        parent = BILLS_PARENT,
        canonicalName = "Electricity & Water",
        keywords = listOf(
            "electricity", "power bill", "water bill", "gas bill", "bescom", "wifi", "broadband", "fiber"
        )
    ),

    // === EDUCATION & LEARNING ===
    EDUCATION_PARENT(
        parent = null,
        canonicalName = "Education",
        keywords = listOf(
            "education", "course", "udemy", "coursera", "school", "college", "tuition",
            "fees", "books", "exam", "training", "degree", "coaching"
        )
    ),

    // === TRAVEL & VACATIONS ===
    TRAVEL_PARENT(
        parent = null,
        canonicalName = "Travel",
        keywords = listOf(
            "travel", "trip", "vacation", "holiday", "tour", "hotel", "resort", "airbnb",
            "flight", "flights", "plane", "irctc", "airline", "airlines", "indigo", "air india",
            "makemytrip", "goibibo"
        )
    ),

    // === INCOME & SALARY HIERARCHY ===
    INCOME_SALARY(
        parent = null,
        canonicalName = "Salary",
        keywords = listOf("salary", "income", "earnings", "paycheck", "stipend", "bonus")
    ),
    INCOME_FREELANCE(
        parent = null,
        canonicalName = "Freelance / Consulting",
        keywords = listOf("freelance", "freelancing", "consulting", "client payment", "upwork", "fiverr", "contract")
    ),
    INCOME_RENTAL(
        parent = null,
        canonicalName = "Rental Income",
        keywords = listOf("rent received", "rental income", "tenant", "rent payment received")
    ),
    INCOME_REWARDS(
        parent = null,
        canonicalName = "Cashback / Rewards",
        keywords = listOf("cashback", "reward", "rewards", "google pay cashback", "cred reward", "coupon")
    ),
    INCOME_INVESTMENT(
        parent = null,
        canonicalName = "Investments",
        keywords = listOf("investments", "investment", "stocks", "mutual funds", "crypto", "dividend", "interest", "zerodha", "groww")
    ),

    // === GIFTS & DONATIONS ===
    GIFTS_PARENT(
        parent = null,
        canonicalName = "Gifts & Donations",
        keywords = listOf("gift", "gifts", "present", "donation", "charity", "shagun", "offering")
    );

    /**
     * Checks if this concept is a child (direct or indirect) of [other].
     */
    fun isChildOf(other: SemanticConcept): Boolean {
        var curr = this.parent
        while (curr != null) {
            if (curr == other) return true
            curr = curr.parent
        }
        return false
    }
}

object SemanticCategoryClassifier {

    /**
     * Matches user categories to semantic concepts based on name similarity.
     * Prefers child concepts when category name matches specific child terms.
     */
    private fun findMatchingConceptForUserCategory(category: Category): SemanticConcept? {
        val catNameLower = category.name.lowercase().trim()

        // 1. Exact match with canonical names
        SemanticConcept.entries.firstOrNull { it.canonicalName.equals(catNameLower, ignoreCase = true) }?.let {
            return it
        }

        val catTokens = catNameLower.split("&", "/", " ", "-").map { it.trim() }.filter { it.length >= 3 }

        // 2. Token match: Check child concepts first for specialized token match
        for (concept in SemanticConcept.entries.filter { it.parent != null }) {
            val conceptTokens = concept.canonicalName.lowercase().split("&", "/", " ", "-").map { it.trim() }.filter { it.length >= 3 }
            if (catTokens.any { ct -> conceptTokens.any { it == ct || it.contains(ct) || ct.contains(it) } }) {
                return concept
            }
        }

        // Check parent concepts for token match
        for (concept in SemanticConcept.entries.filter { it.parent == null }) {
            val conceptTokens = concept.canonicalName.lowercase().split("&", "/", " ", "-").map { it.trim() }.filter { it.length >= 3 }
            if (catTokens.any { ct -> conceptTokens.any { it == ct || it.contains(ct) || ct.contains(it) } }) {
                return concept
            }
        }

        // 3. Keyword match: Check child concepts first, then parents
        for (concept in SemanticConcept.entries.filter { it.parent != null }) {
            if (concept.keywords.any { kw -> catNameLower == kw || catTokens.contains(kw) }) {
                return concept
            }
        }
        for (concept in SemanticConcept.entries.filter { it.parent == null }) {
            if (concept.keywords.any { kw -> catNameLower == kw || catTokens.contains(kw) }) {
                return concept
            }
        }

        return null
    }

    /**
     * Scores a semantic concept against an input text query.
     * Returns a score >= 0.0, where higher indicates stronger semantic affinity.
     */
    private fun scoreConceptAgainstInput(concept: SemanticConcept, lowerInput: String): Float {
        var score = 0f
        val words = lowerInput.split(Regex("[\\s,;:.\\-_]+")).filter { it.isNotBlank() }

        for (kw in concept.keywords) {
            val kwLower = kw.lowercase()
            // Exact phrase match
            if (lowerInput.contains(Regex("\\b${Regex.escape(kwLower)}\\b"))) {
                // Child concepts are more specific; reward them slightly higher
                val specificityBonus = if (concept.parent != null) 1.5f else 1.0f
                val wordCount = kwLower.split(" ").size
                val phraseBonus = 1.0f + (wordCount - 1) * 0.5f
                score = maxOf(score, 2.0f * specificityBonus * phraseBonus)
            } else {
                // Subword / prefix matching for inflections (e.g. "dosas" -> "dosa", "chips" -> "chip")
                for (word in words) {
                    if (word.length >= 4 && kwLower.length >= 4) {
                        if (word.startsWith(kwLower) || kwLower.startsWith(word)) {
                            score = maxOf(score, 1.2f * if (concept.parent != null) 1.3f else 1.0f)
                        }
                    }
                }
            }
        }
        return score
    }

    /**
     * Classifies a user input string into the most appropriate category among [availableCategories].
     *
     * Core Hierarchical / Specificity Logic:
     * - If user input maps to a specific child concept (e.g., "dosa" -> FOOD_MEALS, "samosa" -> FOOD_SNACKS):
     *   - If the user has a category specifically representing that child concept (e.g., "Snacks"),
     *     that child category is preferred over the general parent category ("Food").
     *   - If the user does NOT have that child category, it seamlessly falls back to the parent
     *     category ("Food").
     */
    fun classify(
        query: String,
        availableCategories: List<Category>,
        transactionType: TransactionType = TransactionType.EXPENSE
    ): Category? {
        val applicableCategories = availableCategories.filter { it.isApplicableFor(transactionType) }
        if (applicableCategories.isEmpty()) return null

        val lowerInput = query.lowercase().trim()

        // 1. Direct Category Name Match (User explicitly spoke the category name)
        val directMatches = applicableCategories.filter { cat ->
            val catLower = cat.name.lowercase().trim()
            catLower.length >= 3 && lowerInput.contains(Regex("\\b${Regex.escape(catLower)}\\b"))
        }
        if (directMatches.size == 1) {
            return directMatches.first()
        } else if (directMatches.size > 1) {
            // If one is child and one is parent, prefer child; otherwise prefer longer name
            return directMatches.maxWithOrNull(
                compareBy<Category> { cat ->
                    val concept = findMatchingConceptForUserCategory(cat)
                    if (concept?.parent != null) 2 else 1
                }.thenBy { it.name.length }
            )
        }

        // 2. Score all semantic concepts against the input query
        val scoredConcepts = mutableListOf<Pair<SemanticConcept, Float>>()
        for (concept in SemanticConcept.entries) {
            val score = scoreConceptAgainstInput(concept, lowerInput)
            if (score > 0f) {
                scoredConcepts.add(concept to score)
            }
        }

        if (scoredConcepts.isEmpty()) {
            // Fallback to default or General
            return applicableCategories.firstOrNull { it.isDefault }
                ?: applicableCategories.firstOrNull { it.name.equals("General", ignoreCase = true) }
                ?: applicableCategories.firstOrNull()
        }

        // Map each applicable user category to its best matching semantic concept
        val userCategoryConceptMap = applicableCategories.associateWith { cat ->
            findMatchingConceptForUserCategory(cat)
        }

        // 3. Hierarchical Selection between Parent and Child sets
        // Sort scored concepts: child concepts with higher scores come first
        val sortedScoredConcepts = scoredConcepts.sortedWith(
            compareByDescending<Pair<SemanticConcept, Float>> { it.second }
                .thenByDescending { it.first.parent != null } // Prefer child concepts on tie
        )

        for ((concept, _) in sortedScoredConcepts) {
            // A) Check if any user category directly matches this specific concept
            val directCategoryMatch = applicableCategories.firstOrNull { cat ->
                val mappedConcept = userCategoryConceptMap[cat]
                mappedConcept == concept
            }
            if (directCategoryMatch != null) {
                return directCategoryMatch
            }

            // B) If this was a child concept and user didn't have a child category, check parent concept!
            var parentConcept = concept.parent
            while (parentConcept != null) {
                val parentCategoryMatch = applicableCategories.firstOrNull { cat ->
                    val mappedConcept = userCategoryConceptMap[cat]
                    mappedConcept == parentConcept
                }
                if (parentCategoryMatch != null) {
                    return parentCategoryMatch
                }
                parentConcept = parentConcept.parent
            }
        }

        // 4. Fallback category
        return applicableCategories.firstOrNull { it.isDefault }
            ?: applicableCategories.firstOrNull { it.name.equals("General", ignoreCase = true) }
            ?: applicableCategories.firstOrNull()
    }
}
