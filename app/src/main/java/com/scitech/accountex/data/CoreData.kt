package com.scitech.accountex.data

object CoreData {
    val defaults = mapOf(
        "Household" to listOf("Groceries", "Milk & Veg", "Gas", "Electricity", "Rent", "Maintenance", "Cleaning"),
        "Food" to listOf("Tea/Coffee", "Breakfast", "Lunch", "Dinner", "Snacks", "Restaurant", "Delivery"),
        "Transport" to listOf("Auto", "Bus", "Train", "Taxi/Cab", "Fuel", "Parking", "Toll", "Service"),
        "Health" to listOf("Doctor", "Medicines", "Tests", "Hospital", "Dental", "Insurance"),
        "Utilities" to listOf("Mobile Recharge", "Internet", "OTT", "Subscriptions", "Cloud Storage"),
        "Education" to listOf("Course Fees", "Exam Fees", "Books", "Stationery", "Coaching"),
        "Shopping" to listOf("Clothes", "Shoes", "Accessories", "Online Shopping", "Electronics", "Gifts"),
        "Entertainment" to listOf("Movies", "Streaming", "Games", "Outing", "Vacation"),
        "Bills" to listOf("Credit Card", "Loan EMI", "Insurance", "Tax", "Fines"),
        "Income" to listOf("Salary", "Freelance", "Business", "Interest", "Refund", "Cashback"),
        "Cash" to listOf("Withdrawal", "Deposit", "Transfer", "ATM"),
        "Personal" to listOf("Personal Expense", "Emergency", "Misc")
    )

    // Flattens all descriptions for global search
    val allDescriptions = defaults.values.flatten().sorted()

    // Gets keys as list
    val allCategories = defaults.keys.toList().sorted()
}